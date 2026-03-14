package dev.spyglass.android.connect

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.connect.client.ConnectionState
import kotlin.math.*

// ── Utilities ─────────────────────────────────────────────────────────────────

/** Smooth ease-in-out interpolation (Hermite) */
private fun smoothStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private enum class Phase { LOADING, COMPLETE, DONE }

private data class BurstParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
)

// ── ChestLoadingAnimation (connection progress) ───────────────────────────────

@Composable
fun ChestLoadingAnimation(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    // Progress mapping from connection state
    val rawProgress = when (connectionState) {
        is ConnectionState.Connected -> 1f
        is ConnectionState.Pairing -> 0.6f
        is ConnectionState.Connecting -> 0.3f
        is ConnectionState.Reconnecting -> 0.2f + (0.1f * (connectionState.attempt.coerceAtMost(5)))
        else -> 0f
    }
    val progress by animateFloatAsState(rawProgress, tween(800))

    // Phase state machine
    var phase by remember { mutableStateOf(Phase.LOADING) }
    LaunchedEffect(rawProgress) {
        if (rawProgress >= 1f && phase == Phase.LOADING) phase = Phase.COMPLETE
    }

    // Animation time
    var time by remember { mutableFloatStateOf(0f) }
    var burstTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { nanos ->
                val dt = (nanos - lastNanos) / 1_000_000_000f
                lastNanos = nanos
                time += dt
                if (phase == Phase.COMPLETE) {
                    burstTime += dt
                    if (burstTime > 1.5f) phase = Phase.DONE
                }
            }
        }
    }

    // Burst particles — created once on COMPLETE
    val burstParticles = remember { mutableStateListOf<BurstParticle>() }
    LaunchedEffect(phase) {
        if (phase == Phase.COMPLETE && burstParticles.isEmpty()) {
            val colors = listOf(
                Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFFFEB3B),
                Color(0xFF00BCD4), Color(0xFFFFD700),
            )
            repeat(24) { i ->
                burstParticles.add(
                    BurstParticle(
                        angle = (i * 15f) + (-5..5).random(),
                        speed = 80f + (0..60).random(),
                        size = 3f + (0..4).random().toFloat(),
                        color = colors[i % colors.size],
                    )
                )
            }
        }
    }

    // Status text
    val statusMessage = when (connectionState) {
        is ConnectionState.Error -> stringResource(R.string.connect_anim_error)
        is ConnectionState.Connected -> if (phase == Phase.DONE) stringResource(R.string.connect_anim_ready) else stringResource(R.string.connect_anim_connected)
        is ConnectionState.Pairing -> stringResource(R.string.connect_anim_pairing)
        is ConnectionState.Connecting -> stringResource(R.string.connect_anim_connecting)
        is ConnectionState.Reconnecting -> stringResource(R.string.connect_anim_reconnecting, connectionState.attempt)
        else -> stringResource(R.string.connect_anim_getting_ready)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(
                modifier = Modifier.fillMaxWidth().height(180.dp),
            ) {
                val w = size.width
                val h = size.height

                val chestW = 80f
                val chestH = 56f
                val lidH = 20f
                val cx = w / 2
                val cy = h * 0.42f

                // Idle glow behind chest
                val glowAlpha = 0.12f + 0.06f * sin(time * 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4CAF50).copy(alpha = glowAlpha),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = 100f,
                    ),
                    center = Offset(cx, cy),
                    radius = 100f,
                )

                // Orbiting diamonds (6) — split into behind/front for 3D effect
                val orbitRadius = 70f
                val diamondSize = 8f
                val numDiamonds = 6

                val drawDiamonds = { front: Boolean ->
                    for (i in 0 until numDiamonds) {
                        val baseAngle = (i * 360f / numDiamonds)
                        val angle = Math.toRadians((baseAngle + time * 60f).toDouble())
                        val isFront = sin(angle).toFloat() >= 0f
                        if (isFront != front) continue

                        val dx = cx + orbitRadius * cos(angle).toFloat()
                        val dy = cy + orbitRadius * 0.5f * sin(angle).toFloat()

                        // Depth alpha: behind diamonds are dimmer
                        val depthAlpha = 0.4f + 0.6f * ((sin(angle).toFloat() + 1f) / 2f)

                        // Trail (3 fading copies)
                        for (t in 3 downTo 1) {
                            val trailAngle = Math.toRadians((baseAngle + (time * 60f) - t * 8f).toDouble())
                            val tdx = cx + orbitRadius * cos(trailAngle).toFloat()
                            val tdy = cy + orbitRadius * 0.5f * sin(trailAngle).toFloat()
                            val trailAlpha = (0.15f - t * 0.04f) * depthAlpha
                            val trailPath = Path().apply {
                                moveTo(tdx, tdy - diamondSize * 0.5f)
                                lineTo(tdx + diamondSize * 0.35f, tdy)
                                lineTo(tdx, tdy + diamondSize * 0.5f)
                                lineTo(tdx - diamondSize * 0.35f, tdy)
                                close()
                            }
                            drawPath(trailPath, Color(0xFF00BCD4).copy(alpha = trailAlpha))
                        }

                        // Diamond
                        val diamondPath = Path().apply {
                            moveTo(dx, dy - diamondSize)
                            lineTo(dx + diamondSize * 0.6f, dy)
                            lineTo(dx, dy + diamondSize)
                            lineTo(dx - diamondSize * 0.6f, dy)
                            close()
                        }
                        drawPath(diamondPath, Color(0xFF00BCD4).copy(alpha = depthAlpha))
                        drawPath(
                            diamondPath,
                            Color.White.copy(alpha = 0.3f * depthAlpha),
                            style = Stroke(1f),
                        )
                    }
                }

                // Behind diamonds (upper arc, sin < 0)
                drawDiamonds(false)

                // Chest body
                val chestLeft = cx - chestW / 2
                val chestTop = cy - chestH / 2

                // Body shadow
                drawRoundRect(
                    color = Color(0xFF3E2723),
                    topLeft = Offset(chestLeft - 2, chestTop + 2),
                    size = Size(chestW + 4, chestH + 2),
                    cornerRadius = CornerRadius(4f),
                )

                // Body fill — brown gradient
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8D6E4C), Color(0xFF6D4C3A)),
                        startY = chestTop,
                        endY = chestTop + chestH,
                    ),
                    topLeft = Offset(chestLeft, chestTop),
                    size = Size(chestW, chestH),
                    cornerRadius = CornerRadius(4f),
                )

                // Plank lines
                val plankColor = Color(0xFF5D3A2A).copy(alpha = 0.5f)
                for (py in 1..2) {
                    val lineY = chestTop + chestH * py / 3f
                    drawLine(plankColor, Offset(chestLeft + 4, lineY), Offset(chestLeft + chestW - 4, lineY), 1f)
                }

                // Edge highlight (top)
                drawLine(
                    Color.White.copy(alpha = 0.15f),
                    Offset(chestLeft + 2, chestTop + 1),
                    Offset(chestLeft + chestW - 2, chestTop + 1),
                    1.5f,
                )

                // Latch
                val latchW = 8f
                val latchH = 12f
                drawRoundRect(
                    color = Color(0xFFFFD700),
                    topLeft = Offset(cx - latchW / 2, chestTop - latchH / 2 + 2),
                    size = Size(latchW, latchH),
                    cornerRadius = CornerRadius(2f),
                )

                // Lid — rotates open when progress >= 0.7
                val lidOpenAngle = if (progress >= 0.7f) {
                    val lidProgress = ((progress - 0.7f) / 0.3f).coerceIn(0f, 1f)
                    -45f * lidProgress
                } else 0f

                withTransform({
                    translate(left = 0f, top = 0f)
                    val pivotX = cx
                    val pivotY = chestTop
                    rotate(lidOpenAngle, Offset(pivotX, pivotY))
                }) {
                    // Lid shadow
                    drawRoundRect(
                        color = Color(0xFF3E2723),
                        topLeft = Offset(chestLeft - 2, chestTop - lidH),
                        size = Size(chestW + 4, lidH + 2),
                        cornerRadius = CornerRadius(4f),
                    )

                    // Lid fill
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFA0845C), Color(0xFF8D6E4C)),
                            startY = chestTop - lidH,
                            endY = chestTop,
                        ),
                        topLeft = Offset(chestLeft, chestTop - lidH),
                        size = Size(chestW, lidH),
                        cornerRadius = CornerRadius(4f),
                    )

                    // Lid edge highlight
                    drawLine(
                        Color.White.copy(alpha = 0.2f),
                        Offset(chestLeft + 2, chestTop - lidH + 1),
                        Offset(chestLeft + chestW - 2, chestTop - lidH + 1),
                        1.5f,
                    )

                    // Lid plank line
                    drawLine(
                        plankColor,
                        Offset(chestLeft + 4, chestTop - lidH / 2),
                        Offset(chestLeft + chestW - 4, chestTop - lidH / 2),
                        1f,
                    )
                }

                // Front diamonds (lower arc, sin >= 0)
                drawDiamonds(true)

                // Light rays when lid opens
                if (lidOpenAngle < -5f) {
                    val rayAlpha = ((-lidOpenAngle - 5f) / 40f).coerceIn(0f, 0.4f)
                    val rayCount = 7
                    for (r in 0 until rayCount) {
                        val rayAngle = Math.toRadians((-140.0 + r * 10.0))
                        val rayLen = 50f + 20f * sin(time * 3f + r)
                        drawLine(
                            color = Color(0xFFFFD700).copy(alpha = rayAlpha * (0.5f + 0.5f * sin(time * 4f + r))),
                            start = Offset(cx, chestTop - lidH * 0.5f),
                            end = Offset(
                                cx + rayLen * cos(rayAngle).toFloat(),
                                chestTop - lidH * 0.5f + rayLen * sin(rayAngle).toFloat(),
                            ),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // Burst particles
                if (phase == Phase.COMPLETE || phase == Phase.DONE) {
                    burstParticles.forEach { p ->
                        val pProgress = (burstTime * p.speed / 80f).coerceIn(0f, 1f)
                        val pAlpha = (1f - pProgress).coerceIn(0f, 1f) * 0.8f
                        val dist = pProgress * p.speed
                        val pAngle = Math.toRadians(p.angle.toDouble())
                        val px = cx + dist * cos(pAngle).toFloat()
                        val py = cy + dist * sin(pAngle).toFloat()
                        drawCircle(
                            color = p.color.copy(alpha = pAlpha),
                            radius = p.size * (1f - pProgress * 0.5f),
                            center = Offset(px, py),
                        )
                    }
                }

                // XP bar below chest
                val barW = chestW * 1.5f
                val barH = 8f
                val barLeft = cx - barW / 2
                val barTop = cy + chestH / 2 + 16f
                val segmentCount = 20

                // Bar background
                drawRoundRect(
                    color = Color(0xFF1A1A1A),
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4f),
                )
                drawRoundRect(
                    color = Color(0xFF333333),
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4f),
                    style = Stroke(1f),
                )

                // Bar fill segments
                val segW = barW / segmentCount
                val filledSegments = (progress * segmentCount).toInt()
                for (s in 0 until filledSegments) {
                    val segLeft = barLeft + s * segW
                    val segColor = if (s < segmentCount / 2) {
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFF8BC34A)
                    }
                    drawRoundRect(
                        color = segColor,
                        topLeft = Offset(segLeft + 0.5f, barTop + 0.5f),
                        size = Size(segW - 1f, barH - 1f),
                        cornerRadius = CornerRadius(2f),
                    )
                }

                // Bar shine
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.1f),
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barW, barH / 2),
                    cornerRadius = CornerRadius(4f),
                )
            }

            Text(
                statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

// ── ChestLoadingOneShot (easter egg) ─────────────────────────────────────────

/**
 * One-shot chest loading animation — plays through the full connection
 * sequence once, then calls [onComplete] when done.
 * Secret easter egg triggered by long-pressing the globe on a selected world.
 */
@Composable
fun ChestLoadingOneShot(modifier: Modifier = Modifier, onComplete: () -> Unit) {
    var state by remember { mutableStateOf<ConnectionState>(ConnectionState.Connecting("192.168.1.100", 29170)) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        state = ConnectionState.Pairing
        kotlinx.coroutines.delay(1000)
        state = ConnectionState.Connected("Spyglass Connect")
        kotlinx.coroutines.delay(3000) // let burst finish
        onComplete()
    }

    ChestLoadingAnimation(
        connectionState = state,
        modifier = modifier,
    )
}

// ── ChestDiamondLoader (storage waiting state) ────────────────────────────────

/**
 * Simple chest loading animation — closed chest with diamonds orbiting around it.
 * Used on the Storage screen while waiting for chest data from the desktop.
 */
@Composable
fun ChestDiamondLoader(
    modifier: Modifier = Modifier,
    statusText: String? = null,
) {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { nanos ->
                val dt = (nanos - lastNanos) / 1_000_000_000f
                lastNanos = nanos
                time += dt
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(350.dp),
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2
            val cy = h * 0.45f

            val chestW = 180f
            val chestH = 124f
            val lidH = 44f
            val chestLeft = cx - chestW / 2
            val chestTop = cy - chestH / 2
            val plankColor = Color(0xFF5D3A2A).copy(alpha = 0.5f)

            // Glow behind chest — pulses gently
            val glowAlpha = 0.10f + 0.05f * sin(time * 1.8f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00BCD4).copy(alpha = glowAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = 200f,
                ),
                center = Offset(cx, cy),
                radius = 200f,
            )

            // ── Orbiting diamonds (8) — split into behind/front for 3D effect
            val orbitRadius = 150f
            val diamondSize = 14f
            val numDiamonds = 8

            val drawDiamonds = { front: Boolean ->
                for (i in 0 until numDiamonds) {
                    val baseAngle = (i * 360f / numDiamonds)
                    val angle = Math.toRadians((baseAngle + time * 50f).toDouble())
                    val isFront = sin(angle).toFloat() >= 0f
                    if (isFront != front) continue

                    // Vertical bob per diamond
                    val bob = 6f * sin(time * 2.5f + i * 0.8f)
                    val dx = cx + orbitRadius * cos(angle).toFloat()
                    val dy = cy + orbitRadius * 0.45f * sin(angle).toFloat() + bob

                    // Depth-based alpha: diamonds behind the chest are dimmer
                    val depthAlpha = 0.4f + 0.6f * ((sin(angle).toFloat() + 1f) / 2f)

                    // Trail (3 fading copies)
                    for (t in 3 downTo 1) {
                        val trailAngle = Math.toRadians((baseAngle + (time * 50f) - t * 7f).toDouble())
                        val tdx = cx + orbitRadius * cos(trailAngle).toFloat()
                        val tdy = cy + orbitRadius * 0.45f * sin(trailAngle).toFloat() + bob
                        val trailAlpha = (0.12f - t * 0.03f) * depthAlpha
                        val trailPath = Path().apply {
                            moveTo(tdx, tdy - diamondSize * 0.4f)
                            lineTo(tdx + diamondSize * 0.3f, tdy)
                            lineTo(tdx, tdy + diamondSize * 0.4f)
                            lineTo(tdx - diamondSize * 0.3f, tdy)
                            close()
                        }
                        drawPath(trailPath, Color(0xFF00BCD4).copy(alpha = trailAlpha))
                    }

                    // Diamond shape
                    val diamondPath = Path().apply {
                        moveTo(dx, dy - diamondSize)
                        lineTo(dx + diamondSize * 0.6f, dy)
                        lineTo(dx, dy + diamondSize)
                        lineTo(dx - diamondSize * 0.6f, dy)
                        close()
                    }
                    drawPath(diamondPath, Color(0xFF00BCD4).copy(alpha = depthAlpha))
                    drawPath(
                        diamondPath,
                        Color.White.copy(alpha = 0.25f * depthAlpha),
                        style = Stroke(1f),
                    )
                }
            }

            // Behind diamonds (upper arc, sin < 0)
            drawDiamonds(false)

            // ── Chest body ───────────────────────────────────────────────────
            // Shadow
            drawRoundRect(
                color = Color(0xFF3E2723),
                topLeft = Offset(chestLeft - 2, chestTop + 2),
                size = Size(chestW + 4, chestH + 2),
                cornerRadius = CornerRadius(4f),
            )
            // Body fill
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8D6E4C), Color(0xFF6D4C3A)),
                    startY = chestTop,
                    endY = chestTop + chestH,
                ),
                topLeft = Offset(chestLeft, chestTop),
                size = Size(chestW, chestH),
                cornerRadius = CornerRadius(4f),
            )
            // Plank lines
            for (py in 1..2) {
                val lineY = chestTop + chestH * py / 3f
                drawLine(plankColor, Offset(chestLeft + 4, lineY), Offset(chestLeft + chestW - 4, lineY), 1f)
            }
            // Edge highlight (top of body)
            drawLine(
                Color.White.copy(alpha = 0.15f),
                Offset(chestLeft + 2, chestTop + 1),
                Offset(chestLeft + chestW - 2, chestTop + 1),
                1.5f,
            )

            // ── Lid (lifts straight up and back down on a loop) ─────────────
            val lidLift = 32f  // max pixels the lid rises
            val cycleTime = time % 3.0f  // 3s cycle
            val lidOffset = when {
                cycleTime < 0.8f -> lidLift * smoothStep(cycleTime / 0.8f)               // lift over 0.8s
                cycleTime < 1.6f -> lidLift                                               // hold up 0.8s
                cycleTime < 2.4f -> lidLift * (1f - smoothStep((cycleTime - 1.6f) / 0.8f)) // lower over 0.8s
                else -> 0f                                                                 // hold closed 0.6s
            }

            val lidY = chestTop - lidH - lidOffset

            // Light rays when lid is lifted
            if (lidOffset > 2f) {
                val rayAlpha = (lidOffset / lidLift * 0.3f).coerceIn(0f, 0.3f)
                val gapCenter = chestTop - lidOffset / 2
                val rayCount = 5
                for (r in 0 until rayCount) {
                    val rayAngle = Math.toRadians((-150.0 + r * 15.0))
                    val rayLen = 60f + 24f * sin(time * 3f + r)
                    drawLine(
                        color = Color(0xFFFFD700).copy(alpha = rayAlpha * (0.5f + 0.5f * sin(time * 4f + r))),
                        start = Offset(cx, gapCenter),
                        end = Offset(
                            cx + rayLen * cos(rayAngle).toFloat(),
                            gapCenter + rayLen * sin(rayAngle).toFloat(),
                        ),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Lid shadow
            drawRoundRect(
                color = Color(0xFF3E2723),
                topLeft = Offset(chestLeft - 2, lidY),
                size = Size(chestW + 4, lidH + 2),
                cornerRadius = CornerRadius(4f),
            )
            // Lid fill
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFA0845C), Color(0xFF8D6E4C)),
                    startY = lidY,
                    endY = lidY + lidH,
                ),
                topLeft = Offset(chestLeft, lidY),
                size = Size(chestW, lidH),
                cornerRadius = CornerRadius(4f),
            )
            // Lid edge highlight
            drawLine(
                Color.White.copy(alpha = 0.2f),
                Offset(chestLeft + 2, lidY + 1),
                Offset(chestLeft + chestW - 2, lidY + 1),
                1.5f,
            )
            // Lid plank line
            drawLine(
                plankColor,
                Offset(chestLeft + 4, lidY + lidH / 2),
                Offset(chestLeft + chestW - 4, lidY + lidH / 2),
                1f,
            )

            // ── Latch (moves with lid) ───────────────────────────────────────
            val latchW = 16f
            val latchH = 24f
            drawRoundRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(cx - latchW / 2, lidY + lidH - latchH / 2),
                size = Size(latchW, latchH),
                cornerRadius = CornerRadius(2f),
            )

            // Front diamonds (lower arc, sin >= 0)
            drawDiamonds(true)

            // ── Sparkle dots around the chest ────────────────────────────────
            for (i in 0 until 5) {
                val sparkAngle = time * 1.2f + i * 1.3f
                val sparkAlpha = (0.4f + 0.3f * sin(sparkAngle * 3f)).coerceIn(0f, 0.7f)
                val sparkDist = 110f + 30f * sin(sparkAngle * 0.7f)
                val sparkA = Math.toRadians((i * 72f + time * 15f).toDouble())
                val sx = cx + sparkDist * cos(sparkA).toFloat()
                val sy = cy + sparkDist * 0.5f * sin(sparkA).toFloat()
                drawCircle(
                    color = Color(0xFF00BCD4).copy(alpha = sparkAlpha),
                    radius = 2f,
                    center = Offset(sx, sy),
                )
            }
        }

        if (statusText != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PreviewChestDiamondLoader() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ChestDiamondLoader(statusText = "Loading chests…")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PreviewConnecting() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ChestLoadingAnimation(connectionState = ConnectionState.Connecting("192.168.1.100", 29170))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PreviewPairing() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ChestLoadingAnimation(connectionState = ConnectionState.Pairing)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PreviewConnected() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ChestLoadingAnimation(connectionState = ConnectionState.Connected("Spyglass Connect"))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun PreviewOneShot() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ChestLoadingOneShot(onComplete = {})
    }
}
