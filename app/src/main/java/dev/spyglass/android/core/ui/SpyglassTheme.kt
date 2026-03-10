package dev.spyglass.android.core.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Spyglass colour tokens (Obsidian defaults, kept for backwards compat) ────

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

// ── Semantic accent colours (same across all themes) ─────────────────────────

val NetherRed     = Color(0xFFD32F2F)
val EnderPurple   = Color(0xFFAB47BC)
val PotionBlue    = Color(0xFF42A5F5)
val Emerald       = Color(0xFF66BB6A)

// ── CompositionLocal for SurfaceCard (no Material3 slot) ─────────────────────

val LocalSurfaceCard = compositionLocalOf { Color(0xFF211F1B) }
val LocalIsWideScreen = compositionLocalOf { false }
val LocalHapticEnabled = compositionLocalOf { true }
val LocalReduceAnimations = compositionLocalOf { false }
val LocalThemeKey = compositionLocalOf { DEFAULT_THEME }

/** Theme keys that use a full-screen background image instead of a solid color. */
val ImageThemeKeys = setOf(
    "creeper", "ocean_monument", "deep_dark", "nether_fortress", "desert_sunset",
    "cherry_grove", "lush_cave", "end_city", "sunflower_plains",
    "custom",
)

// ── Theme preset data ────────────────────────────────────────────────────────

data class SpyglassColors(val scheme: ColorScheme, val surfaceCard: Color)

data class ThemeInfo(val label: String, val background: Color, val isDark: Boolean)

private val OnPrimaryDark = Color(0xFF0E0C0A)
private val GoldOnLight   = Color(0xFF806000) // darker gold for readable contrast on light bgs
private val GoldDimLight  = Color(0xFF614800)

private fun buildTheme(
    isDark: Boolean,
    bg: Color,
    surface: Color,
    surfaceVariant: Color,
    surfaceCard: Color,
    outline: Color,
    secondary: Color,
    onSurfaceVariant: Color,
    onSurface: Color,
): SpyglassColors {
    val primary   = if (isDark) Gold else GoldOnLight
    val container = if (isDark) GoldDim else GoldDimLight
    val base = if (isDark) {
        darkColorScheme(
            primary            = primary,
            onPrimary          = OnPrimaryDark,
            primaryContainer   = container,
            onPrimaryContainer = onSurface,
            secondary          = secondary,
            onSecondary        = onSurface,
            background         = bg,
            onBackground       = onSurface,
            surface            = surface,
            onSurface          = onSurface,
            surfaceVariant     = surfaceVariant,
            onSurfaceVariant   = onSurfaceVariant,
            surfaceContainer   = surface,
            outline            = outline,
            error              = Red400,
        )
    } else {
        lightColorScheme(
            primary            = primary,
            onPrimary          = OnPrimaryDark,
            primaryContainer   = container,
            onPrimaryContainer = onSurface,
            secondary          = secondary,
            onSecondary        = onSurface,
            background         = bg,
            onBackground       = onSurface,
            surface            = surface,
            onSurface          = onSurface,
            surfaceVariant     = surfaceVariant,
            onSurfaceVariant   = onSurfaceVariant,
            surfaceContainer   = surface,
            outline            = outline,
            error              = Red400,
        )
    }
    return SpyglassColors(base, surfaceCard)
}

/** Public variant for custom wallpaper themes (always dark, transparent bg). */
fun buildImageTheme(
    surface: Color,
    surfaceVariant: Color,
    surfaceCard: Color,
    outline: Color,
    secondary: Color,
    onSurfaceVariant: Color,
    onSurface: Color,
): SpyglassColors = buildTheme(
    isDark = true, bg = Color.Transparent,
    surface = surface, surfaceVariant = surfaceVariant,
    surfaceCard = surfaceCard, outline = outline,
    secondary = secondary, onSurfaceVariant = onSurfaceVariant, onSurface = onSurface,
)

val ThemePresets: Map<String, SpyglassColors> = mapOf(
    // ── Image themes (semi-transparent surfaces — background image shows through) ──
    "creeper" to buildTheme(
        isDark = true, bg = Color.Transparent,
        surface = Color(0xD9101A10), surfaceVariant = Color(0xD9182818),
        surfaceCard = Color(0xD9142214), outline = Color(0xFF2A3A2A),
        secondary = Color(0xFF7AA07A), onSurfaceVariant = Color(0xFFAAD0AA), onSurface = Color(0xFFEAFFEA),
    ),
    "ocean_monument" to buildTheme(
        isDark = true, bg = Color.Transparent,
        surface = Color(0xD90C1A1E), surfaceVariant = Color(0xD9142628),
        surfaceCard = Color(0xD9102022), outline = Color(0xFF1E3438),
        secondary = Color(0xFF6A9098), onSurfaceVariant = Color(0xFFA0CCCE), onSurface = Color(0xFFE4F8FF),
    ),
    "deep_dark" to buildTheme(
        isDark = true, bg = Color.Transparent,
        surface = Color(0xD9081418), surfaceVariant = Color(0xD9102022),
        surfaceCard = Color(0xD90C1A1E), outline = Color(0xFF182C30),
        secondary = Color(0xFF648890), onSurfaceVariant = Color(0xFF98BCC2), onSurface = Color(0xFFDEF6FA),
    ),
    "nether_fortress" to buildTheme(
        isDark = true, bg = Color.Transparent,
        surface = Color(0xD91A0E08), surfaceVariant = Color(0xD9261810),
        surfaceCard = Color(0xD920120C), outline = Color(0xFF3A2410),
        secondary = Color(0xFF9A7860), onSurfaceVariant = Color(0xFFCCA888), onSurface = Color(0xFFFFF0D8),
    ),
    "desert_sunset" to buildTheme(
        isDark = true, bg = Color.Transparent,
        surface = Color(0xD91C1208), surfaceVariant = Color(0xD9281C10),
        surfaceCard = Color(0xD922160C), outline = Color(0xFF3E2C12),
        secondary = Color(0xFF9E8060), onSurfaceVariant = Color(0xFFCEB088), onSurface = Color(0xFFFFF2D8),
    ),
    "cherry_grove" to buildTheme(
        isDark = true, bg = Color.Transparent,
        surface = Color(0xD91A0E14), surfaceVariant = Color(0xD926181E),
        surfaceCard = Color(0xD9201218), outline = Color(0xFF3A2430),
        secondary = Color(0xFF9A7890), onSurfaceVariant = Color(0xFFCCA8B8), onSurface = Color(0xFFFFECF8),
    ),
    "lush_cave" to buildTheme(
        isDark = true, bg = Color.Transparent,
        surface = Color(0xD910180C), surfaceVariant = Color(0xD9182414),
        surfaceCard = Color(0xD9141E10), outline = Color(0xFF243420),
        secondary = Color(0xFF789070), onSurfaceVariant = Color(0xFFA8C8A0), onSurface = Color(0xFFECFFE4),
    ),
    "end_city" to buildTheme(
        isDark = true, bg = Color.Transparent,
        surface = Color(0xD9140E1A), surfaceVariant = Color(0xD91E1826),
        surfaceCard = Color(0xD918121E), outline = Color(0xFF2E2438),
        secondary = Color(0xFF887A98), onSurfaceVariant = Color(0xFFB8AACA), onSurface = Color(0xFFF4E8FF),
    ),
    "sunflower_plains" to buildTheme(
        isDark = true, bg = Color.Transparent,
        surface = Color(0xD9181408), surfaceVariant = Color(0xD9221E10),
        surfaceCard = Color(0xD91E1A0C), outline = Color(0xFF342E14),
        secondary = Color(0xFF908870), onSurfaceVariant = Color(0xFFC0B898), onSurface = Color(0xFFFFF6E0),
    ),
    // ── Solid colour themes ─────────────────────────────────────────────────────
    "obsidian" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF0E0C0A),
        surface         = Color(0xFF1C1A17),
        surfaceVariant  = Color(0xFF2A2720),
        surfaceCard     = Color(0xFF211F1B),
        outline         = Color(0xFF3A3730),
        secondary       = Color(0xFF6B6860),
        onSurfaceVariant = Color(0xFF9E9B94),
        onSurface       = Color(0xFFD4D1CA),
    ),
    "deepslate" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF17171B),
        surface         = Color(0xFF22222A),
        surfaceVariant  = Color(0xFF2E2E38),
        surfaceCard     = Color(0xFF292930),
        outline         = Color(0xFF3E3E48),
        secondary       = Color(0xFF6E6E78),
        onSurfaceVariant = Color(0xFFA0A0AA),
        onSurface       = Color(0xFFD6D6DE),
    ),
    "spruce" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF16120E),
        surface         = Color(0xFF221E18),
        surfaceVariant  = Color(0xFF302A22),
        surfaceCard     = Color(0xFF28221C),
        outline         = Color(0xFF3E3628),
        secondary       = Color(0xFF6E6558),
        onSurfaceVariant = Color(0xFFA09888),
        onSurface       = Color(0xFFD8D0C2),
    ),
    "warped" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF0E1614),
        surface         = Color(0xFF182420),
        surfaceVariant  = Color(0xFF24302C),
        surfaceCard     = Color(0xFF1E2A26),
        outline         = Color(0xFF304038),
        secondary       = Color(0xFF607068),
        onSurfaceVariant = Color(0xFF94A49C),
        onSurface       = Color(0xFFCCD8D2),
    ),
    "crimson" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF18100F),
        surface         = Color(0xFF261C1A),
        surfaceVariant  = Color(0xFF342826),
        surfaceCard     = Color(0xFF2C2220),
        outline         = Color(0xFF44342E),
        secondary       = Color(0xFF746460),
        onSurfaceVariant = Color(0xFFA69894),
        onSurface       = Color(0xFFDAD0CC),
    ),
    "end_stone" to buildTheme(
        isDark          = false,
        bg              = Color(0xFFF0EBE0),
        surface         = Color(0xFFE4DFD4),
        surfaceVariant  = Color(0xFFD8D3C8),
        surfaceCard     = Color(0xFFDED9CE),
        outline         = Color(0xFFBEB9AE),
        secondary       = Color(0xFF706B5A),
        onSurfaceVariant = Color(0xFF5E5948),
        onSurface       = Color(0xFF2E291E),
    ),
    "birch" to buildTheme(
        isDark          = false,
        bg              = Color(0xFFF5F2EB),
        surface         = Color(0xFFE8E5DE),
        surfaceVariant  = Color(0xFFDBD8D1),
        surfaceCard     = Color(0xFFE2DFD8),
        outline         = Color(0xFFC2BFB8),
        secondary       = Color(0xFF736E62),
        onSurfaceVariant = Color(0xFF625F54),
        onSurface       = Color(0xFF323028),
    ),
    "cherry" to buildTheme(
        isDark          = false,
        bg              = Color(0xFFF5E8EE),
        surface         = Color(0xFFE8DBE1),
        surfaceVariant  = Color(0xFFDBCED4),
        surfaceCard     = Color(0xFFE0D3DA),
        outline         = Color(0xFFBFB2B8),
        secondary       = Color(0xFF706468),
        onSurfaceVariant = Color(0xFF584C52),
        onSurface       = Color(0xFF2C2228),
    ),
    "amethyst" to buildTheme(
        isDark          = false,
        bg              = Color(0xFFEDE4F2),
        surface         = Color(0xFFE0D7E5),
        surfaceVariant  = Color(0xFFD3CAD9),
        surfaceCard     = Color(0xFFD9D0DF),
        outline         = Color(0xFFB9B0BE),
        secondary       = Color(0xFF6E6574),
        onSurfaceVariant = Color(0xFF544B5A),
        onSurface       = Color(0xFF28222E),
    ),
    "pink" to buildTheme(
        isDark          = false,
        bg              = Color(0xFFFCEFF5),
        surface         = Color(0xFFF2E3EB),
        surfaceVariant  = Color(0xFFE8D7DF),
        surfaceCard     = Color(0xFFEDDDE5),
        outline         = Color(0xFFCFBFC7),
        secondary       = Color(0xFF7A6670),
        onSurfaceVariant = Color(0xFF5E4A54),
        onSurface       = Color(0xFF2E1E26),
    ),
    "sandstone" to buildTheme(
        isDark          = false,
        bg              = Color(0xFFE8DCC8),
        surface         = Color(0xFFDCD0BC),
        surfaceVariant  = Color(0xFFD0C4B0),
        surfaceCard     = Color(0xFFD6CABC),
        outline         = Color(0xFFB6AA96),
        secondary       = Color(0xFF6E6454),
        onSurfaceVariant = Color(0xFF54483A),
        onSurface       = Color(0xFF2A2218),
    ),
    "prismarine" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF283634),
        surface         = Color(0xFF344240),
        surfaceVariant  = Color(0xFF3E4E4C),
        surfaceCard     = Color(0xFF303E3C),
        outline         = Color(0xFF4E5E5C),
        secondary       = Color(0xFF788886),
        onSurfaceVariant = Color(0xFFA0B0AE),
        onSurface       = Color(0xFFD4E2E0),
    ),
    "copper" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF342A20),
        surface         = Color(0xFF42362A),
        surfaceVariant  = Color(0xFF504436),
        surfaceCard     = Color(0xFF483C2E),
        outline         = Color(0xFF605438),
        secondary       = Color(0xFF887A68),
        onSurfaceVariant = Color(0xFFAEA090),
        onSurface       = Color(0xFFDED0C0),
    ),
    "mycelium" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF2E2832),
        surface         = Color(0xFF3C3440),
        surfaceVariant  = Color(0xFF4A424E),
        surfaceCard     = Color(0xFF423A46),
        outline         = Color(0xFF585060),
        secondary       = Color(0xFF827A88),
        onSurfaceVariant = Color(0xFFA8A0AE),
        onSurface       = Color(0xFFD8D0DE),
    ),
    "chorus" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF3A2832),
        surface         = Color(0xFF483640),
        surfaceVariant  = Color(0xFF56444E),
        surfaceCard     = Color(0xFF4E3C46),
        outline         = Color(0xFF685460),
        secondary       = Color(0xFF907E86),
        onSurfaceVariant = Color(0xFFB8A6AE),
        onSurface       = Color(0xFFE4D4DC),
    ),
    "magenta" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF2A0E22),
        surface         = Color(0xFF3A1C32),
        surfaceVariant  = Color(0xFF4A2A42),
        surfaceCard     = Color(0xFF42223A),
        outline         = Color(0xFF5E3852),
        secondary       = Color(0xFF8E6882),
        onSurfaceVariant = Color(0xFFBE98B2),
        onSurface       = Color(0xFFEED4E6),
    ),
    "shulker" to buildTheme(
        isDark          = true,
        bg              = Color(0xFF302840),
        surface         = Color(0xFF3E364E),
        surfaceVariant  = Color(0xFF4C445C),
        surfaceCard     = Color(0xFF443C52),
        outline         = Color(0xFF5E5470),
        secondary       = Color(0xFF867C90),
        onSurfaceVariant = Color(0xFFAEA4B6),
        onSurface       = Color(0xFFDCD2E6),
    ),
)

val ThemeInfoMap: Map<String, ThemeInfo> = mapOf(
    // Image themes (background color used for settings preview border/fallback)
    "creeper"           to ThemeInfo("Creeper",           Color(0xFF1A2E1A), isDark = true),
    "ocean_monument"    to ThemeInfo("Ocean Monument",    Color(0xFF0C2A30), isDark = true),
    "deep_dark"         to ThemeInfo("Deep Dark",         Color(0xFF081820), isDark = true),
    "nether_fortress"   to ThemeInfo("Nether Fortress",   Color(0xFF2A1208), isDark = true),
    "desert_sunset"     to ThemeInfo("Desert Sunset",     Color(0xFF2C1A08), isDark = true),
    "cherry_grove"      to ThemeInfo("Cherry Grove",      Color(0xFF2A1420), isDark = true),
    "lush_cave"         to ThemeInfo("Lush Cave",         Color(0xFF142010), isDark = true),
    "end_city"          to ThemeInfo("End City",           Color(0xFF1A1028), isDark = true),
    "sunflower_plains"  to ThemeInfo("Sunflower Plains",  Color(0xFF201C0C), isDark = true),
    "custom"            to ThemeInfo("My Photo",          Color(0xFF1A1A1A), isDark = true),
    // Solid themes
    "obsidian"    to ThemeInfo("Obsidian",    Color(0xFF0E0C0A), isDark = true),
    "deepslate"   to ThemeInfo("Deepslate",   Color(0xFF17171B), isDark = true),
    "spruce"      to ThemeInfo("Spruce",      Color(0xFF16120E), isDark = true),
    "warped"      to ThemeInfo("Warped",      Color(0xFF0E1614), isDark = true),
    "crimson"     to ThemeInfo("Crimson",     Color(0xFF18100F), isDark = true),
    "prismarine"  to ThemeInfo("Prismarine",  Color(0xFF283634), isDark = true),
    "copper"      to ThemeInfo("Copper",      Color(0xFF342A20), isDark = true),
    "mycelium"    to ThemeInfo("Mycelium",    Color(0xFF2E2832), isDark = true),
    "chorus"      to ThemeInfo("Chorus",      Color(0xFF3A2832), isDark = true),
    "magenta"     to ThemeInfo("Magenta",     Color(0xFF2A0E22), isDark = true),
    "shulker"     to ThemeInfo("Shulker",     Color(0xFF302840), isDark = true),
    "end_stone"   to ThemeInfo("End Stone",   Color(0xFFF0EBE0), isDark = false),
    "birch"       to ThemeInfo("Birch",       Color(0xFFF5F2EB), isDark = false),
    "sandstone"   to ThemeInfo("Sandstone",   Color(0xFFE8DCC8), isDark = false),
    "cherry"      to ThemeInfo("Cherry",      Color(0xFFF5E8EE), isDark = false),
    "amethyst"    to ThemeInfo("Amethyst",    Color(0xFFEDE4F2), isDark = false),
    "pink"        to ThemeInfo("Pink",        Color(0xFFFCEFF5), isDark = false),
)

/** Image themes — shown as featured cards in settings. */
val ImageThemeOrder = listOf(
    "creeper", "ocean_monument", "deep_dark", "nether_fortress", "desert_sunset",
    "cherry_grove", "lush_cave", "end_city", "sunflower_plains",
)

/** Solid colour themes — shown as circles in settings. */
val SolidThemeOrder = listOf(
    "obsidian", "deepslate", "spruce", "warped", "crimson",
    "prismarine", "copper", "mycelium", "chorus", "magenta", "shulker",
    "end_stone", "birch", "sandstone", "cherry", "amethyst", "pink",
)

/** All themes in display order (image themes first). */
val ThemeOrder = ImageThemeOrder + SolidThemeOrder

const val DEFAULT_THEME = "creeper"

/** Font scale multipliers: 0=Small, 1=Default, 2=Large, 3=Extra Large */
val FontScaleOptions = listOf("Small" to 0.85f, "Default" to 1.0f, "Large" to 1.15f, "Extra Large" to 1.3f)

@Composable
fun SpyglassTheme(
    theme: String = DEFAULT_THEME,
    isWideScreen: Boolean = false,
    highContrast: Boolean = false,
    hapticEnabled: Boolean = true,
    reduceAnimations: Boolean = false,
    fontScale: Int = 1,
    content: @Composable () -> Unit,
) {
    val colors = if (theme == "custom") {
        CustomWallpaper.cachedTheme ?: ThemePresets.getValue(DEFAULT_THEME)
    } else {
        ThemePresets[theme] ?: ThemePresets.getValue(DEFAULT_THEME)
    }
    val isDark = ThemeInfoMap[theme]?.isDark ?: true

    val scheme = if (highContrast) applyHighContrast(colors.scheme, isDark) else colors.scheme

    val cardColor = colors.surfaceCard

    CompositionLocalProvider(
        LocalThemeKey provides theme,
        LocalSurfaceCard provides cardColor,
        LocalIsWideScreen provides isWideScreen,
        LocalHapticEnabled provides hapticEnabled,
        LocalReduceAnimations provides reduceAnimations,
    ) {
        val scaledTypography = if (fontScale == 1) SpyglassTypography else {
            val factor = FontScaleOptions.getOrNull(fontScale)?.second ?: 1.0f
            SpyglassTypography.run {
                copy(
                    headlineMedium = headlineMedium.copy(fontSize = headlineMedium.fontSize * factor),
                    headlineSmall  = headlineSmall.copy(fontSize = headlineSmall.fontSize * factor),
                    titleLarge     = titleLarge.copy(fontSize = titleLarge.fontSize * factor),
                    titleMedium    = titleMedium.copy(fontSize = titleMedium.fontSize * factor),
                    bodyLarge      = bodyLarge.copy(fontSize = bodyLarge.fontSize * factor),
                    bodyMedium     = bodyMedium.copy(fontSize = bodyMedium.fontSize * factor),
                    labelSmall     = labelSmall.copy(fontSize = labelSmall.fontSize * factor),
                )
            }
        }

        MaterialTheme(
            colorScheme = scheme,
            typography  = scaledTypography,
            content     = content,
        )
    }
}

private fun applyHighContrast(scheme: ColorScheme, isDark: Boolean): ColorScheme {
    return if (isDark) {
        scheme.copy(
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFE0E0E0),
            onBackground = Color.White,
            outline = Color(0xFF888888),
        )
    } else {
        scheme.copy(
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF1A1A1A),
            onBackground = Color.Black,
            outline = Color(0xFF555555),
        )
    }
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
