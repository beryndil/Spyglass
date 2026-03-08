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

        SectionHeader("Spyglass Connect", icon = PixelIcons.Waypoints)
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
                                state.statusText,
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
                title = { Text("Clear All Data") },
                text = { Text("Clear all cached Connect data for every world? Your device pairing will be kept.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearAllDialog = false
                        connectViewModel.clearCachedData()
                        cachedWorlds = emptyList()
                    }) { Text("Clear All") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
                },
            )
        }

        worldToDelete?.let { world ->
            AlertDialog(
                onDismissRequest = { worldToDelete = null },
                title = { Text("Clear World Data") },
                text = { Text("Clear cached data for \"$world\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        val w = world
                        worldToDelete = null
                        scope.launch {
                            dev.spyglass.android.connect.client.ConnectCache.deleteWorld(context, w)
                            cachedWorlds = dev.spyglass.android.connect.client.ConnectCache.listCachedWorlds(context)
                        }
                    }) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { worldToDelete = null }) { Text("Cancel") }
                },
            )
        }

        SectionHeader("Spyglass Connect")
        ResultCard {
            Text(
                "Stream Minecraft world data from your PC over local WiFi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Paired device info and connection settings are managed from the Connect screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Download Spyglass Connect for your computer:",
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
                "Cached Data",
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
                            "Clear",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF44336),
                            modifier = Modifier.clickable { worldToDelete = world },
                        )
                    }
                }
                SpyglassDivider()
                Spacer(Modifier.height(4.dp))
                Text(
                    "Clear All Data",
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
            add(QuickLink(characterIcon, "Character") to "connect_character")
            add(QuickLink(PixelIcons.Backpack, "Inventory") to "connect_inventory")
            add(QuickLink(PixelIcons.Enchant, "Ender Chest") to "connect_enderchest")
            add(QuickLink(PixelIcons.Storage, "Chest Finder") to "connect_chestfinder")
            add(QuickLink(PixelIcons.Biome, "World Map") to "connect_map")
            add(QuickLink(PixelIcons.Waypoints, "Waypoints") to "connect_waypoints")
            val wolfIcon = MobTextures.get("wolf") ?: PixelIcons.Mob
            add(QuickLink(wolfIcon, "Pets") to "connect_pets")
            if (playerCount > 1) {
                add(QuickLink(PixelIcons.Steve, "Players") to "connect_players")
            }
            add(QuickLink(PixelIcons.Anvil, "Statistics") to "connect_statistics")
            add(QuickLink(PixelIcons.Advancement, "Advancements") to "connect_advancements")
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
                        "Stream Minecraft world data from your PC",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Both devices on the same WiFi",
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
                Text("Scan QR Code")
            }
            if (showReconnect) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onReconnect,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reconnect to Last Device")
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

        val (statusColor, statusLabel) = when {
            state.isConnected -> Emerald to "Connected"
            isInProgress -> Color(0xFFFFC107) to if (state is ConnectionState.Reconnecting) "Reconnecting" else "Connecting"
            else -> Color(0xFFF44336) to "Reconnect"
        }

        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear Data") },
                text = { Text("Clear all cached Connect data? Your device pairing will be kept.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDataDialog = false
                        onClearData()
                    }) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
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
                                text = { Text("Switch World") },
                                onClick = {
                                    statusMenuExpanded = false
                                    worldMenuExpanded = true
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Disconnect") },
                            onClick = {
                                statusMenuExpanded = false
                                onDisconnect()
                            },
                        )
                    } else if (isDisconnected) {
                        DropdownMenuItem(
                            text = { Text("Reconnect") },
                            onClick = {
                                statusMenuExpanded = false
                                onReconnect()
                            },
                        )
                    } else if (isInProgress) {
                        DropdownMenuItem(
                            text = { Text("Cancel") },
                            onClick = {
                                statusMenuExpanded = false
                                onDisconnect()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Scan New Device") },
                        onClick = {
                            statusMenuExpanded = false
                            onScanQr()
                        },
                    )
                    SpyglassDivider()
                    DropdownMenuItem(
                        text = { Text("Clear Data", color = Color(0xFFF44336)) },
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
                "Connected" + if (deviceName.isNotEmpty()) " to $deviceName" else "",
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
                Text("Select a World", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text("Disconnect", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336), modifier = Modifier.clickable { onDisconnect() })
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
                        Text(world.displayName + if (world.isModded) " (Modded)" else "", style = MaterialTheme.typography.bodyMedium, color = if (world.isModded) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified)
                        Text("${world.gameMode.replaceFirstChar { it.uppercase() }} \u2022 ${world.difficulty.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (worlds.isEmpty()) {
                Text("No worlds found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    currentWorld?.displayName ?: "Unknown World",
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
            Text("Disconnect", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336), modifier = Modifier.clickable { onDisconnect() })
        }
    }
}
