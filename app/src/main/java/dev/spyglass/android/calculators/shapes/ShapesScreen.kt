package dev.spyglass.android.calculators.shapes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.spyglass.android.core.ui.PixelIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

private val SHAPE_LABELS = mapOf(
    ShapeType.CIRCLE   to "Circle",
    ShapeType.SPHERE   to "Sphere",
    ShapeType.DOME     to "Dome",
    ShapeType.CYLINDER to "Cylinder",
    ShapeType.CONE     to "Cone",
    ShapeType.PYRAMID  to "Pyramid",
    ShapeType.TORUS    to "Torus",
)

@Composable
fun ShapesScreen(vm: ShapesViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    var view3D by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Shape Designer", icon = PixelIcons.Shapes)

        InputCard {
            // Shape type selector — two rows of chips
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                ShapeType.entries.forEach { type ->
                    val selected = s.shapeType == type
                    FilterChip(
                        selected = selected,
                        onClick  = { vm.setShape(type) },
                        label    = { Text(SHAPE_LABELS[type] ?: type.name) },
                    )
                }
            }

            // Radius slider
            val radiusVal = s.radiusInput.toIntOrNull() ?: 10
            val radiusLabel = if (s.shapeType == ShapeType.PYRAMID) "Half-width" else "Radius"
            LabeledSlider(radiusLabel, radiusVal, 1, 100) { vm.setRadius(it.toString()) }

            // Height slider for cylinder / cone / pyramid
            if (s.shapeType in setOf(ShapeType.CYLINDER, ShapeType.CONE, ShapeType.PYRAMID)) {
                val heightVal = s.heightInput.toIntOrNull() ?: 10
                LabeledSlider("Height", heightVal, 1, 256) { vm.setHeight(it.toString()) }
            }

            // Tube radius slider for torus
            if (s.shapeType == ShapeType.TORUS) {
                val tubeVal = s.tubeInput.toIntOrNull() ?: 3
                val maxTube = radiusVal.coerceAtLeast(1)
                LabeledSlider("Tube radius", tubeVal, 1, maxTube) { vm.setTube(it.toString()) }
            }

            // Thickness slider for sphere, dome, torus
            if (s.shapeType in setOf(ShapeType.SPHERE, ShapeType.DOME, ShapeType.TORUS)) {
                val maxThick = when (s.shapeType) {
                    ShapeType.TORUS -> (s.tubeInput.toIntOrNull() ?: 3).coerceAtLeast(1)
                    else -> 3
                }
                LabeledSlider("Thickness", s.thickness.coerceIn(1, maxThick), 1, maxThick) { vm.setThickness(it) }
            }

            // Hollow toggle for cylinder
            if (s.shapeType == ShapeType.CYLINDER) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Hollow", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = s.hollow,
                        onCheckedChange = { vm.setHollow(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }

            // Flip toggle for cone and pyramid
            if (s.shapeType in setOf(ShapeType.CONE, ShapeType.PYRAMID)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Flipped", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = s.flipped,
                        onCheckedChange = { vm.setFlipped(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
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
            "Build perfect shapes in Minecraft. Set a radius and height, then use the Y-layer slider to see exactly which blocks to place on each layer, or switch to 3D for an isometric preview.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
