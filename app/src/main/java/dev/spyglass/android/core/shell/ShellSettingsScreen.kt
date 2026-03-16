package dev.spyglass.android.core.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.core.module.ModuleRegistry
import dev.spyglass.android.core.module.SettingsSection
import dev.spyglass.android.core.module.SettingsSectionScope
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassTab
import dev.spyglass.android.core.ui.SpyglassTabRow

/**
 * Shell settings screen — 4-tab layout (Appearance, Gameplay, General, About & Privacy).
 * Collects [SettingsSection]s from enabled modules, groups by tab index.
 */
@Composable
fun ShellSettingsScreen(
    scope: SettingsSectionScope,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val revision by ModuleRegistry.revision.collectAsStateWithLifecycle()

    val sections by produceState(emptyList<SettingsSection>(), revision) {
        value = ModuleRegistry.enabledModules(context)
            .flatMap { it.settingsSections() }
            .sortedBy { it.weight }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val listStates = remember { List(4) { LazyListState() } }

    val tabs = listOf(
        SpyglassTab(stringResource(R.string.settings_tab_appearance), PixelIcons.Enchant, untinted = true),
        SpyglassTab(stringResource(R.string.settings_tab_gameplay), PixelIcons.Blocks),
        SpyglassTab(stringResource(R.string.settings_tab_general), PixelIcons.Storage),
        SpyglassTab(stringResource(R.string.settings_tab_about_privacy), PixelIcons.Globe),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // ── Tab row ──
        SpyglassTabRow(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        // ── Tab content ──
        val tabSections = sections.filter { it.tab == selectedTab }
        LazyColumn(
            state = listStates[selectedTab],
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            tabSections.forEach { section ->
                item(key = "section_${section.key}") {
                    section.content(scope)
                }
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
        }
    }
}
