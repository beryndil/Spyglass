package dev.spyglass.android.browse.items

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import dev.spyglass.android.data.BiomeResourceMap
import dev.spyglass.android.data.CompostData
import dev.spyglass.android.data.db.entities.EnchantEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.ItemEntity
import dev.spyglass.android.data.db.entities.MobEntity
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.db.entities.StructureEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

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

    private val _sortKey = MutableStateFlow("name")
    val sortKey: StateFlow<String> = _sortKey.asStateFlow()

    val items: StateFlow<List<ItemEntity>> = combine(
        _query.debounce(200), _category, _sortKey
    ) { q, cat, sort -> Triple(q, cat, sort) }
    .flatMapLatest { (q, cat, sort) ->
        val flow = if (cat == "all") repo.searchItems(q) else repo.itemsByCategory(cat)
        flow.map { list ->
            when (sort) {
                "durability" -> list.sortedByDescending { it.durability }
                "attack_damage" -> list.sortedByDescending { it.attackDamage.toFloatOrNull() ?: 0f }
                "defense" -> list.sortedByDescending { it.defensePoints }
                "saturation" -> list.sortedByDescending { it.saturation }
                else -> list
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteItems: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("item")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "item", displayName = displayName))
        }
    }

    fun setQuery(q: String)    { _query.value = q }
    fun setCategory(c: String) { _category.value = c }
    fun setSortKey(k: String)  { _sortKey.value = k }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandItem(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    fun recipesForItem(itemId: String): Flow<List<RecipeEntity>> = repo.recipesForItem(itemId)
    fun recipesUsingItem(itemId: String): Flow<List<RecipeEntity>> = repo.recipesUsingIngredient(itemId)

    fun enchantsForItem(enchantTarget: String): Flow<List<EnchantEntity>> {
        if (enchantTarget.isBlank()) return flowOf(emptyList())
        val targets = enchantTarget.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (targets.isEmpty()) return flowOf(emptyList())
        val flows = targets.map { repo.enchantsForTarget(it) }
        return combine(flows) { arrays ->
            arrays.flatMap { it.toList() }
                .distinctBy { it.id }
                .sortedBy { it.name }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatId(id: String): String =
    id.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

@Composable
private fun categoryColor(cat: String) = when (cat) {
    "tools"          -> Color(0xFF00897B)  // Teal
    "weapons"        -> Color(0xFFC62828)  // Red
    "armor"          -> Color(0xFF1565C0)  // Blue
    "food"           -> Color(0xFF558B2F)  // Green
    "materials"      -> Color(0xFFFF8F00)  // Amber
    "mob_drops"      -> NetherRed
    "brewing"        -> Color(0xFF7B1FA2)  // Purple
    "misc"           -> Color(0xFF546E7A)  // Blue-grey
    "decoration"     -> Color(0xFF546E7A)
    "transportation" -> Color(0xFF546E7A)
    else             -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun rarityColor(rarity: String) = when (rarity) {
    "common"    -> MaterialTheme.colorScheme.onSurfaceVariant
    "uncommon"  -> Emerald
    "rare"      -> PotionBlue
    "very_rare" -> EnderPurple
    else        -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun obtainColor(method: String) = when (method) {
    "crafting"       -> MaterialTheme.colorScheme.primary
    "mob_drop"       -> NetherRed
    "mining"         -> MaterialTheme.colorScheme.onSurfaceVariant
    "trading"        -> Emerald
    "fishing"        -> PotionBlue
    "structure_loot" -> EnderPurple
    "farming"        -> Emerald
    "smelting"       -> MaterialTheme.colorScheme.primary
    "bartering"      -> MaterialTheme.colorScheme.primary
    "found"          -> MaterialTheme.colorScheme.secondary
    "composting"     -> Emerald
    else             -> MaterialTheme.colorScheme.secondary
}

private val ITEM_CATEGORIES = listOf("all", "tools", "weapons", "armor", "food", "materials", "mob_drops", "misc")

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
    onEnchantTap: (String) -> Unit = {},
    entityLinkIndex: EntityLinkIndex = EntityLinkIndex(emptyList()),
    vm: ItemsViewModel = viewModel(),
) {
    val query      by vm.query.collectAsStateWithLifecycle()
    val category   by vm.category.collectAsStateWithLifecycle()
    val sortKey    by vm.sortKey.collectAsStateWithLifecycle()
    val items      by vm.items.collectAsStateWithLifecycle()
    val expandedIds by vm.expandedIds.collectAsStateWithLifecycle()
    val structures  by vm.structures.collectAsStateWithLifecycle()
    val breedingMap by vm.breedingMap.collectAsStateWithLifecycle()
    val favoriteIds    by vm.favoriteIds.collectAsStateWithLifecycle()
    val favoriteItems  by vm.favoriteItems.collectAsStateWithLifecycle()
    val listState   = rememberLazyListState()
    val itemSortOptions = remember { listOf(
        SortOption("Name A\u2192Z", "name"),
        SortOption("Durability \u2193", "durability"),
        SortOption("Attack Damage \u2193", "attack_damage"),
        SortOption("Defense \u2193", "defense"),
        SortOption("Saturation \u2193", "saturation"),
    ) }

    // Auto-expand and scroll to target item from cross-reference
    LaunchedEffect(targetItemId) {
        if (targetItemId != null) {
            vm.setQuery("")
            vm.setCategory("all")
            snapshotFlow { items }
                .first { it.isNotEmpty() }
            val idx = items.indexOfFirst { it.id == targetItemId }
            if (idx >= 0) {
                listState.scrollToItem(idx + 1) // +1 for intro header
                vm.expandItem(targetItemId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query, onValueChange = vm::setQuery,
                placeholder = { Text("Search items\u2026", color = MaterialTheme.colorScheme.secondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
            )
            SortButton(options = itemSortOptions, selectedKey = sortKey, onSelect = vm::setSortKey)
        }

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(ITEM_CATEGORIES, key = { it }) { c ->
                val chipColor = if (c == "all") MaterialTheme.colorScheme.primary else categoryColor(c)
                FilterChip(
                    selected = category == c,
                    onClick = { vm.setCategory(c) },
                    label = {
                        Text(
                            if (c == "all") stringResource(R.string.all) else c.replace('_', ' ').replaceFirstChar { it.uppercase() },
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
            if (favoriteItems.isNotEmpty()) {
                item(key = "fav_header") {
                    SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
                }
                items(favoriteItems, key = { "fav_${it.id}" }) { fav ->
                    val isFav = fav.id in favoriteIds
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        leadingIcon = ItemTextures.get(fav.id) ?: PixelIcons.Item,
                        modifier = Modifier.clickable { vm.toggleExpanded(fav.id) },
                        trailing = {
                            IconButton(onClick = { vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
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
            items(items, key = { it.id }) { item ->
                val isExpanded = item.id in expandedIds
                Column {
                    val texture = ItemTextures.get(item.id)
                    val glanceText = when (item.category) {
                        "weapons" -> if (item.attackDamage.isNotBlank()) "${item.attackDamage} dmg" else null
                        "armor"   -> if (item.defensePoints > 0) "${item.defensePoints} def" else null
                        "food"    -> if (item.saturation > 0f) "${"%.1f".format(item.saturation)} sat" else null
                        "tools"   -> if (item.durability > 0) "\u2764 ${item.durability}" else null
                        else      -> if (item.durability > 0) "\u2764 ${item.durability}" else null
                    }
                    BrowseListItem(
                        headline    = item.name,
                        supporting  = "",
                        leadingIcon = texture ?: PixelIcons.Item,
                        modifier    = Modifier.clickable { vm.toggleExpanded(item.id) },
                        trailing    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    CategoryBadge(
                                        label = item.category.replace('_', ' '),
                                        color = categoryColor(item.category),
                                        modifier = Modifier.clickable { vm.setCategory(item.category) },
                                    )
                                    if (glanceText != null) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            glanceText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                val isFav = item.id in favoriteIds
                                IconButton(onClick = { vm.toggleFavorite(item.id, item.name) }, modifier = Modifier.size(32.dp)) {
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
                        ItemDetailCard(
                            item = item,
                            structures = structures,
                            breedingMap = breedingMap,
                            vm = vm,
                            onMobTap = onMobTap,
                            onBlockTap = onBlockTap,
                            onItemTap = onItemTap,
                            onStructureTap = onStructureTap,
                            onBiomeTap = onBiomeTap,
                            onEnchantTap = onEnchantTap,
                            entityLinkIndex = entityLinkIndex,
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
    structures: List<StructureEntity>,
    breedingMap: Map<String, List<String>>,
    vm: ItemsViewModel,
    onMobTap: (String) -> Unit,
    onBlockTap: (String) -> Unit,
    onItemTap: (String) -> Unit,
    onStructureTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
    onEnchantTap: (String) -> Unit,
    entityLinkIndex: EntityLinkIndex,
) {
    val recipesFor  by vm.recipesForItem(item.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val recipesUsing by vm.recipesUsingItem(item.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val enchants    by vm.enchantsForItem(item.enchantTarget).collectAsStateWithLifecycle(initialValue = emptyList())

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        // ── Enhanced Header ──────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            val texture = ItemTextures.get(item.id)
            if (texture != null) {
                SpyglassIconImage(texture, contentDescription = item.name, modifier = Modifier.size(48.dp), tint = Color.Unspecified)
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                // Badges row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CategoryBadge(
                        label = item.category.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        color = categoryColor(item.category),
                    )
                    if (item.stackSize != 64) {
                        CategoryBadge(label = "Stack: ${item.stackSize}", color = MaterialTheme.colorScheme.secondary)
                    }
                    if (item.isRenewable) {
                        CategoryBadge(label = "Renewable", color = Emerald)
                    } else {
                        CategoryBadge(label = "Non-renewable", color = NetherRed)
                    }
                }
            }
        }

        // Minecraft ID
        MinecraftIdRow(item.id)

        // Description
        if (item.description.isNotEmpty()) {
            LinkedDescription(
                description = item.description,
                linkIndex = entityLinkIndex,
                selfId = item.id,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onBiomeTap = onBiomeTap,
                onStructureTap = onStructureTap,
                onEnchantTap = onEnchantTap,
            )
        }

        // How to Obtain
        val sources = item.obtainedFrom.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (sources.isNotEmpty()) {
            SpyglassDivider()
            Text("How to Obtain", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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

        // ── Conditional: Tools & Weapons Combat Stats ────────────────────
        if (item.category in listOf("tools", "weapons") && (item.durability > 0 || item.attackDamage.isNotBlank())) {
            SpyglassDivider()
            Text("Combat Stats", style = MaterialTheme.typography.labelSmall, color = categoryColor(item.category))
            if (item.durability > 0) StatRow("Durability", "${item.durability}")
            if (item.attackDamage.isNotBlank()) {
                StatRow("Attack Damage", item.attackDamage)
            }
            if (item.attackSpeed.isNotBlank()) {
                StatRow("Attack Speed", item.attackSpeed)
                // DPS calculation
                val dmg = item.attackDamage.toFloatOrNull()
                val spd = item.attackSpeed.toFloatOrNull()
                if (dmg != null && spd != null) {
                    StatRow("DPS", "${"%.1f".format(dmg * spd)}")
                }
            }
            if (item.enchantability > 0) StatRow("Enchantability", "${item.enchantability}")
        }

        // ── Conditional: Armor Stats ─────────────────────────────────────
        if (item.category == "armor" && (item.defensePoints > 0 || item.durability > 0)) {
            SpyglassDivider()
            Text("Armor Stats", style = MaterialTheme.typography.labelSmall, color = categoryColor("armor"))
            if (item.defensePoints > 0) StatRow("Defense Points", "${item.defensePoints}")
            if (item.armorToughness > 0f) StatRow("Armor Toughness", "${"%.1f".format(item.armorToughness)}")
            if (item.knockbackResistance > 0f) StatRow("Knockback Resistance", "${"%.1f".format(item.knockbackResistance)}")
            if (item.durability > 0) StatRow("Durability", "${item.durability}")
            if (item.enchantability > 0) StatRow("Enchantability", "${item.enchantability}")
        }

        // ── Conditional: Food Stats ──────────────────────────────────────
        if (item.category == "food" && item.hunger > 0) {
            SpyglassDivider()
            Text("Food Stats", style = MaterialTheme.typography.labelSmall, color = categoryColor("food"))
            StatRow("Hunger", "${item.hunger}")
            StatRow("Saturation", "${"%.1f".format(item.saturation)}")
            if (item.hunger > 0) {
                val efficiency = item.saturation / item.hunger
                StatRow("Efficiency", "${"%.2f".format(efficiency)}")
            }
            if (item.foodEffect.isNotBlank()) {
                StatRow("Effects", item.foodEffect)
            }
        }

        // ── Compostable ──────────────────────────────────────────────────
        val compostChance = CompostData.chanceFor(item.id)
        if (compostChance != null) {
            SpyglassDivider()
            Text("Uses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Emerald.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .border(0.5.dp, Emerald.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .clickable { onBlockTap("composter") }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val composterIcon = BlockTextures.get("composter")
                if (composterIcon != null) {
                    SpyglassIconImage(composterIcon, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Compostable", style = MaterialTheme.typography.bodyMedium, color = Emerald)
                    Text("$compostChance% chance per item", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // ── Applicable Enchantments ──────────────────────────────────────
        if (enchants.isNotEmpty()) {
            SpyglassDivider()
            Text("Applicable Enchantments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                enchants.forEach { enchant ->
                    val rColor = rarityColor(enchant.rarity)
                    AssistChip(
                        onClick = { onEnchantTap(enchant.id) },
                        label = {
                            Text(
                                "${enchant.name} ${enchant.maxLevel}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = rColor,
                            containerColor = rColor.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // ── Recipe (if craftable) ────────────────────────────────────────
        if (recipesFor.isNotEmpty()) {
            SpyglassDivider()
            Text("Recipe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            recipesFor.forEach { recipe ->
                if (recipe.type.contains("shaped")) {
                    TextureCraftingGrid(recipe = recipe, onItemTap = onItemTap)
                }
                Text(
                    if (recipe.type.contains("crafting")) "Crafting"
                    else recipe.type.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // ── Dropped by (mob chips) ───────────────────────────────────────
        val droppedBy = item.droppedBy.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (droppedBy.isNotEmpty()) {
            SpyglassDivider()
            Text("Dropped by", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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

        // ── Used to breed ────────────────────────────────────────────────
        val breedableMobs = breedingMap[item.id].orEmpty()
        if (breedableMobs.isNotEmpty()) {
            SpyglassDivider()
            Text("Used to Breed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                            SpyglassIconImage(mobIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Unspecified)
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

        // ── Mined from (block chips) ─────────────────────────────────────
        val minedFrom = item.minedFrom.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (minedFrom.isNotEmpty()) {
            SpyglassDivider()
            Text("Mined from", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                minedFrom.forEach { blockId ->
                    AssistChip(
                        onClick = { onBlockTap(blockId) },
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

        // ── Found in biomes ──────────────────────────────────────────────
        val biomes = BiomeResourceMap.biomesForItem(item.id)
        if (biomes.isNotEmpty()) {
            SpyglassDivider()
            Text("Found in Biomes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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

        // ── Found in structures ──────────────────────────────────────────
        val matchingStructures = structures.filter { s ->
            s.loot.split(",").any { it.trim() == item.id }
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

        // ── Used in recipes (max 8 + overflow) ──────────────────────────
        if (recipesUsing.isNotEmpty()) {
            SpyglassDivider()
            Text("Used in ${recipesUsing.size} recipe(s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                recipesUsing.forEach { recipe ->
                    AssistChip(
                        onClick = { onItemTap(recipe.outputItem) },
                        label = { Text(formatId(recipe.outputItem), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // ── Add to todo list ─────────────────────────────────────────────
        SpyglassDivider()
        AddToTodoSection(itemId = item.id, itemName = item.name)

        // ── Add to shopping list ─────────────────────────────────────────
        SpyglassDivider()
        AddToListSection(itemId = item.id, itemName = item.name)
    }
}
