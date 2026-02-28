package dev.spyglass.android.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

// ── Labeled slider ───────────────────────────────────────────────────────────

@Composable
fun LabeledSlider(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column {
        Text("$label: $value", style = MaterialTheme.typography.bodyMedium, color = Stone300)
        Slider(
            value         = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange    = min.toFloat()..max.toFloat(),
            steps         = (max - min - 1).coerceAtLeast(0),
            colors        = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold),
        )
    }
}

// ── 2D layer grid ────────────────────────────────────────────────────────────

@Composable
fun LayerGrid(blocks: Set<Pair<Int, Int>>) {
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
            "${blocks.size} blocks in this layer  \u2022  X ${minX}\u2013${maxX}  Z ${minZ}\u2013${maxZ}",
            style = MaterialTheme.typography.bodySmall,
            color = Stone500,
        )
    }
}

// ── Isometric 3D view ────────────────────────────────────────────────────────

const val ISO_COS = 0.866025f  // cos(30°)
const val ISO_SIN = 0.5f       // sin(30°)

data class IsoBlock(val x: Int, val y: Int, val z: Int, val top: Boolean, val left: Boolean, val right: Boolean)

@Composable
fun IsometricView(layers: Map<Int, Set<Pair<Int, Int>>>) {
    // Build a set of all (x, y, z) blocks for occlusion checks
    val allBlocks = remember(layers) {
        val set = mutableSetOf<Triple<Int, Int, Int>>()
        for ((y, xzSet) in layers) {
            for ((x, z) in xzSet) {
                set.add(Triple(x, y, z))
            }
        }
        set
    }

    // Determine visible faces and sort for painter's algorithm
    val visibleBlocks = remember(allBlocks) {
        allBlocks.map { (x, y, z) ->
            IsoBlock(
                x = x, y = y, z = z,
                top   = Triple(x, y + 1, z) !in allBlocks,
                left  = Triple(x - 1, y, z) !in allBlocks,
                right = Triple(x, y, z + 1) !in allBlocks,
            )
        }
            .filter { it.top || it.left || it.right }
            .sortedWith(compareBy({ it.y }, { it.x + it.z }, { it.x }))
    }

    if (visibleBlocks.isEmpty()) return

    // Compute projection bounds
    val bounds = remember(visibleBlocks) {
        var sxMin = Float.MAX_VALUE; var sxMax = Float.MIN_VALUE
        var syMin = Float.MAX_VALUE; var syMax = Float.MIN_VALUE
        for (b in visibleBlocks) {
            // Check all 4 corners of the isometric diamond for this block
            for (dx in 0..1) for (dz in 0..1) {
                val sx = ((b.x + dx) - (b.z + dz)) * ISO_COS
                val sy = -((b.y + 1).toFloat()) + ((b.x + dx) + (b.z + dz)) * ISO_SIN
                sxMin = min(sxMin, sx); sxMax = max(sxMax, sx)
                syMin = min(syMin, sy); syMax = max(syMax, sy)
            }
            // Also check bottom vertex
            val syBottom = -(b.y.toFloat()) + (b.x + b.z + 1) * ISO_SIN
            syMax = max(syMax, syBottom)
        }
        floatArrayOf(sxMin, sxMax, syMin, syMax)
    }

    val isoW = bounds[1] - bounds[0]
    val isoH = bounds[3] - bounds[2]
    val aspect = if (isoH > 0f) isoW / isoH else 1f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect.coerceIn(0.3f, 3f))
                .background(Color(0xFF0A0907), RoundedCornerShape(4.dp))
                .border(1.dp, Stone700, RoundedCornerShape(4.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val drawW = size.width
                val drawH = size.height
                val scale = min(drawW / isoW, drawH / isoH)
                val offX = (drawW - isoW * scale) / 2f - bounds[0] * scale
                val offY = (drawH - isoH * scale) / 2f - bounds[2] * scale

                fun project(bx: Float, by: Float, bz: Float): Offset {
                    val sx = (bx - bz) * ISO_COS * scale + offX
                    val sy = (-by + (bx + bz) * ISO_SIN) * scale + offY
                    return Offset(sx, sy)
                }

                val topColor = Color(0xFFD4A017)     // bright gold
                val leftColor = Color(0xFF8B6914)     // darker left face
                val rightColor = Color(0xFF6B4F10)    // darkest right face

                for (b in visibleBlocks) {
                    val fx = b.x.toFloat()
                    val fy = b.y.toFloat()
                    val fz = b.z.toFloat()

                    // Draw top face
                    if (b.top) {
                        val tl = project(fx, fy + 1f, fz)
                        val tr = project(fx + 1f, fy + 1f, fz)
                        val bl = project(fx, fy + 1f, fz + 1f)
                        val br = project(fx + 1f, fy + 1f, fz + 1f)
                        val topPath = Path().apply {
                            moveTo(tl.x, tl.y)
                            lineTo(tr.x, tr.y)
                            lineTo(br.x, br.y)
                            lineTo(bl.x, bl.y)
                            close()
                        }
                        drawPath(topPath, topColor)
                    }

                    // Draw left face (x-facing, visible from left)
                    if (b.left) {
                        val lt = project(fx, fy + 1f, fz)
                        val lb = project(fx, fy, fz)
                        val rb = project(fx, fy, fz + 1f)
                        val rt = project(fx, fy + 1f, fz + 1f)
                        val leftPath = Path().apply {
                            moveTo(lt.x, lt.y)
                            lineTo(rt.x, rt.y)
                            lineTo(rb.x, rb.y)
                            lineTo(lb.x, lb.y)
                            close()
                        }
                        drawPath(leftPath, leftColor)
                    }

                    // Draw right face (z-facing, visible from right)
                    if (b.right) {
                        val lt = project(fx, fy + 1f, fz + 1f)
                        val rt = project(fx + 1f, fy + 1f, fz + 1f)
                        val rb = project(fx + 1f, fy, fz + 1f)
                        val lb = project(fx, fy, fz + 1f)
                        val rightPath = Path().apply {
                            moveTo(lt.x, lt.y)
                            lineTo(rt.x, rt.y)
                            lineTo(rb.x, rb.y)
                            lineTo(lb.x, lb.y)
                            close()
                        }
                        drawPath(rightPath, rightColor)
                    }
                }
            }
        }
        Text(
            "${allBlocks.size} total blocks",
            style = MaterialTheme.typography.bodySmall,
            color = Stone500,
        )
    }
}
