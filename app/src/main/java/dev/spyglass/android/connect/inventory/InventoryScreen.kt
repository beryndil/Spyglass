package dev.spyglass.android.connect.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.ItemStack
import dev.spyglass.android.connect.OfflineIndicator
import dev.spyglass.android.connect.PlayerData
import dev.spyglass.android.connect.client.ConnectionState
import dev.spyglass.android.core.ui.SectionHeader
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
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Inventory", style = MaterialTheme.typography.titleMedium)
        }

        if (!isConnected && lastUpdated != null) {
            OfflineIndicator(lastUpdated, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        }

        InventoryContent(
            playerData = playerData,
            isOffline = !isConnected,
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
    onLongPressItem: ((ItemStack) -> Unit)? = null,
) {
    val player = playerData

    if (player == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                if (isOffline) "No cached inventory data" else "No player data",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        SectionHeader("Equipment")
        InventoryGrid(
            items = player.armor.ifEmpty {
                // Fall back to inventory armor slots
                player.inventory.filter { it.slot in 100..103 }
            } + listOfNotNull(player.offhand?.copy(slot = 104)),
            startSlot = 100,
            endSlot = 104,
            columns = 5,
            onLongPressItem = onLongPressItem,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf("Head", "Chest", "Legs", "Feet", "Off").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Hotbar (slots 0-8)
        SectionHeader("Hotbar")
        InventoryGrid(
            items = player.inventory,
            startSlot = 0,
            endSlot = 8,
            columns = 9,
            selectedSlot = player.selectedSlot,
            onLongPressItem = onLongPressItem,
        )

        // Main inventory (slots 9-35)
        SectionHeader("Inventory")
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

