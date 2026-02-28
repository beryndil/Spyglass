package dev.spyglass.android.browse.blocks

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.CompostData
import dev.spyglass.android.data.db.entities.BlockEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.db.entities.StructureEntity
import dev.spyglass.android.data.repository.GameDataRepository
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

    val blocks: StateFlow<List<BlockEntity>> = combine(_query.debounce(200), _category) { q, cat ->
        if (cat == "all") repo.searchBlocks(q) else repo.blocksByCategory(cat)
    }.flatMapLatest { it }
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
    val query       by vm.query.collectAsState()
    val category    by vm.category.collectAsState()
    val blocks      by vm.blocks.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val allRecipes  by vm.allRecipes.collectAsState()
    val structures  by vm.structures.collectAsState()
    val favoriteIds    by vm.favoriteIds.collectAsState()
    val favoriteBlocks by vm.favoriteBlocks.collectAsState()
    val listState   = rememberLazyListState()

    // Auto-expand and scroll to target block from cross-reference
    LaunchedEffect(targetBlockId, blocks) {
        if (targetBlockId != null && blocks.isNotEmpty()) {
            vm.setQuery("")
            vm.setCategory("all")
            val idx = blocks.indexOfFirst { it.id == targetBlockId }
            if (idx >= 0) {
                listState.scrollToItem(idx + 1) // +1 for intro header
                vm.expandBlock(targetBlockId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        // Category filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(BLOCK_CATEGORIES) { c ->
                val chipColor = if (c == "all") MaterialTheme.colorScheme.primary else blockCategoryColor(c)
                FilterChip(
                    selected = category == c,
                    onClick = { vm.setCategory(c) },
                    label = {
                        Text(
                            c.replaceFirstChar { it.uppercase() },
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
                    SectionHeader("Favorites", icon = PixelIcons.Bookmark)
                }
                items(favoriteBlocks, key = { "fav_${it.id}" }) { fav ->
                    val isFav = fav.id in favoriteIds
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        leadingIcon = ItemTextures.get(fav.id) ?: PixelIcons.Blocks,
                        modifier = Modifier.clickable { vm.toggleExpanded(fav.id) },
                        trailing = {
                            IconButton(onClick = { vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "Favorite",
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
                val isExpanded = b.id in expandedIds
                Column {
                    BrowseListItem(
                        headline    = b.name,
                        supporting  = "",
                        leadingIcon = ItemTextures.get(b.id) ?: PixelIcons.Blocks,
                        modifier    = Modifier.clickable { vm.toggleExpanded(b.id) },
                        trailing    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    if (b.category.isNotBlank()) {
                                        CategoryBadge(
                                            label = b.category.replace('_', ' '),
                                            color = blockCategoryColor(b.category),
                                            modifier = Modifier.clickable { vm.setCategory(b.category) },
                                        )
                                        Spacer(Modifier.height(2.dp))
                                    }
                                    // Tool icon instead of text
                                    val toolTexId = toolTextureId(b.toolRequired)
                                    val toolIcon = toolTexId?.let { ItemTextures.get(it) }
                                    if (toolIcon != null) {
                                        SpyglassIconImage(
                                            toolIcon, contentDescription = b.toolRequired,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    } else if (b.toolRequired.isNotBlank() && b.toolRequired != "none") {
                                        Text(b.toolRequired, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                val isFav = b.id in favoriteIds
                                IconButton(onClick = { vm.toggleFavorite(b.id, b.name) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "Favorite",
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
                        BlockDetailContent(b, allRecipes, structures, vm, onItemTap, onBiomeTap, onTradeTap, onStructureTap)
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
) {
    val recipesForItem  by vm.recipesForItem(block.id).collectAsState(initial = emptyList())
    val recipesUsingItem by vm.recipesUsingItem(block.id).collectAsState(initial = emptyList())

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        // ── Minecraft ID ──
        MinecraftIdRow(block.id)

        // ── Stats section ──
        if (block.stackSize != 64) StatRow("Stack Size", "${block.stackSize}")
        StatRow("Hardness", "${block.hardness}")

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
        if (block.isFlammable) StatRow("Flammable", "Yes")
        if (block.isTransparent) StatRow("Transparent", "Yes")

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
    }
}
