package dev.spyglass.android.calculators.shapes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.spyglass.android.core.ui.PixelIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

private val SHAPE_LABELS = mapOf(
    ShapeType.CIRCLE   to "Circle",
    ShapeType.SPHERE   to "Sphere",
    ShapeType.DOME     to "Dome",
    ShapeType.CYLINDER to "Cylinder",
    ShapeType.CONE     to "Cone",
    ShapeType.PYRAMID  to "Pyramid",
    ShapeType.TORUS    to "Torus",
    ShapeType.WALL     to "Wall",
    ShapeType.ARCH     to "Arch",
    ShapeType.ELLIPSOID to "Ellipsoid",
    ShapeType.ARC_WALL to "Arc Wall",
    ShapeType.SPIRAL   to "Spiral",
)

@Composable
fun ShapesScreen(vm: ShapesViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var view3D by remember { mutableStateOf(false) }
    val hapticConfirm = rememberHapticConfirm()

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(stringResource(R.string.shapes_header), icon = PixelIcons.Shapes)

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

            // Radius slider (not shown for Wall)
            if (s.shapeType != ShapeType.WALL) {
                val radiusVal = s.radiusInput.toIntOrNull() ?: 10
                val radiusLabel = when (s.shapeType) {
                    ShapeType.PYRAMID -> "Half-width"
                    ShapeType.ELLIPSOID -> "Radius X"
                    else -> "Radius"
                }
                LabeledSlider(radiusLabel, radiusVal, 1, 100) { vm.setRadius(it.toString()) }
            }

            // Height slider for cylinder / cone / pyramid / wall / arc wall / spiral
            if (s.shapeType in setOf(ShapeType.CYLINDER, ShapeType.CONE, ShapeType.PYRAMID, ShapeType.WALL, ShapeType.ARC_WALL, ShapeType.SPIRAL)) {
                val heightVal = s.heightInput.toIntOrNull() ?: 10
                LabeledSlider("Height", heightVal, 1, 256) { vm.setHeight(it.toString()) }
            }

            // Tube radius slider for torus
            if (s.shapeType == ShapeType.TORUS) {
                val radiusVal = s.radiusInput.toIntOrNull() ?: 10
                val tubeVal = s.tubeInput.toIntOrNull() ?: 3
                val maxTube = radiusVal.coerceAtLeast(1)
                LabeledSlider("Tube radius", tubeVal, 1, maxTube) { vm.setTube(it.toString()) }
            }

            // Thickness slider for sphere, dome, torus, arch, ellipsoid, arc wall
            if (s.shapeType in setOf(ShapeType.SPHERE, ShapeType.DOME, ShapeType.TORUS, ShapeType.ARCH, ShapeType.ELLIPSOID, ShapeType.ARC_WALL)) {
                val maxThick = when (s.shapeType) {
                    ShapeType.TORUS -> (s.tubeInput.toIntOrNull() ?: 3).coerceAtLeast(1)
                    ShapeType.ARC_WALL -> 5
                    else -> 3
                }
                LabeledSlider("Thickness", s.thickness.coerceIn(1, maxThick), 1, maxThick) { vm.setThickness(it) }
            }

            // Wall-specific: X/Z offset sliders
            if (s.shapeType == ShapeType.WALL) {
                val dxVal = s.wallDxInput.toIntOrNull() ?: 20
                LabeledSlider("X Offset", dxVal, -100, 100) { vm.setWallDx(it.toString()) }
                val dzVal = s.wallDzInput.toIntOrNull() ?: 10
                LabeledSlider("Z Offset", dzVal, -100, 100) { vm.setWallDz(it.toString()) }
            }

            // Width slider for wall / spiral
            if (s.shapeType in setOf(ShapeType.WALL, ShapeType.SPIRAL)) {
                val widthVal = s.widthInput.toIntOrNull() ?: 2
                LabeledSlider("Width", widthVal, 1, 5) { vm.setWidth(it.toString()) }
            }

            // Ellipsoid-specific: Radius Y and Radius Z
            if (s.shapeType == ShapeType.ELLIPSOID) {
                val ryVal = s.radiusYInput.toIntOrNull() ?: 10
                LabeledSlider("Radius Y", ryVal, 1, 100) { vm.setRadiusY(it.toString()) }
                val rzVal = s.radiusZInput.toIntOrNull() ?: 10
                LabeledSlider("Radius Z", rzVal, 1, 100) { vm.setRadiusZ(it.toString()) }
            }

            // Arch-specific: Length slider
            if (s.shapeType == ShapeType.ARCH) {
                val lenVal = s.lengthInput.toIntOrNull() ?: 10
                LabeledSlider("Length", lenVal, 1, 100) { vm.setLength(it.toString()) }
            }

            // Arc wall-specific: Angle slider
            if (s.shapeType == ShapeType.ARC_WALL) {
                val angleVal = s.angleInput.toIntOrNull() ?: 180
                LabeledSlider("Angle", angleVal, 10, 360) { vm.setAngle(it.toString()) }
            }

            // Spiral-specific: Step height slider
            if (s.shapeType == ShapeType.SPIRAL) {
                val stepVal = s.stepInput.toIntOrNull() ?: 1
                LabeledSlider("Step Height", stepVal, 1, 5) { vm.setStep(it.toString()) }
            }

            // Hollow toggle for cylinder
            if (s.shapeType == ShapeType.CYLINDER) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.shapes_hollow), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = s.hollow,
                        onCheckedChange = { hapticConfirm(); vm.setHollow(it) },
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
                    Text(stringResource(R.string.shapes_flipped), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = s.flipped,
                        onCheckedChange = { hapticConfirm(); vm.setFlipped(it) },
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

            StatRow(stringResource(R.string.shapes_total_blocks), "%,d".format(s.totalBlocks))

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
                StatRow(stringResource(R.string.shapes_width_x),  fmt(width))
                StatRow(stringResource(R.string.shapes_depth_z),  fmt(depth))
                StatRow(stringResource(R.string.shapes_height_y), fmt(height))
            }

            SpyglassDivider()

            if (view3D) {
                IsometricView(s.layers)
            } else {
                // Layer slider
                if (s.layerMin < s.layerMax) {
                    Text(stringResource(R.string.shapes_y_layer, s.currentLayer), style = MaterialTheme.typography.bodyMedium)
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
            stringResource(R.string.shapes_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
