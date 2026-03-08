package dev.spyglass.android.calculators.maze

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

private val MAZE_TYPE_LABELS = listOf("Rect", "Circle", "Floors")

@Composable
fun MazeScreen(vm: MazeViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var view3D by remember { mutableStateOf(false) }
    val hapticClick = rememberHapticClick()

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Maze Maker", icon = PixelIcons.Maze)

        InputCard {
            // Maze type selector
            TogglePill(
                options  = MAZE_TYPE_LABELS,
                selected = s.mazeType.ordinal,
                onSelect = { vm.setMazeType(MazeType.entries[it]) },
            )

            // Conditional sliders
            if (s.mazeType == MazeType.RECT || s.mazeType == MazeType.FLOORS) {
                LabeledSlider("Width (cells)", s.widthCells, 3, 30) { vm.setWidthCells(it) }
                LabeledSlider("Length (cells)", s.lengthCells, 3, 30) { vm.setLengthCells(it) }
            }

            if (s.mazeType == MazeType.CIRCLE) {
                LabeledSlider("Rings", s.rings, 2, 10) { vm.setRings(it) }
            }

            if (s.mazeType == MazeType.FLOORS) {
                LabeledSlider("Floors", s.floors, 2, 6) { vm.setFloors(it) }
            }

            // All types
            LabeledSlider("Path width", s.pathWidth, 1, 3) { vm.setPathWidth(it) }
            LabeledSlider("Wall height", s.wallHeight, 1, 10) { vm.setWallHeight(it) }

            // Generate / Shuffle buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { hapticClick(); vm.generate() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Generate")
                }
                OutlinedButton(
                    onClick = { hapticClick(); vm.shuffle() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Shuffle")
                }
            }
        }

        if (s.layers.isNotEmpty()) {
            // View toggle
            TogglePill(
                listOf("Layer", "3D"),
                if (view3D) 1 else 0,
                { view3D = it == 1 },
            )

            StatRow("Total blocks", "%,d".format(s.totalBlocks))

            // Dimensions in blocks and chunks
            val allXs = s.layers.values.flatMap { pts -> pts.map { it.first } }
            val allZs = s.layers.values.flatMap { pts -> pts.map { it.second } }
            if (allXs.isNotEmpty()) {
                val width  = allXs.max() - allXs.min() + 1
                val depth  = allZs.max() - allZs.min() + 1
                val height = s.layerMax - s.layerMin + 1
                fun fmt(blocks: Int): String {
                    val chunks = blocks / 16
                    val rem = blocks % 16
                    return if (chunks > 0) "$blocks blocks ($chunks chunk${if (chunks > 1) "s" else ""}${if (rem > 0) " + $rem" else ""})"
                    else "$blocks blocks"
                }
                StatRow("Width (X)",  fmt(width))
                StatRow("Depth (Z)",  fmt(depth))
                StatRow("Height (Y)", fmt(height))
            }

            StatRow("Dead ends", "${s.deadEnds}")
            StatRow("Longest path", "${s.longestPath} cells")

            SpyglassDivider()

            if (view3D) {
                IsometricView(s.layers)
            } else {
                // Layer slider
                if (s.layerMin < s.layerMax) {
                    Text("Y layer: ${s.currentLayer}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value         = s.currentLayer.toFloat(),
                        onValueChange = { vm.setLayer(it.toInt()) },
                        valueRange    = s.layerMin.toFloat()..s.layerMax.toFloat(),
                        steps         = (s.layerMax - s.layerMin - 1).coerceAtLeast(0),
                        colors        = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }

                // Grid view of current layer
                val layer = s.layers[s.currentLayer] ?: emptySet()
                LayerGrid(blocks = layer)
            }
        }

        Text(
            "Generate mazes for Minecraft. Choose rectangular for classic hedge mazes, circular for concentric ring mazes, or multi-floor for stacked mazes connected by staircases. Use the layer slider to build layer by layer, or switch to 3D for an isometric preview.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
