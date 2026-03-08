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
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.navigation.BrowseTarget
import kotlinx.coroutines.launch

/**
 * Ender Chest display: 27-slot grid (9 columns x 3 rows).
 * Tap shows item name, long-press opens item card.
 */
@Composable
fun EnderChestScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
    onBrowseTarget: (BrowseTarget) -> Unit = {},
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected
    val scope = rememberCoroutineScope()
    val hapticClick = rememberHapticClick()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { hapticClick(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Ender Chest", style = MaterialTheme.typography.titleMedium)
        }

        if (!isConnected && lastUpdated != null) {
            OfflineIndicator(lastUpdated, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        }

        EnderChestContent(
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
fun EnderChestContent(
    playerData: PlayerData?,
    isOffline: Boolean = false,
    onLongPressItem: ((ItemStack) -> Unit)? = null,
) {
    val player = playerData

    if (player == null || player.enderChest.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                if (isOffline) "No cached ender chest data" else "No ender chest data",
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
        SectionHeader("Ender Chest (${player.enderChest.size} items)")
        InventoryGrid(
            items = player.enderChest,
            startSlot = 0,
            endSlot = 26,
            columns = 9,
            onLongPressItem = onLongPressItem,
        )
    }
}

