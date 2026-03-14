package dev.spyglass.android.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
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
import dev.spyglass.android.browse.commands.CommandsScreen
import dev.spyglass.android.browse.trades.TradesScreen
import dev.spyglass.android.browse.versions.VersionsScreen
import dev.spyglass.android.calculators.reference.ReferenceScreen
import dev.spyglass.android.core.ui.EntityLink
import dev.spyglass.android.core.ui.EntityLinkIndex
import dev.spyglass.android.core.ui.EntityType
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.navigation.BrowseTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Composable
private fun browseTabs() = listOf(
    SpyglassTab(stringResource(R.string.browse_tab_blocks),       PixelIcons.Blocks),      // 0
    SpyglassTab(stringResource(R.string.browse_tab_items),        PixelIcons.Item),         // 1
    SpyglassTab(stringResource(R.string.browse_tab_recipes),      PixelIcons.Crafting),     // 2
    SpyglassTab(stringResource(R.string.browse_tab_mobs),         PixelIcons.Mob),          // 3
    SpyglassTab(stringResource(R.string.browse_tab_trades),       PixelIcons.Trade, untinted = true), // 4
    SpyglassTab(stringResource(R.string.browse_tab_biomes),       PixelIcons.Biome),        // 5
    SpyglassTab(stringResource(R.string.browse_tab_structures),   PixelIcons.Structure),    // 6
    SpyglassTab(stringResource(R.string.browse_tab_enchants),     PixelIcons.Enchant, untinted = true), // 7
    SpyglassTab(stringResource(R.string.browse_tab_potions),      PixelIcons.Potion),       // 8
    SpyglassTab(stringResource(R.string.browse_tab_commands),     PixelIcons.Command),      // 9
    SpyglassTab(stringResource(R.string.browse_tab_reference),    PixelIcons.Bookmark),     // 10
    SpyglassTab(stringResource(R.string.browse_tab_versions),    PixelIcons.Clock),        // 11
)

@Composable
fun BrowseScreen(
    initialTarget: BrowseTarget? = null,
    scrollToTopTrigger: Int = 0,
    onCalcTab: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val defaultTab by remember {
        context.dataStore.data.map { it[PreferenceKeys.DEFAULT_BROWSE_TAB] ?: 0 }
    }.collectAsStateWithLifecycle(initialValue = 0)

    var tab by remember { mutableIntStateOf(initialTarget?.tab ?: defaultTab) }

    // Apply default from prefs when no explicit initialTarget
    LaunchedEffect(defaultTab) {
        if (initialTarget == null) {
            tab = defaultTab
        }
    }

    var targetMobId by remember { mutableStateOf<String?>(null) }
    var targetBiomeId by remember { mutableStateOf<String?>(null) }
    var targetBlockId by remember { mutableStateOf<String?>(null) }
    var targetRecipeId by remember { mutableStateOf<String?>(null) }
    var targetStructureId by remember { mutableStateOf<String?>(null) }
    var targetItemId by remember { mutableStateOf<String?>(null) }
    var targetProfession by remember { mutableStateOf<String?>(null) }
    var targetCommandId by remember { mutableStateOf<String?>(null) }
    var targetEnchantId by remember { mutableStateOf<String?>(null) }

    // Persistent scroll states per tab — survive tab switches
    val listStates = remember { (0..11).associateWith { LazyListState() } }

    // Internal back stack for system back button support
    data class BrowseBackEntry(val tab: Int, val targetId: String?, val scrollIndex: Int, val scrollOffset: Int)
    val backStack = remember { mutableStateListOf<BrowseBackEntry>() }

    fun clearAllTargets() {
        targetMobId = null; targetBiomeId = null; targetBlockId = null
        targetRecipeId = null; targetStructureId = null; targetItemId = null
        targetProfession = null; targetCommandId = null; targetEnchantId = null
    }

    fun currentTargetId(): String? = when (tab) {
        0 -> targetBlockId; 1 -> targetItemId; 2 -> targetRecipeId
        3 -> targetMobId; 4 -> targetProfession; 5 -> targetBiomeId
        6 -> targetStructureId; 7 -> targetEnchantId; 9 -> targetCommandId
        else -> null
    }

    fun setTargetForTab(t: Int, id: String?) {
        when (t) {
            0 -> targetBlockId = id; 1 -> targetItemId = id; 2 -> targetRecipeId = id
            3 -> targetMobId = id; 4 -> targetProfession = id; 5 -> targetBiomeId = id
            6 -> targetStructureId = id; 7 -> targetEnchantId = id; 9 -> targetCommandId = id
        }
    }

    fun pushAndNavigate(newTab: Int, newTargetId: String?) {
        val ls = listStates[tab]
        backStack.add(BrowseBackEntry(
            tab = tab,
            targetId = currentTargetId(),
            scrollIndex = ls?.firstVisibleItemIndex ?: 0,
            scrollOffset = ls?.firstVisibleItemScrollOffset ?: 0,
        ))
        clearAllTargets()
        setTargetForTab(newTab, newTargetId)
        tab = newTab
    }

    val scope = rememberCoroutineScope()
    BackHandler(enabled = backStack.isNotEmpty()) {
        val entry = backStack.removeLast()
        clearAllTargets()
        setTargetForTab(entry.tab, entry.targetId)
        tab = entry.tab
        // Restore scroll position after composition settles
        scope.launch {
            listStates[entry.tab]?.scrollToItem(entry.scrollIndex, entry.scrollOffset)
        }
    }

    // Handle incoming navigation from Search or cross-link
    LaunchedEffect(initialTarget) {
        if (initialTarget != null) {
            backStack.clear()
            tab = initialTarget.tab
            targetMobId = null; targetBiomeId = null; targetBlockId = null
            targetRecipeId = null; targetStructureId = null; targetItemId = null
            targetProfession = null; targetCommandId = null; targetEnchantId = null
            when (initialTarget.tab) {
                0 -> targetBlockId = initialTarget.id
                1 -> targetItemId = initialTarget.id
                2 -> targetRecipeId = initialTarget.id
                3 -> targetMobId = initialTarget.id
                4 -> targetProfession = initialTarget.id
                5 -> targetBiomeId = initialTarget.id
                6 -> targetStructureId = initialTarget.id
                7 -> targetEnchantId = initialTarget.id
                9 -> targetCommandId = initialTarget.id
            }
        }
    }

    // Async repo access — avoids blocking main thread if database isn't ready yet
    val repo by produceState<GameDataRepository?>(null) {
        value = withContext(Dispatchers.IO) { GameDataRepository.get(context) }
    }

    // Collect block IDs and item IDs so we can route taps to the correct tab
    val blockIds by produceState(emptySet<String>(), repo) {
        repo?.searchBlocks("")?.map { list -> list.map { it.id }.toSet() }?.collect { value = it }
    }
    val itemIds by produceState(emptySet<String>(), repo) {
        repo?.searchItems("")?.map { list -> list.map { it.id }.toSet() }?.collect { value = it }
    }

    // Build EntityLinkIndex on Default dispatcher (regex compilation is CPU-heavy)
    val entityLinkIndex by produceState(EntityLinkIndex(emptyList()), repo) {
        val r = repo ?: return@produceState
        combine(
            r.searchBlocks("").map { list -> list.map { EntityLink(EntityType.BLOCK, it.id, it.name) } },
            r.searchItems("").map { list -> list.map { EntityLink(EntityType.ITEM, it.id, it.name) } },
            r.searchMobs("").map { list -> list.map { EntityLink(EntityType.MOB, it.id, it.name) } },
            r.searchBiomes("").map { list -> list.map { EntityLink(EntityType.BIOME, it.id, it.name) } },
            r.searchStructures("").map { list -> list.map { EntityLink(EntityType.STRUCTURE, it.id, it.name) } },
        ) { blocks, items, mobs, biomes, structures ->
            blocks + items + mobs + biomes + structures
        }.combine(
            r.searchEnchants("").map { list -> list.map { EntityLink(EntityType.ENCHANT, it.id, it.name) } },
        ) { combined, enchants ->
            withContext(Dispatchers.Default) { EntityLinkIndex(combined + enchants) }
        }.collect { value = it }
    }

    // Cross-tab item navigation — 3-way routing: Blocks → Items → Recipes
    val onItemTap: (String) -> Unit = { itemId ->
        val targetTab = when {
            itemId in blockIds -> 0
            itemId in itemIds -> 1
            else -> 2
        }
        pushAndNavigate(targetTab, itemId)
    }

    val onMobTap: (String) -> Unit = { mobId -> pushAndNavigate(3, mobId) }
    val onBiomeTap: (String) -> Unit = { biomeId -> pushAndNavigate(5, biomeId) }
    val onStructureTap: (String) -> Unit = { structureId -> pushAndNavigate(6, structureId) }
    val onTradeTap: (String) -> Unit = { profession -> pushAndNavigate(4, profession) }
    val onEnchantTap: (String) -> Unit = { enchantId -> pushAndNavigate(7, enchantId) }

    Column(modifier = Modifier.fillMaxSize()) {
        SpyglassTabRow(
            tabs          = browseTabs(),
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
                listState = listStates[0]!!,
            )
            1 -> ItemsScreen(
                targetItemId = targetItemId,
                onMobTap = onMobTap,
                onBlockTap = { blockId -> pushAndNavigate(0, blockId) },
                onItemTap = onItemTap,
                onStructureTap = onStructureTap,
                onBiomeTap = onBiomeTap,
                onEnchantTap = onEnchantTap,
                entityLinkIndex = entityLinkIndex,
                listState = listStates[1]!!,
            )
            2 -> CraftingScreen(
                targetRecipeId = targetRecipeId,
                onItemTap = onItemTap,
                onBiomeTap = onBiomeTap,
                listState = listStates[2]!!,
            )
            3 -> MobsScreen(
                targetMobId = targetMobId,
                onNavigateToBiome = onBiomeTap,
                onNavigateToStructure = onStructureTap,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onCalcTab = onCalcTab,
                entityLinkIndex = entityLinkIndex,
                listState = listStates[3]!!,
            )
            4 -> TradesScreen(
                targetProfession = targetProfession,
                onItemTap = onItemTap,
                listState = listStates[4]!!,
            )
            5 -> BiomesScreen(
                targetBiomeId = targetBiomeId,
                onNavigateToMob = onMobTap,
                onNavigateToStructure = onStructureTap,
                onItemTap = onItemTap,
                onCalcTab = onCalcTab,
                listState = listStates[5]!!,
            )
            6 -> StructuresScreen(
                targetStructureId = targetStructureId,
                onNavigateToMob = onMobTap,
                onNavigateToBiome = onBiomeTap,
                onItemTap = onItemTap,
                onCalcTab = onCalcTab,
                entityLinkIndex = entityLinkIndex,
                onEnchantTap = onEnchantTap,
                listState = listStates[6]!!,
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
                listState = listStates[7]!!,
            )
            8 -> PotionsScreen(onItemTap = onItemTap, listState = listStates[8]!!)
            9 -> CommandsScreen(
                targetCommandId = targetCommandId,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onBiomeTap = onBiomeTap,
                onStructureTap = onStructureTap,
                onEnchantTap = onEnchantTap,
                entityLinkIndex = entityLinkIndex,
                listState = listStates[9]!!,
            )
            10 -> ReferenceScreen()
            11 -> VersionsScreen(listState = listStates[11]!!)
        }
    }
}
