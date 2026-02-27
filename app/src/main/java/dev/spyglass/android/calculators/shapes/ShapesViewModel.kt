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
    fun setLayer(y: Int)       { _state.value = _state.value.copy(currentLayer = y) }

    private fun recalc() {
        val s = _state.value
        val r = s.radiusInput.toIntOrNull()?.takeIf { it in 1..100 } ?: return
        val layers: Map<Int, Set<Pair<Int, Int>>> = when (s.shapeType) {
            ShapeType.CIRCLE   -> mapOf(0 to circleLayer(r))
            ShapeType.SPHERE   -> sphereLayers(r)
            ShapeType.DOME     -> sphereLayers(r).filter { (y, _) -> y >= 0 }
            ShapeType.CYLINDER -> {
                val h = s.heightInput.toIntOrNull()?.takeIf { it in 1..256 } ?: return
                cylinderLayers(r, h)
            }
            ShapeType.CONE     -> {
                val h = s.heightInput.toIntOrNull()?.takeIf { it in 1..256 } ?: return
                coneLayers(r, h)
            }
            ShapeType.PYRAMID  -> {
                val h = s.heightInput.toIntOrNull()?.takeIf { it in 1..256 } ?: return
                pyramidLayers(r, h)
            }
            ShapeType.TORUS    -> {
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

    /**
     * Shell sphere: a block is on the surface if it's inside the sphere
     * AND at least one face-neighbor is outside.
     */
    private fun sphereLayers(r: Int): Map<Int, Set<Pair<Int, Int>>> {
        val rSq = (r + 0.5) * (r + 0.5)
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in -r..r) {
            val yy = y.toLong() * y
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -r..r) {
                val xyy = x.toLong() * x + yy
                if (xyy > rSq) continue          // skip early — no z will be inside
                for (z in -r..r) {
                    val distSq = xyy + z.toLong() * z
                    if (distSq > rSq) continue    // outside sphere
                    // Check 6 face-neighbours — if any is outside, this block is shell
                    val isShell =
                        (x + 1L) * (x + 1) + yy + z.toLong() * z > rSq ||
                        (x - 1L) * (x - 1) + yy + z.toLong() * z > rSq ||
                        x.toLong() * x + (y + 1L) * (y + 1) + z.toLong() * z > rSq ||
                        x.toLong() * x + (y - 1L) * (y - 1) + z.toLong() * z > rSq ||
                        xyy + (z + 1L) * (z + 1) > rSq ||
                        xyy + (z - 1L) * (z - 1) > rSq
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

    /** Shell cylinder: circle at each layer, shell-detected via face-neighbors. */
    private fun cylinderLayers(r: Int, h: Int): Map<Int, Set<Pair<Int, Int>>> {
        val rSq = (r + 0.5) * (r + 0.5)
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in 0 until h) {
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -r..r) {
                val xx = x.toLong() * x
                for (z in -r..r) {
                    val distSq = xx + z.toLong() * z
                    if (distSq > rSq) continue
                    val isShell =
                        (x + 1L) * (x + 1) + z.toLong() * z > rSq ||
                        (x - 1L) * (x - 1) + z.toLong() * z > rSq ||
                        xx + (z + 1L) * (z + 1) > rSq ||
                        xx + (z - 1L) * (z - 1) > rSq ||
                        y + 1 >= h ||
                        y - 1 < 0
                    if (isShell) set.add(x to z)
                }
            }
            if (set.isNotEmpty()) layers[y] = set
        }
        return layers
    }

    /** Shell cone: base radius r at y=0, tapers to a point at y=h. */
    private fun coneLayers(r: Int, h: Int): Map<Int, Set<Pair<Int, Int>>> {
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in 0 until h) {
            val effR = r.toDouble() * (h - y) / h
            val rSq = (effR + 0.5) * (effR + 0.5)
            val scan = effR.toInt() + 1
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -scan..scan) {
                for (z in -scan..scan) {
                    val distSq = x.toLong() * x + z.toLong() * z
                    if (distSq > rSq) continue
                    // Check face-neighbors against the cone surface
                    fun inside(nx: Int, ny: Int, nz: Int): Boolean {
                        if (ny < 0 || ny >= h) return false
                        val nEffR = r.toDouble() * (h - ny) / h
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

    /** Shell pyramid: square base half-width r at y=0, tapers to a point at y=h. */
    private fun pyramidLayers(r: Int, h: Int): Map<Int, Set<Pair<Int, Int>>> {
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in 0 until h) {
            val hw = r.toDouble() * (h - y) / h
            val scan = hw.toInt() + 1
            val set = mutableSetOf<Pair<Int, Int>>()
            fun inside(nx: Int, ny: Int, nz: Int): Boolean {
                if (ny < 0 || ny >= h) return false
                val nhw = r.toDouble() * (h - ny) / h
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

    /** Torus: ring radius R, tube radius t. Shell-based like sphere. */
    private fun torusLayers(R: Int, t: Int): Map<Int, Set<Pair<Int, Int>>> {
        val tSq = (t + 0.5) * (t + 0.5)
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        val range = R + t
        for (y in -t..t) {
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -range..range) {
                for (z in -range..range) {
                    // Distance from ring center in XZ plane
                    val dxz = sqrt((x * x + z * z).toDouble()) - R
                    val distSq = dxz * dxz + y * y
                    if (distSq > tSq) continue
                    // Check neighbours
                    fun outside(nx: Int, ny: Int, nz: Int): Boolean {
                        val ndxz = sqrt((nx * nx + nz * nz).toDouble()) - R
                        return ndxz * ndxz + ny * ny > tSq
                    }
                    if (outside(x+1,y,z) || outside(x-1,y,z) ||
                        outside(x,y+1,z) || outside(x,y-1,z) ||
                        outside(x,y,z+1) || outside(x,y,z-1)) {
                        set.add(x to z)
                    }
                }
            }
            if (set.isNotEmpty()) layers.getOrPut(y) { mutableSetOf() }.addAll(set)
        }
        return layers
    }
}
