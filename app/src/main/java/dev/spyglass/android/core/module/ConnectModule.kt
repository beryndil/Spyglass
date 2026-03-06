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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
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
import dev.spyglass.android.connect.client.ConnectionState
import dev.spyglass.android.core.ui.Emerald
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.core.ui.SpyglassIcon
import dev.spyglass.android.core.ui.SpyglassIconImage
import dev.spyglass.android.navigation.BrowseTarget

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
                val connectViewModel: ConnectViewModel = viewModel()
                QrScannerScreen(
                    onPairingDataScanned = { pairingData ->
                        connectViewModel.connectFromQr(pairingData)
                        nav.navigateBack()
                    },
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_character") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel()
                CharacterScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                    onBrowseTarget = { target -> nav.navigateToBrowseTab(target.tab, target.id) },
                )
            },
            ModuleRoute("connect_inventory") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel()
                InventoryScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                    onBrowseTarget = { target -> nav.navigateToBrowseTab(target.tab, target.id) },
                )
            },
            ModuleRoute("connect_enderchest") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel()
                EnderChestScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                    onBrowseTarget = { target -> nav.navigateToBrowseTab(target.tab, target.id) },
                )
            },
            ModuleRoute("connect_chestfinder") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel()
                ChestFinderScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_map") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel()
                MapScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_statistics") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel()
                StatisticsScreen(
                    viewModel = connectViewModel,
                    onBack = { nav.navigateBack() },
                )
            },
            ModuleRoute("connect_advancements") { _, nav ->
                val connectViewModel: ConnectViewModel = viewModel()
                AdvancementsScreen(
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
        val connectViewModel: ConnectViewModel = viewModel()
        val state by connectViewModel.connectionState.collectAsStateWithLifecycle()
        val worlds by connectViewModel.worlds.collectAsStateWithLifecycle()
        val selectedWorld by connectViewModel.selectedWorld.collectAsStateWithLifecycle()
        val playerSkin by connectViewModel.playerSkin.collectAsStateWithLifecycle()

        val links = connectLinks(playerSkin)

        SectionHeader("Spyglass Connect", icon = PixelIcons.Waypoints)
        Spacer(Modifier.height(8.dp))

        val hasCachedData = selectedWorld != null

        when {
            state is ConnectionState.Disconnected || state is ConnectionState.Error -> {
                DisconnectedCard(
                    state = state,
                    onScanQr = { scope.navigateToScanQr() },
                    onReconnect = { connectViewModel.tryReconnect() },
                )
                if (hasCachedData) {
                    Spacer(Modifier.height(8.dp))
                    QuickLinkGrid(links.map { it.first }) { index ->
                        scope.navigateTo(links[index].second)
                    }
                }
            }

            !state.isConnected -> {
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
            }

            state.isConnected && selectedWorld == null -> {
                StatusBar(state)
                Spacer(Modifier.height(8.dp))
                WorldSelector(
                    worlds = worlds,
                    onSelectWorld = {
                        connectViewModel.selectWorld(it)
                        connectViewModel.requestPlayerData()
                    },
                    onDisconnect = { connectViewModel.disconnect() },
                )
            }

            state.isConnected -> {
                StatusBar(state)
                Spacer(Modifier.height(8.dp))
                WorldHeader(
                    worlds = worlds,
                    selectedWorld = selectedWorld,
                    onSelectWorld = {
                        connectViewModel.selectWorld(it)
                        connectViewModel.requestPlayerData()
                    },
                    onDisconnect = { connectViewModel.disconnect() },
                )
                Spacer(Modifier.height(8.dp))
                QuickLinkGrid(links.map { it.first }) { index ->
                    scope.navigateTo(links[index].second)
                }
            }
        }
    }

    // ── Settings composable ─────────────────────────────────────────────────

    @Composable
    private fun ConnectSettingsContent() {
        val uriHandler = LocalUriHandler.current

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
                "hardknocks.university",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://hardknocks.university")
                },
            )
        }
    }

    // ── Shared composable helpers ───────────────────────────────────────────

    private data class QuickLink(
        val icon: SpyglassIcon,
        val label: String,
        val iconTint: Color = Color.Unspecified,
    )

    private fun connectLinks(playerSkin: android.graphics.Bitmap?): List<Pair<QuickLink, String>> {
        val characterIcon: SpyglassIcon = if (playerSkin != null) {
            SpyglassIcon.BitmapIcon(playerSkin)
        } else {
            PixelIcons.Steve
        }
        return listOf(
            QuickLink(characterIcon, "Character") to "connect_character",
            QuickLink(PixelIcons.Item, "Inventory") to "connect_inventory",
            QuickLink(PixelIcons.Enchant, "Ender Chest") to "connect_enderchest",
            QuickLink(PixelIcons.Search, "Chest Finder") to "connect_chestfinder",
            QuickLink(PixelIcons.Biome, "World Map") to "connect_map",
            QuickLink(PixelIcons.Anvil, "Statistics") to "connect_statistics",
            QuickLink(PixelIcons.Advancement, "Advancements") to "connect_advancements",
        )
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
            Text(link.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    @Composable
    private fun DisconnectedCard(
        state: ConnectionState,
        onScanQr: () -> Unit,
        onReconnect: () -> Unit,
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
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onReconnect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reconnect to Last Device")
            }
            if (state is ConnectionState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(state.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFFF44336))
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
