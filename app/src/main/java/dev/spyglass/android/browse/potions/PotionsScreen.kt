package dev.spyglass.android.browse.potions

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.PotionEntity
import dev.spyglass.android.data.repository.GameDataRepository
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

    val sortOptions = remember {
        listOf(
            SortOption("Name A\u2192Z", "name"),
            SortOption("Duration \u2193", "duration"),
            SortOption("Amplifier \u2193", "amplifier"),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query, onValueChange = vm::setQuery,
                placeholder = { Text("Search potions\u2026", color = MaterialTheme.colorScheme.secondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
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
                    onClick = { vm.setCategory(c) },
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
                    title = "Potions",
                    description = "Brewing paths and effects for every potion",
                    stat = "${potions.size} potions",
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
                            IconButton(onClick = { vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
            }
            items(potions, key = { it.id }) { p ->
                val catColor = when (p.category) {
                    "negative" -> NetherRed
                    "positive" -> Emerald
                    else       -> MaterialTheme.colorScheme.secondary
                }
                val isExpanded = p.id in expandedIds
                Column {
                    BrowseListItem(
                        headline    = p.name,
                        supporting  = "",
                        supportingMaxLines = 1,
                        leadingIcon = PotionTextures.get(p.id) ?: PixelIcons.Potion,
                        modifier    = Modifier.clickable { vm.toggleExpanded(p.id) },
                        trailing    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
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
                                IconButton(onClick = { vm.toggleFavorite(p.id, p.name) }, modifier = Modifier.size(32.dp)) {
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
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        PotionDetailCard(p, onItemTap)
                    }
                }
            }
            if (potions.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No potions found",
                    subtitle = "Try a different search term",
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PotionDetailCard(potion: PotionEntity, onItemTap: (String) -> Unit) {
    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        MinecraftIdRow(potion.id)

        // Effect
        if (potion.effect.isNotBlank() && potion.effect != "none") {
            Text(potion.effect, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Stats
        if (potion.durationSeconds > 0) {
            val mins = potion.durationSeconds / 60
            val secs = potion.durationSeconds % 60
            StatRow("Duration", "${mins}:%02d".format(secs))
        }
        if (potion.amplifier > 0) {
            StatRow("Level", "${potion.amplifier + 1}")
        }
        StatRow(stringResource(R.string.category), potion.category.replaceFirstChar { it.uppercase() })

        // Brewing path with clickable ingredients
        if (potion.ingredientPath.isNotBlank()) {
            SpyglassDivider()
            Text("Brewing Path", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
    }
}
