package dev.spyglass.android.browse.potions

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.VersionAvailability
import dev.spyglass.android.core.VersionFilterState
import dev.spyglass.android.core.applyVersionFilter
import dev.spyglass.android.core.checkAvailability
import dev.spyglass.android.core.toTagMap
import dev.spyglass.android.core.versionFilterFrom
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.core.ui.SpyglassSearchBar
import dev.spyglass.android.core.ui.rememberHapticConfirm
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.PotionEntity
import dev.spyglass.android.data.db.entities.VersionTagEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val POTION_CATEGORIES = listOf("all", "positive", "negative", "special")

// Map of known brewing ingredient names to item IDs for navigation
private val BREWING_ITEM_IDS = mapOf(
    "nether_wart" to "nether_wart",
    "nether wart" to "nether_wart",
    "sugar" to "sugar",
    "rabbit_foot" to "rabbit_foot",
    "rabbit foot" to "rabbit_foot",
    "glistering_melon_slice" to "glistering_melon_slice",
    "glistering melon slice" to "glistering_melon_slice",
    "spider_eye" to "spider_eye",
    "spider eye" to "spider_eye",
    "fermented_spider_eye" to "fermented_spider_eye",
    "fermented spider eye" to "fermented_spider_eye",
    "blaze_powder" to "blaze_powder",
    "blaze powder" to "blaze_powder",
    "magma_cream" to "magma_cream",
    "magma cream" to "magma_cream",
    "ghast_tear" to "ghast_tear",
    "ghast tear" to "ghast_tear",
    "golden_carrot" to "golden_carrot",
    "golden carrot" to "golden_carrot",
    "pufferfish" to "pufferfish",
    "phantom_membrane" to "phantom_membrane",
    "phantom membrane" to "phantom_membrane",
    "turtle_helmet" to "turtle_helmet",
    "turtle helmet" to "turtle_helmet",
    "redstone" to "redstone",
    "glowstone_dust" to "glowstone_dust",
    "glowstone dust" to "glowstone_dust",
    "gunpowder" to "gunpowder",
    "dragon_breath" to "dragon_breath",
    "dragon breath" to "dragon_breath",
    "breeze_rod" to "breeze_rod",
    "breeze rod" to "breeze_rod",
)

private val EFFECT_DESCRIPTIONS = mapOf(
    "Speed" to "+20% movement speed per level",
    "Slowness" to "-15% movement speed per level",
    "Haste" to "+20% mining speed per level",
    "Mining Fatigue" to "-70% mining speed (level I)",
    "Strength" to "+3 attack damage per level",
    "Instant Health" to "Restores 4 HP per level",
    "Instant Damage" to "Deals 6 HP damage per level",
    "Jump Boost" to "+0.5 blocks jump height per level",
    "Regeneration" to "1 HP every 2.5s/1.25s per level",
    "Resistance" to "-20% damage taken per level",
    "Fire Resistance" to "Immune to fire and lava damage",
    "Water Breathing" to "Breathe underwater, improved visibility",
    "Invisibility" to "Invisible to mobs (no armor); reduced detection range",
    "Night Vision" to "See in the dark and underwater clearly",
    "Weakness" to "-4 attack damage",
    "Poison" to "1 HP every 2.5s/1.25s (won't kill)",
    "Slow Falling" to "Fall slowly, no fall damage",
    "Turtle Master" to "Slowness IV + Resistance III",
    "Luck" to "+1 luck attribute for loot tables",
    "Wind Charged" to "Emit a wind burst on death",
    "Weaving" to "Spawn cobweb blocks on death",
    "Oozing" to "Spawn slimes on death",
    "Infested" to "Spawn silverfish when hurt",
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PotionsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    private val _sortKey = MutableStateFlow("name")
    val sortKey: StateFlow<String> = _sortKey.asStateFlow()
    fun setSortKey(k: String) { _sortKey.value = k }

    val versionFilter: StateFlow<VersionFilterState> = versionFilterFrom(app.dataStore)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VersionFilterState())
    val versionTags: StateFlow<Map<String, VersionTagEntity>> = repo.allVersionTags()
        .map { it.toTagMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val translations: StateFlow<Map<String, Map<String, String>>> =
        translationMapFlow(app.dataStore, repo, "potion")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val potions: StateFlow<List<PotionEntity>> = combine(_query.debounce(200), _category, _sortKey) { q, cat, sort ->
        repo.searchPotions(q).map { list ->
            val filtered = if (cat == "all") list else list.filter { it.category == cat }
            when (sort) {
                "duration" -> filtered.sortedByDescending { it.durationSeconds }
                "amplifier" -> filtered.sortedByDescending { it.amplifier }
                else -> filtered
            }
        }
    }.flatMapLatest { it }
     .applyVersionFilter(versionFilter, versionTags, "potion") { it.id }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoritePotions: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("potion")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "potion", displayName = displayName))
        }
    }
}

/** Parse an ingredient path like "nether_wart → sugar → redstone" into clickable tokens. */
private fun parseIngredientPath(path: String): List<Pair<String, String?>> {
    if (path.isBlank()) return emptyList()
    return path.split("\u2192", "->", "→").map { segment ->
        val trimmed = segment.trim()
        val itemId = BREWING_ITEM_IDS[trimmed.lowercase()] ?: BREWING_ITEM_IDS[trimmed]
        trimmed to itemId
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PotionsScreen(
    onItemTap: (String) -> Unit = {},
    vm: PotionsViewModel = viewModel(),
) {
    val query      by vm.query.collectAsStateWithLifecycle()
    val category   by vm.category.collectAsStateWithLifecycle()
    val potions    by vm.potions.collectAsStateWithLifecycle()
    val expandedIds by vm.expandedIds.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val favoritePotions by vm.favoritePotions.collectAsStateWithLifecycle()
    val sortKey    by vm.sortKey.collectAsStateWithLifecycle()
    val vFilter     by vm.versionFilter.collectAsStateWithLifecycle()
    val vTags       by vm.versionTags.collectAsStateWithLifecycle()
    val txMap       by vm.translations.collectAsStateWithLifecycle()
    val hapticConfirm = rememberHapticConfirm()
    val hapticClick = rememberHapticClick()

    val sortOptions = listOf(
        SortOption(stringResource(R.string.potions_sort_name), "name"),
        SortOption(stringResource(R.string.potions_sort_duration), "duration"),
        SortOption(stringResource(R.string.potions_sort_amplifier), "amplifier"),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpyglassSearchBar(
                query = query, onQueryChange = vm::setQuery,
                category = "potions", placeholder = stringResource(R.string.potions_search_placeholder),
                modifier = Modifier.weight(1f),
            )
            SortButton(options = sortOptions, selectedKey = sortKey, onSelect = vm::setSortKey)
        }

        // Category filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(POTION_CATEGORIES, key = { it }) { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { hapticClick(); vm.setCategory(c) },
                    label = {
                        Text(
                            if (c == "all") stringResource(R.string.all) else c.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Potion,
                    title = stringResource(R.string.potions_title),
                    description = stringResource(R.string.potions_description),
                    stat = stringResource(R.string.potions_stat, potions.size),
                )
            }
            if (favoritePotions.isNotEmpty()) {
                item(key = "fav_header") {
                    Text(stringResource(R.string.favorites), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(favoritePotions, key = { "fav_${it.id}" }) { fav ->
                    BrowseListItem(
                        headline    = fav.displayName,
                        supporting  = "",
                        supportingMaxLines = 1,
                        leadingIcon = PotionTextures.get(fav.id) ?: PixelIcons.Potion,
                        trailing    = {
                            IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
            }
            items(potions, key = { it.id }) { p ->
                val tag = vTags["potion:${p.id}"]
                val availability = checkAvailability(tag, vFilter)
                val vAlpha = when (availability) {
                    VersionAvailability.NOT_YET_ADDED -> 0.5f
                    VersionAvailability.REMOVED, VersionAvailability.WRONG_EDITION -> 0.4f
                    else -> 1f
                }
                val addedIn = tag?.let { if (vFilter.edition == "java") it.addedInJava else it.addedInBedrock } ?: ""
                val catColor = when (p.category) {
                    "negative" -> NetherRed
                    "positive" -> Emerald
                    else       -> MaterialTheme.colorScheme.secondary
                }
                val isExpanded = p.id in expandedIds
                Column(modifier = Modifier.alpha(vAlpha)) {
                    BrowseListItem(
                        headline    = txMap[p.id]?.get("name") ?: p.name,
                        supporting  = "",
                        supportingMaxLines = 1,
                        leadingIcon = PotionTextures.get(p.id) ?: PixelIcons.Potion,
                        modifier    = Modifier.clickable { vm.toggleExpanded(p.id) },
                        trailing    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    if (addedIn.isNotBlank() && availability != VersionAvailability.AVAILABLE) {
                                        VersionBadge(addedIn)
                                        Spacer(Modifier.height(2.dp))
                                    }
                                    if (p.durationSeconds > 0) {
                                        val mins = p.durationSeconds / 60
                                        val secs = p.durationSeconds % 60
                                        Text("${mins}:%02d".format(secs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    CategoryBadge(label = p.category, color = catColor)
                                }
                                Spacer(Modifier.width(4.dp))
                                val isFav = p.id in favoriteIds
                                IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(p.id, p.name) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = stringResource(R.string.favorite),
                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        },
                    )
                    val reduceMotion = LocalReduceAnimations.current
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = if (reduceMotion) expandVertically(snap()) else expandVertically(),
                        exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(),
                    ) {
                        PotionDetailCard(p, onItemTap, tag, vFilter, txMap)
                    }
                }
            }
            if (potions.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = stringResource(R.string.potions_no_results_title),
                    subtitle = stringResource(R.string.potions_no_results_subtitle),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PotionDetailCard(potion: PotionEntity, onItemTap: (String) -> Unit, tag: VersionTagEntity? = null, vFilter: VersionFilterState = VersionFilterState(), txMap: Map<String, Map<String, String>> = emptyMap()) {
    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        MinecraftIdRow(potion.id)

        if (tag != null) {
            VersionEditionSection(tag, vFilter)
        }

        // Effect
        if (potion.effect.isNotBlank() && potion.effect != "none") {
            Text(txMap[potion.id]?.get("effect") ?: potion.effect, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val desc = EFFECT_DESCRIPTIONS[potion.effect]
            if (desc != null) {
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }

        // Stats
        if (potion.durationSeconds > 0) {
            val mins = potion.durationSeconds / 60
            val secs = potion.durationSeconds % 60
            StatRow(stringResource(R.string.potions_duration), "${mins}:%02d".format(secs))
        }
        if (potion.amplifier > 0) {
            StatRow(stringResource(R.string.potions_level), "${potion.amplifier + 1}")
        }
        StatRow(stringResource(R.string.category), potion.category.replaceFirstChar { it.uppercase() })

        // Brewing path with clickable ingredients
        if (potion.ingredientPath.isNotBlank()) {
            SpyglassDivider()
            Text(stringResource(R.string.potions_brewing_path), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            val steps = parseIngredientPath(potion.ingredientPath)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                steps.forEachIndexed { i, (name, itemId) ->
                    if (i > 0) {
                        Text("\u2192", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 2.dp).align(Alignment.CenterVertically))
                    }
                    if (itemId != null) {
                        AssistChip(
                            onClick = { onItemTap(itemId) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val tex = ItemTextures.get(itemId)
                                    if (tex != null) {
                                        SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(name, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = EnderPurple,
                                containerColor = EnderPurple.copy(alpha = 0.12f),
                            ),
                            border = null,
                        )
                    } else {
                        // Non-linkable step (e.g. "base", "awkward potion")
                        CategoryBadge(label = name, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        SpyglassDivider()
        ReportProblemRow(entityType = "Potion", entityName = potion.name, entityId = potion.id)
        ReportTranslationRow(entityType = "Potion", entityName = potion.name, entityId = potion.id)
    }
}
