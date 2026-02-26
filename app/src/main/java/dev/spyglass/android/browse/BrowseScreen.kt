package dev.spyglass.android.browse

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.spyglass.android.browse.biomes.BiomesScreen
import dev.spyglass.android.browse.blocks.BlocksScreen
import dev.spyglass.android.browse.crafting.CraftingScreen
import dev.spyglass.android.browse.enchants.EnchantsScreen
import dev.spyglass.android.browse.mobs.MobsScreen
import dev.spyglass.android.browse.potions.PotionsScreen
import dev.spyglass.android.browse.trades.TradesScreen
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow

private val BROWSE_TABS = listOf(
    SpyglassTab("Blocks",   Icons.Default.ViewInAr),
    SpyglassTab("Recipes",  Icons.Default.Construction),
    SpyglassTab("Mobs",     Icons.Default.Pets),
    SpyglassTab("Biomes",   Icons.Default.Terrain),
    SpyglassTab("Enchants", Icons.Default.AutoFixHigh),
    SpyglassTab("Potions",  Icons.Default.Science),
    SpyglassTab("Trades",   Icons.Default.Handshake),
)

@Composable
fun BrowseScreen() {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SpyglassTabRow(
            tabs          = BROWSE_TABS,
            selectedIndex = tab,
            onSelect      = { tab = it },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        when (tab) {
            0 -> BlocksScreen()
            1 -> CraftingScreen()
            2 -> MobsScreen()
            3 -> BiomesScreen()
            4 -> EnchantsScreen()
            5 -> PotionsScreen()
            6 -> TradesScreen()
        }
    }
}
