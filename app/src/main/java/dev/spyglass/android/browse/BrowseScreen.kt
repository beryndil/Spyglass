package dev.spyglass.android.browse

import androidx.compose.foundation.layout.*
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
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow

private val BROWSE_TABS = listOf(
    SpyglassTab("Blocks",   PixelIcons.Blocks),
    SpyglassTab("Recipes",  PixelIcons.Crafting),
    SpyglassTab("Mobs",     PixelIcons.Mob),
    SpyglassTab("Biomes",   PixelIcons.Biome),
    SpyglassTab("Enchants", PixelIcons.Enchant),
    SpyglassTab("Potions",  PixelIcons.Potion),
    SpyglassTab("Trades",   PixelIcons.Trade),
)

@Composable
fun BrowseScreen() {
    var tab by remember { mutableIntStateOf(0) }
    var targetMobId by remember { mutableStateOf<String?>(null) }
    var targetBiomeId by remember { mutableStateOf<String?>(null) }
    var targetBlockId by remember { mutableStateOf<String?>(null) }
    var targetRecipeId by remember { mutableStateOf<String?>(null) }

    // Cross-tab item navigation callback
    val onItemTap: (String) -> Unit = { itemId ->
        // Try blocks tab first, then recipes tab
        targetBlockId = itemId
        targetRecipeId = null
        targetMobId = null
        targetBiomeId = null
        tab = 0  // Navigate to Blocks tab
    }

    val onBiomeTap: (String) -> Unit = { biomeId ->
        targetBiomeId = biomeId
        targetBlockId = null
        targetRecipeId = null
        targetMobId = null
        tab = 3  // Navigate to Biomes tab
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpyglassTabRow(
            tabs          = BROWSE_TABS,
            selectedIndex = tab,
            onSelect      = {
                tab = it
                targetMobId = null
                targetBiomeId = null
                targetBlockId = null
                targetRecipeId = null
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        when (tab) {
            0 -> BlocksScreen(
                targetBlockId = targetBlockId,
                onItemTap = onItemTap,
                onBiomeTap = onBiomeTap,
            )
            1 -> CraftingScreen(
                targetRecipeId = targetRecipeId,
                onItemTap = onItemTap,
                onBiomeTap = onBiomeTap,
            )
            2 -> MobsScreen(
                targetMobId = targetMobId,
                onNavigateToBiome = { biomeId ->
                    targetBiomeId = biomeId
                    targetMobId = null
                    targetBlockId = null
                    targetRecipeId = null
                    tab = 3
                },
            )
            3 -> BiomesScreen(
                targetBiomeId = targetBiomeId,
                onNavigateToMob = { mobId ->
                    targetMobId = mobId
                    targetBiomeId = null
                    targetBlockId = null
                    targetRecipeId = null
                    tab = 2
                },
            )
            4 -> EnchantsScreen()
            5 -> PotionsScreen()
            6 -> TradesScreen()
        }
    }
}
