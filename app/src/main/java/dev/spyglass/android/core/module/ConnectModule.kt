package dev.spyglass.android.core.module

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.QrScannerScreen
import dev.spyglass.android.connect.WorldInfo
import dev.spyglass.android.connect.character.CharacterScreen
import dev.spyglass.android.connect.inventory.InventoryScreen
import dev.spyglass.android.connect.inventory.EnderChestScreen
import dev.spyglass.android.connect.chestfinder.ChestFinderScreen
import dev.spyglass.android.connect.map.MapScreen
import dev.spyglass.android.connect.statistics.StatisticsScreen
import dev.spyglass.android.connect.advancements.AdvancementsScreen
import dev.spyglass.android.connect.players.PlayersScreen
import dev.spyglass.android.connect.players.CompareScreen
import dev.spyglass.android.connect.pets.PetsScreen
import dev.spyglass.android.connect.waypoints.ConnectWaypointsScreen
import dev.spyglass.android.connect.client.ConnectionState
import dev.spyglass.android.connect.client.connectionStatusText
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.Emerald
import dev.spyglass.android.core.ui.MobTextures
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.core.ui.SpyglassIcon
import dev.spyglass.android.core.ui.SpyglassIconImage
import dev.spyglass.android.navigation.BrowseTarget
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch

/**
 * Connect module — owns WebSocket client, QR scanner, mDNS discovery,
 * and all connect sub-screens.
 */
object ConnectModule : SpyglassModule {

    override val id = "connect"
    override val name = "Spyglass Connect"
    override val icon: SpyglassIcon = PixelIcons.Waypoints
    override val priority = 5
    override val canDisable = true

    // ── Home sections ───────────────────────────────────────────────────────

    override fun homeSections(): List<HomeSection> = listOf(
        HomeSection("connect", 10) { scope -> ConnectHomeContent(scope) },
    )

    // ── Settings sections ───────────────────────────────────────────────────

    override fun settingsSections(): List<SettingsSection> = listOf(
        SettingsSection("connect", "Spyglass Connect", 50) { ConnectSettingsContent() },
    )

    // ── Nav routes ──────────────────────────────────────────────────────────

    override fun navRoutes(): List<ModuleRoute> {
        return listOf(
            ModuleRoute("connect_scan") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                QrScannerScreen(
                    onPairingDataScanned = { pairingData ->
                        connectViewModel.connectFromQr(pairingData)
                        nav.navigateBack()
                    },
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_character") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                CharacterScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                    onBrowseTarget = { target -> nav.navigateToBrowseTab(target.tab, target.id) },
                )
            },
            ModuleRoute("connect_inventory") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                InventoryScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                    onBrowseTarget = { target -> nav.navigateToBrowseTab(target.tab, target.id) },
                )
            },
            ModuleRoute("connect_enderchest") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                EnderChestScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                    onBrowseTarget = { target -> nav.navigateToBrowseTab(target.tab, target.id) },
                )
            },
            ModuleRoute("connect_chestfinder") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                ChestFinderScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_map") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                MapScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_statistics") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                StatisticsScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_advancements") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                AdvancementsScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_players") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                PlayersScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                    onSelectPlayer = { uuid ->
                        connectViewModel.selectPlayer(uuid)
                        nav.navigateTo("connect_character")
                    },
                    onCompare = { uuid ->
                        connectViewModel.requestComparePlayer(uuid)
                        nav.navigateTo("connect_compare")
                    },
                )
            },
            ModuleRoute("connect_compare") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                CompareScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_pets") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                PetsScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_waypoints") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
                ConnectWaypointsScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
        )
    }

    override fun bottomNavItems(): List<BottomNavItem> = emptyList()

    override fun searchProvider(): SearchProvider? = null

    // ── Home composable ─────────────────────────────────────────────────────

    @Composable
    private fun ConnectHomeContent(scope: HomeSectionScope) {
        val connectViewModel: ConnectViewModel = viewModel(LocalContext.current as ComponentActivity)
        val state by connectViewModel.connectionState.collectAsStateWithLifecycle()
        val worlds by connectViewModel.worlds.collectAsStateWithLifecycle()
        val selectedWorld by connectViewModel.selectedWorld.collectAsStateWithLifecycle()
        val playerSkin by connectViewModel.playerSkin.collectAsStateWithLifecycle()
        val playerList by connectViewModel.playerList.collectAsStateWithLifecycle()

        val links = connectLinks(playerSkin, playerList.size)
        val hasCachedData = selectedWorld != null

        SectionHeader(stringResource(R.string.settings_connect_title), icon = PixelIcons.Waypoints)
        Spacer(Modifier.height(8.dp))

        when {
            // ── Connected, no world selected ──
            state.isConnected && selectedWorld == null -> {
                WorldSelector(
                    worlds = worlds,
                    onSelectWorld = {
                        connectViewModel.selectWorld(it)
                        connectViewModel.requestPlayerData()
                    },
                    onDisconnect = { connectViewModel.disconnect() },
                )
            }

            // ── Has cached data (any connection state) — data-first ──
            hasCachedData -> {
                ConnectStatusLine(
                    state = state,
                    worlds = worlds,
                    selectedWorld = selectedWorld,
                    onSelectWorld = {
                        connectViewModel.selectWorld(it)
                        connectViewModel.requestPlayerData()
                    },
                    onDisconnect = { connectViewModel.disconnect() },
                    onScanQr = { scope.navigateToScanQr() },
                    onReconnect = { connectViewModel.tryReconnect() },
                    onClearData = { connectViewModel.clearCachedData() },
                )
                Spacer(Modifier.height(6.dp))
                QuickLinkGrid(links.map { it.first }) { index ->
                    scope.navigateTo(links[index].second)
                }
            }

            // ── No cached data (any state) — show pairing card or connecting spinner ──
            else -> {
                if (!state.isConnected && state !is ConnectionState.Disconnected && state !is ConnectionState.Error) {
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
                    DisconnectedCard(
                        state = state,
                        onScanQr = { scope.navigateToScanQr() },
                        onReconnect = { connectViewModel.tryReconnect() },
                        showReconnect = false,
                    )
                }
            }
        }
    }

    // ── Settings composable ─────────────────────────────────────────────────

    @Composable
    private fun ConnectSettingsContent() {
        val uriHandler = LocalUriHandler.current
        val context = LocalContext.current
        val connectViewModel: ConnectViewModel = viewModel(context as ComponentActivity)
        val worlds by connectViewModel.worlds.collectAsStateWithLifecycle()
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        var cachedWorlds by remember { mutableStateOf<List<String>>(emptyList()) }
        var showClearAllDialog by remember { mutableStateOf(false) }
        var worldToDelete by remember { mutableStateOf<String?>(null) }

        // Load cached world list
        androidx.compose.runtime.LaunchedEffect(Unit) {
            cachedWorlds = dev.spyglass.android.connect.client.ConnectCache.listCachedWorlds(context)
        }

        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = { Text(stringResource(R.string.settings_connect_clear_all)) },
                text = { Text(stringResource(R.string.settings_connect_clear_all_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showClearAllDialog = false
                        connectViewModel.clearCachedData()
                        cachedWorlds = emptyList()
                    }) { Text(stringResource(R.string.settings_connect_clear_all_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        worldToDelete?.let { world ->
            AlertDialog(
                onDismissRequest = { worldToDelete = null },
                title = { Text(stringResource(R.string.settings_connect_clear_world_title)) },
                text = { Text(stringResource(R.string.settings_connect_clear_world_message, world)) },
                confirmButton = {
                    TextButton(onClick = {
                        val w = world
                        worldToDelete = null
                        scope.launch {
                            dev.spyglass.android.connect.client.ConnectCache.deleteWorld(context, w)
                            cachedWorlds = dev.spyglass.android.connect.client.ConnectCache.listCachedWorlds(context)
                        }
                    }) { Text(stringResource(R.string.home_connect_clear_data_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { worldToDelete = null }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        SectionHeader(stringResource(R.string.settings_connect_title))
        ResultCard {
            Text(
                stringResource(R.string.settings_connect_stream),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_connect_managed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_connect_download_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "hardknocks.com/spyglass-connect",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://hardknocks.com/spyglass-connect")
                },
            )
        }

        // ── Cached Data Management ──
        if (cachedWorlds.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_connect_cached_data),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            ResultCard {
                cachedWorlds.forEach { world ->
                    val displayName = worlds.firstOrNull { it.folderName == world }?.displayName ?: world
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpyglassIconImage(
                            PixelIcons.Globe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.home_connect_clear_data_confirm),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF44336),
                            modifier = Modifier.clickable { worldToDelete = world },
                        )
                    }
                }
                SpyglassDivider()
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_connect_clear_all),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF44336),
                    modifier = Modifier
                        .clickable { showClearAllDialog = true }
                        .padding(vertical = 4.dp),
                )
            }
        }
    }

    // ── Shared composable helpers ───────────────────────────────────────────

    private data class QuickLink(
        val icon: SpyglassIcon,
        val label: String,
        val iconTint: Color = Color.Unspecified,
    )

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
            add(QuickLink(characterIcon, stringResource(R.string.home_connect_link_character)) to "connect_character")
            add(QuickLink(PixelIcons.Backpack, stringResource(R.string.home_connect_link_inventory)) to "connect_inventory")
            add(QuickLink(PixelIcons.EnderChest, stringResource(R.string.home_connect_link_ender_chest)) to "connect_enderchest")
            add(QuickLink(PixelIcons.Storage, stringResource(R.string.home_connect_link_storage)) to "connect_chestfinder")
            add(QuickLink(PixelIcons.Biome, stringResource(R.string.home_connect_link_world_map)) to "connect_map")
            add(QuickLink(PixelIcons.Waypoints, stringResource(R.string.home_connect_link_waypoints)) to "connect_waypoints")
            val wolfIcon = MobTextures.get("wolf") ?: PixelIcons.Mob
            add(QuickLink(wolfIcon, stringResource(R.string.home_connect_link_pets)) to "connect_pets")
            if (playerCount > 1) {
                add(QuickLink(PixelIcons.Steve, stringResource(R.string.home_connect_link_players)) to "connect_players")
            }
            add(QuickLink(PixelIcons.Anvil, stringResource(R.string.home_connect_link_statistics)) to "connect_statistics")
            add(QuickLink(PixelIcons.Advancement, stringResource(R.string.home_connect_link_advancements)) to "connect_advancements")
        }
    }

    @Composable
    private fun QuickLinkGrid(links: List<QuickLink>, onTap: (Int) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                links.forEachIndexed { i, link ->
                    if (i % 2 == 0) QuickLinkCard(link) { onTap(i) }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                links.forEachIndexed { i, link ->
                    if (i % 2 != 0) QuickLinkCard(link) { onTap(i) }
                }
            }
        }
    }

    @Composable
    private fun QuickLinkCard(link: QuickLink, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(dev.spyglass.android.core.ui.LocalSurfaceCard.current, RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpyglassIconImage(link.icon, contentDescription = null, tint = if (link.iconTint == Color.Unspecified) MaterialTheme.colorScheme.primary else link.iconTint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(link.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun DisconnectedCard(
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
                Text(state.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFFF44336))
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
                            text = { Text(reconnectLabel) },
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
    private fun StatusBar(state: ConnectionState) {
        val deviceName = (state as? ConnectionState.Connected)?.deviceName ?: ""
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Emerald.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(Emerald, CircleShape))
            Text(
                if (deviceName.isNotEmpty()) stringResource(R.string.home_connect_connected_to, deviceName) else stringResource(R.string.home_connect_status_connected),
                style = MaterialTheme.typography.labelSmall,
                color = Emerald,
                modifier = Modifier.weight(1f),
            )
        }
    }

    @Composable
    private fun WorldSelector(
        worlds: List<WorldInfo>,
        onSelectWorld: (String) -> Unit,
        onDisconnect: () -> Unit,
    ) {
        ResultCard {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_connect_select_world), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.home_connect_disconnect), style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336), modifier = Modifier.clickable { onDisconnect() })
            }
            Spacer(Modifier.height(8.dp))
            worlds.forEach { world ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectWorld(world.folderName) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SpyglassIconImage(PixelIcons.Globe, contentDescription = null, tint = if (world.isModded) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(world.displayName + if (world.isModded) stringResource(R.string.home_connect_modded_suffix) else "", style = MaterialTheme.typography.bodyMedium, color = if (world.isModded) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified)
                        Text(stringResource(R.string.home_connect_game_mode_difficulty, world.gameMode.replaceFirstChar { it.uppercase() }, world.difficulty.replaceFirstChar { it.uppercase() }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (worlds.isEmpty()) {
                Text(stringResource(R.string.home_connect_no_worlds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    @Composable
    private fun WorldHeader(
        worlds: List<WorldInfo>,
        selectedWorld: String?,
        onSelectWorld: (String) -> Unit,
        onDisconnect: () -> Unit,
    ) {
        val currentWorld = worlds.firstOrNull { it.folderName == selectedWorld }
        var expanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpyglassIconImage(PixelIcons.Globe, contentDescription = null, tint = Emerald, modifier = Modifier.size(20.dp))
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    currentWorld?.displayName ?: stringResource(R.string.home_connect_unknown_world),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { expanded = true },
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    worlds.forEach { world ->
                        DropdownMenuItem(
                            text = { Text(world.displayName) },
                            onClick = { expanded = false; onSelectWorld(world.folderName) },
                        )
                    }
                }
            }
            Text(stringResource(R.string.home_connect_disconnect), style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336), modifier = Modifier.clickable { onDisconnect() })
        }
    }
}
