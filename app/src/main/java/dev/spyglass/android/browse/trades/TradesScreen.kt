package dev.spyglass.android.browse.trades

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.ItemTags
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.TradeEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Job block data ──────────────────────────────────────────────────────────

private data class JobBlockInfo(
    val blockName: String,
    val blockId: String,
    val grid: List<String?>,        // 9 cells (row-major), null = empty
    val ingredients: List<String>,  // display names with counts
)

private val JOB_BLOCKS = mapOf(
    "armorer" to JobBlockInfo(
        "Blast Furnace", "blast_furnace",
        listOf("iron_ingot","iron_ingot","iron_ingot", "iron_ingot","furnace","iron_ingot", "smooth_stone","smooth_stone","smooth_stone"),
        listOf("5× Iron Ingot", "1× Furnace", "3× Smooth Stone"),
    ),
    "butcher" to JobBlockInfo(
        "Smoker", "smoker",
        listOf(null,"oak_log",null, "oak_log","furnace","oak_log", null,"oak_log",null),
        listOf("4× Any Log", "1× Furnace"),
    ),
    "cartographer" to JobBlockInfo(
        "Cartography Table", "cartography_table",
        listOf("paper","paper",null, "oak_planks","oak_planks",null, "oak_planks","oak_planks",null),
        listOf("2× Paper", "4× Any Planks"),
    ),
    "cleric" to JobBlockInfo(
        "Brewing Stand", "brewing_stand",
        listOf(null,"blaze_rod",null, "cobblestone","cobblestone","cobblestone", null,null,null),
        listOf("1× Blaze Rod", "3× Cobblestone"),
    ),
    "farmer" to JobBlockInfo(
        "Composter", "composter",
        listOf("oak_slab",null,"oak_slab", "oak_slab",null,"oak_slab", "oak_slab","oak_slab","oak_slab"),
        listOf("7× Any Wooden Slab"),
    ),
    "fisherman" to JobBlockInfo(
        "Barrel", "barrel",
        listOf("oak_planks","oak_slab","oak_planks", "oak_planks",null,"oak_planks", "oak_planks","oak_slab","oak_planks"),
        listOf("6× Any Planks", "2× Any Wooden Slab"),
    ),
    "fletcher" to JobBlockInfo(
        "Fletching Table", "fletching_table",
        listOf("flint","flint",null, "oak_planks","oak_planks",null, "oak_planks","oak_planks",null),
        listOf("2× Flint", "4× Any Planks"),
    ),
    "leatherworker" to JobBlockInfo(
        "Cauldron", "cauldron",
        listOf("iron_ingot",null,"iron_ingot", "iron_ingot",null,"iron_ingot", "iron_ingot","iron_ingot","iron_ingot"),
        listOf("7× Iron Ingot"),
    ),
    "librarian" to JobBlockInfo(
        "Lectern", "lectern",
        listOf("oak_slab","oak_slab","oak_slab", null,"bookshelf",null, null,"oak_slab",null),
        listOf("4× Any Wooden Slab", "1× Bookshelf"),
    ),
    "mason" to JobBlockInfo(
        "Stonecutter", "stonecutter",
        listOf(null,"iron_ingot",null, "stone","stone","stone", null,null,null),
        listOf("1× Iron Ingot", "3× Stone"),
    ),
    "shepherd" to JobBlockInfo(
        "Loom", "loom",
        listOf("string","string",null, "oak_planks","oak_planks",null, null,null,null),
        listOf("2× String", "2× Any Planks"),
    ),
    "toolsmith" to JobBlockInfo(
        "Smithing Table", "smithing_table",
        listOf("iron_ingot","iron_ingot",null, "oak_planks","oak_planks",null, "oak_planks","oak_planks",null),
        listOf("2× Iron Ingot", "4× Any Planks"),
    ),
    "weaponsmith" to JobBlockInfo(
        "Grindstone", "grindstone",
        listOf("stick","stone_slab","stick", "oak_planks",null,"oak_planks", null,null,null),
        listOf("2× Stick", "1× Stone Slab", "2× Any Planks"),
    ),
)

// ── ViewModel ───────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TradesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query      = MutableStateFlow("")
    private val _profession = MutableStateFlow("all")
    val query:      StateFlow<String> = _query.asStateFlow()
    val profession: StateFlow<String> = _profession.asStateFlow()

    val trades: StateFlow<List<TradeEntity>> = combine(_query.debounce(200), _profession) { q, prof ->
        if (prof == "all") repo.searchTrades(q) else repo.tradesByProfession(prof)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setProfession(p: String) { _profession.value = p }

    val professions = listOf("all", "armorer", "butcher", "cartographer", "cleric",
        "farmer", "fisherman", "fletcher", "leatherworker", "librarian",
        "mason", "shepherd", "toolsmith", "weaponsmith")

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteTrades: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("trade")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "trade", displayName = displayName))
        }
    }
}

// ── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun TradesScreen(
    targetProfession: String? = null,
    onItemTap: (String) -> Unit = {},
    vm: TradesViewModel = viewModel(),
) {
    val query      by vm.query.collectAsStateWithLifecycle()
    val profession by vm.profession.collectAsStateWithLifecycle()
    val trades     by vm.trades.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val favoriteTrades by vm.favoriteTrades.collectAsStateWithLifecycle()

    // Auto-select profession when navigated from a job block
    LaunchedEffect(targetProfession) {
        if (targetProfession != null) {
            vm.setProfession(targetProfession)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search trades…", color = MaterialTheme.colorScheme.secondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            items(vm.professions, key = { it }) { p ->
                FilterChip(selected = profession == p, onClick = { vm.setProfession(p) },
                    label = { Text(if (p == "all") stringResource(R.string.all) else p.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Trade,
                    title = "Trades",
                    description = "Villager trades by profession and level",
                    stat = "${trades.size} trades",
                    iconTint = Color.Unspecified,
                )
            }

            // Show job block card when a specific profession is selected
            if (profession != "all") {
                JOB_BLOCKS[profession]?.let { info ->
                    item(key = "jobblock_$profession") {
                        JobBlockCard(info)
                    }
                }
            }

            if (favoriteTrades.isNotEmpty()) {
                item(key = "fav_header") {
                    Text(stringResource(R.string.favorites), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(favoriteTrades, key = { "fav_${it.id}" }) { fav ->
                    BrowseListItem(
                        headline    = fav.displayName,
                        supporting  = "",
                        supportingMaxLines = 1,
                        leadingIcon = PixelIcons.Trade,
                        trailing    = {
                            IconButton(onClick = { vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
            }
            items(trades, key = { it.rowId }) { t ->
                TradeListItem(t, onItemTap, favoriteIds, vm::toggleFavorite)
            }
            if (trades.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No trades found",
                    subtitle = "Try a different search or profession",
                )
            }
        }
    }
}

// ── Job block card ──────────────────────────────────────────────────────────

@Composable
private fun JobBlockCard(info: JobBlockInfo) {
    val blockIcon = ItemTextures.get(info.blockId) ?: BlockTextures.get(info.blockId)

    ResultCard {
        // Header: block icon + name
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (blockIcon != null) {
                SpyglassIconImage(
                    blockIcon, contentDescription = info.blockName,
                    modifier = Modifier.size(28.dp), tint = Color.Unspecified,
                )
                Spacer(Modifier.width(10.dp))
            }
            Column {
                Text("Job Block", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(info.blockName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        SpyglassDivider()

        // Crafting grid + ingredient list side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            // 3×3 crafting grid with textures
            CraftingGrid(info.grid)

            Spacer(Modifier.width(16.dp))

            // Ingredient list
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Ingredients", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                info.ingredients.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Trade list item with inline item icons ──────────────────────────────────

private fun formatItemName(id: String): String =
    id.substringAfterLast(':').replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun TradeListItem(trade: TradeEntity, onItemTap: (String) -> Unit, favoriteIds: Set<String>, onToggleFavorite: (String, String) -> Unit) {
    val buy1Id = trade.buyItem1.substringAfterLast(':')
    val buy1Icon = ItemTextures.get(buy1Id)
    val buy2Id = if (trade.buyItem2.isNotBlank()) trade.buyItem2.substringAfterLast(':') else null
    val buy2Icon = buy2Id?.let { ItemTextures.get(it) }
    val sellId = trade.sellItem.substringAfterLast(':')
    val sellIcon = ItemTextures.get(sellId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalSurfaceCard.current, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Buy + Sell items with icons
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Buy item 1
                TradeItemChip(buy1Icon, "${trade.buyItem1Count}\u00D7 ${formatItemName(buy1Id)}",
                    onClick = { onItemTap(buy1Id) })

                // Buy item 2 (if present)
                if (buy2Id != null) {
                    Text(" + ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    TradeItemChip(buy2Icon, "${trade.buyItem2Count}\u00D7 ${formatItemName(buy2Id)}",
                        onClick = { onItemTap(buy2Id) })
                }

                Text("  \u2192  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                // Sell item
                TradeItemChip(sellIcon, "${trade.sellItemCount}\u00D7 ${formatItemName(sellId)}",
                    onClick = { onItemTap(sellId) })
            }
            Spacer(Modifier.height(2.dp))
            Row {
                Text(
                    "${trade.profession.replaceFirstChar { it.uppercase() }} \u00B7 ${trade.levelName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (trade.maxUses > 0) {
                    Text(
                        " \u00B7 ${trade.maxUses}/restock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        CategoryBadge(label = "Lvl ${trade.level}", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        val tradeKey = "trade_${trade.rowId}"
        val isFav = tradeKey in favoriteIds
        IconButton(onClick = { onToggleFavorite(tradeKey, "${formatItemName(trade.sellItem)} trade") }, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Star,
                contentDescription = stringResource(R.string.favorite),
                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TradeItemChip(icon: SpyglassIcon?, label: String, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() },
    ) {
        if (icon != null) {
            SpyglassIconImage(
                icon, contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Unspecified,
            )
            Spacer(Modifier.width(3.dp))
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ── 3×3 crafting grid with item textures ────────────────────────────────────

@Composable
private fun CraftingGrid(cells: List<String?>) {
    Column {
        for (row in 0..2) {
            Row {
                for (col in 0..2) {
                    val itemId = cells.getOrNull(row * 3 + col)
                    val tag = itemId?.let { ItemTags.tagForItem(it) }
                    val icon = itemId?.let { ItemTextures.get(it) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                if (itemId != null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
                                RoundedCornerShape(2.dp),
                            )
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
                    ) {
                        if (tag != null) {
                            RotatingTagIcon(tag, modifier = Modifier.size(22.dp))
                        } else if (icon != null) {
                            SpyglassIconImage(
                                icon, contentDescription = itemId,
                                modifier = Modifier.size(22.dp),
                                tint = Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }
    }
}
