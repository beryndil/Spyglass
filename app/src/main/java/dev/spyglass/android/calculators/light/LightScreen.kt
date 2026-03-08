package dev.spyglass.android.calculators.light

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            title = "Light Spacing",
            description = "Calculate torch and light source spacing to prevent hostile mob spawning.",
        )

        // ── Light Source Selector ──
        SectionHeader("Light Source")
        InputCard {
            Text(
                "SELECT LIGHT SOURCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(
                onClick = { hapticClick(); showSourcePicker = !showSourcePicker },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${selectedSource.name} (Level ${selectedSource.level})")
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
                            "Level ${source.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        // ── Results ──
        SectionHeader("Spacing Results")
        ResultCard {
            StatRow("Light Level", "${selectedSource.level}")
            StatRow("Linear Reach", "${selectedSource.level} blocks each direction")
            SpyglassDivider()

            val lineSpacing = 2 * selectedSource.level - 1
            StatRow("Line Spacing", "$lineSpacing blocks apart")
            Text(
                "Place ${selectedSource.name} every $lineSpacing blocks along a line (hallway, tunnel).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            SpyglassDivider()

            if (selectedSource.gridSpacing > 0) {
                StatRow("Grid Spacing", "${selectedSource.gridSpacing} blocks apart")
                Text(
                    "Place ${selectedSource.name} in a grid pattern every ${selectedSource.gridSpacing} blocks in both X and Z directions to fully prevent mob spawning on a flat surface.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                StatRow("Grid Spacing", "Not practical")
                Text(
                    "${selectedSource.name} is too dim (level ${selectedSource.level}) for effective mob-proofing. Light must reach level 1+ at every block. Consider a brighter source.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Red400,
                )
            }
        }

        // ── Mob Spawning Info ──
        SectionHeader("Mob Spawning Rules")
        ResultCard {
            Text(
                "OVERWORLD (1.18+)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Hostile mobs spawn at block light level 0 only. Any light level 1 or above prevents spawning.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(
                "NETHER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Most hostile mobs spawn at light level 11 or lower. Blazes and Wither Skeletons spawn in fortresses at light level 11 or lower.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(
                "MOB SPAWNERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Spawner blocks activate when light level is 11 or lower. Place light sources within 4 blocks to disable them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Quick Reference ──
        SectionHeader("Quick Reference")
        ResultCard {
            Text(
                "COMMON SPACINGS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            StatRow("Torch (14)", "12 block grid / 27 block line")
            StatRow("Lantern (15)", "14 block grid / 29 block line")
            StatRow("Soul Torch (10)", "6 block grid / 19 block line")
            SpyglassDivider()
            Text(
                "HOW LIGHT SPREADS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Light decreases by 1 for each block of distance (Manhattan distance). A torch at level 14 reaches level 1 at 13 blocks away, and level 0 at 14 blocks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
