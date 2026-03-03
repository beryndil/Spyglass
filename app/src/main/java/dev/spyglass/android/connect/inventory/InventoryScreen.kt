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
import dev.spyglass.android.core.ui.SectionHeader

/**
 * Player inventory display: 36-slot grid (9 columns x 4 rows) + 4 armor + 1 offhand.
 * Uses the existing ItemTextures system via InventorySlotView.
 */
@Composable
fun InventoryScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val player = playerData

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

        if (player == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No player data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Armor + Offhand
            SectionHeader("Equipment")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                // Armor: slots 103 (head) → 100 (feet)
                val armorSlots = listOf(103, 102, 101, 100)
                armorSlots.forEach { slot ->
                    val item = player.armor.firstOrNull { it.slot == slot }
                        ?: player.inventory.firstOrNull { it.slot == slot }
                    InventorySlotView(item = item)
                }
                Spacer(Modifier.width(16.dp))
                // Offhand
                InventorySlotView(item = player.offhand)
            }

            // Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                listOf("Head", "Chest", "Legs", "Feet").forEach {
                    Text(
                        it,
                        modifier = Modifier.width(48.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "Off",
                    modifier = Modifier.width(48.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Hotbar (slots 0-8)
            SectionHeader("Hotbar")
            InventoryGrid(
                items = player.inventory,
                startSlot = 0,
                endSlot = 8,
                columns = 9,
            )

            // Main inventory (slots 9-35)
            SectionHeader("Inventory")
            InventoryGrid(
                items = player.inventory,
                startSlot = 9,
                endSlot = 35,
                columns = 9,
            )
        }
    }
}

@Composable
fun InventoryGrid(
    items: List<ItemStack>,
    startSlot: Int,
    endSlot: Int,
    columns: Int,
) {
    val slotCount = endSlot - startSlot + 1
    val rows = (slotCount + columns - 1) / columns

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0 until columns) {
                    val slot = startSlot + row * columns + col
                    if (slot > endSlot) {
                        Spacer(Modifier.size(48.dp))
                    } else {
                        val item = items.firstOrNull { it.slot == slot }
                        InventorySlotView(item = item)
                    }
                }
            }
        }
    }
}
