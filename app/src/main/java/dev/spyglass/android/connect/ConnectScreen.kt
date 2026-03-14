package dev.spyglass.android.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.Bitmap
import dev.spyglass.android.connect.client.ConnectionState
import dev.spyglass.android.core.ui.*
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.connect.client.connectionStatusText

// ── Hub screen ───────────────────────────────────────────────────────────────

/**
 * Main Connect screen showing connection status, pairing controls,
 * and navigation to world viewer features.
 */
@Composable
fun ConnectScreen(
    viewModel: ConnectViewModel,
    onScanQr: () -> Unit,
    onBack: () -> Unit,
    onCharacter: () -> Unit = {},
    onInventory: () -> Unit = {},
    onEnderChest: () -> Unit = {},
    onChestFinder: () -> Unit = {},
    onMap: () -> Unit = {},
) {
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val worlds by viewModel.worlds.collectAsStateWithLifecycle()
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val selectedWorld by viewModel.selectedWorld.collectAsStateWithLifecycle()
    val playerList by viewModel.playerList.collectAsStateWithLifecycle()
    val selectedPlayerUuid by viewModel.selectedPlayerUuid.collectAsStateWithLifecycle()
    val playerSkin by viewModel.playerSkin.collectAsStateWithLifecycle()
    val capabilities by viewModel.desktopCapabilities.collectAsStateWithLifecycle()
    var showChestAnimation by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.setActiveScreen("connect")
        onDispose { viewModel.setActiveScreen(null) }
    }

    val isImageTheme = LocalThemeKey.current in ImageThemeKeys

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isImageTheme) Modifier.background(Color(0xB0080810))
                    else Modifier
                )
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.connect_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isImageTheme) Modifier
                            .background(Color(0x80000000), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                        else Modifier
                    ),
            )
            if (state.isConnected) {
                IconButton(onClick = { hapticConfirm(); viewModel.disconnect() }) {
                    Icon(Icons.Filled.LinkOff, contentDescription = stringResource(R.string.connect_disconnect_desc), tint = Color(0xFFF44336))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConnectionStatusCard(state)

            when {
                state is ConnectionState.Disconnected || state is ConnectionState.Error -> {
                    // Not connected — show pairing controls
                    DisconnectedContent(
                        state = state,
                        hasCachedData = selectedWorld != null,
                        worldName = selectedWorld,
                        playerSkin = playerSkin,
                        onScanQr = onScanQr,
                        onReconnect = { viewModel.tryReconnect() },
                        onCharacter = onCharacter,
                        onInventory = onInventory,
                        onEnderChest = onEnderChest,
                        onChestFinder = onChestFinder,
                        onMap = onMap,
                    )
                }

                state.isConnected -> {
                    // Connected — show world selector and quick access
                    ConnectedContent(
                        worlds = worlds,
                        selectedWorld = selectedWorld,
                        playerData = playerData,
                        playerList = playerList,
                        selectedPlayerUuid = selectedPlayerUuid,
                        capabilities = capabilities,
                        onSelectWorld = { viewModel.selectWorld(it) },
                        onSelectPlayer = { viewModel.selectPlayer(it) },
                        onInventory = onInventory,
                        onEnderChest = onEnderChest,
                        onChestFinder = onChestFinder,
                        onMap = onMap,
                        onLongPressGlobe = {
                            viewModel.requestChests()
                            showChestAnimation = true
                        },
                    )
                }

                else -> {
                    // Connecting/Pairing/Reconnecting — show chest loading animation
                    ChestLoadingAnimation(connectionState = state)
                }
            }
        }
    }

    // Chest loading overlay — triggered by long-pressing selected world row
    if (showChestAnimation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0101014))
                .clickable {},
            contentAlignment = Alignment.Center,
        ) {
            ChestLoadingOneShot(
                modifier = Modifier.padding(horizontal = 32.dp),
                onComplete = { showChestAnimation = false },
            )
        }
    }
    } // Box
}

// ── Status card ───────────────────────────────────────────────────────────────

@Composable
private fun ConnectionStatusCard(state: ConnectionState) {
    val (color, icon) = when (state) {
        is ConnectionState.Connected -> Color(0xFF4CAF50) to Icons.Filled.Wifi
        is ConnectionState.Connecting, is ConnectionState.Pairing, is ConnectionState.Reconnecting ->
            Color(0xFFFFC107) to Icons.Filled.Sync
        is ConnectionState.Error -> Color(0xFFF44336) to Icons.Filled.Error
        else -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Filled.WifiOff
    }

    ResultCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(
                    if (state.isConnected) stringResource(R.string.connect_status_connected) else stringResource(R.string.connect_status_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    connectionStatusText(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ── Disconnected content ─────────────────────────────────────────────────────

@Composable
private fun DisconnectedContent(
    state: ConnectionState,
    hasCachedData: Boolean = false,
    worldName: String? = null,
    playerSkin: Bitmap? = null,
    onScanQr: () -> Unit,
    onReconnect: () -> Unit,
    onCharacter: () -> Unit = {},
    onInventory: () -> Unit = {},
    onEnderChest: () -> Unit = {},
    onChestFinder: () -> Unit = {},
    onMap: () -> Unit = {},
) {
    val hapticClick = rememberHapticClick()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Connection error banner
        if (state is ConnectionState.Error) {
            val displayName = worldName ?: "server"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF44336).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFF44336).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Public,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.connect_failed_to_connect, displayName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF44336),
                )
            }
        }

        // Scan QR button
        Button(
            onClick = { hapticClick(); onScanQr() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.connect_scan_qr_button))
        }

        // Reconnect button
        OutlinedButton(
            onClick = { hapticClick(); onReconnect() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.connect_reconnect_last))
        }

        // Cached data quick links
        if (hasCachedData) {
            SectionHeader(stringResource(R.string.connect_cached_data_offline))
            val characterIcon: SpyglassIcon = if (playerSkin != null) SpyglassIcon.BitmapIcon(playerSkin) else PixelIcons.Steve
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConnectQuickLink(stringResource(R.string.connect_character), characterIcon, Modifier.weight(1f), onClick = onCharacter)
                ConnectQuickLink(stringResource(R.string.connect_inventory), PixelIcons.Backpack, Modifier.weight(1f), tint = Color.Unspecified, onClick = onInventory)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConnectQuickLink(stringResource(R.string.connect_ender_chest), PixelIcons.EnderChest, Modifier.weight(1f), tint = Color.Unspecified, onClick = onEnderChest)
                ConnectQuickLink(stringResource(R.string.connect_map), PixelIcons.Biome, Modifier.weight(1f), onClick = onMap)
            }
        }

        // Instructions
        ResultCard {
            Text(
                stringResource(R.string.connect_how_to_connect),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.connect_instructions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

    }
}

// ── Connected content ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConnectedContent(
    worlds: List<WorldInfo>,
    selectedWorld: String?,
    playerData: PlayerData?,
    playerList: List<PlayerSummary>,
    selectedPlayerUuid: String?,
    capabilities: Set<String>,
    onSelectWorld: (String) -> Unit,
    onSelectPlayer: (String?) -> Unit,
    onInventory: () -> Unit,
    onEnderChest: () -> Unit,
    onChestFinder: () -> Unit,
    onMap: () -> Unit,
    onLongPressGlobe: () -> Unit = {},
) {
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
    // World selector
    if (worlds.isNotEmpty()) {
        SectionHeader(stringResource(R.string.connect_select_world))
        worlds.forEach { world ->
            val isSelected = world.folderName == selectedWorld
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .border(
                        if (isSelected) 1.dp else 0.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .pointerInput(isSelected) {
                        detectTapGestures(
                            onTap = {
                                hapticClick()
                                onSelectWorld(world.folderName)
                            },
                            onLongPress = if (isSelected) {
                                { hapticConfirm(); onLongPressGlobe() }
                            } else null,
                        )
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Public,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        world.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${world.gameMode.replaceFirstChar { it.uppercase() }} • ${world.difficulty.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isSelected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Player selector (shown whenever a world is selected and player list has loaded)
    if (playerList.isNotEmpty() && selectedWorld != null) {
        Spacer(Modifier.height(4.dp))

        // First-connect prompt for multi-player worlds when no player chosen yet
        if (playerList.size > 1 && selectedPlayerUuid == null) {
            ResultCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.connect_choose_player),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        PlayerSelector(
            players = playerList,
            selectedUuid = selectedPlayerUuid,
            onSelectPlayer = onSelectPlayer,
        )
    }

    // Player summary (when world selected)
    if (playerData != null && selectedWorld != null) {
        Spacer(Modifier.height(8.dp))
        PlayerSummaryCard(playerData)
        Spacer(Modifier.height(8.dp))

        // Quick access grid
        SectionHeader(stringResource(R.string.connect_world_viewer))
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConnectQuickLink(stringResource(R.string.connect_inventory), PixelIcons.Backpack, Modifier.weight(1f), tint = Color.Unspecified, enabled = capabilities.contains(Capability.PLAYER_DATA), onClick = onInventory)
            ConnectQuickLink(stringResource(R.string.connect_ender_chest), PixelIcons.EnderChest, Modifier.weight(1f), tint = Color.Unspecified, enabled = capabilities.contains(Capability.PLAYER_DATA), onClick = onEnderChest)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConnectQuickLink(stringResource(R.string.connect_storage), PixelIcons.Search, Modifier.weight(1f), enabled = capabilities.contains(Capability.CHEST_CONTENTS), onClick = onChestFinder)
            ConnectQuickLink(stringResource(R.string.connect_map), PixelIcons.Biome, Modifier.weight(1f), enabled = capabilities.contains(Capability.MAP_RENDER), onClick = onMap)
        }
    }
}

// ── Player summary ────────────────────────────────────────────────────────────

@Composable
private fun PlayerSummaryCard(player: PlayerData) {
    ResultCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatColumn(stringResource(R.string.connect_health), "${player.health.toInt()} / 20")
            StatColumn(stringResource(R.string.connect_food), "${player.foodLevel} / 20")
            StatColumn(stringResource(R.string.connect_xp_level), "${player.xpLevel}")
            StatColumn(stringResource(R.string.dimension), player.dimension.replace("_", " ").replaceFirstChar { it.uppercase() })
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.connect_position_format, player.posX.toInt(), player.posY.toInt(), player.posZ.toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Player selector ───────────────────────────────────────────────────────────

@Composable
private fun PlayerSelector(
    players: List<PlayerSummary>,
    selectedUuid: String?,
    onSelectPlayer: (String?) -> Unit,
) {
    val hapticClick = rememberHapticClick()
    val hasMultipleNonOwner = players.count { !it.isOwner } > 0

    SectionHeader(stringResource(R.string.connect_players_count, players.size))

    if (hasMultipleNonOwner) {
        ResultCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.connect_fair_play_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    players.forEach { player ->
        val isSelected = player.uuid == (selectedUuid ?: players.firstOrNull()?.uuid)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else Color.Transparent,
                    RoundedCornerShape(8.dp),
                )
                .border(
                    if (isSelected) 1.dp else 0.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(8.dp),
                )
                .clickable { hapticClick(); onSelectPlayer(player.uuid) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.name ?: player.uuid.take(8),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (player.isOwner) {
                    Text(
                        stringResource(R.string.connect_world_owner),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Quick link card ───────────────────────────────────────────────────────────

@Composable
private fun ConnectQuickLink(
    label: String,
    icon: SpyglassIcon,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val hapticClick = rememberHapticClick()
    val alpha = if (enabled) 1f else 0.4f
    Column(
        modifier = modifier
            .background(LocalSurfaceCard.current, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable { hapticClick(); onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer { this.alpha = alpha },
        ) {
            SpyglassIconImage(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (!enabled) {
            Text(
                stringResource(R.string.connect_requires_desktop_update),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
