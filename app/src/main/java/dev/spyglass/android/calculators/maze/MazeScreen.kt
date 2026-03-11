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
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
fun MazeScreen(vm: MazeViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var view3D by remember { mutableStateOf(false) }
    val hapticClick = rememberHapticClick()
    val mazeTypeLabels = listOf(
        stringResource(R.string.maze_type_rect),
        stringResource(R.string.maze_type_circle),
        stringResource(R.string.maze_type_floors),
    )

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(stringResource(R.string.maze_header), icon = PixelIcons.Maze)

        InputCard {
            // Maze type selector
            TogglePill(
                options  = mazeTypeLabels,
                selected = s.mazeType.ordinal,
                onSelect = { vm.setMazeType(MazeType.entries[it]) },
            )

            // Conditional sliders
            if (s.mazeType == MazeType.RECT || s.mazeType == MazeType.FLOORS) {
                LabeledSlider(stringResource(R.string.maze_width_cells), s.widthCells, 3, 30) { vm.setWidthCells(it) }
                LabeledSlider(stringResource(R.string.maze_length_cells), s.lengthCells, 3, 30) { vm.setLengthCells(it) }
            }

            if (s.mazeType == MazeType.CIRCLE) {
                LabeledSlider(stringResource(R.string.maze_rings), s.rings, 2, 10) { vm.setRings(it) }
            }

            if (s.mazeType == MazeType.FLOORS) {
                LabeledSlider(stringResource(R.string.maze_floors), s.floors, 2, 6) { vm.setFloors(it) }
            }

            // All types
            LabeledSlider(stringResource(R.string.maze_path_width), s.pathWidth, 1, 3) { vm.setPathWidth(it) }
            LabeledSlider(stringResource(R.string.maze_wall_height), s.wallHeight, 1, 10) { vm.setWallHeight(it) }

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
                    Text(stringResource(R.string.maze_generate))
                }
                OutlinedButton(
                    onClick = { hapticClick(); vm.shuffle() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.maze_shuffle))
                }
            }
        }

        if (s.layers.isNotEmpty()) {
            // View toggle
            TogglePill(
                listOf(stringResource(R.string.shapes_view_layer), stringResource(R.string.shapes_view_3d)),
                if (view3D) 1 else 0,
                { view3D = it == 1 },
            )

            StatRow(stringResource(R.string.maze_total_blocks), "%,d".format(s.totalBlocks))

            // Dimensions in blocks and chunks
            val allXs = s.layers.values.flatMap { pts -> pts.map { it.first } }
            val allZs = s.layers.values.flatMap { pts -> pts.map { it.second } }
            if (allXs.isNotEmpty()) {
                val width  = allXs.max() - allXs.min() + 1
                val depth  = allZs.max() - allZs.min() + 1
                val height = s.layerMax - s.layerMin + 1
                @Composable
                fun fmt(blocks: Int): String {
                    val chunks = blocks / 16
                    val rem = blocks % 16
                    return when {
                        chunks > 0 && rem > 0 -> stringResource(R.string.shapes_blocks_chunks_rem, blocks, chunks, rem)
                        chunks > 0            -> stringResource(R.string.shapes_blocks_chunks, blocks, chunks)
                        else                  -> stringResource(R.string.shapes_blocks_count, blocks)
                    }
                }
                StatRow(stringResource(R.string.maze_width_x),  fmt(width))
                StatRow(stringResource(R.string.maze_depth_z),  fmt(depth))
                StatRow(stringResource(R.string.maze_height_y), fmt(height))
            }

            StatRow(stringResource(R.string.maze_dead_ends), "${s.deadEnds}")
            StatRow(stringResource(R.string.maze_longest_path), stringResource(R.string.maze_longest_path_val, s.longestPath))

            SpyglassDivider()

            if (view3D) {
                IsometricView(s.layers)
            } else {
                // Layer slider
                if (s.layerMin < s.layerMax) {
                    Text(stringResource(R.string.maze_y_layer, s.currentLayer), style = MaterialTheme.typography.bodyMedium)
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
            stringResource(R.string.maze_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
