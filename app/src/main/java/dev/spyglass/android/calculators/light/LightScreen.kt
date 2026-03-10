package dev.spyglass.android.calculators.light

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

private data class LightSource(
    val name: String,
    val level: Int,
    val gridSpacing: Int, // max blocks apart in a grid to prevent light level 0
)

private val LIGHT_SOURCES = listOf(
    LightSource("Beacon", 15, 14),
    LightSource("Conduit", 15, 14),
    LightSource("Glowstone", 15, 14),
    LightSource("Jack o'Lantern", 15, 14),
    LightSource("Lantern", 15, 14),
    LightSource("Sea Lantern", 15, 14),
    LightSource("Shroomlight", 15, 14),
    LightSource("Torch", 14, 12),
    LightSource("End Rod", 14, 12),
    LightSource("Soul Lantern", 10, 6),
    LightSource("Soul Torch", 10, 6),
    LightSource("Soul Campfire", 10, 6),
    LightSource("Redstone Torch", 7, 0),
    LightSource("Candle (4)", 12, 10),
    LightSource("Candle (3)", 9, 4),
    LightSource("Candle (2)", 6, 0),
    LightSource("Candle (1)", 3, 0),
    LightSource("Sea Pickle (4)", 15, 14),
    LightSource("Sea Pickle (3)", 12, 10),
    LightSource("Sea Pickle (2)", 9, 4),
    LightSource("Sea Pickle (1)", 6, 0),
)

@Composable
fun LightScreen() {
    val hapticClick = rememberHapticClick()
    var selectedSource by remember { mutableStateOf(LIGHT_SOURCES[7]) } // Default to Torch
    var showSourcePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Torch,
            title = stringResource(R.string.light_title),
            description = stringResource(R.string.light_description),
        )

        // ── Light Source Selector ──
        SectionHeader(stringResource(R.string.light_source))
        InputCard {
            Text(
                stringResource(R.string.light_select_source),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(
                onClick = { hapticClick(); showSourcePicker = !showSourcePicker },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.light_source_level, selectedSource.name, selectedSource.level))
            }
            if (showSourcePicker) {
                LIGHT_SOURCES.forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = {
                                hapticClick()
                                selectedSource = source
                                showSourcePicker = false
                            },
                        ) {
                            Text(
                                source.name,
                                color = if (source == selectedSource) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            stringResource(R.string.light_level_label, source.level),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        // ── Results ──
        SectionHeader(stringResource(R.string.light_spacing_results))
        ResultCard {
            StatRow(stringResource(R.string.light_level), "${selectedSource.level}")
            StatRow(stringResource(R.string.light_linear_reach), stringResource(R.string.light_linear_reach_val, selectedSource.level))
            SpyglassDivider()

            val lineSpacing = 2 * selectedSource.level - 1
            StatRow(stringResource(R.string.light_line_spacing), stringResource(R.string.light_line_spacing_val, lineSpacing))
            Text(
                stringResource(R.string.light_line_place, selectedSource.name, lineSpacing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            SpyglassDivider()

            if (selectedSource.gridSpacing > 0) {
                StatRow(stringResource(R.string.light_grid_spacing), stringResource(R.string.light_grid_spacing_val, selectedSource.gridSpacing))
                Text(
                    stringResource(R.string.light_grid_place, selectedSource.name, selectedSource.gridSpacing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                StatRow(stringResource(R.string.light_grid_spacing), stringResource(R.string.light_not_practical))
                Text(
                    stringResource(R.string.light_too_dim, selectedSource.name, selectedSource.level),
                    style = MaterialTheme.typography.bodySmall,
                    color = Red400,
                )
            }
        }

        // ── Mob Spawning Info ──
        SectionHeader(stringResource(R.string.light_mob_spawning))
        ResultCard {
            Text(
                stringResource(R.string.light_overworld),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.light_overworld_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(
                stringResource(R.string.light_nether_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.light_nether_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(
                stringResource(R.string.light_mob_spawners),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.light_mob_spawners_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Quick Reference ──
        SectionHeader(stringResource(R.string.light_quick_reference))
        ResultCard {
            Text(
                stringResource(R.string.light_common_spacings),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            StatRow(stringResource(R.string.light_torch_14), stringResource(R.string.light_torch_14_val))
            StatRow(stringResource(R.string.light_lantern_15), stringResource(R.string.light_lantern_15_val))
            StatRow(stringResource(R.string.light_soul_torch_10), stringResource(R.string.light_soul_torch_10_val))
            SpyglassDivider()
            Text(
                stringResource(R.string.light_how_light_spreads),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.light_spread_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
