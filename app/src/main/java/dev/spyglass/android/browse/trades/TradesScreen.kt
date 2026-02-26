package dev.spyglass.android.browse.trades

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import dev.spyglass.android.data.db.entities.TradeEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

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
        listOf("4× Oak Log", "1× Furnace"),
    ),
    "cartographer" to JobBlockInfo(
        "Cartography Table", "cartography_table",
        listOf("paper","paper",null, "oak_planks","oak_planks",null, "oak_planks","oak_planks",null),
        listOf("2× Paper", "4× Oak Planks"),
    ),
    "cleric" to JobBlockInfo(
        "Brewing Stand", "brewing_stand",
        listOf(null,"blaze_rod",null, "cobblestone","cobblestone","cobblestone", null,null,null),
        listOf("1× Blaze Rod", "3× Cobblestone"),
    ),
    "farmer" to JobBlockInfo(
        "Composter", "composter",
        listOf("oak_slab",null,"oak_slab", "oak_slab",null,"oak_slab", "oak_slab","oak_slab","oak_slab"),
        listOf("7× Oak Slab"),
    ),
    "fisherman" to JobBlockInfo(
        "Barrel", "barrel",
        listOf("oak_planks","oak_slab","oak_planks", "oak_planks",null,"oak_planks", "oak_planks","oak_slab","oak_planks"),
        listOf("6× Oak Planks", "2× Oak Slab"),
    ),
    "fletcher" to JobBlockInfo(
        "Fletching Table", "fletching_table",
        listOf("flint","flint",null, "oak_planks","oak_planks",null, "oak_planks","oak_planks",null),
        listOf("2× Flint", "4× Oak Planks"),
    ),
    "leatherworker" to JobBlockInfo(
        "Cauldron", "cauldron",
        listOf("iron_ingot",null,"iron_ingot", "iron_ingot",null,"iron_ingot", "iron_ingot","iron_ingot","iron_ingot"),
        listOf("7× Iron Ingot"),
    ),
    "librarian" to JobBlockInfo(
        "Lectern", "lectern",
        listOf("oak_slab","oak_slab","oak_slab", null,"bookshelf",null, null,"oak_slab",null),
        listOf("4× Oak Slab", "1× Bookshelf"),
    ),
    "mason" to JobBlockInfo(
        "Stonecutter", "stonecutter",
        listOf(null,"iron_ingot",null, "stone","stone","stone", null,null,null),
        listOf("1× Iron Ingot", "3× Stone"),
    ),
    "shepherd" to JobBlockInfo(
        "Loom", "loom",
        listOf("string","string",null, "oak_planks","oak_planks",null, null,null,null),
        listOf("2× String", "2× Oak Planks"),
    ),
    "toolsmith" to JobBlockInfo(
        "Smithing Table", "smithing_table",
        listOf("iron_ingot","iron_ingot",null, "oak_planks","oak_planks",null, "oak_planks","oak_planks",null),
        listOf("2× Iron Ingot", "4× Oak Planks"),
    ),
    "weaponsmith" to JobBlockInfo(
        "Grindstone", "grindstone",
        listOf("stick","stone_slab","stick", "oak_planks",null,"oak_planks", null,null,null),
        listOf("2× Stick", "1× Stone Slab", "2× Oak Planks"),
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
}

// ── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun TradesScreen(vm: TradesViewModel = viewModel()) {
    val query      by vm.query.collectAsState()
    val profession by vm.profession.collectAsState()
    val trades     by vm.trades.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search trades…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            items(vm.professions) { p ->
                FilterChip(selected = profession == p, onClick = { vm.setProfession(p) },
                    label = { Text(p.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
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

            items(trades, key = { it.rowId }) { t ->
                val sellId = t.sellItem.substringAfterLast(':')
                val sellIcon = ItemTextures.get(sellId)
                BrowseListItem(
                    headline    = "${t.buyItem1Count}× ${t.buyItem1.substringAfterLast(':')}${if (t.buyItem2.isNotBlank()) " + ${t.buyItem2Count}× ${t.buyItem2.substringAfterLast(':')}" else ""} → ${t.sellItemCount}× $sellId",
                    supporting  = "${t.profession.replaceFirstChar { it.uppercase() }} · ${t.levelName}",
                    leadingIcon = sellIcon ?: PixelIcons.Trade,
                    leadingIconTint = if (sellIcon != null) Color.Unspecified else Emerald,
                    trailing    = {
                        CategoryBadge(label = "Lvl ${t.level}", color = Gold)
                    },
                )
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
                Text("Job Block", style = MaterialTheme.typography.labelSmall, color = Stone500)
                Text(info.blockName, style = MaterialTheme.typography.titleMedium, color = Gold)
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
                Text("Ingredients", style = MaterialTheme.typography.labelSmall, color = Stone500)
                info.ingredients.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = Stone300)
                }
            }
        }
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
                    val icon = itemId?.let { ItemTextures.get(it) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                if (itemId != null) SurfaceMid else Background,
                                RoundedCornerShape(2.dp),
                            )
                            .border(0.5.dp, Stone700, RoundedCornerShape(2.dp)),
                    ) {
                        if (icon != null) {
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
