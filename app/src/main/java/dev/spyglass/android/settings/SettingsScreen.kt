package dev.spyglass.android.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalUriHandler
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onCalcTab: (Int) -> Unit = {},
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

    val uriHandler = LocalUriHandler.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var versionExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "back") {
            IconButton(onClick = onBack) {
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
                                .clickable { vm.setBackgroundTheme(key) },
                        )
                    }
                }
                Text(
                    ThemeInfoMap[backgroundTheme]?.label ?: "Obsidian",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
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

                // Edition toggle
                Text("Edition", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                TogglePill(
                    options = listOf("Java", "Bedrock"),
                    selected = if (minecraftEdition == "bedrock") 1 else 0,
                    onSelect = { vm.setMinecraftEdition(if (it == 1) "bedrock" else "java") },
                )

                // Version dropdown
                val versions = if (minecraftEdition == "bedrock") MinecraftVersions.BEDROCK_VERSIONS else MinecraftVersions.JAVA_VERSIONS
                val displayVersion = minecraftVersion.ifBlank { "Latest" }
                Text("Version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Box {
                    OutlinedButton(onClick = { versionExpanded = true }) {
                        Text(displayVersion, color = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(expanded = versionExpanded, onDismissRequest = { versionExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Latest", color = if (minecraftVersion.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                            onClick = { vm.setMinecraftVersion(""); versionExpanded = false },
                        )
                        versions.reversed().forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v, color = if (minecraftVersion == v) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                onClick = { vm.setMinecraftVersion(v); versionExpanded = false },
                            )
                        }
                    }
                }

                // Filter mode
                Text("Filter Mode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val modes = listOf("show_all" to "Show All", "highlight" to "Highlight Unavailable", "hide" to "Hide Unavailable")
                    modes.forEach { (key, label) ->
                        FilterChip(
                            selected = versionFilterMode == key,
                            onClick = { vm.setVersionFilterMode(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            // Version info card
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
                            onClick = { vm.setDefaultBrowseTab(i) },
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
                            onClick = { vm.setDefaultToolTab(i) },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }

        // ── Toggles ─────────────────────────────────────────────────────
        item(key = "display") {
            SectionHeader(stringResource(R.string.settings_display))
            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_tip_of_day), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.settings_tip_of_day_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Switch(
                        checked = showTipOfDay,
                        onCheckedChange = vm::setShowTipOfDay,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                SpyglassDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_favorites_on_home), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.settings_favorites_on_home_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Switch(
                        checked = showFavoritesOnHome,
                        onCheckedChange = vm::setShowFavoritesOnHome,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
            }
        }

        // ── Game Clock ─────────────────────────────────────────────────────
        item(key = "clock") {
            SectionHeader(stringResource(R.string.settings_game_clock))
            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_game_clock), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.settings_game_clock_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Switch(
                        checked = gameClockEnabled,
                        onCheckedChange = vm::setGameClockEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                SpyglassDivider()
                Text(
                    stringResource(R.string.settings_configure_clock),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onCalcTab(9) },
                )
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
                    TextButton(onClick = vm::clearAllFavorites) {
                        Text(stringResource(R.string.settings_clear_all_favorites), color = Red400)
                    }
                }
            }
        }

        // ── Privacy & Data ─────────────────────────────────────────────
        item(key = "privacy") {
            SectionHeader(stringResource(R.string.settings_privacy))
            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.consent_analytics), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.consent_analytics_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Switch(
                        checked = analyticsConsent,
                        onCheckedChange = vm::setAnalyticsConsent,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                SpyglassDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.consent_crash_reports), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.consent_crash_reports_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Switch(
                        checked = crashConsent,
                        onCheckedChange = vm::setCrashConsent,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                SpyglassDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.consent_personalized_ads), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.consent_personalized_ads_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Switch(
                        checked = adPersonalizationConsent,
                        onCheckedChange = vm::setAdPersonalizationConsent,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                SpyglassDivider()
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text(stringResource(R.string.settings_delete_data), color = Red400)
                }
                SpyglassDivider()
                Text(
                    text = stringResource(R.string.settings_privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://dev-vulx.github.io/Spyglass/privacy-policy.html")
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
                    vm.deleteAllUserData()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.settings_delete_data_confirm), color = Red400)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}
