package dev.spyglass.android.browse.items

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.BiomeResourceMap
import dev.spyglass.android.data.db.entities.ItemEntity
import dev.spyglass.android.data.db.entities.MobEntity
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.db.entities.StructureEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

// ── ViewModel ────────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ItemsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query    = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query:    StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val items: StateFlow<List<ItemEntity>> = combine(_query.debounce(200), _category) { q, cat ->
        if (cat == "all") repo.searchItems(q) else repo.itemsByCategory(cat)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<Map<String, RecipeEntity>> = repo.searchRecipes("")
        .map { list -> list.associateBy { it.outputItem } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val structures: StateFlow<List<StructureEntity>> = repo.searchStructures("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val breedingMap: StateFlow<Map<String, List<String>>> = repo.searchMobs("")
        .map { mobs ->
            val map = mutableMapOf<String, MutableList<String>>()
            mobs.forEach { mob ->
                if (mob.breeding.isNotEmpty()) {
                    mob.breeding.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { food ->
                        map.getOrPut(food) { mutableListOf() }.add(mob.id)
                    }
                }
            }
            map
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setQuery(q: String)    { _query.value = q }
    fun setCategory(c: String) { _category.value = c }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandItem(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    fun recipesForItem(itemId: String): Flow<List<RecipeEntity>> = repo.recipesForItem(itemId)
    fun recipesUsingItem(itemId: String): Flow<List<RecipeEntity>> = repo.recipesUsingIngredient(itemId)
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatId(id: String): String =
    id.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun categoryColor(cat: String) = when (cat) {
    "tools"     -> Gold
    "weapons"   -> NetherRed
    "armor"     -> PotionBlue
    "food"      -> Emerald
    "materials" -> Stone300
    "mob_drops" -> NetherRed
    "brewing"   -> EnderPurple
    "misc"      -> Stone500
    else        -> Stone500
}

private fun obtainColor(method: String) = when (method) {
    "crafting"       -> Gold
    "mob_drop"       -> NetherRed
    "mining"         -> Stone300
    "trading"        -> Emerald
    "fishing"        -> PotionBlue
    "structure_loot" -> EnderPurple
    "farming"        -> Emerald
    "smelting"       -> Gold
    "bartering"      -> Gold
    "found"          -> Stone500
    "composting"     -> Emerald
    else             -> Stone500
}

private val ITEM_CATEGORIES = listOf("all", "tools", "weapons", "armor", "food", "materials", "mob_drops", "brewing", "misc")

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemsScreen(
    targetItemId: String? = null,
    onMobTap: (String) -> Unit = {},
    onBlockTap: (String) -> Unit = {},
    onItemTap: (String) -> Unit = {},
    onStructureTap: (String) -> Unit = {},
    onBiomeTap: (String) -> Unit = {},
    vm: ItemsViewModel = viewModel(),
) {
    val query      by vm.query.collectAsState()
    val category   by vm.category.collectAsState()
    val items      by vm.items.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val allRecipes  by vm.allRecipes.collectAsState()
    val structures  by vm.structures.collectAsState()
    val breedingMap by vm.breedingMap.collectAsState()
    val listState   = rememberLazyListState()

    // Auto-expand and scroll to target item from cross-reference
    LaunchedEffect(targetItemId, items) {
        if (targetItemId != null && items.isNotEmpty()) {
            vm.expandItem(targetItemId)
            val idx = items.indexOfFirst { it.id == targetItemId }
            if (idx >= 0) listState.animateScrollToItem(idx + 1) // +1 for intro header
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search items\u2026", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(ITEM_CATEGORIES) { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { vm.setCategory(c) },
                    label = {
                        Text(
                            c.replace('_', ' ').replaceFirstChar { it.uppercase() },
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
                    icon = PixelIcons.Item,
                    title = "Items",
                    description = "Non-block items: tools, weapons, armor, food, materials, and more — with sources and cross-links",
                    stat = "${items.size} items",
                )
            }
            items(items, key = { it.id }) { item ->
                val isExpanded = item.id in expandedIds
                Column {
                    val texture = ItemTextures.get(item.id)
                    BrowseListItem(
                        headline    = item.name,
                        supporting  = item.id,
                        leadingIcon = texture ?: PixelIcons.Item,
                        modifier    = Modifier.clickable { vm.toggleExpanded(item.id) },
                        trailing    = {
                            Column(horizontalAlignment = Alignment.End) {
                                CategoryBadge(
                                    label = item.category.replace('_', ' '),
                                    color = categoryColor(item.category),
                                )
                                if (item.durability > 0) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "\u2764 ${item.durability}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Stone500,
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
                        ItemDetailCard(
                            item = item,
                            allRecipes = allRecipes,
                            structures = structures,
                            breedingMap = breedingMap,
                            vm = vm,
                            onMobTap = onMobTap,
                            onBlockTap = onBlockTap,
                            onItemTap = onItemTap,
                            onStructureTap = onStructureTap,
                            onBiomeTap = onBiomeTap,
                        )
                    }
                }
            }
            if (items.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No items found",
                    subtitle = "Try a different search or category",
                )
            }
        }
    }
}

// ── Detail card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemDetailCard(
    item: ItemEntity,
    allRecipes: Map<String, RecipeEntity>,
    structures: List<StructureEntity>,
    breedingMap: Map<String, List<String>>,
    vm: ItemsViewModel,
    onMobTap: (String) -> Unit,
    onBlockTap: (String) -> Unit,
    onItemTap: (String) -> Unit,
    onStructureTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
) {
    val recipesFor  by vm.recipesForItem(item.id).collectAsState(initial = emptyList())
    val recipesUsing by vm.recipesUsingItem(item.id).collectAsState(initial = emptyList())

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        // 1. Description
        if (item.description.isNotEmpty()) {
            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = Stone300)
        }

        // 2. Stats
        StatRow("Stack Size", "${item.stackSize}")
        if (item.durability > 0) StatRow("Durability", "${item.durability}")
        StatRow("Category", item.category.replace('_', ' ').replaceFirstChar { it.uppercase() })

        // 3. How to Obtain
        val sources = item.obtainedFrom.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (sources.isNotEmpty()) {
            SpyglassDivider()
            Text("How to Obtain", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                sources.forEach { source ->
                    CategoryBadge(
                        label = source.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        color = obtainColor(source),
                    )
                }
            }
        }

        // 4. Recipe (if craftable)
        if (recipesFor.isNotEmpty()) {
            SpyglassDivider()
            Text("Recipe", style = MaterialTheme.typography.labelSmall, color = Gold)
            recipesFor.forEach { recipe ->
                if (recipe.type.contains("shaped")) {
                    TextureCraftingGrid(recipe = recipe, onItemTap = onItemTap)
                }
                // Recipe type badge
                Text(
                    recipe.type.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone500,
                )
            }
        }

        // 5. Dropped by (mob chips)
        val droppedBy = item.droppedBy.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (droppedBy.isNotEmpty()) {
            SpyglassDivider()
            Text("Dropped by", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                droppedBy.forEach { mobId ->
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

        // 6. Used to breed (reverse lookup from breeding map)
        val breedableMobs = breedingMap[item.id].orEmpty()
        if (breedableMobs.isNotEmpty()) {
            SpyglassDivider()
            Text("Used to Breed", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                breedableMobs.forEach { mobId ->
                    val mobIcon = MobTextures.get(mobId)
                    AssistChip(
                        onClick = { onMobTap(mobId) },
                        label = { Text(formatId(mobId), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (mobIcon != null) { {
                            SpyglassIconImage(mobIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = androidx.compose.ui.graphics.Color.Unspecified)
                        } } else null,
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Emerald,
                            containerColor = Emerald.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // 7. Mined from (block chips)
        val minedFrom = item.minedFrom.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (minedFrom.isNotEmpty()) {
            SpyglassDivider()
            Text("Mined from", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                minedFrom.forEach { blockId ->
                    AssistChip(
                        onClick = { onBlockTap(blockId) },
                        label = { Text(formatId(blockId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Stone300,
                            containerColor = Stone300.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // 7. Found in biomes (from BiomeResourceMap)
        val biomes = BiomeResourceMap.biomesForItem(item.id)
        if (biomes.isNotEmpty()) {
            SpyglassDivider()
            Text("Found in Biomes", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
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

        // 8. Found in structures (reverse lookup)
        val matchingStructures = structures.filter { s ->
            s.loot.split(",").any { it.trim() == item.id }
        }
        if (matchingStructures.isNotEmpty()) {
            SpyglassDivider()
            Text("Found in Structures", style = MaterialTheme.typography.labelSmall, color = Gold)
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

        // 9. Used in recipes
        if (recipesUsing.isNotEmpty()) {
            SpyglassDivider()
            Text("Used in ${recipesUsing.size} recipe(s)", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                recipesUsing.take(10).forEach { recipe ->
                    AssistChip(
                        onClick = { onItemTap(recipe.outputItem) },
                        label = { Text(formatId(recipe.outputItem), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Gold,
                            containerColor = Gold.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }
    }
}
