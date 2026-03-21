package dev.spyglass.android.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.R
import dev.spyglass.android.connect.*
import dev.spyglass.android.connect.client.ConnectionState
import dev.spyglass.android.connect.client.connectionStatusText
import dev.spyglass.android.core.ui.*

// ── Connect quick links ─────────────────────────────────────────────────────

@Composable
private fun connectLinks(
    playerSkin: android.graphics.Bitmap?,
    playerCount: Int = 1,
): List<Pair<QuickLink, String>> {
    val characterIcon: SpyglassIcon = if (playerSkin != null) {
        SpyglassIcon.BitmapIcon(playerSkin)
    } else {
        PixelIcons.Steve
    }
    return buildList {
        add(QuickLink(characterIcon,          stringResource(R.string.home_connect_link_character))    to "connect_character")
        add(QuickLink(PixelIcons.Backpack,    stringResource(R.string.home_connect_link_inventory))     to "connect_inventory")
        add(QuickLink(PixelIcons.EnderChest,  stringResource(R.string.home_connect_link_ender_chest))   to "connect_enderchest")
        add(QuickLink(PixelIcons.Storage,     stringResource(R.string.home_connect_link_storage))       to "connect_chestfinder")
        add(QuickLink(PixelIcons.Biome,       stringResource(R.string.home_connect_link_world_map))     to "connect_map")
        add(QuickLink(PixelIcons.Waypoints,   stringResource(R.string.home_connect_link_waypoints))     to "connect_waypoints")
        val wolfIcon = MobTextures.get("wolf") ?: PixelIcons.Mob
        add(QuickLink(wolfIcon,               stringResource(R.string.home_connect_link_pets))          to "connect_pets")
        if (playerCount > 1) {
            add(QuickLink(PixelIcons.Steve,   stringResource(R.string.home_connect_link_players))       to "connect_players")
        }
        add(QuickLink(PixelIcons.Anvil,       stringResource(R.string.home_connect_link_statistics))    to "connect_statistics")
    }
}

// ── Spyglass Connect hub ────────────────────────────────────────────────────

@Composable
internal fun HomeConnectSection(
    connectViewModel: ConnectViewModel,
    onScanQr: () -> Unit,
    onBrowseTarget: (dev.spyglass.android.navigation.BrowseTarget) -> Unit = {},
    onConnectNav: (String) -> Unit = {},
    onCalcTab: (Int) -> Unit = {},
) {
    val state by connectViewModel.connectionState.collectAsStateWithLifecycle()
    val worlds by connectViewModel.worlds.collectAsStateWithLifecycle()
    val selectedWorld by connectViewModel.selectedWorld.collectAsStateWithLifecycle()
    val playerSkin by connectViewModel.playerSkin.collectAsStateWithLifecycle()
    val playerList by connectViewModel.playerList.collectAsStateWithLifecycle()
    val links = connectLinks(playerSkin, playerList.size)

    val hasCachedData = selectedWorld != null

    when {
        // ── Connected, no world selected ──
        state.isConnected && selectedWorld == null -> {
            SectionHeader(stringResource(R.string.home_connect_title), icon = PixelIcons.Waypoints)
            Spacer(Modifier.height(8.dp))
            ConnectWorldSelector(
                worlds = worlds,
                selectedWorld = null,
                onSelectWorld = {
                    connectViewModel.selectWorld(it)
                    connectViewModel.requestPlayerData()
                },
                onDisconnect = { connectViewModel.disconnect() },
            )
        }

        // ── Has cached data (any connection state) — data-first ──
        hasCachedData -> {
            QuickLinkGrid(links.map { it.first }) { index ->
                onConnectNav(links[index].second)
            }
            Spacer(Modifier.height(6.dp))
            ConnectStatusLine(
                state = state,
                worlds = worlds,
                selectedWorld = selectedWorld,
                onSelectWorld = {
                    connectViewModel.selectWorld(it)
                    connectViewModel.requestPlayerData()
                },
                onDisconnect = { connectViewModel.disconnect() },
                onScanQr = onScanQr,
                onReconnect = { connectViewModel.tryReconnect() },
                onClearData = { connectViewModel.clearCachedData() },
            )
        }

        // ── No cached data (any state) — show pairing card or connecting spinner ──
        else -> {
            SectionHeader(stringResource(R.string.home_connect_title), icon = PixelIcons.Waypoints)
            Spacer(Modifier.height(8.dp))
            if (!state.isConnected && state !is ConnectionState.Disconnected && state !is ConnectionState.Error) {
                // Actively connecting/pairing — show spinner
                ResultCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            connectionStatusText(state),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                ConnectDisconnectedCard(
                    state = state,
                    onScanQr = onScanQr,
                    onReconnect = { connectViewModel.tryReconnect() },
                    showReconnect = false,
                )
            }
        }
    }
}

@Composable
private fun ConnectDisconnectedCard(
    state: ConnectionState,
    onScanQr: () -> Unit,
    onReconnect: () -> Unit,
    showReconnect: Boolean = true,
) {
    ResultCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpyglassIconImage(
                PixelIcons.Waypoints,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_connect_stream),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.home_connect_wifi),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onScanQr,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(stringResource(R.string.home_connect_scan_qr))
        }
        if (showReconnect) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onReconnect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_connect_reconnect_last))
            }
        }

        if (state is ConnectionState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                state.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFF44336),
            )
        }
    }
}

@Composable
private fun ConnectStatusLine(
    state: ConnectionState,
    worlds: List<WorldInfo>,
    selectedWorld: String?,
    onSelectWorld: (String) -> Unit,
    onDisconnect: () -> Unit,
    onScanQr: () -> Unit,
    onReconnect: () -> Unit,
    onClearData: () -> Unit = {},
) {
    val currentWorld = worlds.firstOrNull { it.folderName == selectedWorld }
    var worldMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val isInProgress = state is ConnectionState.Reconnecting ||
        state is ConnectionState.Connecting || state is ConnectionState.Pairing
    val isDisconnected = state is ConnectionState.Disconnected || state is ConnectionState.Error

    val connectedLabel = stringResource(R.string.home_connect_status_connected)
    val reconnectingLabel = stringResource(R.string.home_connect_status_reconnecting)
    val connectingLabel = stringResource(R.string.home_connect_status_connecting)
    val reconnectLabel = stringResource(R.string.home_connect_status_reconnect)

    val (statusColor, statusLabel) = when {
        state.isConnected -> Emerald to connectedLabel
        isInProgress -> Color(0xFFFFC107) to if (state is ConnectionState.Reconnecting) reconnectingLabel else connectingLabel
        else -> Color(0xFFF44336) to reconnectLabel
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(stringResource(R.string.home_connect_clear_data_title)) },
            text = { Text(stringResource(R.string.home_connect_clear_data_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataDialog = false
                    onClearData()
                }) { Text(stringResource(R.string.home_connect_clear_data_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Left side: globe icon + world name (clickable to switch worlds)
        if (currentWorld != null) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = if (state.isConnected && worlds.size > 1) {
                        Modifier.clickable { worldMenuExpanded = true }
                    } else {
                        Modifier
                    },
                ) {
                    SpyglassIconImage(
                        PixelIcons.Globe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        currentWorld.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // World switcher dropdown anchored to world name
                if (worldMenuExpanded && worlds.size > 1) {
                    DropdownMenu(
                        expanded = worldMenuExpanded,
                        onDismissRequest = { worldMenuExpanded = false },
                    ) {
                        worlds.forEach { world ->
                            DropdownMenuItem(
                                text = { Text(world.displayName) },
                                onClick = {
                                    worldMenuExpanded = false
                                    onSelectWorld(world.folderName)
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Right side: tappable status indicator with dropdown menu
        Box {
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                modifier = Modifier.clickable { statusMenuExpanded = true },
            )
            DropdownMenu(
                expanded = statusMenuExpanded,
                onDismissRequest = { statusMenuExpanded = false },
            ) {
                if (state.isConnected) {
                    if (worlds.size > 1) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_connect_switch_world)) },
                            onClick = {
                                statusMenuExpanded = false
                                worldMenuExpanded = true
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_connect_disconnect)) },
                        onClick = {
                            statusMenuExpanded = false
                            onDisconnect()
                        },
                    )
                } else if (isDisconnected) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_connect_status_reconnect)) },
                        onClick = {
                            statusMenuExpanded = false
                            onReconnect()
                        },
                    )
                } else if (isInProgress) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cancel)) },
                        onClick = {
                            statusMenuExpanded = false
                            onDisconnect()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_connect_scan_new_device)) },
                    onClick = {
                        statusMenuExpanded = false
                        onScanQr()
                    },
                )
                SpyglassDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_connect_clear_data_title), color = Color(0xFFF44336)) },
                    onClick = {
                        statusMenuExpanded = false
                        showClearDataDialog = true
                    },
                )
            }
        }
    }
}

@Composable
private fun ConnectWorldSelector(
    worlds: List<WorldInfo>,
    selectedWorld: String?,
    onSelectWorld: (String) -> Unit,
    onDisconnect: () -> Unit,
) {
    ResultCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.home_connect_select_world),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.home_connect_disconnect),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFF44336),
                modifier = Modifier.clickable { onDisconnect() },
            )
        }
        Spacer(Modifier.height(8.dp))
        worlds.forEach { world ->
            val isModded = world.isModded
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectWorld(world.folderName) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SpyglassIconImage(
                    PixelIcons.Globe,
                    contentDescription = null,
                    tint = if (isModded) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        world.displayName + if (isModded) stringResource(R.string.home_connect_modded_suffix) else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isModded) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    )
                    Text(
                        stringResource(R.string.home_connect_game_mode_difficulty, world.gameMode.replaceFirstChar { it.uppercase() }, world.difficulty.replaceFirstChar { it.uppercase() }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (worlds.isEmpty()) {
            Text(
                stringResource(R.string.home_connect_no_worlds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
