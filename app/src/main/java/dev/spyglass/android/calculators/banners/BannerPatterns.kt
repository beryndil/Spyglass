package dev.spyglass.android.calculators.banners

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Dye colors ──────────────────────────────────────────────────────────────

enum class DyeColor(val displayName: String, val argb: Long) {
    WHITE("White", 0xFFF9FFFE),
    ORANGE("Orange", 0xFFF9801D),
    MAGENTA("Magenta", 0xFFC74EBD),
    LIGHT_BLUE("Light Blue", 0xFF3AB3DA),
    YELLOW("Yellow", 0xFFFED83D),
    LIME("Lime", 0xFF80C71F),
    PINK("Pink", 0xFFF38BAA),
    GRAY("Gray", 0xFF474F52),
    LIGHT_GRAY("Light Gray", 0xFF9D9D97),
    CYAN("Cyan", 0xFF169C9C),
    PURPLE("Purple", 0xFF8932B8),
    BLUE("Blue", 0xFF3C44AA),
    BROWN("Brown", 0xFF835432),
    GREEN("Green", 0xFF5E7C16),
    RED("Red", 0xFFB02E26),
    BLACK("Black", 0xFF1D1D21);

    val color: Color get() = Color(argb)
}

// ── Banner patterns ─────────────────────────────────────────────────────────

enum class BannerPattern(
    val displayName: String,
    val category: String,
    val requiresItem: Boolean = false,
    val itemName: String = "",
) {
    // Basic
    BASE("Base", "basic"),
    STRIPE_TOP("Chief", "basic"),
    STRIPE_BOTTOM("Base / Fess Bottom", "basic"),
    STRIPE_CENTER("Pale", "basic"),
    STRIPE_MIDDLE("Fess", "basic"),

    // Diagonal
    STRIPE_DOWNRIGHT("Bend", "diagonal"),
    STRIPE_DOWNLEFT("Bend Sinister", "diagonal"),
    DIAGONAL_RIGHT("Per Bend", "diagonal"),
    DIAGONAL_LEFT("Per Bend Sinister", "diagonal"),
    DIAGONAL_RIGHT_MIRROR("Per Bend Inverted", "diagonal"),
    DIAGONAL_LEFT_MIRROR("Per Bend Sinister Inverted", "diagonal"),

    // Vertical
    STRIPE_RIGHT("Pale Dexter", "vertical"),
    STRIPE_LEFT("Pale Sinister", "vertical"),
    HALF_VERTICAL("Per Pale", "vertical"),
    HALF_VERTICAL_MIRROR("Per Pale Inverted", "vertical"),

    // Horizontal
    HALF_HORIZONTAL("Per Fess", "horizontal"),
    HALF_HORIZONTAL_MIRROR("Per Fess Inverted", "horizontal"),

    // Cross
    CROSS("Saltire", "cross"),
    STRAIGHT_CROSS("Cross", "cross"),
    TRIANGLE_BOTTOM("Chevron", "cross"),
    TRIANGLE_TOP("Inverted Chevron", "cross"),

    // Corner
    SQUARE_BOTTOM_LEFT("Base Dexter Canton", "corner"),
    SQUARE_BOTTOM_RIGHT("Base Sinister Canton", "corner"),
    SQUARE_TOP_LEFT("Chief Dexter Canton", "corner"),
    SQUARE_TOP_RIGHT("Chief Sinister Canton", "corner"),

    // Decorative
    CIRCLE_MIDDLE("Roundel", "decorative"),
    RHOMBUS_MIDDLE("Lozenge", "decorative"),
    BORDER("Bordure", "decorative"),
    TRIANGLES_BOTTOM("Base Indented", "decorative"),
    TRIANGLES_TOP("Chief Indented", "decorative"),
    GRADIENT("Gradient", "decorative"),
    GRADIENT_UP("Base Gradient", "decorative"),

    // Special (require banner pattern items)
    BRICKS("Field Masoned", "special", true, "Field Masoned Banner Pattern"),
    CURLY_BORDER("Bordure Indented", "special", true, "Bordure Indented Banner Pattern"),
    FLOWER("Flower Charge", "special", true, "Flower Charge Banner Pattern"),
    GLOBE("Globe", "special", true, "Globe Banner Pattern"),
    CREEPER("Creeper Charge", "special", true, "Creeper Charge Banner Pattern"),
    PIGLIN("Snout", "special", true, "Snout Banner Pattern"),
    SKULL("Skull Charge", "special", true, "Skull Charge Banner Pattern"),
    MOJANG("Thing", "special", true, "Thing Banner Pattern"),
    FLOW("Flow", "special", true, "Flow Banner Pattern"),
    GUSTER("Guster", "special", true, "Guster Banner Pattern"),
}

val PATTERN_CATEGORIES = listOf("basic", "diagonal", "vertical", "horizontal", "cross", "corner", "decorative", "special")

// ── Banner layer ────────────────────────────────────────────────────────────

data class BannerLayer(val pattern: BannerPattern, val color: DyeColor)

// ── Banner shape (clip path) ────────────────────────────────────────────────

fun bannerShapePath(w: Float, h: Float): Path = Path().apply {
    val notchDepth = h * 0.15f
    val notchHalfWidth = w * 0.35f
    val cx = w / 2f
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h - notchDepth)
    lineTo(cx + notchHalfWidth, h - notchDepth)
    lineTo(cx, h)
    lineTo(cx - notchHalfWidth, h - notchDepth)
    lineTo(0f, h - notchDepth)
    close()
}

// ── Pattern drawing ─────────────────────────────────────────────────────────
// All coordinates are relative to (0,0) in a w×h area.
// BannerPreview uses translate() to position patterns on the canvas.

fun DrawScope.drawPattern(pattern: BannerPattern, color: Color, w: Float, h: Float) {
    when (pattern) {
        BannerPattern.BASE -> drawRect(color, Offset.Zero, Size(w, h))

        BannerPattern.STRIPE_TOP -> drawRect(color, Offset.Zero, Size(w, h / 3f))
        BannerPattern.STRIPE_BOTTOM -> drawRect(color, Offset(0f, h * 2f / 3f), Size(w, h / 3f))
        BannerPattern.STRIPE_CENTER -> drawRect(color, Offset(w / 3f, 0f), Size(w / 3f, h))
        BannerPattern.STRIPE_MIDDLE -> drawRect(color, Offset(0f, h / 3f), Size(w, h / 3f))

        BannerPattern.STRIPE_DOWNRIGHT -> {
            val sw = w * 0.2f
            val p = Path().apply {
                moveTo(0f, 0f); lineTo(sw, 0f); lineTo(w, h - sw); lineTo(w, h)
                lineTo(w - sw, h); lineTo(0f, sw); close()
            }
            drawPath(p, color)
        }
        BannerPattern.STRIPE_DOWNLEFT -> {
            val sw = w * 0.2f
            val p = Path().apply {
                moveTo(w, 0f); lineTo(w - sw, 0f); lineTo(0f, h - sw); lineTo(0f, h)
                lineTo(sw, h); lineTo(w, sw); close()
            }
            drawPath(p, color)
        }
        BannerPattern.DIAGONAL_RIGHT -> {
            val p = Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h); close() }
            drawPath(p, color)
        }
        BannerPattern.DIAGONAL_LEFT -> {
            val p = Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, h); close() }
            drawPath(p, color)
        }
        BannerPattern.DIAGONAL_RIGHT_MIRROR -> {
            val p = Path().apply { moveTo(0f, h); lineTo(w, 0f); lineTo(w, h); close() }
            drawPath(p, color)
        }
        BannerPattern.DIAGONAL_LEFT_MIRROR -> {
            val p = Path().apply { moveTo(0f, 0f); lineTo(0f, h); lineTo(w, h); close() }
            drawPath(p, color)
        }

        BannerPattern.STRIPE_RIGHT -> drawRect(color, Offset(w * 2f / 3f, 0f), Size(w / 3f, h))
        BannerPattern.STRIPE_LEFT -> drawRect(color, Offset.Zero, Size(w / 3f, h))
        BannerPattern.HALF_VERTICAL -> drawRect(color, Offset.Zero, Size(w / 2f, h))
        BannerPattern.HALF_VERTICAL_MIRROR -> drawRect(color, Offset(w / 2f, 0f), Size(w / 2f, h))

        BannerPattern.HALF_HORIZONTAL -> drawRect(color, Offset.Zero, Size(w, h / 2f))
        BannerPattern.HALF_HORIZONTAL_MIRROR -> drawRect(color, Offset(0f, h / 2f), Size(w, h / 2f))

        BannerPattern.CROSS -> {
            val p = Path().apply {
                moveTo(0f, 0f); lineTo(w * 0.15f, 0f); lineTo(w, h * 0.85f); lineTo(w, h)
                lineTo(w * 0.85f, h); lineTo(0f, h * 0.15f); close()
            }
            drawPath(p, color)
            val p2 = Path().apply {
                moveTo(w, 0f); lineTo(w * 0.85f, 0f); lineTo(0f, h * 0.85f); lineTo(0f, h)
                lineTo(w * 0.15f, h); lineTo(w, h * 0.15f); close()
            }
            drawPath(p2, color)
        }
        BannerPattern.STRAIGHT_CROSS -> {
            drawRect(color, Offset(w * 0.4f, 0f), Size(w * 0.2f, h))
            drawRect(color, Offset(0f, h * 0.4f), Size(w, h * 0.2f))
        }
        BannerPattern.TRIANGLE_BOTTOM -> {
            val p = Path().apply {
                moveTo(0f, h); lineTo(w / 2f, h * 0.5f); lineTo(w, h); close()
            }
            drawPath(p, color)
        }
        BannerPattern.TRIANGLE_TOP -> {
            val p = Path().apply {
                moveTo(0f, 0f); lineTo(w / 2f, h * 0.5f); lineTo(w, 0f); close()
            }
            drawPath(p, color)
        }

        BannerPattern.SQUARE_BOTTOM_LEFT -> drawRect(color, Offset(0f, h * 2f / 3f), Size(w / 3f, h / 3f))
        BannerPattern.SQUARE_BOTTOM_RIGHT -> drawRect(color, Offset(w * 2f / 3f, h * 2f / 3f), Size(w / 3f, h / 3f))
        BannerPattern.SQUARE_TOP_LEFT -> drawRect(color, Offset.Zero, Size(w / 3f, h / 3f))
        BannerPattern.SQUARE_TOP_RIGHT -> drawRect(color, Offset(w * 2f / 3f, 0f), Size(w / 3f, h / 3f))

        BannerPattern.CIRCLE_MIDDLE -> {
            val r = w.coerceAtMost(h) * 0.25f
            drawCircle(color, r, Offset(w / 2f, h / 2f))
        }
        BannerPattern.RHOMBUS_MIDDLE -> {
            val p = Path().apply {
                moveTo(w / 2f, h * 0.2f)
                lineTo(w * 0.8f, h / 2f)
                lineTo(w / 2f, h * 0.8f)
                lineTo(w * 0.2f, h / 2f)
                close()
            }
            drawPath(p, color)
        }
        BannerPattern.BORDER -> {
            val t = w * 0.12f
            drawRect(color, Offset.Zero, Size(w, t))
            drawRect(color, Offset(0f, h - t), Size(w, t))
            drawRect(color, Offset.Zero, Size(t, h))
            drawRect(color, Offset(w - t, 0f), Size(t, h))
        }
        BannerPattern.TRIANGLES_BOTTOM -> {
            val teeth = 4; val tw = w / teeth; val th = h * 0.2f
            for (i in 0 until teeth) {
                val p = Path().apply {
                    moveTo(i * tw, h); lineTo(i * tw + tw / 2f, h - th); lineTo((i + 1) * tw, h); close()
                }
                drawPath(p, color)
            }
        }
        BannerPattern.TRIANGLES_TOP -> {
            val teeth = 4; val tw = w / teeth; val th = h * 0.2f
            for (i in 0 until teeth) {
                val p = Path().apply {
                    moveTo(i * tw, 0f); lineTo(i * tw + tw / 2f, th); lineTo((i + 1) * tw, 0f); close()
                }
                drawPath(p, color)
            }
        }
        BannerPattern.GRADIENT -> {
            drawRect(Brush.verticalGradient(listOf(color, Color.Transparent)), Offset.Zero, Size(w, h))
        }
        BannerPattern.GRADIENT_UP -> {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, color)), Offset.Zero, Size(w, h))
        }

        // Special patterns — simplified iconic silhouettes
        BannerPattern.BRICKS -> {
            val rows = 6; val rh = h / rows; val brickW = w / 4f; val strokeW = w * 0.03f
            for (r in 0 until rows) {
                val y = r * rh
                drawLine(color, Offset(0f, y), Offset(w, y), strokeWidth = strokeW)
                val off = if (r % 2 == 0) 0f else brickW / 2f
                var x = off
                while (x < w) { drawLine(color, Offset(x, y), Offset(x, y + rh), strokeWidth = strokeW); x += brickW }
            }
        }
        BannerPattern.CURLY_BORDER -> {
            val t = w * 0.15f; val scallops = 5
            drawRect(color, Offset.Zero, Size(w, t))
            drawRect(color, Offset(0f, h - t), Size(w, t))
            drawRect(color, Offset.Zero, Size(t, h))
            drawRect(color, Offset(w - t, 0f), Size(t, h))
            val spacing = h / scallops
            for (i in 0 until scallops) {
                val cy = spacing * (i + 0.5f)
                drawCircle(color, t * 0.6f, Offset(t, cy))
                drawCircle(color, t * 0.6f, Offset(w - t, cy))
            }
            val hSpacing = w / scallops
            for (i in 0 until scallops) {
                val cx = hSpacing * (i + 0.5f)
                drawCircle(color, t * 0.6f, Offset(cx, t))
                drawCircle(color, t * 0.6f, Offset(cx, h - t))
            }
        }
        BannerPattern.FLOWER -> {
            val cx = w / 2f; val cy = h / 2f; val petalR = w * 0.1f; val dist = w * 0.13f
            drawCircle(color, petalR, Offset(cx, cy))
            for (angle in listOf(0f, 60f, 120f, 180f, 240f, 300f)) {
                val rad = Math.toRadians(angle.toDouble())
                drawCircle(color, petalR, Offset(cx + (dist * Math.cos(rad)).toFloat(), cy + (dist * Math.sin(rad)).toFloat()))
            }
        }
        BannerPattern.GLOBE -> {
            val cx = w / 2f; val cy = h / 2f; val r = w * 0.3f; val sw = w * 0.02f
            drawCircle(color, r, Offset(cx, cy))
            drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = sw)
            drawLine(color, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = sw)
            drawArc(color, topLeft = Offset(cx - r * 0.5f, cy - r), size = Size(r, r * 2f), startAngle = 270f, sweepAngle = 180f, useCenter = false, style = Stroke(sw))
            drawArc(color, topLeft = Offset(cx - r * 0.5f, cy - r), size = Size(r, r * 2f), startAngle = 90f, sweepAngle = 180f, useCenter = false, style = Stroke(sw))
        }
        BannerPattern.CREEPER -> {
            val cx = w / 2f; val cy = h * 0.4f; val u = w * 0.08f
            drawRect(color, Offset(cx - 3 * u, cy - 2 * u), Size(2 * u, 2 * u))
            drawRect(color, Offset(cx + u, cy - 2 * u), Size(2 * u, 2 * u))
            drawRect(color, Offset(cx - u, cy), Size(2 * u, 2 * u))
            drawRect(color, Offset(cx - 2 * u, cy + 2 * u), Size(4 * u, u))
            drawRect(color, Offset(cx - 2 * u, cy), Size(u, 2 * u))
            drawRect(color, Offset(cx + u, cy), Size(u, 2 * u))
        }
        BannerPattern.PIGLIN -> {
            val cx = w / 2f; val cy = h * 0.4f; val u = w * 0.06f
            drawRect(color, Offset(cx - 3 * u, cy - u), Size(6 * u, 3 * u))
            drawRect(Color.Black.copy(alpha = 0.3f), Offset(cx - 2 * u, cy), Size(u, u))
            drawRect(Color.Black.copy(alpha = 0.3f), Offset(cx + u, cy), Size(u, u))
            drawRect(color, Offset(cx - 4 * u, cy - 4 * u), Size(2.5f * u, 2 * u))
            drawRect(color, Offset(cx + 1.5f * u, cy - 4 * u), Size(2.5f * u, 2 * u))
        }
        BannerPattern.SKULL -> {
            val cx = w / 2f; val cy = h * 0.35f; val u = w * 0.07f
            drawCircle(color, 4 * u, Offset(cx, cy))
            drawRect(color, Offset(cx - 3 * u, cy + 2 * u), Size(6 * u, 2 * u))
            drawRect(Color.Black.copy(alpha = 0.4f), Offset(cx - 2.5f * u, cy - u), Size(2 * u, 1.5f * u))
            drawRect(Color.Black.copy(alpha = 0.4f), Offset(cx + 0.5f * u, cy - u), Size(2 * u, 1.5f * u))
        }
        BannerPattern.MOJANG -> {
            val cx = w / 2f
            val p = Path().apply {
                moveTo(cx - w * 0.3f, h * 0.6f); lineTo(cx - w * 0.3f, h * 0.25f)
                lineTo(cx - w * 0.1f, h * 0.4f); lineTo(cx, h * 0.25f)
                lineTo(cx + w * 0.1f, h * 0.4f); lineTo(cx + w * 0.3f, h * 0.25f)
                lineTo(cx + w * 0.3f, h * 0.6f); close()
            }
            drawPath(p, color)
        }
        BannerPattern.FLOW -> {
            val sw = w * 0.04f
            for (i in 1..4) {
                val y = h * i / 5f
                val p = Path().apply {
                    moveTo(0f, y)
                    cubicTo(w * 0.25f, y - h * 0.06f, w * 0.5f, y + h * 0.06f, w * 0.75f, y - h * 0.03f)
                    cubicTo(w * 0.85f, y - h * 0.04f, w * 0.95f, y + h * 0.04f, w, y)
                }
                drawPath(p, color, style = Stroke(sw))
            }
        }
        BannerPattern.GUSTER -> {
            val cx = w / 2f; val cy = h * 0.45f; val sw = w * 0.04f
            drawArc(color, topLeft = Offset(cx - w * 0.15f, cy - w * 0.15f), size = Size(w * 0.3f, w * 0.3f), startAngle = 0f, sweepAngle = 270f, useCenter = false, style = Stroke(sw))
            drawArc(color, topLeft = Offset(cx - w * 0.25f, cy - w * 0.25f), size = Size(w * 0.5f, w * 0.5f), startAngle = 270f, sweepAngle = 270f, useCenter = false, style = Stroke(sw))
            drawLine(color, Offset(cx - w * 0.35f, cy + h * 0.15f), Offset(cx + w * 0.35f, cy + h * 0.15f), strokeWidth = sw)
            drawLine(color, Offset(cx - w * 0.25f, cy + h * 0.25f), Offset(cx + w * 0.25f, cy + h * 0.25f), strokeWidth = sw)
        }
    }
}

// ── Banner preview composable ───────────────────────────────────────────────

@Composable
fun BannerPreview(
    baseColor: DyeColor,
    layers: List<BannerLayer>,
    modifier: Modifier = Modifier,
    widthDp: Dp = 100.dp,
    heightDp: Dp = 180.dp,
    showPole: Boolean = true,
) {
    Box(contentAlignment = Alignment.TopCenter, modifier = modifier) {
        Canvas(modifier = Modifier.width(widthDp).height(heightDp)) {
            val w = size.width
            val h = size.height

            val poleWidth = w * 0.04f
            val crossbarWidth = w * 1.1f
            val crossbarHeight = h * 0.02f
            val topOffset = if (showPole) crossbarHeight + 2f else 0f
            val bannerW = w * 0.9f
            val bannerH = h - topOffset - (if (showPole) h * 0.02f else 0f)
            val bannerX = (w - bannerW) / 2f

            if (showPole) {
                drawRect(Color(0xFF6B4226), Offset(w / 2f - poleWidth / 2f, 0f), Size(poleWidth, h))
                drawRect(Color(0xFF6B4226), Offset((w - crossbarWidth) / 2f, 0f), Size(crossbarWidth, crossbarHeight))
            }

            val shape = bannerShapePath(bannerW, bannerH)
            shape.translate(Offset(bannerX, topOffset))

            clipPath(shape) {
                translate(left = bannerX, top = topOffset) {
                    drawPattern(BannerPattern.BASE, baseColor.color, bannerW, bannerH)
                    for (layer in layers) {
                        drawPattern(layer.pattern, layer.color.color, bannerW, bannerH)
                    }
                }
            }

            drawPath(shape, Color.Black.copy(alpha = 0.5f), style = Stroke(2f))
        }
    }
}
