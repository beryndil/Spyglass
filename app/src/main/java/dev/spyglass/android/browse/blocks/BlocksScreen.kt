package dev.spyglass.android.browse.blocks

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.MechanicsInfo
import dev.spyglass.android.core.VersionAvailability
import dev.spyglass.android.core.VersionFilterState
import dev.spyglass.android.core.applyVersionFilter
import dev.spyglass.android.core.checkAvailability
import dev.spyglass.android.core.checkMechanicsChanged
import dev.spyglass.android.core.toTagMap
import dev.spyglass.android.core.versionFilterFrom
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.core.ui.rememberHapticConfirm
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.data.CompostData
import dev.spyglass.android.data.db.entities.BlockEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.db.entities.StructureEntity
import dev.spyglass.android.data.db.entities.VersionTagEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Job block reverse-lookup: blockId → profession ──────────────────────────

private val BLOCK_TO_PROFESSION = mapOf(
    "blast_furnace" to "Armorer",
    "smoker" to "Butcher",
    "cartography_table" to "Cartographer",
    "brewing_stand" to "Cleric",
    "composter" to "Farmer",
    "barrel" to "Fisherman",
    "fletching_table" to "Fletcher",
    "cauldron" to "Leatherworker",
    "lectern" to "Librarian",
    "stonecutter" to "Mason",
    "loom" to "Shepherd",
    "smithing_table" to "Toolsmith",
    "grindstone" to "Weaponsmith",
)

// ── Tool icon helper ────────────────────────────────────────────────────────

private fun toolTextureId(toolRequired: String): String? {
    if (toolRequired.isBlank() || toolRequired == "none") return null
    // toolRequired may be "pickaxe_stone", "pickaxe_iron", "pickaxe_diamond", or just "pickaxe"
    val parts = toolRequired.split("_")
    return when {
        parts.size >= 2 && parts[0] in setOf("pickaxe", "axe", "shovel", "hoe") -> {
            val level = parts.drop(1).joinToString("_")
            "${level}_${parts[0]}"
        }
        parts[0] in setOf("pickaxe", "axe", "shovel", "hoe", "shears") -> {
            if (parts[0] == "shears") "shears"
            else "wooden_${parts[0]}"
        }
        else -> null
    }
}

private fun formatId(id: String): String =
    id.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

@Composable
private fun blockCategoryColor(cat: String) = when (cat) {
    "building"   -> MaterialTheme.colorScheme.onSurfaceVariant
    "natural"    -> Emerald
    "decoration" -> PotionBlue
    "utility"    -> MaterialTheme.colorScheme.primary
    "redstone"   -> NetherRed
    else         -> MaterialTheme.colorScheme.secondary
}

private val BLOCK_CATEGORIES = listOf("all", "building", "natural", "decoration", "utility", "redstone")

// ── ViewModel ───────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BlocksViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    private val _sortKey = MutableStateFlow("name")
    val sortKey: StateFlow<String> = _sortKey.asStateFlow()

    val versionFilter: StateFlow<VersionFilterState> = versionFilterFrom(app.dataStore)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VersionFilterState())
    val versionTags: StateFlow<Map<String, VersionTagEntity>> = repo.allVersionTags()
        .map { it.toTagMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val hideUnobtainable: StateFlow<Boolean> = app.dataStore.data
        .map { it[PreferenceKeys.HIDE_UNOBTAINABLE_BLOCKS] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val blocks: StateFlow<List<BlockEntity>> = combine(
        _query.debounce(200), _category, _sortKey, hideUnobtainable
    ) { q, cat, sort, hide -> arrayOf(q, cat, sort, hide) }
    .flatMapLatest { args ->
        val q = args[0] as String; val cat = args[1] as String
        val sort = args[2] as String; val hide = args[3] as Boolean
        val flow = if (cat == "all") repo.searchBlocks(q) else repo.blocksByCategory(cat)
        flow.map { list ->
            val filtered = if (hide) list.filter { it.isObtainable } else list
            when (sort) {
                "hardness" -> filtered.sortedByDescending { it.hardness }
                "blast_resistance" -> filtered.sortedByDescending { it.blastResistance }
                "light_level" -> filtered.sortedByDescending { it.lightLevel }
                else -> filtered
            }
        }
    }.applyVersionFilter(versionFilter, versionTags, "block") { it.id }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Build a map of all recipes keyed by outputItem for chain calculation
    val allRecipes: StateFlow<Map<String, RecipeEntity>> = repo.searchRecipes("")
        .map { list -> list.associateBy { it.outputItem } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Structures for reverse-lookup
    val structures: StateFlow<List<StructureEntity>> = repo.searchStructures("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteBlocks: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("block")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "block", displayName = displayName))
        }
    }

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }
    fun setSortKey(k: String) { _sortKey.value = k }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandBlock(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    fun recipesForItem(itemId: String) = repo.recipesForItem(itemId)
    fun recipesUsingItem(itemId: String) = repo.recipesUsingIngredient(itemId)
}

// ── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun BlocksScreen(
    targetBlockId: String? = null,
    onItemTap: (String) -> Unit = {},
    onBiomeTap: (String) -> Unit = {},
    onTradeTap: (String) -> Unit = {},
    onStructureTap: (String) -> Unit = {},
    vm: BlocksViewModel = viewModel(),
) {
    val query       by vm.query.collectAsStateWithLifecycle()
    val category    by vm.category.collectAsStateWithLifecycle()
    val sortKey     by vm.sortKey.collectAsStateWithLifecycle()
    val blocks      by vm.blocks.collectAsStateWithLifecycle()
    val expandedIds by vm.expandedIds.collectAsStateWithLifecycle()
    val allRecipes  by vm.allRecipes.collectAsStateWithLifecycle()
    val structures  by vm.structures.collectAsStateWithLifecycle()
    val favoriteIds    by vm.favoriteIds.collectAsStateWithLifecycle()
    val favoriteBlocks by vm.favoriteBlocks.collectAsStateWithLifecycle()
    val vFilter     by vm.versionFilter.collectAsStateWithLifecycle()
    val vTags       by vm.versionTags.collectAsStateWithLifecycle()
    val listState   = rememberLazyListState()
    val hapticConfirm = rememberHapticConfirm()
    val hapticClick = rememberHapticClick()
    val blockSortOptions = remember { listOf(
        SortOption("Name A\u2192Z", "name"),
        SortOption("Hardness \u2193", "hardness"),
        SortOption("Blast Resistance \u2193", "blast_resistance"),
        SortOption("Light Level \u2193", "light_level"),
    ) }

    // Auto-expand and scroll to target block from cross-reference (runs once per targetBlockId)
    LaunchedEffect(targetBlockId) {
        if (targetBlockId != null) {
            vm.setQuery("")
            vm.setCategory("all")
            // Wait for the blocks list to populate after clearing filters
            snapshotFlow { blocks }
                .first { it.isNotEmpty() }
            val idx = blocks.indexOfFirst { it.id == targetBlockId }
            if (idx >= 0) {
                listState.scrollToItem(idx + 1) // +1 for intro header
                vm.expandBlock(targetBlockId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value         = query,
                onValueChange = vm::setQuery,
                placeholder   = { Text("Search blocks\u2026", color = MaterialTheme.colorScheme.secondary) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.weight(1f),
            )
            SortButton(options = blockSortOptions, selectedKey = sortKey, onSelect = vm::setSortKey)
        }

        // Category filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(BLOCK_CATEGORIES, key = { it }) { c ->
                val chipColor = if (c == "all") MaterialTheme.colorScheme.primary else blockCategoryColor(c)
                FilterChip(
                    selected = category == c,
                    onClick = { hapticClick(); vm.setCategory(c) },
                    label = {
                        Text(
                            if (c == "all") stringResource(R.string.all) else c.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = chipColor,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor.copy(alpha = 0.2f),
                        selectedLabelColor = chipColor,
                    ),
                )
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Blocks,
                    title = "Blocks",
                    description = "Every placeable block with hardness and tool info",
                    stat = "${blocks.size} blocks",
                )
            }
            if (favoriteBlocks.isNotEmpty()) {
                item(key = "fav_header") {
                    SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
                }
                items(favoriteBlocks, key = { "fav_${it.id}" }) { fav ->
                    val isFav = fav.id in favoriteIds
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        leadingIcon = ItemTextures.get(fav.id) ?: PixelIcons.Blocks,
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
            items(blocks, key = { it.id }) { b ->
                val tag = vTags["block:${b.id}"]
                val availability = checkAvailability(tag, vFilter)
                val vAlpha = when (availability) {
                    VersionAvailability.NOT_YET_ADDED -> 0.5f
                    VersionAvailability.REMOVED, VersionAvailability.WRONG_EDITION -> 0.4f
                    else -> 1f
                }
                val addedIn = tag?.let { if (vFilter.edition == "java") it.addedInJava else it.addedInBedrock } ?: ""
                val isExpanded = b.id in expandedIds
                val glanceText = when {
                    b.minY != null && b.maxY != null -> "Y ${b.minY} to ${b.maxY}"
                    b.lightLevel > 0 -> "${b.lightLevel} light"
                    b.hasGravity -> "gravity"
                    b.category == "redstone" -> "redstone"
                    else -> "${"%.1f".format(b.hardness)} hard"
                }
                Column(modifier = Modifier.alpha(vAlpha)) {
                    BrowseListItem(
                        headline    = b.name,
                        supporting  = "",
                        leadingIcon = ItemTextures.get(b.id) ?: PixelIcons.Blocks,
                        modifier    = Modifier.clickable { vm.toggleExpanded(b.id) },
                        trailing    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    if (addedIn.isNotBlank() && availability != VersionAvailability.AVAILABLE) {
                                        VersionBadge(addedIn)
                                        Spacer(Modifier.height(2.dp))
                                    }
                                    if (b.category.isNotBlank()) {
                                        CategoryBadge(
                                            label = b.category.replace('_', ' '),
                                            color = blockCategoryColor(b.category),
                                            modifier = Modifier.clickable { vm.setCategory(b.category) },
                                        )
                                        Spacer(Modifier.height(2.dp))
                                    }
                                    Text(
                                        glanceText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                    // Tool icon instead of text
                                    val toolTexId = toolTextureId(b.toolRequired)
                                    val toolIcon = toolTexId?.let { ItemTextures.get(it) }
                                    if (toolIcon != null) {
                                        Spacer(Modifier.height(2.dp))
                                        SpyglassIconImage(
                                            toolIcon, contentDescription = b.toolRequired,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    } else if (b.toolRequired.isNotBlank() && b.toolRequired != "none") {
                                        Spacer(Modifier.height(2.dp))
                                        Text(b.toolRequired, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                val isFav = b.id in favoriteIds
                                IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(b.id, b.name) }, modifier = Modifier.size(32.dp)) {
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
                        BlockDetailContent(b, allRecipes, structures, vm, onItemTap, onBiomeTap, onTradeTap, onStructureTap, tag, vFilter)
                    }
                }
            }
            if (blocks.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No blocks found",
                    subtitle = "Try a different search term",
                )
            }
        }
    }
}

// ── Block detail ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockDetailContent(
    block: BlockEntity,
    allRecipes: Map<String, RecipeEntity>,
    structures: List<StructureEntity>,
    vm: BlocksViewModel,
    onItemTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
    onTradeTap: (String) -> Unit,
    onStructureTap: (String) -> Unit,
    tag: VersionTagEntity? = null,
    vFilter: VersionFilterState = VersionFilterState(),
) {
    val recipesForItem  by vm.recipesForItem(block.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val recipesUsingItem by vm.recipesUsingItem(block.id).collectAsStateWithLifecycle(initialValue = emptyList())

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        // ── Minecraft ID ──
        MinecraftIdRow(block.id)

        // ── Version & Edition info ──
        if (tag != null) {
            VersionEditionSection(tag, vFilter)
        }

        // ── Description ──
        if (block.description.isNotBlank()) {
            Text(block.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // ── Property badges ──
        val badges = buildList {
            if (!block.isObtainable) add("Unobtainable" to NetherRed)
            if (block.isFlammable) add("Flammable" to NetherRed)
            if (block.isTransparent) add("Transparent" to PotionBlue)
            if (block.hasGravity) add("Gravity" to MaterialTheme.colorScheme.primary)
            if (block.isWaterloggable) add("Waterloggable" to PotionBlue)
            if (block.lightLevel > 0) add("Light: ${block.lightLevel}" to Color(0xFFFFA726))
        }
        if (badges.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                badges.forEach { (label, color) ->
                    CategoryBadge(label = label, color = color)
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // ── Stats section ──
        if (block.stackSize != 64) StatRow("Stack Size", "${block.stackSize}")
        StatRow("Hardness", "${block.hardness}")
        StatRow("Blast Resistance", "${block.blastResistance}")

        if (block.toolRequired.isNotBlank() && block.toolRequired != "none") {
            val toolTexId = toolTextureId(block.toolRequired)
            val toolIcon = toolTexId?.let { ItemTextures.get(it) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tool Required", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (toolIcon != null) {
                        SpyglassIconImage(toolIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(formatId(block.toolRequired), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (block.toolLevel.isNotBlank()) {
            StatRow("Min. Tool Tier", formatId(block.toolLevel))
        }

        // ── Light level bar ──
        if (block.lightLevel > 0) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Light Level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text("${block.lightLevel}/15", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (i in 1..15) {
                    val fraction = i / 15f
                    val segmentColor = if (i <= block.lightLevel)
                        Color(
                            red = 0.2f + 0.8f * fraction,
                            green = 0.6f + 0.4f * fraction,
                            blue = 0.1f,
                        )
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(segmentColor),
                    )
                }
            }
        }

        // ── Ore Generation section ──
        if (block.minY != null) {
            SpyglassDivider()
            Text("Ore Generation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Min Y", "${block.minY}")
            if (block.maxY != null) StatRow("Max Y", "${block.maxY}")
            if (block.peakY != null) StatRow("Peak Y", "${block.peakY}")
        }

        // ── Drops section ──
        val drops = block.drops.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (drops.isNotEmpty()) {
            SpyglassDivider()
            if (drops.size == 1 && drops[0] == "none") {
                Text("Drops nothing", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            } else {
                val isSelfDrop = drops.size == 1 && drops[0] == block.id
                if (!isSelfDrop) {
                    Text("Drops", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        drops.forEach { dropId ->
                            AssistChip(
                                onClick = { onItemTap(dropId) },
                                label = { Text(formatId(dropId), style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                                ),
                                border = null,
                            )
                        }
                    }
                }
            }
        }

        // ── Job block link ──
        val profession = BLOCK_TO_PROFESSION[block.id]
        if (profession != null) {
            SpyglassDivider()
            Text("Job Block", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            AssistChip(
                onClick = { onTradeTap(profession.lowercase()) },
                label = { Text("Job block for: $profession", style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
                border = null,
            )
        }

        // ── Compostable items (shown for composter block) ──
        if (block.id == "composter") {
            SpyglassDivider()
            Text("Compostable Items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            CompostData.byChance.forEach { (chance, items) ->
                Text("$chance% chance", style = MaterialTheme.typography.bodySmall, color = Emerald)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items.sorted().forEach { itemId ->
                        AssistChip(
                            onClick = { onItemTap(itemId) },
                            label = { Text(formatId(itemId), style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                val tex = ItemTextures.get(itemId) ?: BlockTextures.get(itemId)
                                if (tex != null) SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Emerald,
                                containerColor = Emerald.copy(alpha = 0.12f),
                            ),
                            border = null,
                        )
                    }
                }
            }
        }

        // ── Found in structures ──
        val matchingStructures = structures.filter { s ->
            s.uniqueBlocks.split(",").any { it.trim() == block.id }
        }
        if (matchingStructures.isNotEmpty()) {
            SpyglassDivider()
            Text("Found in Structures", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                matchingStructures.forEach { structure ->
                    AssistChip(
                        onClick = { onStructureTap(structure.id) },
                        label = { Text(structure.name, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = EnderPurple,
                            containerColor = EnderPurple.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // ── Recipe / Uses pager ──
        SpyglassDivider()
        ItemDetailPager(
            itemId = block.id,
            recipesForItem = recipesForItem,
            recipesUsingItem = recipesUsingItem,
            allRecipes = allRecipes,
            onItemTap = onItemTap,
            onBiomeTap = onBiomeTap,
        )

        // ── Add to todo list ──
        SpyglassDivider()
        AddToTodoSection(itemId = block.id, itemName = block.name)

        // ── Add to shopping list ──
        SpyglassDivider()
        AddToListSection(itemId = block.id, itemName = block.name)

        SpyglassDivider()
        ReportProblemRow(entityType = "Block", entityName = block.name, entityId = block.id)
    }
}
