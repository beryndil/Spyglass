package dev.spyglass.android.calculators.shapes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

enum class ShapeType { CIRCLE, SPHERE, DOME, TORUS }

data class ShapesState(
    val shapeType:    ShapeType = ShapeType.CIRCLE,
    val radiusInput:  String    = "10",
    val tubeInput:    String    = "3",        // torus tube radius
    val currentLayer: Int       = 0,
    val layers:       Map<Int, Set<Pair<Int, Int>>> = emptyMap(),  // y → set of (x,z) offsets
    val totalBlocks:  Int       = 0,
    val layerMin:     Int       = 0,
    val layerMax:     Int       = 0,
)

class ShapesViewModel : ViewModel() {
    private val _state = MutableStateFlow(ShapesState())
    val state: StateFlow<ShapesState> = _state.asStateFlow()

    fun setShape(t: ShapeType) { _state.value = _state.value.copy(shapeType = t); recalc() }
    fun setRadius(v: String)   { _state.value = _state.value.copy(radiusInput = v); recalc() }
    fun setTube(v: String)     { _state.value = _state.value.copy(tubeInput = v); recalc() }
    fun setLayer(y: Int)       { _state.value = _state.value.copy(currentLayer = y) }

    private fun recalc() {
        val s = _state.value
        val r = s.radiusInput.toIntOrNull()?.takeIf { it in 1..100 } ?: return
        val layers: Map<Int, Set<Pair<Int, Int>>> = when (s.shapeType) {
            ShapeType.CIRCLE -> mapOf(0 to circleLayer(r))
            ShapeType.SPHERE -> sphereLayers(r)
            ShapeType.DOME   -> sphereLayers(r).filter { (y, _) -> y >= 0 }
            ShapeType.TORUS  -> {
                val tube = s.tubeInput.toIntOrNull()?.takeIf { it in 1..r } ?: return
                torusLayers(r, tube)
            }
        }
        val yMin = layers.keys.minOrNull() ?: 0
        val yMax = layers.keys.maxOrNull() ?: 0
        val total = layers.values.sumOf { it.size }
        _state.value = s.copy(
            layers      = layers,
            totalBlocks = total,
            layerMin    = yMin,
            layerMax    = yMax,
            currentLayer = currentLayer(s.currentLayer, yMin, yMax),
        )
    }

    private fun currentLayer(prev: Int, min: Int, max: Int) = prev.coerceIn(min, max)

    // ── Geometry ──────────────────────────────────────────────────────────────

    /** Midpoint circle algorithm — hollow ring of blocks. */
    private fun circleLayer(r: Int): Set<Pair<Int, Int>> {
        val points = mutableSetOf<Pair<Int, Int>>()
        var x = 0; var z = r; var d = 1 - r
        while (x <= z) {
            for ((px, pz) in listOf(x to z, z to x, -x to z, -z to x, x to -z, z to -x, -x to -z, -z to -x))
                points.add(px to pz)
            if (d < 0) d += 2 * x + 3 else { d += 2 * (x - z) + 5; z-- }
            x++
        }
        return points
    }

    /** Sphere: for each Y slice compute the circle radius at that height. */
    private fun sphereLayers(r: Int): Map<Int, Set<Pair<Int, Int>>> {
        val layers = mutableMapOf<Int, Set<Pair<Int, Int>>>()
        for (y in -r..r) {
            val sliceR = sqrt((r * r - y * y).toDouble()).roundToInt()
            if (sliceR >= 0) layers[y] = circleLayer(sliceR)
        }
        return layers
    }

    /** Torus: ring radius R, tube radius t. */
    private fun torusLayers(R: Int, t: Int): Map<Int, Set<Pair<Int, Int>>> {
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in -t..t) {
            val set = mutableSetOf<Pair<Int, Int>>()
            // At height y the tube slice is a circle of radius sqrt(t²-y²)
            val sliceR = sqrt((t * t - y * y).toDouble())
            // The centres of tube circles sit on a ring of radius R in XZ
            // Approximate the ring with enough sample points
            val steps = (R * 2 * PI).toInt().coerceAtLeast(64)
            for (i in 0 until steps) {
                val angle = 2 * PI * i / steps
                val cx = (R * cos(angle)).roundToInt()
                val cz = (R * sin(angle)).roundToInt()
                // Add a disc of radius sliceR around (cx, cz)
                val sr = sliceR.toInt()
                for (dx in -sr..sr) for (dz in -sr..sr)
                    if (dx * dx + dz * dz <= sliceR * sliceR + sliceR)
                        set.add((cx + dx) to (cz + dz))
            }
            if (set.isNotEmpty()) layers.getOrPut(y) { mutableSetOf() }.addAll(set)
        }
        return layers
    }
}
