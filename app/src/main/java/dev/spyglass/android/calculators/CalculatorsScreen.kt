package dev.spyglass.android.calculators

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.spyglass.android.calculators.anvil.AnvilScreen
import dev.spyglass.android.calculators.blockfill.BlockFillScreen
import dev.spyglass.android.calculators.nether.NetherScreen
import dev.spyglass.android.calculators.shapes.ShapesScreen
import dev.spyglass.android.calculators.smelting.SmeltingScreen
import dev.spyglass.android.calculators.storage.StorageScreen
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow

private val CALC_TABS = listOf(
    SpyglassTab("Fill",        PixelIcons.Fill),
    SpyglassTab("Smelt",       PixelIcons.Smelt),
    SpyglassTab("Storage",     PixelIcons.Storage),
    SpyglassTab("Nether",      PixelIcons.Nether),
    SpyglassTab("Shapes",      PixelIcons.Shapes),
    SpyglassTab("Enchanting",  PixelIcons.Anvil),
)

@Composable
fun CalculatorsScreen(initialTab: Int? = null) {
    var selectedTab by remember { mutableIntStateOf(initialTab ?: 0) }

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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> BlockFillScreen()
                1 -> SmeltingScreen()
                2 -> StorageScreen()
                3 -> NetherScreen()
                4 -> ShapesScreen()
                5 -> AnvilScreen()
            }
        }
    }
}
