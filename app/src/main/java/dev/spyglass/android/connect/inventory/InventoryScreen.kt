package dev.spyglass.android.connect.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ChestDiamondLoader
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.ItemStack
import dev.spyglass.android.connect.OfflineIndicator
import dev.spyglass.android.connect.ServerSyncNote
import dev.spyglass.android.connect.PlayerData
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.navigation.BrowseTarget
import kotlinx.coroutines.launch

/**
 * Player inventory display: 36-slot grid (9 columns x 4 rows) + 4 armor + 1 offhand.
 * Tap shows item name, long-press opens item card.
 */
@Composable
fun InventoryScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
    onBrowseTarget: (BrowseTarget) -> Unit = {},
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val loadingStatus by viewModel.loadingStatus.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val selectedWorld by viewModel.selectedWorld.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected
    val isServerWorld = selectedWorld?.startsWith("ptero_") == true
    val scope = rememberCoroutineScope()
    val hapticClick = rememberHapticClick()

    DisposableEffect(Unit) {
        viewModel.setActiveScreen("inventory")
        onDispose { viewModel.setActiveScreen(null) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.connect_inventory), style = MaterialTheme.typography.titleMedium)
        }

        if (!isConnected && lastUpdated != null) {
            OfflineIndicator(lastUpdated, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        } else if (isConnected && isServerWorld) {
            ServerSyncNote(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        }

        InventoryContent(
            playerData = playerData,
            isOffline = !isConnected,
            loadingStatus = loadingStatus,
            onLongPressItem = { item ->
                scope.launch {
                    val tab = viewModel.resolveBrowseTab(item.id)
                    onBrowseTarget(BrowseTarget(tab, item.id))
                }
            },
        )
    }
}

@Composable
fun InventoryContent(
    playerData: PlayerData?,
    isOffline: Boolean = false,
    loadingStatus: String? = null,
    onLongPressItem: ((ItemStack) -> Unit)? = null,
) {
    val player = playerData

    if (player == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            if (isOffline) {
                Text(
                    stringResource(R.string.connect_no_cached_inventory),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ChestDiamondLoader(statusText = loadingStatus)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Armor + Offhand
        SectionHeader(stringResource(R.string.connect_equipment))
        val mainHandItem = player.inventory.firstOrNull { it.slot == player.selectedSlot }
        val equipmentItems = (player.armor.ifEmpty {
            player.inventory.filter { it.slot in 100..103 }
        }).sortedByDescending { it.slot }
            .mapIndexed { i, item -> item.copy(slot = 100 + i) } +
            listOfNotNull(mainHandItem?.copy(slot = 104)) +
            listOfNotNull(player.offhand?.copy(slot = 105))

        InventoryGrid(
            items = equipmentItems,
            startSlot = 100,
            endSlot = 105,
            columns = 6,
            onLongPressItem = onLongPressItem,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(stringResource(R.string.connect_armor_head), stringResource(R.string.connect_armor_chest), stringResource(R.string.connect_armor_legs), stringResource(R.string.connect_armor_feet), stringResource(R.string.connect_armor_main), stringResource(R.string.connect_armor_off)).forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Hotbar (slots 0-8)
        SectionHeader(stringResource(R.string.connect_hotbar))
        InventoryGrid(
            items = player.inventory,
            startSlot = 0,
            endSlot = 8,
            columns = 9,
            selectedSlot = player.selectedSlot,
            onLongPressItem = onLongPressItem,
        )

        // Main inventory (slots 9-35)
        SectionHeader(stringResource(R.string.connect_inventory))
        InventoryGrid(
            items = player.inventory,
            startSlot = 9,
            endSlot = 35,
            columns = 9,
            onLongPressItem = onLongPressItem,
        )
    }
}

@Composable
fun InventoryGrid(
    items: List<ItemStack>,
    startSlot: Int,
    endSlot: Int,
    columns: Int,
    selectedSlot: Int? = null,
    onLongPressItem: ((ItemStack) -> Unit)? = null,
) {
    val slotCount = endSlot - startSlot + 1
    val rows = (slotCount + columns - 1) / columns

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (col in 0 until columns) {
                    val slot = startSlot + row * columns + col
                    if (slot > endSlot) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val item = items.firstOrNull { it.slot == slot }
                        InventorySlotView(
                            item = item,
                            modifier = Modifier.weight(1f),
                            isSelected = selectedSlot != null && slot == selectedSlot,
                            onLongPress = onLongPressItem,
                        )
                    }
                }
            }
        }
    }
}

