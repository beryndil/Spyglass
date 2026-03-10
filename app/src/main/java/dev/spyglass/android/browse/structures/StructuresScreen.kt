package dev.spyglass.android.browse.structures

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import dev.spyglass.android.core.ui.SpyglassSearchBar
import dev.spyglass.android.core.ui.rememberHapticConfirm
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.StructureEntity
import dev.spyglass.android.data.db.entities.VersionTagEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private fun formatId(id: String): String =
    id.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun parseCommaSeparated(csv: String): List<String> {
    if (csv.isBlank()) return emptyList()
    return csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class StructuresViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _dimension = MutableStateFlow("all")
    private val _sortKey = MutableStateFlow("name")
    val query: StateFlow<String> = _query.asStateFlow()
    val dimension: StateFlow<String> = _dimension.asStateFlow()
    val sortKey: StateFlow<String> = _sortKey.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val versionFilter: StateFlow<VersionFilterState> = versionFilterFrom(app.dataStore)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VersionFilterState())
    val versionTags: StateFlow<Map<String, VersionTagEntity>> = repo.allVersionTags()
        .map { it.toTagMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val translations: StateFlow<Map<String, Map<String, String>>> =
        translationMapFlow(app.dataStore, repo, "structure")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val structures: StateFlow<List<StructureEntity>> = combine(
        _query.debounce(200), _dimension, _sortKey
    ) { q, dim, sort ->
        repo.searchStructures(q).map { list ->
            val filtered = if (dim == "all") list else list.filter { it.dimension == dim }
            when (sort) {
                "difficulty" -> filtered.sortedWith(compareByDescending {
                    when (it.difficulty.lowercase()) { "hard" -> 3; "medium" -> 2; "easy" -> 1; else -> 0 }
                })
                else -> filtered
            }
        }
    }.flatMapLatest { it }
     .applyVersionFilter(versionFilter, versionTags, "structure") { it.id }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setDimension(d: String) { _dimension.value = d }
    fun setSortKey(k: String) { _sortKey.value = k }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandStructure(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteStructures: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("structure")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "structure", displayName = displayName))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StructuresScreen(
    targetStructureId: String? = null,
    onNavigateToMob: (mobId: String) -> Unit = {},
    onNavigateToBiome: (biomeId: String) -> Unit = {},
    onItemTap: (itemId: String) -> Unit = {},
    onCalcTab: (Int) -> Unit = {},
    entityLinkIndex: EntityLinkIndex = EntityLinkIndex(emptyList()),
    onEnchantTap: (String) -> Unit = {},
    vm: StructuresViewModel = viewModel(),
) {
    val query        by vm.query.collectAsStateWithLifecycle()
    val sortKey      by vm.sortKey.collectAsStateWithLifecycle()
    val dimension    by vm.dimension.collectAsStateWithLifecycle()
    val structures   by vm.structures.collectAsStateWithLifecycle()
    val expandedIds  by vm.expandedIds.collectAsStateWithLifecycle()
    val favoriteIds  by vm.favoriteIds.collectAsStateWithLifecycle()
    val favoriteStructures by vm.favoriteStructures.collectAsStateWithLifecycle()
    val vFilter     by vm.versionFilter.collectAsStateWithLifecycle()
    val vTags       by vm.versionTags.collectAsStateWithLifecycle()
    val txMap       by vm.translations.collectAsStateWithLifecycle()
    val listState    = rememberLazyListState()
    val hapticConfirm = rememberHapticConfirm()
    val hapticClick = rememberHapticClick()

    val dimensions = listOf("all", "overworld", "nether", "end")

    // Auto-expand and scroll to target structure from cross-reference
    LaunchedEffect(targetStructureId) {
        if (targetStructureId != null) {
            vm.setQuery("")
            vm.setDimension("all")
            snapshotFlow { structures }
                .first { it.isNotEmpty() }
            val idx = structures.indexOfFirst { it.id == targetStructureId }
            if (idx >= 0) {
                listState.scrollToItem(idx + 1) // +1 for intro header
                vm.expandStructure(targetStructureId)
            }
        }
    }

    val sortOptions = listOf(
        SortOption(stringResource(R.string.structures_sort_name), "name"),
        SortOption(stringResource(R.string.structures_sort_difficulty), "difficulty"),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpyglassSearchBar(
                query = query, onQueryChange = vm::setQuery,
                category = "structures", placeholder = stringResource(R.string.structures_search_placeholder),
                modifier = Modifier.weight(1f),
            )
            SortButton(options = sortOptions, selectedKey = sortKey, onSelect = vm::setSortKey)
        }
        Row(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dimensions.forEach { d ->
                FilterChip(selected = dimension == d, onClick = { hapticClick(); vm.setDimension(d) },
                    label = { Text(if (d == "all") stringResource(R.string.all) else d.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
            }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Structure,
                    title = stringResource(R.string.structures_title),
                    description = stringResource(R.string.structures_description),
                    stat = stringResource(R.string.structures_stat, structures.size),
                )
            }
            if (favoriteStructures.isNotEmpty()) {
                item(key = "fav_header") {
                    SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
                }
                items(favoriteStructures, key = { "fav_${it.id}" }) { fav ->
                    val isFav = fav.id in favoriteIds
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        leadingIcon = StructureTextures.get(fav.id) ?: PixelIcons.Structure,
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
            items(structures, key = { it.id }) { s ->
                val tag = vTags["structure:${s.id}"]
                val availability = checkAvailability(tag, vFilter)
                val vAlpha = when (availability) {
                    VersionAvailability.NOT_YET_ADDED -> 0.5f
                    VersionAvailability.REMOVED, VersionAvailability.WRONG_EDITION -> 0.4f
                    else -> 1f
                }
                val addedIn = tag?.let { if (vFilter.edition == "java") it.addedInJava else it.addedInBedrock } ?: ""
                val isExpanded = s.id in expandedIds
                Column(modifier = Modifier.alpha(vAlpha)) {
                    StructureListItem(s, isFavorite = s.id in favoriteIds, onToggleFavorite = { hapticConfirm(); vm.toggleFavorite(s.id, s.name) }, onClick = { vm.toggleExpanded(s.id) }, addedIn = addedIn, availability = availability, txMap = txMap)
                    val reduceMotion = LocalReduceAnimations.current
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = if (reduceMotion) expandVertically(snap()) else expandVertically(),
                        exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(),
                    ) {
                        StructureDetailCard(s, onNavigateToMob, onNavigateToBiome, onItemTap, onCalcTab, entityLinkIndex, onEnchantTap, tag, vFilter, txMap)
                    }
                }
            }
            if (structures.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = stringResource(R.string.structures_no_results_title),
                    subtitle = stringResource(R.string.structures_no_results_subtitle),
                )
            }
        }
    }
}

@Composable
private fun StructureListItem(s: StructureEntity, isFavorite: Boolean, onToggleFavorite: () -> Unit, onClick: () -> Unit, addedIn: String = "", availability: VersionAvailability = VersionAvailability.AVAILABLE, txMap: Map<String, Map<String, String>> = emptyMap()) {
    val dimensionColor = when (s.dimension) {
        "nether" -> NetherRed
        "end"    -> EnderPurple
        else     -> Emerald
    }
    val difficultyColor = when (s.difficulty) {
        "hard"   -> NetherRed
        "medium" -> MaterialTheme.colorScheme.primary
        else     -> Emerald
    }

    BrowseListItem(
        headline    = txMap[s.id]?.get("name") ?: s.name,
        supporting  = "",
        leadingIcon = StructureTextures.get(s.id) ?: PixelIcons.Structure,
        modifier    = Modifier.clickable { onClick() },
        trailing    = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    if (addedIn.isNotBlank() && availability != VersionAvailability.AVAILABLE) {
                        VersionBadge(addedIn)
                        Spacer(Modifier.height(2.dp))
                    }
                    CategoryBadge(label = s.dimension, color = dimensionColor)
                    if (s.difficulty.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        CategoryBadge(label = s.difficulty, color = difficultyColor)
                    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StructureDetailCard(
    structure: StructureEntity,
    onMobTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
    onItemTap: (String) -> Unit,
    onCalcTab: (Int) -> Unit,
    entityLinkIndex: EntityLinkIndex,
    onEnchantTap: (String) -> Unit,
    tag: VersionTagEntity? = null,
    vFilter: VersionFilterState = VersionFilterState(),
    txMap: Map<String, Map<String, String>> = emptyMap(),
) {
    val biomes      = parseCommaSeparated(structure.biomes)
    val mobs        = parseCommaSeparated(structure.mobs)
    val loot        = parseCommaSeparated(structure.loot)
    val uniqueBlocks = parseCommaSeparated(structure.uniqueBlocks)

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        MinecraftIdRow(structure.id)

        if (tag != null) {
            VersionEditionSection(tag, vFilter)
        }

        // Description
        if (structure.description.isNotEmpty()) {
            LinkedDescription(
                description = txMap[structure.id]?.get("description") ?: structure.description,
                linkIndex = entityLinkIndex,
                selfId = structure.id,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onBiomeTap = onBiomeTap,
                onStructureTap = { }, // already on structure tab
                onEnchantTap = onEnchantTap,
            )
        }

        // How to Find
        if (structure.findMethod.isNotEmpty()) {
            SpyglassDivider()
            Text(stringResource(R.string.structures_how_to_find), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(structure.findMethod, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Biomes
        if (biomes.isNotEmpty()) {
            SpyglassDivider()
            Text(stringResource(R.string.structures_biomes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                biomes.forEach { biomeId ->
                    AssistChip(
                        onClick = { onBiomeTap(biomeId) },
                        label = { Text(formatId(biomeId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Emerald,
                            containerColor = Emerald.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Mobs
        if (mobs.isNotEmpty()) {
            SpyglassDivider()
            Text(stringResource(R.string.structures_mobs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mobs.forEach { mobId ->
                    AssistChip(
                        onClick = { onMobTap(mobId) },
                        label = { Text(formatId(mobId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = NetherRed,
                            containerColor = NetherRed.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Loot
        if (loot.isNotEmpty()) {
            SpyglassDivider()
            Text(stringResource(R.string.structures_loot), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                loot.forEach { itemId ->
                    AssistChip(
                        onClick = { onItemTap(itemId) },
                        label = { Text(formatId(itemId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Unique Blocks
        if (uniqueBlocks.isNotEmpty()) {
            SpyglassDivider()
            Text(stringResource(R.string.structures_unique_blocks), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                uniqueBlocks.forEach { blockId ->
                    AssistChip(
                        onClick = { onItemTap(blockId) },
                        label = { Text(formatId(blockId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Cross-links to tool tabs
        if (loot.isNotEmpty()) {
            SpyglassDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.structures_loot_guide),
                    style = MaterialTheme.typography.labelSmall,
                    color = PotionBlue,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onCalcTab(18) },
                )
                Text(
                    stringResource(R.string.structures_armor_trims),
                    style = MaterialTheme.typography.labelSmall,
                    color = PotionBlue,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onCalcTab(17) },
                )
            }
        }

        SpyglassDivider()
        ReportProblemRow(entityType = "Structure", entityName = structure.name, entityId = structure.id)
        ReportTranslationRow(entityType = "Structure", entityName = structure.name, entityId = structure.id)
    }
}
