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
    SpyglassTab("Blocks",     PixelIcons.Blocks),      // 0
    SpyglassTab("Items",      PixelIcons.Item),         // 1
    SpyglassTab("Recipes",    PixelIcons.Crafting),     // 2
    SpyglassTab("Mobs",       PixelIcons.Mob),          // 3
    SpyglassTab("Trades",     PixelIcons.Trade, untinted = true), // 4
    SpyglassTab("Biomes",     PixelIcons.Biome),        // 5
    SpyglassTab("Structures", PixelIcons.Structure),    // 6
    SpyglassTab("Enchants",   PixelIcons.Enchant),      // 7
    SpyglassTab("Potions",    PixelIcons.Potion),       // 8
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
    var targetProfession by remember { mutableStateOf<String?>(null) }

    // Handle incoming navigation from Search
    LaunchedEffect(initialTarget) {
        if (initialTarget != null) {
            tab = initialTarget.tab
            targetMobId = null; targetBiomeId = null; targetBlockId = null
            targetRecipeId = null; targetStructureId = null; targetItemId = null
            targetProfession = null
            when (initialTarget.tab) {
                0 -> targetBlockId = initialTarget.id
                1 -> targetItemId = initialTarget.id
                2 -> targetRecipeId = initialTarget.id
                3 -> targetMobId = initialTarget.id
                5 -> targetBiomeId = initialTarget.id
                6 -> targetStructureId = initialTarget.id
            }
        }
    }

    // Collect block IDs and item IDs so we can route taps to the correct tab
    val context = LocalContext.current
    val repo = remember { GameDataRepository.get(context) }
    val blockIdsFlow = remember { repo.searchBlocks("").map { list -> list.map { it.id }.toSet() } }
    val blockIds by blockIdsFlow.collectAsState(initial = emptySet())
    val itemIdsFlow = remember { repo.searchItems("").map { list -> list.map { it.id }.toSet() } }
    val itemIds by itemIdsFlow.collectAsState(initial = emptySet())

    fun clearAllTargets() {
        targetMobId = null
        targetBiomeId = null
        targetBlockId = null
        targetRecipeId = null
        targetStructureId = null
        targetItemId = null
        targetProfession = null
    }

    // Cross-tab item navigation — 3-way routing: Blocks → Items → Recipes
    val onItemTap: (String) -> Unit = { itemId ->
        clearAllTargets()
        if (itemId in blockIds) {
            targetBlockId = itemId
            tab = 0  // Blocks
        } else if (itemId in itemIds) {
            targetItemId = itemId
            tab = 1  // Items
        } else {
            targetRecipeId = itemId
            tab = 2  // Recipes
        }
    }

    val onMobTap: (String) -> Unit = { mobId ->
        clearAllTargets()
        targetMobId = mobId
        tab = 3  // Mobs
    }

    val onBiomeTap: (String) -> Unit = { biomeId ->
        clearAllTargets()
        targetBiomeId = biomeId
        tab = 5  // Biomes
    }

    val onStructureTap: (String) -> Unit = { structureId ->
        clearAllTargets()
        targetStructureId = structureId
        tab = 6  // Structures
    }

    val onTradeTap: (String) -> Unit = { profession ->
        clearAllTargets()
        targetProfession = profession
        tab = 4  // Trades
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpyglassTabRow(
            tabs          = BROWSE_TABS,
            selectedIndex = tab,
            onSelect      = { tab = it; clearAllTargets() },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        when (tab) {
            0 -> BlocksScreen(
                targetBlockId = targetBlockId,
                onItemTap = onItemTap,
                onBiomeTap = onBiomeTap,
                onTradeTap = onTradeTap,
                onStructureTap = onStructureTap,
            )
            1 -> ItemsScreen(
                targetItemId = targetItemId,
                onMobTap = onMobTap,
                onBlockTap = { blockId ->
                    clearAllTargets()
                    targetBlockId = blockId
                    tab = 0
                },
                onItemTap = onItemTap,
                onStructureTap = onStructureTap,
                onBiomeTap = onBiomeTap,
            )
            2 -> CraftingScreen(
                targetRecipeId = targetRecipeId,
                onItemTap = onItemTap,
                onBiomeTap = onBiomeTap,
            )
            3 -> MobsScreen(
                targetMobId = targetMobId,
                onNavigateToBiome = onBiomeTap,
                onNavigateToStructure = onStructureTap,
                onItemTap = onItemTap,
            )
            4 -> TradesScreen(
                targetProfession = targetProfession,
                onItemTap = onItemTap,
            )
            5 -> BiomesScreen(
                targetBiomeId = targetBiomeId,
                onNavigateToMob = onMobTap,
                onNavigateToStructure = onStructureTap,
                onItemTap = onItemTap,
            )
            6 -> StructuresScreen(
                targetStructureId = targetStructureId,
                onNavigateToMob = onMobTap,
                onNavigateToBiome = onBiomeTap,
                onItemTap = onItemTap,
            )
            7 -> EnchantsScreen()
            8 -> PotionsScreen(onItemTap = onItemTap)
        }
    }
}
