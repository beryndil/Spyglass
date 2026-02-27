package dev.spyglass.android.calculators

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.spyglass.android.calculators.anvil.AnvilScreen
import dev.spyglass.android.calculators.blockfill.BlockFillScreen
import dev.spyglass.android.calculators.nether.NetherScreen
import dev.spyglass.android.calculators.reference.ReferenceScreen
import dev.spyglass.android.calculators.shapes.ShapesScreen
import dev.spyglass.android.calculators.smelting.SmeltingScreen
import dev.spyglass.android.calculators.shopping.ShoppingScreen
import dev.spyglass.android.calculators.storage.StorageScreen
import dev.spyglass.android.calculators.todo.TodoScreen
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.map

private val CALC_TABS = listOf(
    SpyglassTab("Fill",        PixelIcons.Fill),
    SpyglassTab("Smelt",       PixelIcons.Smelt),
    SpyglassTab("Storage",     PixelIcons.Storage),
    SpyglassTab("Nether",      PixelIcons.Nether),
    SpyglassTab("Shapes",      PixelIcons.Shapes),
    SpyglassTab("Enchanting",  PixelIcons.Anvil),
    SpyglassTab("Reference",   PixelIcons.Bookmark),
    SpyglassTab("Shopping",    PixelIcons.Storage),
    SpyglassTab("Todo",        PixelIcons.Todo),
)

@Composable
fun CalculatorsScreen(initialTab: Int? = null) {
    val context = LocalContext.current
    val defaultTab by remember {
        context.dataStore.data.map { it[PreferenceKeys.DEFAULT_TOOL_TAB] ?: 0 }
    }.collectAsState(initial = 0)

    var selectedTab by remember { mutableIntStateOf(initialTab ?: 0) }

    // Apply default from prefs only once on first composition when no explicit initialTab
    var defaultApplied by remember { mutableStateOf(false) }
    LaunchedEffect(defaultTab) {
        if (!defaultApplied && initialTab == null) {
            selectedTab = defaultTab
            defaultApplied = true
        }
    }

    LaunchedEffect(initialTab) {
        if (initialTab != null) selectedTab = initialTab
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpyglassTabRow(
            tabs          = CALC_TABS,
            selectedIndex = selectedTab,
            onSelect      = { selectedTab = it },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> BlockFillScreen()
                1 -> SmeltingScreen()
                2 -> StorageScreen()
                3 -> NetherScreen()
                4 -> ShapesScreen()
                5 -> AnvilScreen()
                6 -> ReferenceScreen()
                7 -> ShoppingScreen()
                8 -> TodoScreen()
            }
        }
    }
}
