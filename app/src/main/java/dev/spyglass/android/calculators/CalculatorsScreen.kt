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
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow
import dev.spyglass.android.navigation.BrowseTarget
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.map

@Composable
private fun calcTabs() = listOf(
    SpyglassTab(stringResource(R.string.calc_tab_todo),        PixelIcons.Todo),        // 0 — daily planning
    SpyglassTab(stringResource(R.string.calc_tab_shopping),    PixelIcons.Storage),      // 1 — list management
    SpyglassTab(stringResource(R.string.calc_tab_enchanting),  PixelIcons.Anvil),        // 2 — optimizer
    SpyglassTab(stringResource(R.string.calc_tab_fill),        PixelIcons.Fill),         // 3 — building
    SpyglassTab(stringResource(R.string.calc_tab_shapes),      PixelIcons.Shapes),       // 4 — building
    SpyglassTab(stringResource(R.string.calc_tab_maze),        PixelIcons.Maze),          // 5 — building
    SpyglassTab(stringResource(R.string.calc_tab_storage),     PixelIcons.Storage),      // 6 — building
    SpyglassTab(stringResource(R.string.calc_tab_smelt),       PixelIcons.Smelt),        // 7 — resources
    SpyglassTab(stringResource(R.string.calc_tab_nether),      PixelIcons.Nether),       // 8 — travel
    SpyglassTab(stringResource(R.string.calc_tab_game_clock),  PixelIcons.Clock),         // 9 — day/night cycle
    SpyglassTab(stringResource(R.string.calc_tab_light),       PixelIcons.Torch),         // 10 — light spacing
    SpyglassTab(stringResource(R.string.calc_tab_notes),       PixelIcons.Bookmark),      // 11 — user notes
    SpyglassTab(stringResource(R.string.calc_tab_waypoints),   PixelIcons.Waypoints),     // 12 — coordinate saver
    SpyglassTab(stringResource(R.string.calc_tab_redstone),    PixelIcons.Blocks),        // 13 — signal strength
    SpyglassTab(stringResource(R.string.calc_tab_librarian),   PixelIcons.Enchant),       // 14 — biome enchantments
    SpyglassTab(stringResource(R.string.calc_tab_food),        PixelIcons.Item),          // 15 — food & saturation
    SpyglassTab(stringResource(R.string.calc_tab_banners),     PixelIcons.Blocks),        // 16 — banner patterns
    SpyglassTab(stringResource(R.string.calc_tab_trims),       PixelIcons.Item),          // 17 — armor trims
    SpyglassTab(stringResource(R.string.calc_tab_loot),        PixelIcons.Structure),     // 18 — structure loot
)

@Composable
fun CalculatorsScreen(
    initialTab: Int? = null,
    onBrowseTarget: (BrowseTarget) -> Unit = {},
) {
    val context = LocalContext.current
    val defaultTab by remember {
        context.dataStore.data.map { it[PreferenceKeys.DEFAULT_TOOL_TAB] ?: 0 }
    }.collectAsStateWithLifecycle(initialValue = 0)

    var selectedTab by remember { mutableIntStateOf(initialTab ?: defaultTab) }

    // Apply default from prefs when no explicit initialTab
    LaunchedEffect(defaultTab) {
        if (initialTab == null) {
            selectedTab = defaultTab
        }
    }

    LaunchedEffect(initialTab) {
        if (initialTab != null) selectedTab = initialTab
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpyglassTabRow(
            tabs          = calcTabs(),
            selectedIndex = selectedTab,
            onSelect      = { selectedTab = it },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
            val onStructureTap: (String) -> Unit = { structureId ->
                onBrowseTarget(BrowseTarget(6, structureId))
            }
            when (selectedTab) {
                0 -> TodoScreen()
                1 -> ShoppingScreen()
                2 -> AnvilScreen()
                3 -> BlockFillScreen()
                4 -> ShapesScreen()
                5 -> MazeScreen()
                6 -> StorageScreen()
                7 -> SmeltingScreen()
                8 -> NetherScreen()
                9 -> ClockScreen()
                10 -> LightScreen()
                11 -> NotesScreen()
                12 -> WaypointsScreen()
                13 -> RedstoneScreen()
                14 -> LibrarianScreen()
                15 -> FoodScreen()
                16 -> BannerScreen()
                17 -> TrimScreen(onStructureTap = onStructureTap)
                18 -> LootScreen(onStructureTap = onStructureTap)
            }
        }
    }
}
