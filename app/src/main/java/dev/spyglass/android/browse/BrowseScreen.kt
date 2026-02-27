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
import dev.spyglass.android.browse.items.ItemsScreen
import dev.spyglass.android.browse.mobs.MobsScreen
import dev.spyglass.android.browse.potions.PotionsScreen
import dev.spyglass.android.browse.structures.StructuresScreen
import dev.spyglass.android.browse.trades.TradesScreen
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.navigation.BrowseTarget
import kotlinx.coroutines.flow.map

private val BROWSE_TABS = listOf(
    SpyglassTab("Blocks",     PixelIcons.Blocks),
    SpyglassTab("Recipes",    PixelIcons.Crafting),
    SpyglassTab("Mobs",       PixelIcons.Mob),
    SpyglassTab("Biomes",     PixelIcons.Biome),
    SpyglassTab("Enchants",   PixelIcons.Enchant),
    SpyglassTab("Potions",    PixelIcons.Potion),
    SpyglassTab("Trades",     PixelIcons.Trade, untinted = true),
    SpyglassTab("Structures", PixelIcons.Structure),
    SpyglassTab("Items",      PixelIcons.Item),
)

@Composable
fun BrowseScreen(initialTarget: BrowseTarget? = null) {
    var tab by remember { mutableIntStateOf(initialTarget?.tab ?: 0) }
    var targetMobId by remember { mutableStateOf<String?>(null) }
    var targetBiomeId by remember { mutableStateOf<String?>(null) }
    var targetBlockId by remember { mutableStateOf<String?>(null) }
    var targetRecipeId by remember { mutableStateOf<String?>(null) }
    var targetStructureId by remember { mutableStateOf<String?>(null) }
    var targetItemId by remember { mutableStateOf<String?>(null) }

    // Handle incoming navigation from Search
    LaunchedEffect(initialTarget) {
        if (initialTarget != null) {
            tab = initialTarget.tab
            targetMobId = null; targetBiomeId = null; targetBlockId = null
            targetRecipeId = null; targetStructureId = null; targetItemId = null
            when (initialTarget.tab) {
                0 -> targetBlockId = initialTarget.id
                1 -> targetRecipeId = initialTarget.id
                2 -> targetMobId = initialTarget.id
                3 -> targetBiomeId = initialTarget.id
                7 -> targetStructureId = initialTarget.id
                8 -> targetItemId = initialTarget.id
            }
        }
    }

    // Collect block IDs and item IDs so we can route taps to the correct tab
    val context = LocalContext.current
    val repo = remember { GameDataRepository.get(context) }
    val blockIds by repo.searchBlocks("").map { list -> list.map { it.id }.toSet() }
        .collectAsState(initial = emptySet())
    val itemIds by repo.searchItems("").map { list -> list.map { it.id }.toSet() }
        .collectAsState(initial = emptySet())

    // Cross-tab item navigation — 3-way routing: Blocks → Items → Recipes
    val onItemTap: (String) -> Unit = { itemId ->
        targetMobId = null
        targetBiomeId = null
        targetStructureId = null
        if (itemId in blockIds) {
            targetBlockId = itemId
            targetRecipeId = null
            targetItemId = null
            tab = 0  // Navigate to Blocks tab
        } else if (itemId in itemIds) {
            targetItemId = itemId
            targetBlockId = null
            targetRecipeId = null
            tab = 8  // Navigate to Items tab
        } else {
            targetRecipeId = itemId
            targetBlockId = null
            targetItemId = null
            tab = 1  // Navigate to Recipes tab
        }
    }

    val onMobTap: (String) -> Unit = { mobId ->
        targetMobId = mobId
        targetBiomeId = null
        targetBlockId = null
        targetRecipeId = null
        targetStructureId = null
        targetItemId = null
        tab = 2  // Navigate to Mobs tab
    }

    val onBiomeTap: (String) -> Unit = { biomeId ->
        targetBiomeId = biomeId
        targetBlockId = null
        targetRecipeId = null
        targetMobId = null
        targetStructureId = null
        targetItemId = null
        tab = 3  // Navigate to Biomes tab
    }

    val onStructureTap: (String) -> Unit = { structureId ->
        targetStructureId = structureId
        targetBlockId = null
        targetRecipeId = null
        targetMobId = null
        targetBiomeId = null
        targetItemId = null
        tab = 7  // Navigate to Structures tab
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
                targetStructureId = null
                targetItemId = null
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
                    targetStructureId = null
                    targetItemId = null
                    tab = 3
                },
                onNavigateToStructure = onStructureTap,
            )
            3 -> BiomesScreen(
                targetBiomeId = targetBiomeId,
                onNavigateToMob = { mobId ->
                    targetMobId = mobId
                    targetBiomeId = null
                    targetBlockId = null
                    targetRecipeId = null
                    targetStructureId = null
                    targetItemId = null
                    tab = 2
                },
                onNavigateToStructure = onStructureTap,
            )
            4 -> EnchantsScreen()
            5 -> PotionsScreen()
            6 -> TradesScreen()
            7 -> StructuresScreen(
                targetStructureId = targetStructureId,
                onNavigateToMob = { mobId ->
                    targetMobId = mobId
                    targetBiomeId = null
                    targetBlockId = null
                    targetRecipeId = null
                    targetStructureId = null
                    targetItemId = null
                    tab = 2
                },
                onNavigateToBiome = onBiomeTap,
                onItemTap = onItemTap,
            )
            8 -> ItemsScreen(
                targetItemId = targetItemId,
                onMobTap = onMobTap,
                onBlockTap = { blockId ->
                    targetBlockId = blockId
                    targetRecipeId = null
                    targetMobId = null
                    targetBiomeId = null
                    targetStructureId = null
                    targetItemId = null
                    tab = 0
                },
                onItemTap = onItemTap,
                onStructureTap = onStructureTap,
            )
        }
    }
}
