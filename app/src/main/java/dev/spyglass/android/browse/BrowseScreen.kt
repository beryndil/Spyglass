package dev.spyglass.android.browse

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import dev.spyglass.android.browse.biomes.BiomesScreen
import dev.spyglass.android.browse.blocks.BlocksScreen
import dev.spyglass.android.browse.crafting.CraftingScreen
import dev.spyglass.android.browse.enchants.EnchantsScreen
import dev.spyglass.android.browse.items.ItemsScreen
import dev.spyglass.android.browse.mobs.MobsScreen
import dev.spyglass.android.browse.potions.PotionsScreen
import dev.spyglass.android.browse.structures.StructuresScreen
import dev.spyglass.android.browse.advancements.AdvancementsScreen
import dev.spyglass.android.browse.commands.CommandsScreen
import dev.spyglass.android.browse.trades.TradesScreen
import dev.spyglass.android.calculators.reference.ReferenceScreen
import dev.spyglass.android.core.ui.EntityLink
import dev.spyglass.android.core.ui.EntityLinkIndex
import dev.spyglass.android.core.ui.EntityType
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.navigation.BrowseTarget
import kotlinx.coroutines.flow.combine
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
    SpyglassTab("Planner",   PixelIcons.Enchant),       // 9
    SpyglassTab("Commands",  PixelIcons.Blocks),       // 10
    SpyglassTab("Reference", PixelIcons.Bookmark),     // 11
)

@Composable
fun BrowseScreen(
    initialTarget: BrowseTarget? = null,
    onCalcTab: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val defaultTab by remember {
        context.dataStore.data.map { it[PreferenceKeys.DEFAULT_BROWSE_TAB] ?: 0 }
    }.collectAsState(initial = 0)

    var tab by remember { mutableIntStateOf(initialTarget?.tab ?: 0) }

    // Apply default from prefs only once on first composition when no explicit initialTarget
    var defaultApplied by remember { mutableStateOf(false) }
    LaunchedEffect(defaultTab) {
        if (!defaultApplied && initialTarget == null) {
            tab = defaultTab
            defaultApplied = true
        }
    }

    var targetMobId by remember { mutableStateOf<String?>(null) }
    var targetBiomeId by remember { mutableStateOf<String?>(null) }
    var targetBlockId by remember { mutableStateOf<String?>(null) }
    var targetRecipeId by remember { mutableStateOf<String?>(null) }
    var targetStructureId by remember { mutableStateOf<String?>(null) }
    var targetItemId by remember { mutableStateOf<String?>(null) }
    var targetProfession by remember { mutableStateOf<String?>(null) }
    var targetAdvancementId by remember { mutableStateOf<String?>(null) }
    var targetCommandId by remember { mutableStateOf<String?>(null) }
    var targetEnchantId by remember { mutableStateOf<String?>(null) }

    // Handle incoming navigation from Search
    LaunchedEffect(initialTarget) {
        if (initialTarget != null) {
            tab = initialTarget.tab
            targetMobId = null; targetBiomeId = null; targetBlockId = null
            targetRecipeId = null; targetStructureId = null; targetItemId = null
            targetProfession = null; targetAdvancementId = null; targetCommandId = null
            targetEnchantId = null
            when (initialTarget.tab) {
                0 -> targetBlockId = initialTarget.id
                1 -> targetItemId = initialTarget.id
                2 -> targetRecipeId = initialTarget.id
                3 -> targetMobId = initialTarget.id
                5 -> targetBiomeId = initialTarget.id
                6 -> targetStructureId = initialTarget.id
                7 -> targetEnchantId = initialTarget.id
                9 -> targetAdvancementId = initialTarget.id
                10 -> targetCommandId = initialTarget.id
            }
        }
    }

    // Collect block IDs and item IDs so we can route taps to the correct tab
    val repo = remember { GameDataRepository.get(context) }
    val blockIdsFlow = remember { repo.searchBlocks("").map { list -> list.map { it.id }.toSet() } }
    val blockIds by blockIdsFlow.collectAsState(initial = emptySet())
    val itemIdsFlow = remember { repo.searchItems("").map { list -> list.map { it.id }.toSet() } }
    val itemIds by itemIdsFlow.collectAsState(initial = emptySet())

    // Build EntityLinkIndex from all entity names for linkified descriptions
    val entityLinkIndex by remember {
        combine(
            repo.searchBlocks("").map { list -> list.map { EntityLink(EntityType.BLOCK, it.id, it.name) } },
            repo.searchItems("").map { list -> list.map { EntityLink(EntityType.ITEM, it.id, it.name) } },
            repo.searchMobs("").map { list -> list.map { EntityLink(EntityType.MOB, it.id, it.name) } },
            repo.searchBiomes("").map { list -> list.map { EntityLink(EntityType.BIOME, it.id, it.name) } },
            repo.searchStructures("").map { list -> list.map { EntityLink(EntityType.STRUCTURE, it.id, it.name) } },
        ) { blocks, items, mobs, biomes, structures ->
            blocks + items + mobs + biomes + structures
        }.combine(
            repo.searchEnchants("").map { list -> list.map { EntityLink(EntityType.ENCHANT, it.id, it.name) } },
        ) { combined, enchants ->
            EntityLinkIndex(combined + enchants)
        }
    }.collectAsState(initial = EntityLinkIndex(emptyList()))

    fun clearAllTargets() {
        targetMobId = null
        targetBiomeId = null
        targetBlockId = null
        targetRecipeId = null
        targetStructureId = null
        targetItemId = null
        targetProfession = null
        targetAdvancementId = null
        targetCommandId = null
        targetEnchantId = null
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

    val onEnchantTap: (String) -> Unit = { enchantId ->
        clearAllTargets()
        targetEnchantId = enchantId
        tab = 7  // Enchants
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
                onEnchantTap = onEnchantTap,
                entityLinkIndex = entityLinkIndex,
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
                onMobTap = onMobTap,
                onCalcTab = onCalcTab,
                entityLinkIndex = entityLinkIndex,
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
                onCalcTab = onCalcTab,
            )
            6 -> StructuresScreen(
                targetStructureId = targetStructureId,
                onNavigateToMob = onMobTap,
                onNavigateToBiome = onBiomeTap,
                onItemTap = onItemTap,
                onCalcTab = onCalcTab,
                entityLinkIndex = entityLinkIndex,
                onEnchantTap = onEnchantTap,
            )
            7 -> EnchantsScreen(
                targetEnchantId = targetEnchantId,
                onCalcTab = onCalcTab,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onBiomeTap = onBiomeTap,
                onStructureTap = onStructureTap,
                onEnchantTap = onEnchantTap,
                entityLinkIndex = entityLinkIndex,
            )
            8 -> PotionsScreen(onItemTap = onItemTap)
            9 -> AdvancementsScreen(
                targetAdvancementId = targetAdvancementId,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onStructureTap = onStructureTap,
                onBiomeTap = onBiomeTap,
                onCalcTab = onCalcTab,
                onEnchantTap = onEnchantTap,
                entityLinkIndex = entityLinkIndex,
            )
            10 -> CommandsScreen(
                targetCommandId = targetCommandId,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onBiomeTap = onBiomeTap,
                onStructureTap = onStructureTap,
                onEnchantTap = onEnchantTap,
                entityLinkIndex = entityLinkIndex,
            )
            11 -> ReferenceScreen()
        }
    }
}
