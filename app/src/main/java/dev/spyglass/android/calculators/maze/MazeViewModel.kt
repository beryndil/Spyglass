package dev.spyglass.android.calculators.maze

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*
import kotlin.math.roundToInt
import kotlin.random.Random

enum class MazeType { RECT, CIRCLE, FLOORS }

data class MazeState(
    val mazeType:     MazeType = MazeType.RECT,
    val widthCells:   Int = 8,
    val lengthCells:  Int = 8,
    val wallHeight:   Int = 3,
    val pathWidth:    Int = 2,
    val rings:        Int = 4,
    val floors:       Int = 3,
    val seed:         Long = System.nanoTime(),
    val generated:    Boolean = false,
    val currentLayer: Int = 0,
    val layers:       Map<Int, Set<Pair<Int, Int>>> = emptyMap(),
    val totalBlocks:  Int = 0,
    val layerMin:     Int = 0,
    val layerMax:     Int = 0,
    val deadEnds:     Int = 0,
    val longestPath:  Int = 0,
)

class MazeViewModel : ViewModel() {
    private val _state = MutableStateFlow(MazeState())
    val state: StateFlow<MazeState> = _state.asStateFlow()

    fun setMazeType(t: MazeType)  { _state.value = _state.value.copy(mazeType = t); maybeRecalc() }
    fun setWidthCells(v: Int)     { _state.value = _state.value.copy(widthCells = v); maybeRecalc() }
    fun setLengthCells(v: Int)    { _state.value = _state.value.copy(lengthCells = v); maybeRecalc() }
    fun setWallHeight(v: Int)     { _state.value = _state.value.copy(wallHeight = v); maybeRecalc() }
    fun setPathWidth(v: Int)      { _state.value = _state.value.copy(pathWidth = v); maybeRecalc() }
    fun setRings(v: Int)          { _state.value = _state.value.copy(rings = v); maybeRecalc() }
    fun setFloors(v: Int)         { _state.value = _state.value.copy(floors = v); maybeRecalc() }
    fun setLayer(y: Int)          { _state.value = _state.value.copy(currentLayer = y) }

    fun generate() {
        _state.value = _state.value.copy(generated = true)
        recalc()
    }

    fun shuffle() {
        _state.value = _state.value.copy(seed = System.nanoTime(), generated = true)
        recalc()
    }

    private fun maybeRecalc() {
        if (_state.value.generated) recalc()
    }

    private fun recalc() {
        val s = _state.value
        val rng = Random(s.seed)

        val result = when (s.mazeType) {
            MazeType.RECT   -> generateRectMaze(s.widthCells, s.lengthCells, s.wallHeight, s.pathWidth, rng)
            MazeType.CIRCLE -> generateCircleMaze(s.rings, s.wallHeight, s.pathWidth, rng)
            MazeType.FLOORS -> generateMultiFloorMaze(s.widthCells, s.lengthCells, s.wallHeight, s.pathWidth, s.floors, rng)
        }

        val yMin = result.layers.keys.minOrNull() ?: 0
        val yMax = result.layers.keys.maxOrNull() ?: 0
        val total = result.layers.values.sumOf { it.size }

        _state.value = s.copy(
            layers       = result.layers,
            totalBlocks  = total,
            layerMin     = yMin,
            layerMax     = yMax,
            currentLayer = s.currentLayer.coerceIn(yMin, yMax),
            deadEnds     = result.deadEnds,
            longestPath  = result.longestPath,
        )
    }

    // ── Result container ─────────────────────────────────────────────────────

    private data class MazeResult(
        val layers: Map<Int, Set<Pair<Int, Int>>>,
        val deadEnds: Int,
        val longestPath: Int,
    )

    // ── Rectangular maze — Recursive Backtracker (DFS) ───────────────────────

    private fun generateRectMaze(
        w: Int, l: Int, wallH: Int, pathW: Int, rng: Random,
    ): MazeResult {
        // Generate maze cell grid using DFS
        val cells = carveMazeDFS(w, l, rng)

        // Convert cell grid to block grid
        val blockW = w * (pathW + 1) + 1
        val blockL = l * (pathW + 1) + 1
        val walls = Array(blockW) { BooleanArray(blockL) { true } }

        // Carve cell interiors and passages
        for (cx in 0 until w) {
            for (cz in 0 until l) {
                // Cell interior
                val bx0 = cx * (pathW + 1) + 1
                val bz0 = cz * (pathW + 1) + 1
                for (dx in 0 until pathW) for (dz in 0 until pathW) {
                    walls[bx0 + dx][bz0 + dz] = false
                }
                // Passage east
                if (cells[cx][cz].openEast && cx + 1 < w) {
                    val px = bx0 + pathW
                    for (dz in 0 until pathW) walls[px][bz0 + dz] = false
                }
                // Passage south
                if (cells[cx][cz].openSouth && cz + 1 < l) {
                    val pz = bz0 + pathW
                    for (dx in 0 until pathW) walls[bx0 + dx][pz] = false
                }
            }
        }

        // Build layers
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in 0 until wallH) {
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in 0 until blockW) for (z in 0 until blockL) {
                if (walls[x][z]) set.add(x to z)
            }
            if (set.isNotEmpty()) layers[y] = set
        }

        // Stats
        val stats = computeStats(cells, w, l)
        return MazeResult(layers, stats.first, stats.second)
    }

    // ── Circular maze — DFS on ring graph ────────────────────────────────────

    private fun generateCircleMaze(
        numRings: Int, wallH: Int, pathW: Int, rng: Random,
    ): MazeResult {
        // Build ring structure: ring i has cellCounts[i] cells
        val cellCounts = IntArray(numRings + 1)
        cellCounts[0] = 1 // center
        if (numRings >= 1) cellCounts[1] = 6
        for (r in 2..numRings) {
            // Double cells when arc length allows
            val prev = cellCounts[r - 1]
            cellCounts[r] = if (prev < r * 6) prev * 2 else prev
        }

        // Cell ID = (ring, index)
        data class RingCell(val ring: Int, val idx: Int)

        // Build adjacency
        val adj = mutableMapOf<RingCell, MutableList<RingCell>>()
        fun addEdge(a: RingCell, b: RingCell) {
            adj.getOrPut(a) { mutableListOf() }.add(b)
            adj.getOrPut(b) { mutableListOf() }.add(a)
        }

        // Same-ring neighbors (CW/CCW)
        for (r in 1..numRings) {
            val count = cellCounts[r]
            for (i in 0 until count) {
                addEdge(RingCell(r, i), RingCell(r, (i + 1) % count))
            }
        }

        // Inner-outer ring connections
        // Ring 0 connects to all cells in ring 1
        if (numRings >= 1) {
            for (i in 0 until cellCounts[1]) {
                addEdge(RingCell(0, 0), RingCell(1, i))
            }
        }
        for (r in 2..numRings) {
            val inner = cellCounts[r - 1]
            val outer = cellCounts[r]
            val ratio = outer / inner
            for (i in 0 until inner) {
                for (j in 0 until ratio) {
                    addEdge(RingCell(r - 1, i), RingCell(r, i * ratio + j))
                }
            }
        }

        // DFS carve
        val visited = mutableSetOf<RingCell>()
        val passages = mutableSetOf<Pair<RingCell, RingCell>>()
        val stack = ArrayDeque<RingCell>()
        val start = RingCell(0, 0)
        visited.add(start)
        stack.addLast(start)

        while (stack.isNotEmpty()) {
            val current = stack.last()
            val neighbors = adj[current]?.filter { it !in visited } ?: emptyList()
            if (neighbors.isEmpty()) {
                stack.removeLast()
            } else {
                val next = neighbors[rng.nextInt(neighbors.size)]
                visited.add(next)
                passages.add(current to next)
                passages.add(next to current)
                stack.addLast(next)
            }
        }

        // Rasterize to blocks
        val outerRadius = (numRings + 1) * (pathW + 1) + 1
        val size = outerRadius * 2 + 1
        val cx = outerRadius
        val cz = outerRadius
        val wallGrid = Array(size) { BooleanArray(size) { false } }

        // Fill all rings with walls first, then carve
        // Mark all blocks within the outer circle as wall
        for (x in 0 until size) for (z in 0 until size) {
            val dx = x - cx
            val dz = z - cz
            if (dx * dx + dz * dz <= outerRadius * outerRadius) {
                wallGrid[x][z] = true
            }
        }

        // Carve center (ring 0)
        val centerR = pathW
        for (x in -centerR..centerR) for (z in -centerR..centerR) {
            if (x * x + z * z <= centerR * centerR) {
                wallGrid[cx + x][cz + z] = false
            }
        }

        // Carve ring corridors
        for (r in 1..numRings) {
            val innerEdge = r * (pathW + 1)
            val outerEdge = innerEdge + pathW
            for (angle in 0 until 360) {
                val theta = Math.toRadians(angle.toDouble())
                for (dist in innerEdge until outerEdge) {
                    val x = cx + (dist * cos(theta)).roundToInt()
                    val z = cz + (dist * sin(theta)).roundToInt()
                    if (x in 0 until size && z in 0 until size) {
                        wallGrid[x][z] = false
                    }
                }
            }
        }

        // Re-add radial walls between cells in each ring
        for (r in 1..numRings) {
            val count = cellCounts[r]
            val innerEdge = r * (pathW + 1)
            val outerEdge = innerEdge + pathW
            for (i in 0 until count) {
                val theta = 2.0 * PI * i / count
                // Draw radial wall line
                for (dist in innerEdge - 1..outerEdge) {
                    val x = cx + (dist * cos(theta)).roundToInt()
                    val z = cz + (dist * sin(theta)).roundToInt()
                    if (x in 0 until size && z in 0 until size) {
                        wallGrid[x][z] = true
                    }
                }
            }
        }

        // Carve passages between connected cells
        for ((a, b) in passages) {
            if (a.ring == b.ring) {
                // CW/CCW passage — remove radial wall between them
                val r = a.ring
                val count = cellCounts[r]
                val wallIdx = max(a.idx, b.idx)
                // Handle wraparound
                val actualIdx = if (abs(a.idx - b.idx) > 1) 0 else wallIdx
                val theta = 2.0 * PI * actualIdx / count
                val innerEdge = r * (pathW + 1)
                val outerEdge = innerEdge + pathW
                for (dist in innerEdge until outerEdge) {
                    val x = cx + (dist * cos(theta)).roundToInt()
                    val z = cz + (dist * sin(theta)).roundToInt()
                    if (x in 0 until size && z in 0 until size) {
                        wallGrid[x][z] = false
                    }
                }
            } else {
                // Inner-outer passage — remove concentric wall
                val inner = minOf(a.ring, b.ring)
                val outer = maxOf(a.ring, b.ring)
                val outerCell = if (a.ring > b.ring) a else b
                val count = cellCounts[outer]
                val thetaStart = 2.0 * PI * outerCell.idx / count
                val thetaEnd = 2.0 * PI * (outerCell.idx + 1) / count
                val thetaMid = (thetaStart + thetaEnd) / 2.0
                val wallDist = outer * (pathW + 1) - 1
                // Carve a passage through the concentric wall
                for (angleOff in -pathW..pathW) {
                    val theta = thetaMid + angleOff * 0.02
                    val x = cx + (wallDist * cos(theta)).roundToInt()
                    val z = cz + (wallDist * sin(theta)).roundToInt()
                    if (x in 0 until size && z in 0 until size) {
                        wallGrid[x][z] = false
                    }
                }
            }
        }

        // Build layers from wall grid
        val layers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        for (y in 0 until wallH) {
            val set = mutableSetOf<Pair<Int, Int>>()
            for (x in 0 until size) for (z in 0 until size) {
                if (wallGrid[x][z]) set.add(x to z)
            }
            if (set.isNotEmpty()) layers[y] = set
        }

        // Stats: count dead ends and longest path on the ring graph
        val deadEnds = visited.count { cell ->
            val connected = passages.count { it.first == cell }
            connected == 1 && cell != start
        }
        val longest = bfsDiameter(visited.toList(), passages)

        return MazeResult(layers, deadEnds, longest)
    }

    // ── Multi-floor maze — Stacked rectangular mazes ─────────────────────────

    private fun generateMultiFloorMaze(
        w: Int, l: Int, wallH: Int, pathW: Int, numFloors: Int, rng: Random,
    ): MazeResult {
        val floorHeight = wallH + 1 // wall + 1 floor slab
        val allLayers = mutableMapOf<Int, MutableSet<Pair<Int, Int>>>()
        val blockW = w * (pathW + 1) + 1
        val blockL = l * (pathW + 1) + 1

        // Generate each floor's maze independently
        val floorCells = Array(numFloors) { carveMazeDFS(w, l, rng) }

        // Pick stairwell positions (connect floor f to floor f+1)
        data class Stairwell(val cx: Int, val cz: Int) // cell position

        val stairwells = mutableListOf<Pair<Int, Stairwell>>() // floor index, position
        for (f in 0 until numFloors - 1) {
            // Pick a random cell that isn't on the border
            val sx = 1 + rng.nextInt((w - 2).coerceAtLeast(1))
            val sz = 1 + rng.nextInt((l - 2).coerceAtLeast(1))
            stairwells.add(f to Stairwell(sx, sz))
        }

        // Build each floor
        for (f in 0 until numFloors) {
            val cells = floorCells[f]
            val yBase = f * floorHeight
            val walls = Array(blockW) { BooleanArray(blockL) { true } }

            // Carve cell interiors and passages
            for (cx in 0 until w) {
                for (cz in 0 until l) {
                    val bx0 = cx * (pathW + 1) + 1
                    val bz0 = cz * (pathW + 1) + 1
                    for (dx in 0 until pathW) for (dz in 0 until pathW) {
                        walls[bx0 + dx][bz0 + dz] = false
                    }
                    if (cells[cx][cz].openEast && cx + 1 < w) {
                        val px = bx0 + pathW
                        for (dz in 0 until pathW) walls[px][bz0 + dz] = false
                    }
                    if (cells[cx][cz].openSouth && cz + 1 < l) {
                        val pz = bz0 + pathW
                        for (dx in 0 until pathW) walls[bx0 + dx][pz] = false
                    }
                }
            }

            // Wall layers
            for (y in 0 until wallH) {
                val set = allLayers.getOrPut(yBase + y) { mutableSetOf() }
                for (x in 0 until blockW) for (z in 0 until blockL) {
                    if (walls[x][z]) set.add(x to z)
                }
            }

            // Floor slab (ceiling of current floor) — only if not last floor
            if (f < numFloors - 1) {
                val slabY = yBase + wallH
                val slabSet = allLayers.getOrPut(slabY) { mutableSetOf() }
                for (x in 0 until blockW) for (z in 0 until blockL) {
                    slabSet.add(x to z)
                }
            }
        }

        // Create stairwells — clear ceiling and add stairs
        for ((f, sw) in stairwells) {
            val yBase = f * floorHeight
            val slabY = yBase + wallH
            val bx0 = sw.cx * (pathW + 1) + 1
            val bz0 = sw.cz * (pathW + 1) + 1

            // Clear ceiling slab above stairwell
            val slabSet = allLayers[slabY]
            if (slabSet != null) {
                for (dx in 0 until pathW) for (dz in 0 until pathW) {
                    slabSet.remove(bx0 + dx to bz0 + dz)
                }
            }

            // Add ascending stair blocks within the stairwell
            for (step in 0 until wallH) {
                val stairSet = allLayers.getOrPut(yBase + step) { mutableSetOf() }
                // Place one stair block at the appropriate Z position
                val dz = (step * pathW / wallH).coerceIn(0, pathW - 1)
                stairSet.add(bx0 to bz0 + dz)
            }

            // Clear walls around stairwell on the floor above to allow passage
            val upperBase = (f + 1) * floorHeight
            for (y in 0 until wallH) {
                val upperSet = allLayers[upperBase + y]
                if (upperSet != null) {
                    // Ensure the stairwell area is passable on the upper floor
                    for (dx in 0 until pathW) for (dz in 0 until pathW) {
                        upperSet.remove(bx0 + dx to bz0 + dz)
                    }
                }
            }
        }

        // Stats — combine all floor stats
        var totalDeadEnds = 0
        var maxLongest = 0
        for (f in 0 until numFloors) {
            val stats = computeStats(floorCells[f], w, l)
            totalDeadEnds += stats.first
            maxLongest = max(maxLongest, stats.second)
        }

        return MazeResult(allLayers, totalDeadEnds, maxLongest)
    }

    // ── Shared maze cell structure ───────────────────────────────────────────

    private data class Cell(
        var visited: Boolean = false,
        var openEast: Boolean = false,
        var openSouth: Boolean = false,
        var openWest: Boolean = false,
        var openNorth: Boolean = false,
    )

    private fun carveMazeDFS(w: Int, l: Int, rng: Random): Array<Array<Cell>> {
        val cells = Array(w) { Array(l) { Cell() } }
        val stack = ArrayDeque<Pair<Int, Int>>()
        cells[0][0].visited = true
        stack.addLast(0 to 0)

        val dirs = arrayOf(0 to 1, 1 to 0, 0 to -1, -1 to 0) // S, E, N, W

        while (stack.isNotEmpty()) {
            val (cx, cz) = stack.last()
            val unvisited = dirs.filter { (dx, dz) ->
                val nx = cx + dx; val nz = cz + dz
                nx in 0 until w && nz in 0 until l && !cells[nx][nz].visited
            }
            if (unvisited.isEmpty()) {
                stack.removeLast()
            } else {
                val (dx, dz) = unvisited[rng.nextInt(unvisited.size)]
                val nx = cx + dx; val nz = cz + dz
                // Carve passage
                when {
                    dx == 1  -> { cells[cx][cz].openEast = true; cells[nx][nz].openWest = true }
                    dx == -1 -> { cells[cx][cz].openWest = true; cells[nx][nz].openEast = true }
                    dz == 1  -> { cells[cx][cz].openSouth = true; cells[nx][nz].openNorth = true }
                    dz == -1 -> { cells[cx][cz].openNorth = true; cells[nx][nz].openSouth = true }
                }
                cells[nx][nz].visited = true
                stack.addLast(nx to nz)
            }
        }
        return cells
    }

    // ── Stats helpers ────────────────────────────────────────────────────────

    /** Returns (deadEnds, longestPath) for a rectangular cell grid. */
    private fun computeStats(cells: Array<Array<Cell>>, w: Int, l: Int): Pair<Int, Int> {
        // Count dead ends (cells with exactly 1 open wall)
        var deadEnds = 0
        for (cx in 0 until w) for (cz in 0 until l) {
            val c = cells[cx][cz]
            val openCount = listOf(c.openEast, c.openWest, c.openSouth, c.openNorth).count { it }
            if (openCount == 1) deadEnds++
        }

        // Longest path = diameter of the maze tree (double BFS)
        fun bfs(startX: Int, startZ: Int): Pair<Pair<Int, Int>, Int> {
            val dist = Array(w) { IntArray(l) { -1 } }
            dist[startX][startZ] = 0
            val queue = ArrayDeque<Pair<Int, Int>>()
            queue.addLast(startX to startZ)
            var farthest = startX to startZ
            var maxDist = 0
            while (queue.isNotEmpty()) {
                val (cx, cz) = queue.removeFirst()
                val d = dist[cx][cz]
                if (d > maxDist) { maxDist = d; farthest = cx to cz }
                val c = cells[cx][cz]
                if (c.openEast && cx + 1 < w && dist[cx + 1][cz] == -1) {
                    dist[cx + 1][cz] = d + 1; queue.addLast(cx + 1 to cz)
                }
                if (c.openWest && cx - 1 >= 0 && dist[cx - 1][cz] == -1) {
                    dist[cx - 1][cz] = d + 1; queue.addLast(cx - 1 to cz)
                }
                if (c.openSouth && cz + 1 < l && dist[cx][cz + 1] == -1) {
                    dist[cx][cz + 1] = d + 1; queue.addLast(cx to cz + 1)
                }
                if (c.openNorth && cz - 1 >= 0 && dist[cx][cz - 1] == -1) {
                    dist[cx][cz - 1] = d + 1; queue.addLast(cx to cz - 1)
                }
            }
            return farthest to maxDist
        }

        val (far1, _) = bfs(0, 0)
        val (_, diameter) = bfs(far1.first, far1.second)
        return deadEnds to diameter
    }

    /** BFS diameter for ring-cell graph. */
    private fun <T> bfsDiameter(nodes: List<T>, edges: Set<Pair<T, T>>): Int {
        if (nodes.isEmpty()) return 0
        fun bfs(start: T): Pair<T, Int> {
            val dist = mutableMapOf(start to 0)
            val queue = ArrayDeque<T>()
            queue.addLast(start)
            var farthest = start
            var maxDist = 0
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                val d = dist[cur]!!
                if (d > maxDist) { maxDist = d; farthest = cur }
                for ((a, b) in edges) {
                    if (a == cur && b !in dist) { dist[b] = d + 1; queue.addLast(b) }
                }
            }
            return farthest to maxDist
        }
        val (far1, _) = bfs(nodes.first())
        val (_, diameter) = bfs(far1)
        return diameter
    }

}
