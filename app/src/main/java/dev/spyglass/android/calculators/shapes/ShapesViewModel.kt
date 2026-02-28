package dev.spyglass.android.calculators.shapes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

enum class ShapeType { CIRCLE, SPHERE, DOME, CYLINDER, CONE, PYRAMID, TORUS }

data class ShapesState(
    val shapeType:    ShapeType = ShapeType.CIRCLE,
    val radiusInput:  String    = "10",
    val heightInput:  String    = "10",       // cylinder / cone / pyramid height
    val tubeInput:    String    = "3",        // torus tube radius
    val thickness:    Int       = 1,         // wall/border thickness (circle, cone, pyramid, torus)
    val flipped:      Boolean   = false,     // flip cone/pyramid upside down
    val hollow:       Boolean   = false,     // hollow cylinder (no top/bottom caps)
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
    fun setHeight(v: String)   { _state.value = _state.value.copy(heightInput = v); recalc() }
    fun setTube(v: String)     { _state.value = _state.value.copy(tubeInput = v); recalc() }
    fun setThickness(v: Int)   { _state.value = _state.value.copy(thickness = v); recalc() }
    fun setFlipped(v: Boolean) { _state.value = _state.value.copy(flipped = v); recalc() }
    fun setHollow(v: Boolean)  { _state.value = _state.value.copy(hollow = v); recalc() }
    fun setLayer(y: Int)       { _state.value = _state.value.copy(currentLayer = y) }

    private fun recalc() {
        val s = _state.value
        val r = s.radiusInput.toIntOrNull()?.takeIf { it in 1..100 } ?: return
        val thick = s.thickness.coerceIn(1, 3)
        val layers: Map<Int, Set<Pair<Int, Int>>> = when (s.shapeType) {
            ShapeType.CIRCLE   -> mapOf(0 to circleLayer(r))
            ShapeType.SPHERE   -> sphereLayers(r, thick)
            ShapeType.DOME     -> sphereLayers(r, thick).filter { (y, _) -> y >= 0 }
            ShapeType.CYLINDER -> {
                val h = s.heightInput.toIntOrNull()?.takeIf { it in 1..256 } ?: return
                cylinderLayers(r, h, s.hollow)
            }
            ShapeType.CONE     -> {
                val h = s.heightInput.toIntOrNull()?.takeIf { it in 1..256 } ?: return
                coneLayers(r, h, s.flipped)
            }
            ShapeType.PYRAMID  -> {
                val h = s.heightInput.toIntOrNull()?.takeIf { it in 1..256 } ?: return
                pyramidLayers(r, h, s.flipped)
            }
            ShapeType.TORUS    -> {
                val tube = s.tubeInput.toIntOrNull()?.takeIf { it in 1..r } ?: return
                torusLayers(r, tube, s.thickness.coerceIn(1, tube))
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

    /**
     * Shell sphere with adjustable wall thickness (1–3).
     * A block is on the thick shell if it's inside the sphere AND at least one
     * block at [thickness] steps away along any axis is outside.
     */
    private fun sphereLayers(r: Int, thickness: Int): Map<Int, Set<Pair<Int, Int>>> {
        val rSq = (r + 0.5) * (r + 0.5)
        val t = thickness.toLong()
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in -r..r) {
            val yy = y.toLong() * y
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -r..r) {
                val xyy = x.toLong() * x + yy
                if (xyy > rSq) continue
                for (z in -r..r) {
                    val distSq = xyy + z.toLong() * z
                    if (distSq > rSq) continue
                    val isShell =
                        (x + t) * (x + t) + yy + z.toLong() * z > rSq ||
                        (x - t) * (x - t) + yy + z.toLong() * z > rSq ||
                        x.toLong() * x + (y + t) * (y + t) + z.toLong() * z > rSq ||
                        x.toLong() * x + (y - t) * (y - t) + z.toLong() * z > rSq ||
                        xyy + (z + t) * (z + t) > rSq ||
                        xyy + (z - t) * (z - t) > rSq
                    if (isShell) set.add(x to z)
                }
            }
            if (set.isNotEmpty()) layers[y] = set
        }
        return layers
    }

    /** Flat circle outline using midpoint algorithm. */
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

    /** Shell cylinder. When hollow, only the circular wall is kept (no top/bottom caps). */
    private fun cylinderLayers(r: Int, h: Int, hollow: Boolean): Map<Int, Set<Pair<Int, Int>>> {
        val rSq = (r + 0.5) * (r + 0.5)
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in 0 until h) {
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -r..r) {
                val xx = x.toLong() * x
                for (z in -r..r) {
                    val distSq = xx + z.toLong() * z
                    if (distSq > rSq) continue
                    val onWall =
                        (x + 1L) * (x + 1) + z.toLong() * z > rSq ||
                        (x - 1L) * (x - 1) + z.toLong() * z > rSq ||
                        xx + (z + 1L) * (z + 1) > rSq ||
                        xx + (z - 1L) * (z - 1) > rSq
                    val onCap = !hollow && (y + 1 >= h || y - 1 < 0)
                    if (onWall || onCap) set.add(x to z)
                }
            }
            if (set.isNotEmpty()) layers[y] = set
        }
        return layers
    }

    /** Shell cone with optional flip (tip at bottom when flipped). */
    private fun coneLayers(r: Int, h: Int, flipped: Boolean): Map<Int, Set<Pair<Int, Int>>> {
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in 0 until h) {
            val effY = if (flipped) h - 1 - y else y
            val effR = r.toDouble() * (h - effY) / h
            val rSq = (effR + 0.5) * (effR + 0.5)
            val scan = effR.toInt() + 1
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -scan..scan) {
                for (z in -scan..scan) {
                    val distSq = x.toLong() * x + z.toLong() * z
                    if (distSq > rSq) continue
                    fun inside(nx: Int, ny: Int, nz: Int): Boolean {
                        if (ny < 0 || ny >= h) return false
                        val nEffY = if (flipped) h - 1 - ny else ny
                        val nEffR = r.toDouble() * (h - nEffY) / h
                        val nRSq = (nEffR + 0.5) * (nEffR + 0.5)
                        return nx.toLong() * nx + nz.toLong() * nz <= nRSq
                    }
                    val isShell =
                        !inside(x + 1, y, z) || !inside(x - 1, y, z) ||
                        !inside(x, y + 1, z) || !inside(x, y - 1, z) ||
                        !inside(x, y, z + 1) || !inside(x, y, z - 1)
                    if (isShell) set.add(x to z)
                }
            }
            if (set.isNotEmpty()) layers[y] = set
        }
        return layers
    }

    /** Shell pyramid with optional flip (tip at bottom when flipped). */
    private fun pyramidLayers(r: Int, h: Int, flipped: Boolean): Map<Int, Set<Pair<Int, Int>>> {
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in 0 until h) {
            val effY = if (flipped) h - 1 - y else y
            val hw = r.toDouble() * (h - effY) / h
            val scan = hw.toInt() + 1
            val set = mutableSetOf<Pair<Int, Int>>()
            fun inside(nx: Int, ny: Int, nz: Int): Boolean {
                if (ny < 0 || ny >= h) return false
                val nEffY = if (flipped) h - 1 - ny else ny
                val nhw = r.toDouble() * (h - nEffY) / h
                return abs(nx) <= nhw + 0.5 && abs(nz) <= nhw + 0.5
            }
            for (x in -scan..scan) {
                for (z in -scan..scan) {
                    if (abs(x) > hw + 0.5 || abs(z) > hw + 0.5) continue
                    val isShell =
                        !inside(x + 1, y, z) || !inside(x - 1, y, z) ||
                        !inside(x, y + 1, z) || !inside(x, y - 1, z) ||
                        !inside(x, y, z + 1) || !inside(x, y, z - 1)
                    if (isShell) set.add(x to z)
                }
            }
            if (set.isNotEmpty()) layers[y] = set
        }
        return layers
    }

    /** Torus with adjustable tube wall thickness. thickness=1 is the normal shell. */
    private fun torusLayers(R: Int, t: Int, thickness: Int): Map<Int, Set<Pair<Int, Int>>> {
        val outerSq = (t + 0.5) * (t + 0.5)
        val innerR = t - thickness + 0.5
        val filled = innerR <= 0
        val innerSq = innerR * innerR
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        val range = R + t
        for (y in -t..t) {
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -range..range) {
                for (z in -range..range) {
                    val dxz = sqrt((x * x + z * z).toDouble()) - R
                    val distSq = dxz * dxz + y * y
                    if (distSq <= outerSq && (filled || distSq > innerSq)) {
                        set.add(x to z)
                    }
                }
            }
            if (set.isNotEmpty()) layers.getOrPut(y) { mutableSetOf() }.addAll(set)
        }
        return layers
    }
}
