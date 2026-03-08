package dev.spyglass.android.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
private fun browseTabNames() = listOf(
    stringResource(R.string.browse_tab_blocks), stringResource(R.string.browse_tab_items),
    stringResource(R.string.browse_tab_recipes), stringResource(R.string.browse_tab_mobs),
    stringResource(R.string.browse_tab_trades), stringResource(R.string.browse_tab_biomes),
    stringResource(R.string.browse_tab_structures), stringResource(R.string.browse_tab_enchants),
    stringResource(R.string.browse_tab_potions), stringResource(R.string.browse_tab_advancements),
    stringResource(R.string.browse_tab_commands), stringResource(R.string.browse_tab_reference),
    stringResource(R.string.browse_tab_versions),
)

@Composable
private fun toolTabNames() = listOf(
    stringResource(R.string.calc_tab_todo), stringResource(R.string.calc_tab_shopping),
    stringResource(R.string.calc_tab_enchanting), stringResource(R.string.calc_tab_fill),
    stringResource(R.string.calc_tab_shapes), stringResource(R.string.calc_tab_maze),
    stringResource(R.string.calc_tab_storage), stringResource(R.string.calc_tab_smelt),
    stringResource(R.string.calc_tab_nether), stringResource(R.string.calc_tab_game_clock),
    stringResource(R.string.calc_tab_light), stringResource(R.string.calc_tab_notes),
    stringResource(R.string.calc_tab_waypoints), stringResource(R.string.calc_tab_redstone),
    stringResource(R.string.calc_tab_librarian), stringResource(R.string.calc_tab_food),
    stringResource(R.string.calc_tab_banners), stringResource(R.string.calc_tab_trims),
    stringResource(R.string.calc_tab_loot),
)

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onCalcTab: (Int) -> Unit = {},
    onAbout: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onChangelog: () -> Unit = {},
    vm: SettingsViewModel = viewModel(),
) {
    val defaultBrowseTab    by vm.defaultBrowseTab.collectAsStateWithLifecycle()
    val defaultToolTab      by vm.defaultToolTab.collectAsStateWithLifecycle()
    val showTipOfDay        by vm.showTipOfDay.collectAsStateWithLifecycle()
    val showFavoritesOnHome by vm.showFavoritesOnHome.collectAsStateWithLifecycle()
    val gameClockEnabled    by vm.gameClockEnabled.collectAsStateWithLifecycle()
    val allFavorites        by vm.allFavorites.collectAsStateWithLifecycle()
    val backgroundTheme     by vm.backgroundTheme.collectAsStateWithLifecycle()
    val minecraftEdition    by vm.minecraftEdition.collectAsStateWithLifecycle()
    val minecraftVersion    by vm.minecraftVersion.collectAsStateWithLifecycle()
    val versionFilterMode   by vm.versionFilterMode.collectAsStateWithLifecycle()
    val analyticsConsent    by vm.analyticsConsent.collectAsStateWithLifecycle()
    val crashConsent        by vm.crashConsent.collectAsStateWithLifecycle()
    val adPersonalizationConsent by vm.adPersonalizationConsent.collectAsStateWithLifecycle()
    val hapticFeedback      by vm.hapticFeedback.collectAsStateWithLifecycle()
    val reduceAnimations    by vm.reduceAnimations.collectAsStateWithLifecycle()
    val dynamicColor        by vm.dynamicColor.collectAsStateWithLifecycle()
    val highContrast        by vm.highContrast.collectAsStateWithLifecycle()
    val defaultStartupTab   by vm.defaultStartupTab.collectAsStateWithLifecycle()
    val hideUnobtainable    by vm.hideUnobtainableBlocks.collectAsStateWithLifecycle()
    val showExperimental    by vm.showExperimental.collectAsStateWithLifecycle()
    val appLockEnabled      by vm.appLockEnabled.collectAsStateWithLifecycle()
    val syncFrequencyHours  by vm.syncFrequencyHours.collectAsStateWithLifecycle()
    val offlineMode         by vm.offlineMode.collectAsStateWithLifecycle()
    val textureState        by TextureManager.state.collectAsStateWithLifecycle()

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var versionExpanded by remember { mutableStateOf(false) }
    var storageBytes by remember { mutableStateOf(vm.getTextureStorageBytes()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "back") {
            IconButton(onClick = { hapticClick(); onBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionHeader(stringResource(R.string.settings))
        }

        // ── Theme ───────────────────────────────────────────────────────
        item(key = "theme") {
            SectionHeader(stringResource(R.string.settings_theme))
            ResultCard {
                Text(
                    stringResource(R.string.settings_background),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ThemeOrder.forEach { key ->
                        val info = ThemeInfoMap[key] ?: return@forEach
                        val isSelected = backgroundTheme == key
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(info.background, CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = borderColor,
                                    shape = CircleShape,
                                )
                                .clickable { hapticClick(); vm.setBackgroundTheme(key) },
                        )
                    }
                }
                Text(
                    ThemeInfoMap[backgroundTheme]?.label ?: "Obsidian",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )

                SpyglassDivider()

                // Dynamic Color (Material You) — Android 12+ only
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsToggle(
                        title = stringResource(R.string.settings_dynamic_color),
                        description = stringResource(R.string.settings_dynamic_color_desc),
                        checked = dynamicColor,
                        onCheckedChange = vm::setDynamicColor,
                    )
                    SpyglassDivider()
                }

                SettingsToggle(
                    title = stringResource(R.string.settings_high_contrast),
                    description = stringResource(R.string.settings_high_contrast_desc),
                    checked = highContrast,
                    onCheckedChange = vm::setHighContrast,
                )
            }
        }

        // ── Game Version ────────────────────────────────────────────────
        item(key = "game_version") {
            SectionHeader("Game Version")
            ResultCard {
                Text(
                    "Filter content by Minecraft edition and version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Text("Edition", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                TogglePill(
                    options = listOf("Java", "Bedrock"),
                    selected = if (minecraftEdition == "bedrock") 1 else 0,
                    onSelect = { vm.setMinecraftEdition(if (it == 1) "bedrock" else "java") },
                )

                val versions = if (minecraftEdition == "bedrock") MinecraftVersions.BEDROCK_VERSIONS else MinecraftVersions.JAVA_VERSIONS
                val displayVersion = minecraftVersion.ifBlank { "Latest" }
                Text("Version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Box {
                    OutlinedButton(onClick = { hapticClick(); versionExpanded = true }) {
                        Text(displayVersion, color = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(expanded = versionExpanded, onDismissRequest = { versionExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Latest", color = if (minecraftVersion.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                            onClick = { hapticClick(); vm.setMinecraftVersion(""); versionExpanded = false },
                        )
                        versions.reversed().forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v, color = if (minecraftVersion == v) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                onClick = { hapticClick(); vm.setMinecraftVersion(v); versionExpanded = false },
                            )
                        }
                    }
                }

                Text("Filter Mode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val modes = listOf("show_all" to "Show All", "highlight" to "Highlight Unavailable", "hide" to "Hide Unavailable")
                    modes.forEach { (key, label) ->
                        FilterChip(
                            selected = versionFilterMode == key,
                            onClick = { hapticClick(); vm.setVersionFilterMode(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            val selectedVersion = minecraftVersion.ifBlank { MinecraftVersions.JAVA_VERSIONS.last() }
            val updateInfo = remember(selectedVersion) { MinecraftUpdates.forVersion(selectedVersion) }
            if (updateInfo != null) {
                Spacer(Modifier.height(4.dp))
                VersionCard(
                    version = updateInfo.version,
                    name = updateInfo.name,
                    releaseDate = updateInfo.releaseDate,
                    accentColor = updateInfo.color,
                    icon = updateInfo.icon,
                    changelog = updateInfo.changelog,
                )
            }
        }

        // ── Default Startup Screen ──────────────────────────────────────
        item(key = "startup_tab") {
            SectionHeader(stringResource(R.string.settings_startup_tab))
            ResultCard {
                Text(
                    stringResource(R.string.settings_startup_tab_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val tabs = listOf(
                        stringResource(R.string.nav_home),
                        stringResource(R.string.nav_browse),
                        stringResource(R.string.nav_tools),
                        stringResource(R.string.nav_search),
                    )
                    tabs.forEachIndexed { i, name ->
                        FilterChip(
                            selected = defaultStartupTab == i,
                            onClick = { hapticClick(); vm.setDefaultStartupTab(i) },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }

        // ── Default Browse Tab ──────────────────────────────────────────
        item(key = "browse_tab") {
            SectionHeader(stringResource(R.string.settings_default_browse_tab))
            ResultCard {
                Text(
                    stringResource(R.string.settings_browse_tab_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    browseTabNames().forEachIndexed { i, name ->
                        FilterChip(
                            selected = defaultBrowseTab == i,
                            onClick = { hapticClick(); vm.setDefaultBrowseTab(i) },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }

        // ── Default Tool Tab ────────────────────────────────────────────
        item(key = "tool_tab") {
            SectionHeader(stringResource(R.string.settings_default_tool_tab))
            ResultCard {
                Text(
                    stringResource(R.string.settings_tool_tab_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    toolTabNames().forEachIndexed { i, name ->
                        FilterChip(
                            selected = defaultToolTab == i,
                            onClick = { hapticClick(); vm.setDefaultToolTab(i) },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }

        // ── Display Toggles ─────────────────────────────────────────────
        item(key = "display") {
            SectionHeader(stringResource(R.string.settings_display))
            ResultCard {
                SettingsToggle(
                    title = stringResource(R.string.settings_tip_of_day),
                    description = stringResource(R.string.settings_tip_of_day_desc),
                    checked = showTipOfDay,
                    onCheckedChange = vm::setShowTipOfDay,
                )
                SpyglassDivider()
                SettingsToggle(
                    title = stringResource(R.string.settings_favorites_on_home),
                    description = stringResource(R.string.settings_favorites_on_home_desc),
                    checked = showFavoritesOnHome,
                    onCheckedChange = vm::setShowFavoritesOnHome,
                )
                SpyglassDivider()
                SettingsToggle(
                    title = stringResource(R.string.settings_hide_unobtainable),
                    description = stringResource(R.string.settings_hide_unobtainable_desc),
                    checked = hideUnobtainable,
                    onCheckedChange = vm::setHideUnobtainableBlocks,
                )
                SpyglassDivider()
                SettingsToggle(
                    title = stringResource(R.string.settings_show_experimental),
                    description = stringResource(R.string.settings_show_experimental_desc),
                    checked = showExperimental,
                    onCheckedChange = vm::setShowExperimental,
                )
                SpyglassDivider()
                SettingsToggle(
                    title = stringResource(R.string.settings_haptic_feedback),
                    description = stringResource(R.string.settings_haptic_feedback_desc),
                    checked = hapticFeedback,
                    onCheckedChange = vm::setHapticFeedback,
                )
                SpyglassDivider()
                SettingsToggle(
                    title = stringResource(R.string.settings_reduce_animations),
                    description = stringResource(R.string.settings_reduce_animations_desc),
                    checked = reduceAnimations,
                    onCheckedChange = vm::setReduceAnimations,
                )
            }
        }

        // ── Game Clock ─────────────────────────────────────────────────────
        item(key = "clock") {
            SectionHeader(stringResource(R.string.settings_game_clock))
            ResultCard {
                SettingsToggle(
                    title = stringResource(R.string.settings_game_clock),
                    description = stringResource(R.string.settings_game_clock_desc),
                    checked = gameClockEnabled,
                    onCheckedChange = vm::setGameClockEnabled,
                )
                SpyglassDivider()
                Text(
                    stringResource(R.string.settings_configure_clock),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { hapticClick(); onCalcTab(9) },
                )
            }
        }

        // ── Security ────────────────────────────────────────────────────
        item(key = "security") {
            SectionHeader(stringResource(R.string.settings_security))
            ResultCard {
                SettingsToggle(
                    title = stringResource(R.string.settings_app_lock),
                    description = stringResource(R.string.settings_app_lock_desc),
                    checked = appLockEnabled,
                    onCheckedChange = vm::setAppLockEnabled,
                )
            }
        }

        // ── Data & Storage ────────────────────────────────────────────────
        item(key = "data_storage") {
            SectionHeader(stringResource(R.string.settings_data_storage))
            ResultCard {
                // Offline mode
                SettingsToggle(
                    title = stringResource(R.string.settings_offline_mode),
                    description = stringResource(R.string.settings_offline_mode_desc),
                    checked = offlineMode,
                    onCheckedChange = vm::setOfflineMode,
                )

                SpyglassDivider()

                // Sync frequency
                Text(
                    stringResource(R.string.settings_sync_frequency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (offlineMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_sync_frequency_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (offlineMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val options = listOf(1 to "1h", 6 to "6h", 12 to "12h", 24 to "24h")
                    options.forEach { (hours, label) ->
                        FilterChip(
                            selected = syncFrequencyHours == hours,
                            onClick = { hapticClick(); if (!offlineMode) vm.setSyncFrequencyHours(hours) },
                            enabled = !offlineMode,
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                SpyglassDivider()

                // Storage usage
                Text(
                    stringResource(R.string.settings_storage_usage),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Downloaded textures: ${formatBytes(storageBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (textureState == TextureManager.TextureState.DOWNLOADED) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        hapticConfirm()
                        vm.clearTextureCache()
                        storageBytes = 0L
                    }) {
                        Text(stringResource(R.string.settings_clear_cache), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ── Favorites Management ────────────────────────────────────────
        item(key = "favorites") {
            SectionHeader(stringResource(R.string.settings_favorites))
            ResultCard {
                if (allFavorites.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_no_favorites),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    Text(
                        stringResource(R.string.settings_favorites_count, allFavorites.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    allFavorites.forEach { fav ->
                        Text(
                            "\u2605  ${fav.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SpyglassDivider()
                    TextButton(onClick = { hapticConfirm(); vm.clearAllFavorites() }) {
                        Text(stringResource(R.string.settings_clear_all_favorites), color = Red400)
                    }
                }
            }
        }

        // ── Spyglass Connect ──────────────────────────────────────────
        item(key = "connect") {
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
                        hapticClick()
                        uriHandler.openUri("https://hardknocks.university")
                    },
                )
            }
        }

        // ── Privacy & Data ─────────────────────────────────────────────
        item(key = "privacy") {
            SectionHeader(stringResource(R.string.settings_privacy))
            ResultCard {
                SettingsToggle(
                    title = stringResource(R.string.consent_analytics),
                    description = stringResource(R.string.consent_analytics_desc),
                    checked = analyticsConsent,
                    onCheckedChange = vm::setAnalyticsConsent,
                )
                SpyglassDivider()
                SettingsToggle(
                    title = stringResource(R.string.consent_crash_reports),
                    description = stringResource(R.string.consent_crash_reports_desc),
                    checked = crashConsent,
                    onCheckedChange = vm::setCrashConsent,
                )
                SpyglassDivider()
                SettingsToggle(
                    title = stringResource(R.string.consent_personalized_ads),
                    description = stringResource(R.string.consent_personalized_ads_desc),
                    checked = adPersonalizationConsent,
                    onCheckedChange = vm::setAdPersonalizationConsent,
                )
                SpyglassDivider()
                TextButton(onClick = { hapticConfirm(); showDeleteConfirm = true }) {
                    Text(stringResource(R.string.settings_delete_data), color = Red400)
                }
                SpyglassDivider()
                Text(
                    text = stringResource(R.string.settings_privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        hapticClick()
                        uriHandler.openUri("https://hardknocks.university/privacy-policy.html")
                    },
                )
            }
        }

        // ── Quick Links ──────────────────────────────────────────────────
        item(key = "quick_links") {
            SectionHeader(stringResource(R.string.settings_quick_links))
            ResultCard {
                SettingsLink(
                    title = stringResource(R.string.settings_rate_app),
                    description = stringResource(R.string.settings_rate_app_desc),
                    onClick = {
                        uriHandler.openUri("https://play.google.com/store/apps/details?id=dev.spyglass.android")
                    },
                )
                SpyglassDivider()
                SettingsLink(
                    title = stringResource(R.string.settings_send_feedback),
                    description = stringResource(R.string.settings_send_feedback_desc),
                    onClick = onFeedback,
                )
                SpyglassDivider()
                SettingsLink(
                    title = stringResource(R.string.settings_changelog),
                    description = stringResource(R.string.settings_changelog_desc),
                    onClick = onChangelog,
                )
                SpyglassDivider()
                SettingsLink(
                    title = stringResource(R.string.settings_about),
                    description = stringResource(R.string.settings_about_desc),
                    onClick = onAbout,
                )
                SpyglassDivider()
                SettingsLink(
                    title = stringResource(R.string.settings_app_permissions),
                    description = stringResource(R.string.settings_app_permissions_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                )
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_data_title), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    stringResource(R.string.settings_delete_data_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticConfirm()
                    vm.deleteAllUserData()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.settings_delete_data_confirm), color = Red400)
                }
            },
            dismissButton = {
                TextButton(onClick = { hapticClick(); showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val hapticConfirm = rememberHapticConfirm()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = { hapticConfirm(); onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun SettingsLink(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val hapticClick = rememberHapticClick()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { hapticClick(); onClick() }
            .padding(vertical = 6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
