package dev.spyglass.android.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Spyglass colour tokens ────────────────────────────────────────────────────

val Gold          = Color(0xFFC9A84C)
val GoldDim       = Color(0xFF9E7A2A)
val Background    = Color(0xFF0E0C0A)
val SurfaceDark   = Color(0xFF1C1A17)
val SurfaceMid    = Color(0xFF2A2720)
val Stone700      = Color(0xFF3A3730)
val Stone500      = Color(0xFF6B6860)
val Stone300      = Color(0xFF9E9B94)
val Stone100      = Color(0xFFD4D1CA)
val Red400        = Color(0xFFEF5350)
val Green400      = Color(0xFF66BB6A)

// ── Semantic accent colours ─────────────────────────────────────────────────
val NetherRed     = Color(0xFFD32F2F)
val EnderPurple   = Color(0xFFAB47BC)
val PotionBlue    = Color(0xFF42A5F5)
val Emerald       = Color(0xFF66BB6A)
val SurfaceCard   = Color(0xFF211F1B)

private val DarkColorScheme = darkColorScheme(
    primary            = Gold,
    onPrimary          = Background,
    primaryContainer   = GoldDim,
    onPrimaryContainer = Stone100,
    secondary          = Stone500,
    onSecondary        = Stone100,
    background         = Background,
    onBackground       = Stone100,
    surface            = SurfaceDark,
    onSurface          = Stone100,
    surfaceVariant     = SurfaceMid,
    onSurfaceVariant   = Stone300,
    surfaceContainer   = SurfaceDark,
    outline            = Stone700,
    error              = Red400,
)

@Composable
fun SpyglassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = SpyglassTypography,
        content     = content,
    )
}

private val SpyglassTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.5.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = 0.5.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 16.sp, letterSpacing = 0.3.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp, letterSpacing = 0.2.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 15.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 13.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp, letterSpacing = 0.8.sp),
)
