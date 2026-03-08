package dev.spyglass.android.calculators.shapes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.*

enum class ShapeType { CIRCLE, SPHERE, DOME, CYLINDER, CONE, PYRAMID, TORUS, WALL, ARCH, ELLIPSOID, ARC_WALL, SPIRAL }

data class ShapesState(
    val shapeType:    ShapeType = ShapeType.CIRCLE,
    val radiusInput:  String    = "10",
    val heightInput:  String    = "10",       // cylinder / cone / pyramid height
    val tubeInput:    String    = "3",        // torus tube radius
    val thickness:    Int       = 1,         // wall/border thickness (circle, cone, pyramid, torus)
    val flipped:      Boolean   = false,     // flip cone/pyramid upside down
    val hollow:       Boolean   = false,     // hollow cylinder (no top/bottom caps)
    val radiusYInput: String    = "10",      // ellipsoid Y radius
    val radiusZInput: String    = "10",      // ellipsoid Z radius
    val lengthInput:  String    = "10",      // arch/tunnel depth
    val angleInput:   String    = "180",     // arc wall angle span (degrees)
    val wallDxInput:  String    = "20",      // diagonal wall X offset
    val wallDzInput:  String    = "10",      // diagonal wall Z offset
    val stepInput:    String    = "1",       // spiral staircase step height
    val widthInput:   String    = "2",       // spiral/wall width
    val currentLayer: Int       = 0,
    val layers:       Map<Int, Set<Pair<Int, Int>>> = emptyMap(),  // y → set of (x,z) offsets
    val totalBlocks:  Int       = 0,
    val layerMin:     Int       = 0,
    val layerMax:     Int       = 0,
)

class ShapesViewModel : ViewModel() {
    private val _state = MutableStateFlow(ShapesState())
    val state: StateFlow<ShapesState> = _state.asStateFlow()
    private var calcJob: Job? = null

    fun setShape(t: ShapeType) { _state.value = _state.value.copy(shapeType = t); recalc() }
    fun setRadius(v: String)   { _state.value = _state.value.copy(radiusInput = v); recalc() }
    fun setHeight(v: String)   { _state.value = _state.value.copy(heightInput = v); recalc() }
    fun setTube(v: String)     { _state.value = _state.value.copy(tubeInput = v); recalc() }
    fun setThickness(v: Int)   { _state.value = _state.value.copy(thickness = v); recalc() }
    fun setFlipped(v: Boolean) { _state.value = _state.value.copy(flipped = v); recalc() }
    fun setHollow(v: Boolean)  { _state.value = _state.value.copy(hollow = v); recalc() }
    fun setRadiusY(v: String)  { _state.value = _state.value.copy(radiusYInput = v); recalc() }
    fun setRadiusZ(v: String)  { _state.value = _state.value.copy(radiusZInput = v); recalc() }
    fun setLength(v: String)   { _state.value = _state.value.copy(lengthInput = v); recalc() }
    fun setAngle(v: String)    { _state.value = _state.value.copy(angleInput = v); recalc() }
    fun setWallDx(v: String)   { _state.value = _state.value.copy(wallDxInput = v); recalc() }
    fun setWallDz(v: String)   { _state.value = _state.value.copy(wallDzInput = v); recalc() }
    fun setStep(v: String)     { _state.value = _state.value.copy(stepInput = v); recalc() }
    fun setWidth(v: String)    { _state.value = _state.value.copy(widthInput = v); recalc() }
    fun setLayer(y: Int)       { _state.value = _state.value.copy(currentLayer = y) }

    private fun recalc() {
        calcJob?.cancel()
        calcJob = viewModelScope.launch(Dispatchers.Default) { doRecalc() }
    }

    private suspend fun doRecalc() {
        val s = _state.value
        val r = s.radiusInput.toIntOrNull()?.takeIf { it in 1..100 }
        if (r == null) {
            _state.update { it.copy(layers = emptyMap(), totalBlocks = 0, layerMin = 0, layerMax = 0, currentLayer = 0) }
            return
        }
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
            ShapeType.WALL     -> {
                val dx = s.wallDxInput.toIntOrNull()?.takeIf { it in -100..100 } ?: return
                val dz = s.wallDzInput.toIntOrNull()?.takeIf { it in -100..100 } ?: return
                val h  = s.heightInput.toIntOrNull()?.takeIf { it in 1..256 } ?: return
                val w  = s.widthInput.toIntOrNull()?.takeIf { it in 1..5 } ?: return
                wallLayers(dx, dz, h, w)
            }
            ShapeType.ARCH     -> {
                val len = s.lengthInput.toIntOrNull()?.takeIf { it in 1..100 } ?: return
                archLayers(r, len, thick)
            }
            ShapeType.ELLIPSOID -> {
                val ry = s.radiusYInput.toIntOrNull()?.takeIf { it in 1..100 } ?: return
                val rz = s.radiusZInput.toIntOrNull()?.takeIf { it in 1..100 } ?: return
                ellipsoidLayers(r, ry, rz, thick)
            }
            ShapeType.ARC_WALL -> {
                val angle = s.angleInput.toIntOrNull()?.takeIf { it in 10..360 } ?: return
                val h = s.heightInput.toIntOrNull()?.takeIf { it in 1..256 } ?: return
                val t = s.thickness.coerceIn(1, 5)
                arcWallLayers(r, angle, h, t)
            }
            ShapeType.SPIRAL   -> {
                val h    = s.heightInput.toIntOrNull()?.takeIf { it in 1..256 } ?: return
                val step = s.stepInput.toIntOrNull()?.takeIf { it in 1..5 } ?: return
                val w    = s.widthInput.toIntOrNull()?.takeIf { it in 1..5 } ?: return
                spiralLayers(r, h, step, w)
            }
        }
        val yMin = layers.keys.minOrNull() ?: 0
        val yMax = layers.keys.maxOrNull() ?: 0
        val total = layers.values.sumOf { it.size }
        coroutineContext.ensureActive()
        _state.update { current ->
            current.copy(
                layers      = layers,
                totalBlocks = total,
                layerMin    = yMin,
                layerMax    = yMax,
                currentLayer = currentLayer(current.currentLayer, yMin, yMax),
            )
        }
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

    // ── New shapes ──────────────────────────────────────────────────────────

    /** Diagonal wall using Bresenham's line from (0,0) to (dx,dz), thickened and extruded to height. */
    private fun wallLayers(dx: Int, dz: Int, height: Int, width: Int): Map<Int, Set<Pair<Int, Int>>> {
        val line = bresenhamLine(0, 0, dx, dz)
        // Compute perpendicular direction for thickening
        val len = sqrt((dx.toDouble() * dx + dz.toDouble() * dz)).coerceAtLeast(1.0)
        val px = -dz.toDouble() / len
        val pz = dx.toDouble() / len
        val footprint = mutableSetOf<Pair<Int, Int>>()
        for ((lx, lz) in line) {
            for (w in 0 until width) {
                val offset = w - (width - 1) / 2.0
                val bx = (lx + px * offset).roundToInt()
                val bz = (lz + pz * offset).roundToInt()
                footprint.add(bx to bz)
            }
        }
        val layers = mutableMapOf<Int, Set<Pair<Int, Int>>>()
        for (y in 0 until height) {
            layers[y] = footprint
        }
        return layers
    }

    /** Bresenham's line algorithm from (x0,z0) to (x1,z1). */
    private fun bresenhamLine(x0: Int, z0: Int, x1: Int, z1: Int): List<Pair<Int, Int>> {
        val points = mutableListOf<Pair<Int, Int>>()
        var cx = x0; var cz = z0
        val adx = abs(x1 - x0); val adz = abs(z1 - z0)
        val sx = if (x0 < x1) 1 else -1
        val sz = if (z0 < z1) 1 else -1
        var err = adx - adz
        while (true) {
            points.add(cx to cz)
            if (cx == x1 && cz == z1) break
            val e2 = 2 * err
            if (e2 > -adz) { err -= adz; cx += sx }
            if (e2 < adx) { err += adx; cz += sz }
        }
        return points
    }

    /** Arch/tunnel — upper half of a cylinder shell, extruded along Z for [length] blocks. */
    private fun archLayers(radius: Int, length: Int, thickness: Int): Map<Int, Set<Pair<Int, Int>>> {
        val rSq = (radius + 0.5) * (radius + 0.5)
        val t = thickness.toLong()
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in 0..radius) {
            val yy = y.toLong() * y
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -radius..radius) {
                val distSq = x.toLong() * x + yy
                if (distSq > rSq) continue
                val isShell =
                    (x + t) * (x + t) + yy > rSq ||
                    (x - t) * (x - t) + yy > rSq ||
                    x.toLong() * x + (y + t) * (y + t) > rSq ||
                    (y - t < 0) ||
                    x.toLong() * x + (y - t) * (y - t) > rSq
                if (isShell) {
                    for (z in 0 until length) {
                        set.add(x to z)
                    }
                }
            }
            if (set.isNotEmpty()) layers[y] = set
        }
        return layers
    }

    /** Ellipsoid shell with independent X/Y/Z radii. */
    private fun ellipsoidLayers(rx: Int, ry: Int, rz: Int, thickness: Int): Map<Int, Set<Pair<Int, Int>>> {
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        val t = thickness.toDouble()
        for (y in -ry..ry) {
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -rx..rx) {
                for (z in -rz..rz) {
                    val d = (x.toDouble() / rx).let { it * it } +
                            (y.toDouble() / ry).let { it * it } +
                            (z.toDouble() / rz).let { it * it }
                    if (d > 1.0) continue
                    // Shell check: at least one neighbor at thickness steps is outside
                    val isShell =
                        ((x + t) / rx).let { it * it } + (y.toDouble() / ry).let { it * it } + (z.toDouble() / rz).let { it * it } > 1.0 ||
                        ((x - t) / rx).let { it * it } + (y.toDouble() / ry).let { it * it } + (z.toDouble() / rz).let { it * it } > 1.0 ||
                        (x.toDouble() / rx).let { it * it } + ((y + t) / ry).let { it * it } + (z.toDouble() / rz).let { it * it } > 1.0 ||
                        (x.toDouble() / rx).let { it * it } + ((y - t) / ry).let { it * it } + (z.toDouble() / rz).let { it * it } > 1.0 ||
                        (x.toDouble() / rx).let { it * it } + (y.toDouble() / ry).let { it * it } + ((z + t) / rz).let { it * it } > 1.0 ||
                        (x.toDouble() / rx).let { it * it } + (y.toDouble() / ry).let { it * it } + ((z - t) / rz).let { it * it } > 1.0
                    if (isShell) set.add(x to z)
                }
            }
            if (set.isNotEmpty()) layers[y] = set
        }
        return layers
    }

    /** Arc wall — a curved wall segment spanning [angle] degrees, extruded to [height]. */
    private fun arcWallLayers(radius: Int, angle: Int, height: Int, thickness: Int): Map<Int, Set<Pair<Int, Int>>> {
        val angleRad = Math.toRadians(angle.toDouble())
        val rOuter = radius + thickness / 2.0
        val rInner = radius - thickness / 2.0
        val outerSq = (rOuter + 0.5) * (rOuter + 0.5)
        val innerSq = if (rInner > 0) (rInner - 0.5).let { it * it } else 0.0
        val scan = rOuter.toInt() + 1
        val footprint = mutableSetOf<Pair<Int, Int>>()
        for (x in -scan..scan) {
            for (z in -scan..scan) {
                val distSq = x.toLong() * x + z.toLong() * z
                if (distSq > outerSq || distSq < innerSq) continue
                var a = atan2(z.toDouble(), x.toDouble())
                if (a < 0) a += 2.0 * PI
                if (a <= angleRad) footprint.add(x to z)
            }
        }
        val layers = mutableMapOf<Int, Set<Pair<Int, Int>>>()
        for (y in 0 until height) {
            layers[y] = footprint
        }
        return layers
    }

    /** Spiral staircase — blocks rotate around the center as Y increases. */
    private fun spiralLayers(radius: Int, height: Int, @Suppress("UNUSED_PARAMETER") stepHeight: Int, width: Int): Map<Int, Set<Pair<Int, Int>>> {
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        val rOuter = radius + 0.5
        val rInner = (radius - width).coerceAtLeast(0) + 0.5
        val wedge = PI / 4.0  // 45-degree wedge per step
        for (y in 0 until height) {
            val centerAngle = 2.0 * PI * y / height
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in -radius..radius) {
                for (z in -radius..radius) {
                    val distSq = x.toLong() * x + z.toLong() * z
                    if (distSq > rOuter * rOuter) continue
                    if (distSq < rInner * rInner) continue
                    var a = atan2(z.toDouble(), x.toDouble()) - centerAngle
                    // Normalize to -PI..PI
                    a = a - 2.0 * PI * floor((a + PI) / (2.0 * PI))
                    if (abs(a) <= wedge / 2.0) set.add(x to z)
                }
            }
            if (set.isNotEmpty()) layers[y] = set
        }
        return layers
    }
}
