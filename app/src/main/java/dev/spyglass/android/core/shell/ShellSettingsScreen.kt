package dev.spyglass.android.core.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.core.module.ModuleRegistry
import dev.spyglass.android.core.module.SettingsSection
import dev.spyglass.android.core.module.SettingsSectionScope
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.core.ui.SpyglassIconImage
import kotlinx.coroutines.launch

/**
 * Thin shell settings screen — shows module toggle section at top,
 * then collects [SettingsSection]s from all enabled modules sorted by weight.
 */
@Composable
fun ShellSettingsScreen(
    scope: SettingsSectionScope,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sections by produceState(emptyList<SettingsSection>()) {
        value = ModuleRegistry.enabledModules(context)
            .flatMap { it.settingsSections() }
            .sortedBy { it.weight }
    }

    // Module enable/disable states
    val modules = ModuleRegistry.modules
    val moduleStates by produceState(emptyMap<String, Boolean>()) {
        val map = mutableMapOf<String, Boolean>()
        modules.forEach { m ->
            map[m.id] = ModuleRegistry.isEnabled(context, m)
        }
        value = map
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Back + Title ──
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

        // ── Module Management ──
        item(key = "modules") {
            SectionHeader("Modules")
            ResultCard {
                Text(
                    "Enable or disable app features",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                modules.forEach { module ->
                    val enabled = moduleStates[module.id] ?: true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            SpyglassIconImage(
                                module.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                module.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (module.canDisable) {
                            Switch(
                                checked = enabled,
                                onCheckedChange = { newEnabled ->
                                    coroutineScope.launch {
                                        ModuleRegistry.setEnabled(context, module.id, newEnabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                                ),
                            )
                        } else {
                            Text(
                                "Required",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    if (module != modules.last()) {
                        SpyglassDivider()
                    }
                }
            }
        }

        // ── Module Settings Sections ──
        sections.forEach { section ->
            item(key = "section_${section.key}") {
                section.content(scope)
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
    }
}
