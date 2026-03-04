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
import dev.spyglass.android.connect.PlayerData
import dev.spyglass.android.core.ui.SectionHeader

/**
 * Ender Chest display: 27-slot grid (9 columns x 3 rows).
 * Reuses InventorySlotView and InventoryGrid.
 */
@Composable
fun EnderChestScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
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
            Text("Ender Chest", style = MaterialTheme.typography.titleMedium)
        }

        EnderChestContent(playerData = playerData)
    }
}

@Composable
fun EnderChestContent(playerData: PlayerData?) {
    val player = playerData

    if (player == null || player.enderChest.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No ender chest data", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        SectionHeader("Ender Chest (${player.enderChest.size} items)")
        InventoryGrid(
            items = player.enderChest,
            startSlot = 0,
            endSlot = 26,
            columns = 9,
        )
    }
}
