package dev.spyglass.android.browse.biomes

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.VersionAvailability
import dev.spyglass.android.core.VersionFilterState
import dev.spyglass.android.core.applyVersionFilter
import dev.spyglass.android.core.checkAvailability
import dev.spyglass.android.core.toTagMap
import dev.spyglass.android.core.versionFilterFrom
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.BiomeResourceMap
import dev.spyglass.android.data.db.entities.BiomeEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.VersionTagEntity
import dev.spyglass.android.data.repository.GameDataRepository
import androidx.compose.ui.platform.LocalContext
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Constants ───────────────────────────────────────────────────────────────

// Mob IDs known to exist in the mobs table
private val KNOWN_MOB_IDS = setOf(
    "zombie", "skeleton", "creeper", "spider", "cave_spider", "enderman", "witch",
    "slime", "ghast", "zombie_pigman", "blaze", "magma_cube", "wither_skeleton",
    "endermite", "silverfish", "guardian", "elder_guardian", "shulker", "evoker",
    "vindicator", "pillager", "ravager", "vex", "husk", "stray", "drowned",
    "phantom", "piglin", "hoglin", "zoglin", "piglin_brute", "strider",
    "cow", "pig", "sheep", "chicken", "rabbit", "fox", "wolf", "ocelot", "cat",
    "parrot", "horse", "donkey", "mule", "llama", "polar_bear", "panda", "bee",
    "goat", "axolotl", "glow_squid", "squid", "dolphin", "pufferfish", "turtle", "tropical_fish",
    "mooshroom", "frog", "allay", "sniffer", "camel", "armadillo", "breeze",
    "bogged", "creaking", "warden", "ender_dragon", "wither", "villager",
    "wandering_trader", "bat", "iron_golem", "snow_golem",
)

private val BIOME_CATEGORIES = listOf("all", "forest", "ocean", "desert", "mountain", "cave", "nether", "end")

// ── ViewModel ───────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BiomesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    private val _sortKey = MutableStateFlow("name")
    val query: StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()
    val sortKey: StateFlow<String> = _sortKey.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val versionFilter: StateFlow<VersionFilterState> = versionFilterFrom(app.dataStore)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VersionFilterState())
    val versionTags: StateFlow<Map<String, VersionTagEntity>> = repo.allVersionTags()
        .map { it.toTagMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val translations: StateFlow<Map<String, Map<String, String>>> =
        translationMapFlow(app.dataStore, repo, "biome")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val biomes: StateFlow<List<BiomeEntity>> = combine(_query.debounce(200), _category, _sortKey) { q, cat, sort ->
        repo.searchBiomes(q).map { list ->
            val filtered = if (cat == "all") list else list.filter { it.category.equals(cat, ignoreCase = true) }
            when (sort) {
                "temperature" -> filtered.sortedByDescending { it.temperature }
                else -> filtered
            }
        }
    }.flatMapLatest { it }
     .applyVersionFilter(versionFilter, versionTags, "biome") { it.id }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }
    fun setSortKey(k: String) { _sortKey.value = k }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandBiome(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteBiomes: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("biome")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "biome", displayName = displayName))
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun parseMobs(mobsJson: String): List<String> {
    val trimmed = mobsJson.trim()
    if (trimmed.isBlank() || trimmed == "[]") return emptyList()
    return trimmed.removeSurrounding("[", "]")
        .replace("\"", "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun parseStructures(structures: String): List<String> {
    if (structures.isBlank()) return emptyList()
    return structures.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

private fun formatId(id: String): String =
    id.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun parseBiomeColor(hex: String): Color? =
    if (hex.isNotEmpty()) runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() else null

// ── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BiomesScreen(
    targetBiomeId: String? = null,
    onNavigateToMob: (mobId: String) -> Unit = {},
    onNavigateToStructure: (structureId: String) -> Unit = {},
    onItemTap: (String) -> Unit = {},
    onCalcTab: (Int) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    vm: BiomesViewModel = viewModel(),
) {
    val query        by vm.query.collectAsStateWithLifecycle()
    val sortKey      by vm.sortKey.collectAsStateWithLifecycle()
    val category     by vm.category.collectAsStateWithLifecycle()
    val biomes       by vm.biomes.collectAsStateWithLifecycle()
    val expandedIds  by vm.expandedIds.collectAsStateWithLifecycle()
    val favoriteIds  by vm.favoriteIds.collectAsStateWithLifecycle()
    val favoriteBiomes by vm.favoriteBiomes.collectAsStateWithLifecycle()
    val vFilter     by vm.versionFilter.collectAsStateWithLifecycle()
    val vTags       by vm.versionTags.collectAsStateWithLifecycle()
    val txMap       by vm.translations.collectAsStateWithLifecycle()
    val hapticConfirm = rememberHapticConfirm()
    val hapticClick = rememberHapticClick()

    // Auto-expand and scroll to target biome from cross-reference
    LaunchedEffect(targetBiomeId) {
        if (targetBiomeId != null) {
            vm.setQuery("")
            vm.setCategory("all")
            snapshotFlow { biomes }
                .first { it.isNotEmpty() }
            val idx = biomes.indexOfFirst { it.id == targetBiomeId }
            if (idx >= 0) {
                listState.scrollToItem(idx + 1) // +1 for intro header
                vm.expandBiome(targetBiomeId)
            }
        }
    }

    val sortOptions = listOf(
        SortOption(stringResource(R.string.biomes_sort_name), "name"),
        SortOption(stringResource(R.string.biomes_sort_temperature), "temperature"),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpyglassSearchBar(
                query = query, onQueryChange = vm::setQuery,
                category = "biomes", placeholder = stringResource(R.string.biomes_search_placeholder),
                modifier = Modifier.weight(1f),
            )
            SortButton(options = sortOptions, selectedKey = sortKey, onSelect = vm::setSortKey)
        }

        // Category filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(BIOME_CATEGORIES, key = { it }) { c ->
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
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Biome,
                    title = stringResource(R.string.biomes_title),
                    description = stringResource(R.string.biomes_description),
                    stat = stringResource(R.string.biomes_stat, biomes.size),
                )
            }
            if (favoriteBiomes.isNotEmpty()) {
                item(key = "fav_header") {
                    SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
                }
                items(favoriteBiomes, key = { "fav_${it.id}" }) { fav ->
                    val isFav = fav.id in favoriteIds
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        leadingIcon = BiomeTextures.get(fav.id) ?: PixelIcons.Biome,
                        modifier = Modifier.clickable { vm.toggleExpanded(fav.id) },
                        trailing = {
                            IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = stringResource(R.string.favorite),
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                }
                item(key = "fav_divider") { SpyglassDivider() }
            }
            items(biomes, key = { it.id }) { b ->
                val tag = vTags["biome:${b.id}"]
                val availability = checkAvailability(tag, vFilter)
                val vAlpha = when (availability) {
                    VersionAvailability.NOT_YET_ADDED -> 0.5f
                    VersionAvailability.REMOVED, VersionAvailability.WRONG_EDITION -> 0.4f
                    else -> 1f
                }
                val addedIn = tag?.let { if (vFilter.edition == "java") it.addedInJava else it.addedInBedrock } ?: ""
                val isExpanded = b.id in expandedIds
                Column(modifier = Modifier.alpha(vAlpha)) {
                    BiomeListItem(b, isFavorite = b.id in favoriteIds, onToggleFavorite = { hapticConfirm(); vm.toggleFavorite(b.id, b.name) }, onClick = { vm.toggleExpanded(b.id) }, addedIn = addedIn, availability = availability, txMap = txMap)
                    val reduceMotion = LocalReduceAnimations.current
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = if (reduceMotion) expandVertically(snap()) else expandVertically(),
                        exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(),
                    ) {
                        BiomeDetailCard(b, onNavigateToMob, onNavigateToStructure, onItemTap, onCalcTab, tag, vFilter, txMap)
                    }
                }
            }
            if (biomes.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = stringResource(R.string.biomes_no_results_title),
                    subtitle = stringResource(R.string.biomes_no_results_subtitle),
                )
            }
        }
    }
}

// ── Biome list item ──────────────────────────────────────────────────────────

@Composable
private fun BiomeListItem(b: BiomeEntity, isFavorite: Boolean, onToggleFavorite: () -> Unit, onClick: () -> Unit, addedIn: String = "", availability: VersionAvailability = VersionAvailability.AVAILABLE, txMap: Map<String, Map<String, String>> = emptyMap()) {
    val biomeColor = parseBiomeColor(b.color)

    BrowseListItem(
        headline    = txMap[b.id]?.get("name") ?: b.name,
        supporting  = "",
        leadingIcon = BiomeTextures.get(b.id) ?: PixelIcons.Biome,
        modifier    = Modifier.clickable { onClick() },
        trailing    = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    if (addedIn.isNotBlank() && availability != VersionAvailability.AVAILABLE) {
                        VersionBadge(addedIn)
                        Spacer(Modifier.height(2.dp))
                    }
                    CategoryBadge(label = b.category.ifEmpty { stringResource(R.string.biomes_category_unknown) }, color = Emerald)
                    Spacer(Modifier.height(2.dp))
                    Text("${b.temperature}\u00B0  ${b.precipitation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = stringResource(R.string.favorite),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    )
}

// ── Biome detail ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BiomeDetailCard(
    biome: BiomeEntity,
    onMobTap: (String) -> Unit,
    onStructureTap: (String) -> Unit,
    onItemTap: (String) -> Unit,
    onCalcTab: (Int) -> Unit,
    tag: VersionTagEntity? = null,
    vFilter: VersionFilterState = VersionFilterState(),
    txMap: Map<String, Map<String, String>> = emptyMap(),
) {
    val bgColor   = parseBiomeColor(biome.color) ?: MaterialTheme.colorScheme.surface
    val isLight   = (0.299 * bgColor.red + 0.587 * bgColor.green + 0.114 * bgColor.blue) > 0.5
    val textColor    = if (isLight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val subtextColor = if (isLight) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor   = if (isLight) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary

    val mobs       = parseMobs(biome.mobsJson)
    val structures = parseStructures(biome.structures)
    val resources  = BiomeResourceMap.itemsForBiome(biome.id)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MinecraftIdRow(biome.id)

        if (tag != null) {
            VersionEditionSection(tag, vFilter)
        }

        // Description
        if (biome.description.isNotBlank()) {
            Text(txMap[biome.id]?.get("description") ?: biome.description, style = MaterialTheme.typography.bodySmall, color = subtextColor)
        }

        // Stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.biomes_temperature), style = MaterialTheme.typography.bodyMedium, color = subtextColor)
            Text("${biome.temperature}\u00B0", style = MaterialTheme.typography.bodyLarge, color = textColor)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.biomes_precipitation), style = MaterialTheme.typography.bodyMedium, color = subtextColor)
            Text(biome.precipitation.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, color = textColor)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.category), style = MaterialTheme.typography.bodyMedium, color = subtextColor)
            Text(biome.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, color = textColor)
        }

        // Features
        if (biome.features.isNotEmpty()) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Text(stringResource(R.string.biomes_features), style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text(biome.features, style = MaterialTheme.typography.bodySmall, color = subtextColor)
        }

        // Resources (from BiomeResourceMap)
        if (resources.isNotEmpty()) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Text(stringResource(R.string.biomes_resources), style = MaterialTheme.typography.labelSmall, color = labelColor)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                resources.forEach { itemId ->
                    AssistChip(
                        onClick = { onItemTap(itemId) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val tex = ItemTextures.get(itemId)
                                if (tex != null) {
                                    SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(formatId(itemId), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = textColor,
                            containerColor = textColor.copy(alpha = 0.15f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Building palette
        val palette = biome.buildingPalette.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (palette.isNotEmpty()) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Text(stringResource(R.string.biomes_building_palette), style = MaterialTheme.typography.labelSmall, color = labelColor)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                palette.forEach { blockId ->
                    AssistChip(
                        onClick = { onItemTap(blockId) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val tex = BlockTextures.get(blockId) ?: ItemTextures.get(blockId)
                                if (tex != null) {
                                    SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(formatId(blockId), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = textColor,
                            containerColor = textColor.copy(alpha = 0.15f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Structures
        if (structures.isNotEmpty()) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Text(stringResource(R.string.biomes_structures), style = MaterialTheme.typography.labelSmall, color = labelColor)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                structures.forEach { s ->
                    AssistChip(
                        onClick = { onStructureTap(s) },
                        label = { Text(formatId(s), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = textColor,
                            containerColor = textColor.copy(alpha = 0.15f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Mobs
        if (mobs.isNotEmpty()) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Text(stringResource(R.string.biomes_mobs_found_here), style = MaterialTheme.typography.labelSmall, color = labelColor)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mobs.forEach { mobId ->
                    if (mobId in KNOWN_MOB_IDS) {
                        AssistChip(
                            onClick = { onMobTap(mobId) },
                            label = { Text(formatId(mobId), style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = textColor,
                                containerColor = textColor.copy(alpha = 0.15f),
                            ),
                            border = null,
                        )
                    }
                    // Skip unknown mob IDs gracefully
                }
            }
        }

        // Cross-links to tool tabs
        val context = LocalContext.current
        val showExperimental by remember {
            context.dataStore.data.map { it[PreferenceKeys.SHOW_EXPERIMENTAL] ?: false }
        }.collectAsStateWithLifecycle(initialValue = true)
        val isLibrarianBiome = showExperimental && biome.id in LIBRARIAN_BIOME_IDS
        val hasStructures = structures.isNotEmpty()
        if (isLibrarianBiome || hasStructures) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isLibrarianBiome) {
                    Text(
                        stringResource(R.string.biomes_librarian_guide),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLight) MaterialTheme.colorScheme.onPrimary else PotionBlue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onCalcTab(15) },
                    )
                }
                if (hasStructures) {
                    Text(
                        stringResource(R.string.biomes_structure_loot),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLight) MaterialTheme.colorScheme.onPrimary else PotionBlue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onCalcTab(19) },
                    )
                }
            }
        }

        HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
        ReportProblemRow(entityType = "Biome", entityName = biome.name, entityId = biome.id, textColor = Color.White.copy(alpha = 0.7f))
        ReportTranslationRow(entityType = "Biome", entityName = biome.name, entityId = biome.id, textColor = Color.White.copy(alpha = 0.7f))
    }
}

private val LIBRARIAN_BIOME_IDS = setOf(
    "plains", "sunflower_plains", "meadow",
    "desert",
    "savanna", "savanna_plateau", "windswept_savanna",
    "snowy_plains", "snowy_taiga", "ice_spikes", "frozen_river", "snowy_slopes",
    "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga",
    "jungle", "sparse_jungle", "bamboo_jungle",
    "swamp", "mangrove_swamp",
)
