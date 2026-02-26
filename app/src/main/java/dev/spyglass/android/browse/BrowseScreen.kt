package dev.spyglass.android.browse

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.navigation.BrowseTarget
import kotlinx.coroutines.flow.map

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
fun BrowseScreen(initialTarget: BrowseTarget? = null) {
    var tab by remember { mutableIntStateOf(initialTarget?.tab ?: 0) }
    var targetMobId by remember { mutableStateOf<String?>(null) }
    var targetBiomeId by remember { mutableStateOf<String?>(null) }
    var targetBlockId by remember { mutableStateOf<String?>(null) }
    var targetRecipeId by remember { mutableStateOf<String?>(null) }

    // Handle incoming navigation from Search
    LaunchedEffect(initialTarget) {
        if (initialTarget != null) {
            tab = initialTarget.tab
            targetMobId = null; targetBiomeId = null; targetBlockId = null; targetRecipeId = null
            when (initialTarget.tab) {
                0 -> targetBlockId = initialTarget.id
                1 -> targetRecipeId = initialTarget.id
                2 -> targetMobId = initialTarget.id
                3 -> targetBiomeId = initialTarget.id
                // Tabs 4-6 (enchants, potions, trades) just switch to the tab
            }
        }
    }

    // Collect block IDs so we can route item taps to the correct tab
    val context = LocalContext.current
    val repo = remember { GameDataRepository.get(context) }
    val blockIds by repo.searchBlocks("").map { list -> list.map { it.id }.toSet() }
        .collectAsState(initial = emptySet())

    // Cross-tab item navigation — route to Blocks if item is a block, else Recipes
    val onItemTap: (String) -> Unit = { itemId ->
        targetMobId = null
        targetBiomeId = null
        if (itemId in blockIds) {
            targetBlockId = itemId
            targetRecipeId = null
            tab = 0  // Navigate to Blocks tab
        } else {
            targetRecipeId = itemId
            targetBlockId = null
            tab = 1  // Navigate to Recipes tab
        }
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
