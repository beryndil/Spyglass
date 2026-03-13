package dev.spyglass.android.browse.advancements

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import dev.spyglass.android.data.db.entities.AdvancementEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.VersionTagEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Tree node model ─────────────────────────────────────────────────────────

data class TreeNode(
    val advancement: AdvancementEntity,
    val depth: Int,
    val children: List<TreeNode>,
    val totalDescendantCount: Int,
)

// ── ViewModel ───────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AdvancementsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    private val _treeExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    val treeExpandedIds: StateFlow<Set<String>> = _treeExpandedIds.asStateFlow()

    private val _sortKey = MutableStateFlow("name")
    val sortKey: StateFlow<String> = _sortKey.asStateFlow()
    fun setSortKey(k: String) { _sortKey.value = k }

    val versionFilter: StateFlow<VersionFilterState> = versionFilterFrom(app.dataStore)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VersionFilterState())
    val versionTags: StateFlow<Map<String, VersionTagEntity>> = repo.allVersionTags()
        .map { it.toTagMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val translations: StateFlow<Map<String, Map<String, String>>> =
        translationMapFlow(app.dataStore, repo, "advancement")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val advancements: StateFlow<List<AdvancementEntity>> = combine(_query.debounce(200), _category, _sortKey) { q, cat, sort ->
        val flow = if (q.isBlank() && cat == "all") repo.searchAdvancements("")
        else if (cat != "all") repo.advancementsByCategory(cat)
        else repo.searchAdvancements(q)
        flow.map { list ->
            when (sort) {
                "type" -> list.sortedWith(compareByDescending {
                    when (it.type.lowercase()) { "challenge" -> 3; "goal" -> 2; else -> 1 }
                })
                else -> list
            }
        }
    }.flatMapLatest { it }
     .applyVersionFilter(versionFilter, versionTags, "advancement") { it.id }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteAdvancements: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("advancement")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Build tree structure from flat list
    val flatTreeItems: StateFlow<List<Pair<AdvancementEntity, Int>>> = combine(advancements, _treeExpandedIds, _query) { advs, expanded, q ->
        if (q.isNotBlank()) {
            // In search mode, show flat list
            advs.map { it to 0 }
        } else {
            val tree = buildTree(advs)
            flattenTree(tree, expanded)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }

    fun toggleExpanded(id: String) {
        val current = _expandedIds.value
        _expandedIds.value = if (id in current) current - id else current + id
    }

    fun expandAdvancement(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    fun toggleTreeExpanded(id: String) {
        val current = _treeExpandedIds.value
        _treeExpandedIds.value = if (id in current) current - id else current + id
    }

    fun expandTreeNode(id: String) {
        _treeExpandedIds.value = _treeExpandedIds.value + id
    }

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "advancement", displayName = displayName))
        }
    }

    // Navigate to a parent advancement: expand its tree path and detail card
    fun navigateToAdvancement(id: String, allAdvancements: List<AdvancementEntity>) {
        // Expand all ancestors in tree
        val ancestorIds = mutableListOf<String>()
        var current = id
        val byId = allAdvancements.associateBy { it.id }
        while (true) {
            val adv = byId[current] ?: break
            if (adv.parent.isNotEmpty()) {
                ancestorIds.add(adv.parent)
                current = adv.parent
            } else break
        }
        _treeExpandedIds.value = _treeExpandedIds.value + ancestorIds.toSet()
        _expandedIds.value = _expandedIds.value + id
    }

    private fun buildTree(advancements: List<AdvancementEntity>): List<TreeNode> {
        val byParent = advancements.groupBy { it.parent }
        fun build(parentId: String, depth: Int): List<TreeNode> {
            val children = byParent[parentId] ?: return emptyList()
            return children.sortedBy { it.name }.map { adv ->
                val childNodes = build(adv.id, depth + 1)
                val descendantCount = childNodes.sumOf { it.totalDescendantCount + 1 }
                TreeNode(adv, depth, childNodes, descendantCount)
            }
        }
        // Roots have empty parent
        return build("", 0)
    }

    private fun flattenTree(nodes: List<TreeNode>, expandedIds: Set<String>): List<Pair<AdvancementEntity, Int>> {
        val result = mutableListOf<Pair<AdvancementEntity, Int>>()
        fun flatten(node: TreeNode) {
            result.add(node.advancement to node.depth)
            if (node.advancement.id in expandedIds) {
                node.children.forEach { flatten(it) }
            }
        }
        nodes.forEach { flatten(it) }
        return result
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun typeLabel(type: String): String = when (type) {
    "task" -> stringResource(R.string.adv_type_task)
    "goal" -> stringResource(R.string.adv_type_goal)
    "challenge" -> stringResource(R.string.adv_type_challenge)
    else -> type.replaceFirstChar { it.uppercase() }
}

@Composable
private fun categoryLabel(cat: String): String = when (cat) {
    "minecraft" -> stringResource(R.string.adv_tab_minecraft)
    "nether" -> stringResource(R.string.adv_tab_nether)
    "end" -> stringResource(R.string.adv_tab_end)
    "adventure" -> stringResource(R.string.adv_tab_adventure)
    "husbandry" -> stringResource(R.string.adv_tab_husbandry)
    else -> cat.replaceFirstChar { it.uppercase() }
}

private fun difficultyColor(difficulty: String): Color = when (difficulty) {
    "trivial" -> Emerald
    "easy" -> PotionBlue
    "medium" -> Color(0xFFFFA726)
    "hard" -> NetherRed
    "expert" -> EnderPurple
    else -> Color.Gray
}

private fun formatId(id: String): String =
    id.removePrefix("minecraft:").split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun parseCommaSeparated(csv: String): List<String> {
    if (csv.isBlank()) return emptyList()
    return csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

// ── Main Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancementsScreen(
    targetAdvancementId: String? = null,
    onItemTap: (String) -> Unit = {},
    onMobTap: (String) -> Unit = {},
    onStructureTap: (String) -> Unit = {},
    onBiomeTap: (String) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onCalcTab: (Int) -> Unit = {},
    onEnchantTap: (String) -> Unit = {},
    entityLinkIndex: EntityLinkIndex = EntityLinkIndex(emptyList()),
    vm: AdvancementsViewModel = viewModel(),
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val advancements by vm.advancements.collectAsStateWithLifecycle()
    val flatTreeItems by vm.flatTreeItems.collectAsStateWithLifecycle()
    val expandedIds by vm.expandedIds.collectAsStateWithLifecycle()
    val treeExpandedIds by vm.treeExpandedIds.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val favoriteAdvancements by vm.favoriteAdvancements.collectAsStateWithLifecycle()
    val sortKey by vm.sortKey.collectAsStateWithLifecycle()
    val vFilter     by vm.versionFilter.collectAsStateWithLifecycle()
    val vTags       by vm.versionTags.collectAsStateWithLifecycle()
    val txMap       by vm.translations.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val hapticConfirm = rememberHapticConfirm()
    val reduceMotion = LocalReduceAnimations.current
    val hapticClick = rememberHapticClick()
    val isSearching = query.isNotBlank()

    val sortOptions = listOf(
        SortOption(stringResource(R.string.advancements_sort_name), "name"),
        SortOption(stringResource(R.string.advancements_sort_type), "type"),
    )

    LaunchedEffect(targetAdvancementId) {
        if (targetAdvancementId != null) {
            snapshotFlow { advancements }
                .first { it.isNotEmpty() }
            vm.navigateToAdvancement(targetAdvancementId, advancements)
            val idx = flatTreeItems.indexOfFirst { it.first.id == targetAdvancementId }
            if (idx >= 0) {
                if (reduceMotion) listState.scrollToItem(idx + 2)
                else listState.animateScrollToItem(idx + 2) // offset for header + progress
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "search_bar") {
            Row(
                modifier = Modifier.padding(end = 0.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpyglassSearchBar(
                    query = query, onQueryChange = vm::setQuery,
                    category = "advancements", placeholder = stringResource(R.string.advancements_search_placeholder),
                    modifier = Modifier.weight(1f),
                )
                SortButton(options = sortOptions, selectedKey = sortKey, onSelect = vm::setSortKey)
            }
        }
        item(key = "category_chips") {
            FlowRow(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf("all", "minecraft", "nether", "end", "adventure", "husbandry").forEach { cat ->
                    val chipLabel = if (cat == "all") stringResource(R.string.all)
                        else categoryLabel(cat)
                    FilterChip(
                        selected = category == cat,
                        onClick = { hapticClick(); vm.setCategory(cat) },
                        label = { Text(chipLabel, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
        item {
            TabIntroHeader(
                icon = PixelIcons.Advancement,
                title = stringResource(R.string.advancements_title),
                description = stringResource(R.string.advancements_description),
                stat = "${advancements.size} advancements",
            )
        }
        // Favorites section
        if (favoriteAdvancements.isNotEmpty()) {
            item(key = "fav_header") {
                Text(stringResource(R.string.favorites), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            }
            items(favoriteAdvancements, key = { "fav_${it.id}" }) { fav ->
                BrowseListItem(
                    headline = fav.displayName,
                    supporting = "",
                    supportingMaxLines = 1,
                    leadingIcon = PixelIcons.Advancement,
                    trailing = {
                        IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    },
                )
            }
        }
        // Tree view items
        items(flatTreeItems, key = { it.first.id }) { (adv, depth) ->
            val tag = vTags["advancement:${adv.id}"]
            val availability = checkAvailability(tag, vFilter)
            val vAlpha = when (availability) {
                VersionAvailability.NOT_YET_ADDED -> 0.5f
                VersionAvailability.REMOVED, VersionAvailability.WRONG_EDITION -> 0.4f
                else -> 1f
            }
            val addedIn = tag?.let { if (vFilter.edition == "java") it.addedInJava else it.addedInBedrock } ?: ""
            val isDetailExpanded = adv.id in expandedIds
            val isTreeExpanded = adv.id in treeExpandedIds
            val hasChildren = advancements.any { it.parent == adv.id }

            Column(modifier = Modifier.alpha(vAlpha)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (depth * 24).dp)
                        .background(LocalSurfaceCard.current, RoundedCornerShape(10.dp))
                        .clickable { vm.toggleExpanded(adv.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Tree expand/collapse
                    if (hasChildren) {
                        IconButton(
                            onClick = { vm.toggleTreeExpanded(adv.id) },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                if (isTreeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = stringResource(R.string.advancements_toggle_children),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    // Name
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            txMap[adv.id]?.get("name") ?: adv.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    // Badges
                    Column(horizontalAlignment = Alignment.End) {
                        if (addedIn.isNotBlank() && availability != VersionAvailability.AVAILABLE) {
                            VersionBadge(addedIn)
                            Spacer(Modifier.height(2.dp))
                        }
                        val typeColor = when (adv.type) {
                            "challenge" -> MaterialTheme.colorScheme.primary
                            "goal" -> PotionBlue
                            else -> Emerald
                        }
                        CategoryBadge(label = typeLabel(adv.type), color = typeColor)
                        if (adv.difficulty.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            CategoryBadge(label = adv.difficulty.replaceFirstChar { it.uppercase() }, color = difficultyColor(adv.difficulty))
                        }
                        if (hasChildren) {
                            Spacer(Modifier.height(2.dp))
                            val childCount = advancements.count { it.parent == adv.id }
                            CategoryBadge(label = stringResource(R.string.advancements_children_count, childCount), color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    // Favorite star
                    val isFav = adv.id in favoriteIds
                    IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(adv.id, adv.name) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = stringResource(R.string.favorite),
                            tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                // Detail card
                val reduceMotion = LocalReduceAnimations.current
                AnimatedVisibility(
                    visible = isDetailExpanded,
                    enter = if (reduceMotion) expandVertically(snap()) else expandVertically(),
                    exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(),
                ) {
                    AdvancementDetailCard(
                        adv = adv,
                        allAdvancements = advancements,
                        onItemTap = onItemTap,
                        onMobTap = onMobTap,
                        onStructureTap = onStructureTap,
                        onBiomeTap = onBiomeTap,
                        onEnchantTap = onEnchantTap,
                        entityLinkIndex = entityLinkIndex,
                        tag = tag,
                        vFilter = vFilter,
                        txMap = txMap,
                        onAdvancementTap = { parentId ->
                            vm.navigateToAdvancement(parentId, advancements)
                            // Scroll to it
                            val idx = flatTreeItems.indexOfFirst { it.first.id == parentId }
                            if (idx >= 0) {
                                vm.viewModelScope.launch {
                                    if (reduceMotion) listState.scrollToItem(idx + 2)
                                    else listState.animateScrollToItem(idx + 2)
                                }
                            }
                        },
                        modifier = Modifier.padding(start = (depth * 24).dp, top = 4.dp),
                    )
                }
            }
        }
        if (flatTreeItems.isEmpty()) item {
            EmptyState(
                icon = PixelIcons.SearchOff,
                title = stringResource(R.string.advancements_no_results_title),
                subtitle = stringResource(R.string.advancements_no_results_subtitle),
            )
        }
    }
}

// ── Detail Card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancementDetailCard(
    adv: AdvancementEntity,
    allAdvancements: List<AdvancementEntity>,
    onItemTap: (String) -> Unit,
    onMobTap: (String) -> Unit,
    onStructureTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
    onEnchantTap: (String) -> Unit,
    entityLinkIndex: EntityLinkIndex,
    tag: VersionTagEntity? = null,
    vFilter: VersionFilterState = VersionFilterState(),
    txMap: Map<String, Map<String, String>> = emptyMap(),
    onAdvancementTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ResultCard(modifier = modifier) {
        MinecraftIdRow(adv.id)
        if (tag != null) {
            VersionEditionSection(tag, vFilter)
        }
        if (adv.description.isNotEmpty()) {
            LinkedDescription(
                description = txMap[adv.id]?.get("description") ?: adv.description,
                linkIndex = entityLinkIndex,
                selfId = adv.id,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onBiomeTap = onBiomeTap,
                onStructureTap = onStructureTap,
                onEnchantTap = onEnchantTap,
            )
        }

        // Requirements section
        if (adv.requirements.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.advancements_requirements))
            Text(txMap[adv.id]?.get("requirements") ?: adv.requirements, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        // How to get this section
        if (adv.hint.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.advancements_how_to_get))
            Text(txMap[adv.id]?.get("hint") ?: adv.hint, style = MaterialTheme.typography.bodyMedium, color = Emerald)
        }

        // Tutorial section
        if (adv.tutorial.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.advancements_tutorial))
            Text(txMap[adv.id]?.get("tutorial") ?: adv.tutorial, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        // Stats
        SectionHeader(title = stringResource(R.string.advancements_stats))
        StatRow(stringResource(R.string.category), categoryLabel(adv.category))
        StatRow(stringResource(R.string.type), typeLabel(adv.type))
        if (adv.difficulty.isNotEmpty()) {
            StatRow(stringResource(R.string.advancements_difficulty), adv.difficulty.replaceFirstChar { it.uppercase() })
        }
        if (adv.dimension.isNotEmpty()) {
            StatRow(stringResource(R.string.dimension), adv.dimension.replaceFirstChar { it.uppercase() })
        }
        if (adv.xpReward.isNotEmpty() && adv.xpReward != "0") {
            StatRow(stringResource(R.string.advancements_xp_reward), adv.xpReward)
        }

        // Requires (parent advancement)
        if (adv.parent.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.advancements_requires))
            val parentAdv = allAdvancements.find { it.id == adv.parent }
            val parentName = parentAdv?.name ?: adv.parent.substringAfterLast('/').replace('_', ' ').replaceFirstChar { it.uppercase() }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(
                    onClick = { onAdvancementTap(adv.parent) },
                    label = { Text(parentName, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = PotionBlue,
                        containerColor = PotionBlue.copy(alpha = 0.12f),
                    ),
                    border = null,
                )
            }
        }

        // Related Items
        val relatedItems = parseCommaSeparated(adv.relatedItems)
        if (relatedItems.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.advancements_related_items))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                relatedItems.forEach { itemId ->
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

        // Related Mobs
        val relatedMobs = parseCommaSeparated(adv.relatedMobs)
        if (relatedMobs.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.advancements_related_mobs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                relatedMobs.forEach { mobId ->
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

        // Related Structures
        val relatedStructures = parseCommaSeparated(adv.relatedStructures)
        if (relatedStructures.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.advancements_related_structures))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                relatedStructures.forEach { structureId ->
                    AssistChip(
                        onClick = { onStructureTap(structureId) },
                        label = { Text(formatId(structureId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = EnderPurple,
                            containerColor = EnderPurple.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Related Biomes
        val relatedBiomes = parseCommaSeparated(adv.relatedBiomes)
        if (relatedBiomes.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.advancements_related_biomes))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                relatedBiomes.forEach { biomeId ->
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

        SpyglassDivider()
        ReportProblemRow(entityType = "Advancement", entityName = adv.name, entityId = adv.id)
        ReportTranslationRow(entityType = "Advancement", entityName = adv.name, entityId = adv.id)
    }
}
