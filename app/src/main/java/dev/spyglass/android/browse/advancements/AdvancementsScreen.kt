package dev.spyglass.android.browse.advancements

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.AdvancementEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.repository.GameDataRepository
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

    val advancements: StateFlow<List<AdvancementEntity>> = combine(_query.debounce(200), _category) { q, cat ->
        if (q.isBlank() && cat == "all") repo.searchAdvancements("")
        else if (cat != "all") repo.advancementsByCategory(cat)
        else repo.searchAdvancements(q)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedIds: StateFlow<Set<String>> = repo.advancementCompletedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val completedCount: StateFlow<Int> = repo.advancementCompletedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    // Per-category completed/total counts
    val categoryCounts: StateFlow<Map<String, Pair<Int, Int>>> = combine(advancements, completedIds) { advs, completed ->
        // Get all advancements (not filtered) for category counts
        val byCategory = advs.groupBy { it.category }
        byCategory.mapValues { (_, catAdvs) ->
            val completedInCat = catAdvs.count { it.id in completed }
            completedInCat to catAdvs.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    fun toggleCompleted(id: String) {
        viewModelScope.launch {
            val isCompleted = id in completedIds.value
            repo.toggleAdvancementCompleted(id, !isCompleted)
        }
    }

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "advancement", displayName = displayName))
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch { repo.resetAllAdvancementProgress() }
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

private fun typeLabel(type: String): String = when (type) {
    "task" -> "Task"
    "goal" -> "Goal"
    "challenge" -> "Challenge"
    else -> type.replaceFirstChar { it.uppercase() }
}

private fun categoryLabel(cat: String): String = when (cat) {
    "minecraft" -> "Minecraft"
    "nether" -> "Nether"
    "end" -> "The End"
    "adventure" -> "Adventure"
    "husbandry" -> "Husbandry"
    else -> cat.replaceFirstChar { it.uppercase() }
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
    onCalcTab: (Int) -> Unit = {},
    onEnchantTap: (String) -> Unit = {},
    entityLinkIndex: EntityLinkIndex = EntityLinkIndex(emptyList()),
    vm: AdvancementsViewModel = viewModel(),
) {
    val query by vm.query.collectAsState()
    val category by vm.category.collectAsState()
    val advancements by vm.advancements.collectAsState()
    val flatTreeItems by vm.flatTreeItems.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val treeExpandedIds by vm.treeExpandedIds.collectAsState()
    val completedIds by vm.completedIds.collectAsState()
    val completedCount by vm.completedCount.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val favoriteAdvancements by vm.favoriteAdvancements.collectAsState()
    val categoryCounts by vm.categoryCounts.collectAsState()
    val listState = rememberLazyListState()
    var showResetDialog by remember { mutableStateOf(false) }
    val isSearching = query.isNotBlank()

    LaunchedEffect(targetAdvancementId, advancements) {
        if (targetAdvancementId != null && advancements.isNotEmpty()) {
            vm.navigateToAdvancement(targetAdvancementId, advancements)
            val idx = flatTreeItems.indexOfFirst { it.first.id == targetAdvancementId }
            if (idx >= 0) listState.animateScrollToItem(idx + 2) // offset for header + progress
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Progress", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Clear all advancement checkmarks? This cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { vm.resetAllProgress(); showResetDialog = false }) {
                    Text("Reset", color = Red400)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search advancements\u2026", color = MaterialTheme.colorScheme.secondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("all", "minecraft", "nether", "end", "adventure", "husbandry").forEach { cat ->
                val counts = if (cat == "all") null else categoryCounts[cat]
                val chipLabel = if (cat == "all") "All"
                    else if (counts != null) "${categoryLabel(cat)} (${counts.first}/${counts.second})"
                    else categoryLabel(cat)
                FilterChip(
                    selected = category == cat,
                    onClick = { vm.setCategory(cat) },
                    label = { Text(chipLabel, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Enchant,
                    title = "Advancement Planner",
                    description = "Track your progress through all Minecraft advancements with practical guides",
                    stat = "$completedCount / ${advancements.size} completed",
                )
            }
            // Progress bar
            item(key = "progress_bar") {
                val progress = if (advancements.isNotEmpty()) completedCount.toFloat() / advancements.size else 0f
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Emerald,
                        trackColor = MaterialTheme.colorScheme.outline,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Emerald)
                        Text(
                            "Reset Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.clickable { showResetDialog = true },
                        )
                    }
                }
            }
            // Favorites section
            if (favoriteAdvancements.isNotEmpty()) {
                item(key = "fav_header") {
                    Text("Favorites", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(favoriteAdvancements, key = { "fav_${it.id}" }) { fav ->
                    val isComplete = fav.id in completedIds
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        supportingMaxLines = 1,
                        leadingIcon = PixelIcons.Enchant,
                        leadingIconTint = if (isComplete) Emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        trailing = {
                            IconButton(onClick = { vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
            }
            // Tree view items
            items(flatTreeItems, key = { it.first.id }) { (adv, depth) ->
                val isDetailExpanded = adv.id in expandedIds
                val isTreeExpanded = adv.id in treeExpandedIds
                val isComplete = adv.id in completedIds
                val hasChildren = advancements.any { it.parent == adv.id }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (depth * 24).dp)
                            .background(LocalSurfaceCard.current, RoundedCornerShape(10.dp))
                            .clickable { vm.toggleExpanded(adv.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Checkbox
                        Checkbox(
                            checked = isComplete,
                            onCheckedChange = { vm.toggleCompleted(adv.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Emerald,
                                uncheckedColor = MaterialTheme.colorScheme.secondary,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        // Tree expand/collapse
                        if (hasChildren) {
                            IconButton(
                                onClick = { vm.toggleTreeExpanded(adv.id) },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    if (isTreeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle children",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        // Name
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                adv.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isComplete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (isComplete) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Badges
                        Column(horizontalAlignment = Alignment.End) {
                            val typeColor = when (adv.type) {
                                "challenge" -> MaterialTheme.colorScheme.primary
                                "goal" -> PotionBlue
                                else -> Emerald
                            }
                            CategoryBadge(label = typeLabel(adv.type), color = typeColor)
                            if (hasChildren) {
                                Spacer(Modifier.height(2.dp))
                                val childCount = advancements.count { it.parent == adv.id }
                                CategoryBadge(label = "$childCount children", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        // Favorite star
                        val isFav = adv.id in favoriteIds
                        IconButton(onClick = { vm.toggleFavorite(adv.id, adv.name) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Favorite",
                                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    // Detail card
                    AnimatedVisibility(
                        visible = isDetailExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        AdvancementDetailCard(
                            adv = adv,
                            isComplete = isComplete,
                            allAdvancements = advancements,
                            onToggleComplete = { vm.toggleCompleted(adv.id) },
                            onItemTap = onItemTap,
                            onMobTap = onMobTap,
                            onStructureTap = onStructureTap,
                            onBiomeTap = onBiomeTap,
                            onEnchantTap = onEnchantTap,
                            entityLinkIndex = entityLinkIndex,
                            onAdvancementTap = { parentId ->
                                vm.navigateToAdvancement(parentId, advancements)
                                // Scroll to it
                                val idx = flatTreeItems.indexOfFirst { it.first.id == parentId }
                                if (idx >= 0) {
                                    vm.viewModelScope.launch {
                                        listState.animateScrollToItem(idx + 2)
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
                    title = "No advancements found",
                    subtitle = "Try a different search or filter",
                )
            }
        }
    }
}

// ── Detail Card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancementDetailCard(
    adv: AdvancementEntity,
    isComplete: Boolean,
    allAdvancements: List<AdvancementEntity>,
    onToggleComplete: () -> Unit,
    onItemTap: (String) -> Unit,
    onMobTap: (String) -> Unit,
    onStructureTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
    onEnchantTap: (String) -> Unit,
    entityLinkIndex: EntityLinkIndex,
    onAdvancementTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ResultCard(modifier = modifier) {
        MinecraftIdRow(adv.id)
        if (adv.description.isNotEmpty()) {
            LinkedDescription(
                description = adv.description,
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
            SectionHeader(title = "Requirements")
            Text(adv.requirements, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        // How to get this section
        if (adv.hint.isNotEmpty()) {
            SectionHeader(title = "How to Get This")
            Text(adv.hint, style = MaterialTheme.typography.bodyMedium, color = Emerald)
        }

        // Stats
        SectionHeader(title = "Stats")
        StatRow("Category", categoryLabel(adv.category))
        StatRow("Type", typeLabel(adv.type))
        if (adv.dimension.isNotEmpty()) {
            StatRow("Dimension", adv.dimension.replaceFirstChar { it.uppercase() })
        }
        if (adv.xpReward.isNotEmpty() && adv.xpReward != "0") {
            StatRow("XP Reward", adv.xpReward)
        }

        // Requires (parent advancement)
        if (adv.parent.isNotEmpty()) {
            SectionHeader(title = "Requires")
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
            SectionHeader(title = "Related Items")
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
            SectionHeader(title = "Related Mobs")
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
            SectionHeader(title = "Related Structures")
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
            SectionHeader(title = "Related Biomes")
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

        // Completion toggle
        SpyglassDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = onToggleComplete) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isComplete) Emerald else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isComplete) "Completed" else "Mark Complete",
                    color = if (isComplete) Emerald else MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
