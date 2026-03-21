package dev.spyglass.android.core.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.Red400
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.core.ui.rememberHapticConfirm
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
internal fun PlayerProfileContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticConfirm = rememberHapticConfirm()
    val hapticClick = rememberHapticClick()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val ign by remember {
        context.dataStore.data.map { it[PreferenceKeys.PLAYER_IGN] ?: "" }
    }.collectAsStateWithLifecycle(initialValue = "")

    val uuid by remember {
        context.dataStore.data.map { it[PreferenceKeys.PLAYER_UUID] ?: "" }
    }.collectAsStateWithLifecycle(initialValue = "")

    SectionHeader(stringResource(R.string.settings_player_profile))
    ResultCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.connect_ign),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                ign.ifBlank { stringResource(R.string.settings_not_set) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (ign.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
            )
        }
        SpyglassDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.connect_uuid),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                uuid.ifBlank { "\u2014" },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = if (uuid.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
            )
        }
        SpyglassDivider()
        TextButton(onClick = { hapticConfirm(); showDeleteConfirm = true }) {
            Text(stringResource(R.string.settings_delete_data), color = Red400)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_data_title), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(stringResource(R.string.settings_delete_data_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                TextButton(onClick = {
                    hapticConfirm()
                    scope.launch {
                        dev.spyglass.android.data.repository.GameDataRepository.get(context).deleteAllUserData()
                    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GameFiltersContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticClick = rememberHapticClick()

    val minecraftEdition by remember {
        context.dataStore.data.map { it[PreferenceKeys.MINECRAFT_EDITION] ?: "java" }
    }.collectAsStateWithLifecycle(initialValue = "java")

    val minecraftVersion by remember {
        context.dataStore.data.map { it[PreferenceKeys.MINECRAFT_VERSION] ?: "" }
    }.collectAsStateWithLifecycle(initialValue = "")

    val versionFilterMode by remember {
        context.dataStore.data.map { it[PreferenceKeys.VERSION_FILTER_MODE] ?: "show_all" }
    }.collectAsStateWithLifecycle(initialValue = "show_all")

    val hideUnobtainable by remember {
        context.dataStore.data.map { it[PreferenceKeys.HIDE_UNOBTAINABLE_BLOCKS] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    val showExperimental by remember {
        context.dataStore.data.map { it[PreferenceKeys.SHOW_EXPERIMENTAL] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    val gameClockEnabled by remember {
        context.dataStore.data.map { it[PreferenceKeys.GAME_CLOCK_ENABLED] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    var versionExpanded by remember { mutableStateOf(false) }

    SectionHeader(stringResource(R.string.settings_game_settings))
    ResultCard {
        Text(
            stringResource(R.string.settings_game_filter_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        Text(stringResource(R.string.settings_edition), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        dev.spyglass.android.core.ui.TogglePill(
            options = listOf(stringResource(R.string.settings_edition_java), stringResource(R.string.settings_edition_bedrock)),
            selected = if (minecraftEdition == "bedrock") 1 else 0,
            onSelect = {
                val edition = if (it == 1) "bedrock" else "java"
                scope.launch { context.dataStore.edit { prefs -> prefs[PreferenceKeys.MINECRAFT_EDITION] = edition } }
            },
        )

        val versions = if (minecraftEdition == "bedrock") dev.spyglass.android.settings.MinecraftVersions.BEDROCK_VERSIONS else dev.spyglass.android.settings.MinecraftVersions.JAVA_VERSIONS
        val latestLabel = stringResource(R.string.settings_version_latest)
        val displayVersion = minecraftVersion.ifBlank { latestLabel }
        Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Box {
            androidx.compose.material3.OutlinedButton(onClick = { hapticClick(); versionExpanded = true }) {
                Text(displayVersion, color = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = versionExpanded, onDismissRequest = { versionExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(latestLabel, color = if (minecraftVersion.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                    onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.MINECRAFT_VERSION] = "" } }; versionExpanded = false },
                )
                versions.reversed().forEach { v ->
                    DropdownMenuItem(
                        text = { Text(v, color = if (minecraftVersion == v) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                        onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.MINECRAFT_VERSION] = v } }; versionExpanded = false },
                    )
                }
            }
        }

        Text(stringResource(R.string.settings_filter_mode), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val modes = listOf("show_all" to stringResource(R.string.settings_filter_show_all), "highlight" to stringResource(R.string.settings_filter_highlight), "hide" to stringResource(R.string.settings_filter_hide))
            modes.forEach { (key, label) ->
                FilterChip(
                    selected = versionFilterMode == key,
                    onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.VERSION_FILTER_MODE] = key } } },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        SpyglassDivider()

        SettingsToggle(
            title = stringResource(R.string.settings_hide_unobtainable),
            description = stringResource(R.string.settings_hide_unobtainable_desc),
            checked = hideUnobtainable,
            onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.HIDE_UNOBTAINABLE_BLOCKS] = !hideUnobtainable } } },
        )

        SpyglassDivider()

        SettingsToggle(
            title = stringResource(R.string.settings_show_experimental),
            description = stringResource(R.string.settings_show_experimental_desc),
            checked = showExperimental,
            onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.SHOW_EXPERIMENTAL] = !showExperimental } } },
        )

        SpyglassDivider()

        SettingsToggle(
            title = stringResource(R.string.settings_game_clock),
            description = stringResource(R.string.settings_game_clock_desc),
            checked = gameClockEnabled,
            onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.GAME_CLOCK_ENABLED] = !gameClockEnabled } } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DefaultTabsContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticClick = rememberHapticClick()

    val defaultBrowseTab by remember {
        context.dataStore.data.map { it[PreferenceKeys.DEFAULT_BROWSE_TAB] ?: 0 }
    }.collectAsStateWithLifecycle(initialValue = 0)

    val defaultToolTab by remember {
        context.dataStore.data.map { it[PreferenceKeys.DEFAULT_TOOL_TAB] ?: 0 }
    }.collectAsStateWithLifecycle(initialValue = 0)

    SectionHeader(stringResource(R.string.settings_defaults))
    ResultCard {
        Text(
            stringResource(R.string.settings_default_browse_tab),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.settings_browse_tab_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val browseNames = listOf(
                stringResource(R.string.browse_tab_blocks), stringResource(R.string.browse_tab_items),
                stringResource(R.string.browse_tab_recipes), stringResource(R.string.browse_tab_mobs),
                stringResource(R.string.browse_tab_trades), stringResource(R.string.browse_tab_biomes),
                stringResource(R.string.browse_tab_structures), stringResource(R.string.browse_tab_enchants),
                stringResource(R.string.browse_tab_potions),
                stringResource(R.string.browse_tab_commands), stringResource(R.string.browse_tab_reference),
                stringResource(R.string.browse_tab_versions),
            )
            browseNames.forEachIndexed { i, name ->
                FilterChip(
                    selected = defaultBrowseTab == i,
                    onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.DEFAULT_BROWSE_TAB] = i } } },
                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        SpyglassDivider()

        Text(
            stringResource(R.string.settings_default_tool_tab),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.settings_tool_tab_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val toolNames = listOf(
                stringResource(R.string.calc_tab_todo), stringResource(R.string.calc_tab_shopping),
                stringResource(R.string.calc_tab_notes), stringResource(R.string.calc_tab_waypoints),
                stringResource(R.string.calc_tab_tracker), stringResource(R.string.calc_tab_enchanting),
                stringResource(R.string.calc_tab_fill), stringResource(R.string.calc_tab_shapes),
                stringResource(R.string.calc_tab_maze), stringResource(R.string.calc_tab_storage),
                stringResource(R.string.calc_tab_smelt), stringResource(R.string.calc_tab_nether),
                stringResource(R.string.calc_tab_game_clock), stringResource(R.string.calc_tab_light),
                stringResource(R.string.calc_tab_redstone), stringResource(R.string.calc_tab_librarian),
                stringResource(R.string.calc_tab_food), stringResource(R.string.calc_tab_banners),
                stringResource(R.string.calc_tab_trims), stringResource(R.string.calc_tab_loot),
            )
            toolNames.forEachIndexed { i, name ->
                FilterChip(
                    selected = defaultToolTab == i,
                    onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.DEFAULT_TOOL_TAB] = i } } },
                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}
