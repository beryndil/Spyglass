package dev.spyglass.android.calculators

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.calculators.anvil.AnvilScreen
import dev.spyglass.android.calculators.blockfill.BlockFillScreen
import dev.spyglass.android.calculators.nether.NetherScreen
import dev.spyglass.android.calculators.maze.MazeScreen
import dev.spyglass.android.calculators.shapes.ShapesScreen
import dev.spyglass.android.calculators.smelting.SmeltingScreen
import dev.spyglass.android.calculators.shopping.ShoppingScreen
import dev.spyglass.android.calculators.storage.StorageScreen
import dev.spyglass.android.calculators.clock.ClockScreen
import dev.spyglass.android.calculators.light.LightScreen
import dev.spyglass.android.calculators.banners.BannerScreen
import dev.spyglass.android.calculators.food.FoodScreen
import dev.spyglass.android.calculators.trims.TrimScreen
import dev.spyglass.android.calculators.librarian.LibrarianScreen
import dev.spyglass.android.calculators.loot.LootScreen
import dev.spyglass.android.calculators.notes.NotesScreen
import dev.spyglass.android.calculators.redstone.RedstoneScreen
import dev.spyglass.android.calculators.waypoints.WaypointsScreen
import dev.spyglass.android.calculators.todo.TodoScreen
import dev.spyglass.android.calculators.tracker.TrackerScreen
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow
import dev.spyglass.android.navigation.BrowseTarget
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.map

/** Stable string key for each calculator tab — decoupled from visual index. */
private data class CalcTabEntry(
    val key: String,
    val tab: SpyglassTab,
    val experimental: Boolean = false,
)

@Composable
private fun allCalcTabs() = listOf(
    CalcTabEntry("todo",       SpyglassTab(stringResource(R.string.calc_tab_todo),        PixelIcons.Todo)),
    CalcTabEntry("shopping",   SpyglassTab(stringResource(R.string.calc_tab_shopping),    PixelIcons.Storage)),
    CalcTabEntry("enchanting", SpyglassTab(stringResource(R.string.calc_tab_enchanting),  PixelIcons.Anvil)),
    CalcTabEntry("fill",       SpyglassTab(stringResource(R.string.calc_tab_fill),        PixelIcons.Fill)),
    CalcTabEntry("shapes",     SpyglassTab(stringResource(R.string.calc_tab_shapes),      PixelIcons.Shapes)),
    CalcTabEntry("maze",       SpyglassTab(stringResource(R.string.calc_tab_maze),        PixelIcons.Maze)),
    CalcTabEntry("storage",    SpyglassTab(stringResource(R.string.calc_tab_storage),     PixelIcons.Storage)),
    CalcTabEntry("smelt",      SpyglassTab(stringResource(R.string.calc_tab_smelt),       PixelIcons.Smelt)),
    CalcTabEntry("nether",     SpyglassTab(stringResource(R.string.calc_tab_nether),      PixelIcons.Nether)),
    CalcTabEntry("clock",      SpyglassTab(stringResource(R.string.calc_tab_game_clock),  PixelIcons.Clock)),
    CalcTabEntry("light",      SpyglassTab(stringResource(R.string.calc_tab_light),       PixelIcons.Torch)),
    CalcTabEntry("notes",      SpyglassTab(stringResource(R.string.calc_tab_notes),       PixelIcons.Bookmark)),
    CalcTabEntry("waypoints",  SpyglassTab(stringResource(R.string.calc_tab_waypoints),   PixelIcons.Waypoints)),
    CalcTabEntry("redstone",   SpyglassTab(stringResource(R.string.calc_tab_redstone),    PixelIcons.Blocks)),
    CalcTabEntry("librarian",  SpyglassTab(stringResource(R.string.calc_tab_librarian),   PixelIcons.Enchant), experimental = true),
    CalcTabEntry("food",       SpyglassTab(stringResource(R.string.calc_tab_food),        PixelIcons.Item)),
    CalcTabEntry("banners",    SpyglassTab(stringResource(R.string.calc_tab_banners),     PixelIcons.Blocks)),
    CalcTabEntry("trims",      SpyglassTab(stringResource(R.string.calc_tab_trims),       PixelIcons.Item)),
    CalcTabEntry("loot",       SpyglassTab(stringResource(R.string.calc_tab_loot),        PixelIcons.Structure)),
    CalcTabEntry("tracker",    SpyglassTab(stringResource(R.string.calc_tab_tracker),    PixelIcons.Advancement)),
)

/** Map legacy integer tab index → stable key for external callers (HomeScreen, etc.) */
fun calcTabKey(legacyIndex: Int): String = when (legacyIndex) {
    0 -> "todo"; 1 -> "shopping"; 2 -> "enchanting"; 3 -> "fill"; 4 -> "shapes"
    5 -> "maze"; 6 -> "storage"; 7 -> "smelt"; 8 -> "nether"; 9 -> "clock"
    10 -> "light"; 11 -> "notes"; 12 -> "waypoints"; 13 -> "redstone"; 14 -> "librarian"
    15 -> "food"; 16 -> "banners"; 17 -> "trims"; 18 -> "loot"
    19 -> "tracker"
    else -> "todo"
}

@Composable
fun CalculatorsScreen(
    initialTab: Int? = null,
    onBrowseTarget: (BrowseTarget) -> Unit = {},
) {
    val context = LocalContext.current
    val defaultTab by remember {
        context.dataStore.data.map { it[PreferenceKeys.DEFAULT_TOOL_TAB] ?: 0 }
    }.collectAsStateWithLifecycle(initialValue = 0)

    val showExperimental by remember {
        context.dataStore.data.map { it[PreferenceKeys.SHOW_EXPERIMENTAL] ?: false }
    }.collectAsStateWithLifecycle(initialValue = true)

    val allTabs = allCalcTabs()
    val visibleTabs = remember(allTabs, showExperimental) {
        if (showExperimental) allTabs else allTabs.filter { !it.experimental }
    }

    // Resolve the target key from legacy index
    val targetKey = when {
        initialTab != null -> calcTabKey(initialTab)
        else -> calcTabKey(defaultTab)
    }

    var selectedKey by remember { mutableStateOf(targetKey) }

    // React to external navigation
    LaunchedEffect(initialTab) {
        if (initialTab != null) selectedKey = calcTabKey(initialTab)
    }
    LaunchedEffect(defaultTab) {
        if (initialTab == null) selectedKey = calcTabKey(defaultTab)
    }

    // Resolve visual index from key
    val selectedIndex = visibleTabs.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxSize()) {
        SpyglassTabRow(
            tabs          = visibleTabs.map { it.tab },
            selectedIndex = selectedIndex,
            onSelect      = { idx -> selectedKey = visibleTabs[idx].key },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        when (selectedKey) {
            "tracker" -> {
                val activity = LocalContext.current as ComponentActivity
                val connectVm: ConnectViewModel = viewModel(activity)
                TrackerScreen(
                    connectViewModel = connectVm,
                    onItemTap = { id -> onBrowseTarget(BrowseTarget(1, id)) },
                    onMobTap = { id -> onBrowseTarget(BrowseTarget(3, id)) },
                    onStructureTap = { id -> onBrowseTarget(BrowseTarget(6, id)) },
                    onBiomeTap = { id -> onBrowseTarget(BrowseTarget(5, id)) },
                    onEnchantTap = { id -> onBrowseTarget(BrowseTarget(7, id)) },
                )
            }
            else -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                ) {
                    val onStructureTap: (String) -> Unit = { structureId ->
                        onBrowseTarget(BrowseTarget(6, structureId))
                    }
                    when (selectedKey) {
                        "todo"       -> TodoScreen()
                        "shopping"   -> ShoppingScreen()
                        "enchanting" -> AnvilScreen()
                        "fill"       -> BlockFillScreen()
                        "shapes"     -> ShapesScreen()
                        "maze"       -> MazeScreen()
                        "storage"    -> StorageScreen()
                        "smelt"      -> SmeltingScreen()
                        "nether"     -> NetherScreen()
                        "clock"      -> ClockScreen()
                        "light"      -> LightScreen()
                        "notes"      -> NotesScreen()
                        "waypoints"  -> WaypointsScreen()
                        "redstone"   -> RedstoneScreen()
                        "librarian"  -> LibrarianScreen()
                        "food"       -> FoodScreen()
                        "banners"    -> BannerScreen()
                        "trims"      -> TrimScreen(onStructureTap = onStructureTap)
                        "loot"       -> LootScreen(onStructureTap = onStructureTap)
                    }
                }
            }
        }
    }
}
