package dev.spyglass.android.core.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.Red400
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.core.ui.TextureManager
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.core.ui.rememberHapticConfirm
import dev.spyglass.android.data.sync.DataSyncWorker
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppBehaviorContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticClick = rememberHapticClick()

    val defaultStartupTab by remember {
        context.dataStore.data.map { it[PreferenceKeys.DEFAULT_STARTUP_TAB] ?: 0 }
    }.collectAsStateWithLifecycle(initialValue = 0)

    val showTipOfDay by remember {
        context.dataStore.data.map { it[PreferenceKeys.SHOW_TIP_OF_DAY] ?: true }
    }.collectAsStateWithLifecycle(initialValue = true)

    val showFavoritesOnHome by remember {
        context.dataStore.data.map { it[PreferenceKeys.SHOW_FAVORITES_ON_HOME] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    SectionHeader(stringResource(R.string.settings_defaults))
    ResultCard {
        Text(
            stringResource(R.string.settings_startup_tab),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
                    onClick = { hapticClick(); scope.launch { context.dataStore.edit { it[PreferenceKeys.DEFAULT_STARTUP_TAB] = i } } },
                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        SpyglassDivider()

        SettingsToggle(
            title = stringResource(R.string.settings_tip_of_day),
            description = stringResource(R.string.settings_tip_of_day_desc),
            checked = showTipOfDay,
            onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.SHOW_TIP_OF_DAY] = !showTipOfDay } } },
        )

        SpyglassDivider()

        SettingsToggle(
            title = stringResource(R.string.settings_favorites_on_home),
            description = stringResource(R.string.settings_favorites_on_home_desc),
            checked = showFavoritesOnHome,
            onCheckedChange = { scope.launch { context.dataStore.edit { it[PreferenceKeys.SHOW_FAVORITES_ON_HOME] = !showFavoritesOnHome } } },
        )
    }
}

@Composable
internal fun FavoritesContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticConfirm = rememberHapticConfirm()

    val repo by androidx.compose.runtime.produceState<dev.spyglass.android.data.repository.GameDataRepository?>(null) {
        value = kotlinx.coroutines.withContext(Dispatchers.IO) { dev.spyglass.android.data.repository.GameDataRepository.get(context) }
    }
    val allFavorites by remember(repo) {
        repo?.allFavorites() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    ResultCard {
        Text(
            stringResource(R.string.settings_favorites),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (allFavorites.isEmpty()) {
            Text(
                stringResource(R.string.settings_no_favorites),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Text(
                stringResource(R.string.settings_favorites_count, allFavorites.size),
                style = MaterialTheme.typography.bodySmall,
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
            TextButton(onClick = {
                hapticConfirm()
                scope.launch(Dispatchers.IO) { repo?.deleteAllFavorites() }
            }) {
                Text(stringResource(R.string.settings_clear_all_favorites), color = Red400)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DataSyncContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()

    val offlineMode by remember {
        context.dataStore.data.map { it[PreferenceKeys.OFFLINE_MODE] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    val syncFrequencyHours by remember {
        context.dataStore.data.map { it[PreferenceKeys.SYNC_FREQUENCY_HOURS] ?: 12 }
    }.collectAsStateWithLifecycle(initialValue = 12)

    val textureState by TextureManager.state.collectAsStateWithLifecycle()
    var storageBytes by remember {
        mutableStateOf(
            File(context.filesDir, "textures").let { dir ->
                if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
            }
        )
    }

    var syncing by remember { mutableStateOf(false) }

    SectionHeader(stringResource(R.string.settings_data_sync))
    ResultCard {
        // Sync Now
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_sync_now), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.settings_sync_check_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            if (syncing) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                TextButton(
                    onClick = {
                        hapticClick()
                        if (!offlineMode) {
                            syncing = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    dev.spyglass.android.data.sync.DataSyncManager.sync(context)
                                } finally {
                                    syncing = false
                                }
                            }
                        }
                    },
                    enabled = !offlineMode,
                ) {
                    Text(stringResource(R.string.settings_sync_btn), color = if (offlineMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
                }
            }
        }

        SpyglassDivider()

        SettingsToggle(
            title = stringResource(R.string.settings_offline_mode),
            description = stringResource(R.string.settings_offline_mode_desc),
            checked = offlineMode,
            onCheckedChange = {
                scope.launch {
                    val newValue = !offlineMode
                    context.dataStore.edit { it[PreferenceKeys.OFFLINE_MODE] = newValue }
                    if (newValue) DataSyncWorker.cancel(context)
                    else DataSyncWorker.enqueue(context, syncFrequencyHours)
                }
            },
        )

        SpyglassDivider()

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
                    onClick = {
                        hapticClick()
                        if (!offlineMode) scope.launch {
                            context.dataStore.edit { it[PreferenceKeys.SYNC_FREQUENCY_HOURS] = hours }
                            DataSyncWorker.enqueue(context, hours)
                        }
                    },
                    enabled = !offlineMode,
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        SpyglassDivider()

        Text(
            stringResource(R.string.settings_storage_usage),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.settings_downloaded_textures, formatBytes(storageBytes)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (textureState == TextureManager.TextureState.DOWNLOADED) {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = {
                hapticConfirm()
                scope.launch { TextureManager.delete(context) }
                storageBytes = 0L
            }) {
                Text(stringResource(R.string.settings_clear_cache), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
