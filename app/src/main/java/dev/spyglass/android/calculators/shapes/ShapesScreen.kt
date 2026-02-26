package dev.spyglass.android.calculators.shapes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

@Composable
fun ShapesScreen(vm: ShapesViewModel = viewModel()) {
    val s by vm.state.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("Shape Designer", icon = Icons.Default.Hexagon)

        InputCard {
            // Shape type selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ShapeType.entries.forEach { type ->
                    val selected = s.shapeType == type
                    FilterChip(
                        selected = selected,
                        onClick  = { vm.setShape(type) },
                        label    = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Radius
            SpyglassTextField(s.radiusInput, vm::setRadius, "Radius (1–100)")

            // Tube radius for torus
            if (s.shapeType == ShapeType.TORUS) {
                SpyglassTextField(s.tubeInput, vm::setTube, "Tube radius")
            }
        }

        if (s.layers.isNotEmpty()) {
            // Layer slider
            if (s.layerMin < s.layerMax) {
                Text("Y layer: ${s.currentLayer}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value         = s.currentLayer.toFloat(),
                    onValueChange = { vm.setLayer(it.toInt()) },
                    valueRange    = s.layerMin.toFloat()..s.layerMax.toFloat(),
                    steps         = (s.layerMax - s.layerMin - 1).coerceAtLeast(0),
                    colors        = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold),
                )
            }

            StatRow("Total blocks", "%,d".format(s.totalBlocks))

            // Grid view of current layer
            val layer = s.layers[s.currentLayer] ?: emptySet()
            LayerGrid(blocks = layer)
        }
    }
}

@Composable
private fun LayerGrid(blocks: Set<Pair<Int, Int>>) {
    if (blocks.isEmpty()) return
    val xs = blocks.map { it.first }
    val zs = blocks.map { it.second }
    val minX = xs.min(); val maxX = xs.max()
    val minZ = zs.min(); val maxZ = zs.max()
    val rangeX = (maxX - minX + 1).coerceAtLeast(1)
    val rangeZ = (maxZ - minZ + 1).coerceAtLeast(1)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(rangeX.toFloat() / rangeZ.toFloat())
                .background(Color(0xFF0A0907), RoundedCornerShape(4.dp))
                .border(1.dp, Stone700, RoundedCornerShape(4.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width  / rangeX
                val cellH = size.height / rangeZ
                blocks.forEach { (x, z) ->
                    val px = (x - minX) * cellW
                    val pz = (z - minZ) * cellH
                    drawRect(
                        color   = Gold,
                        topLeft = Offset(px, pz),
                        size    = Size(cellW - 0.5f, cellH - 0.5f),
                    )
                }
            }
        }
        Text(
            "${blocks.size} blocks in this layer  •  X ${minX}–${maxX}  Z ${minZ}–${maxZ}",
            style = MaterialTheme.typography.bodySmall,
            color = Stone500,
        )
    }
}
