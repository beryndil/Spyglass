package dev.spyglass.android.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.palette.graphics.Palette
import timber.log.Timber
import java.io.File

/**
 * Manages a user-chosen wallpaper image stored in internal storage.
 * Extracts a color palette from the image to auto-generate a matching theme.
 */
object CustomWallpaper {

    private const val FILENAME = "custom_wallpaper.webp"
    private const val MAX_WIDTH = 1080

    /** Cached theme generated from the wallpaper palette. */
    var cachedTheme: SpyglassColors? = null
        private set

    /** Cached bitmap for the background image (loaded at display resolution). */
    var cachedBitmap: Bitmap? = null
        private set

    private fun wallpaperFile(context: Context) = File(context.filesDir, FILENAME)

    fun hasWallpaper(context: Context): Boolean = wallpaperFile(context).exists()

    /** Call at app startup to load cached theme if a wallpaper exists. */
    fun init(context: Context) {
        val file = wallpaperFile(context)
        if (file.exists()) {
            cachedBitmap = decodeBitmap(file)
            cachedBitmap?.let { cachedTheme = extractTheme(it) }
            Timber.i("CustomWallpaper: loaded wallpaper (${file.length() / 1024}KB)")
        }
    }

    /** Save image from a content URI, extract theme, cache everything. */
    fun save(context: Context, uri: Uri) {
        val file = wallpaperFile(context)
        try {
            // Decode with downsampling to max width
            val input = context.contentResolver.openInputStream(uri) ?: return
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, opts)
            input.close()

            val sampleSize = (opts.outWidth / MAX_WIDTH).coerceAtLeast(1)
            val input2 = context.contentResolver.openInputStream(uri) ?: return
            val opts2 = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeStream(input2, null, opts2)
            input2.close()

            if (bitmap == null) {
                Timber.w("CustomWallpaper: failed to decode image")
                return
            }

            // Fix EXIF rotation
            val rotated = context.contentResolver.openInputStream(uri)?.use { exifStream ->
                val exif = ExifInterface(exifStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
                val degrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (degrees != 0f) {
                    val matrix = Matrix().apply { postRotate(degrees) }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                        if (it !== bitmap) bitmap.recycle()
                    }
                } else bitmap
            } ?: bitmap

            // Scale to max width if still too large
            val scaled = if (rotated.width > MAX_WIDTH) {
                val ratio = MAX_WIDTH.toFloat() / rotated.width
                Bitmap.createScaledBitmap(rotated, MAX_WIDTH, (rotated.height * ratio).toInt(), true).also {
                    if (it !== rotated) rotated.recycle()
                }
            } else rotated

            // Save as WebP
            file.outputStream().use { out ->
                @Suppress("DEPRECATION")
                scaled.compress(Bitmap.CompressFormat.WEBP, 85, out)
            }

            cachedBitmap = scaled
            cachedTheme = extractTheme(scaled)
            Timber.i("CustomWallpaper: saved ${scaled.width}x${scaled.height} (${file.length() / 1024}KB)")
        } catch (e: Exception) {
            Timber.e(e, "CustomWallpaper: failed to save")
        }
    }

    /** Delete the wallpaper and clear caches. */
    fun delete(context: Context) {
        wallpaperFile(context).delete()
        cachedBitmap?.recycle()
        cachedBitmap = null
        cachedTheme = null
    }

    private fun decodeBitmap(file: File): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            val sampleSize = (opts.outWidth / MAX_WIDTH).coerceAtLeast(1)
            val opts2 = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, opts2)
        } catch (e: Exception) {
            Timber.e(e, "CustomWallpaper: failed to decode bitmap")
            null
        }
    }

    /** Extract a SpyglassColors theme from the dominant colors in the bitmap. */
    private fun extractTheme(bitmap: Bitmap): SpyglassColors {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()

        val dominant = palette.darkMutedSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
        val accent = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: dominant

        val dominantRgb = dominant?.rgb ?: 0xFF1A1A1A.toInt()
        val accentRgb = accent?.rgb ?: 0xFF888888.toInt()

        // Extract HSL to determine brightness
        val hsl = dominant?.hsl ?: floatArrayOf(0f, 0f, 0.1f)

        // Darken the dominant color for surfaces (we want dark, semi-transparent surfaces)
        val r = ((dominantRgb shr 16) and 0xFF)
        val g = ((dominantRgb shr 8) and 0xFF)
        val b = (dominantRgb and 0xFF)

        // Create dark, tinted surface colors at 85% opacity
        val surfR = (r * 0.3f).toInt().coerceIn(0, 40)
        val surfG = (g * 0.3f).toInt().coerceIn(0, 40)
        val surfB = (b * 0.3f).toInt().coerceIn(0, 40)

        val surface = Color(surfR, surfG, surfB, 0xD9)
        val surfaceVariant = Color(surfR + 8, surfG + 8, surfB + 8, 0xD9)
        val surfaceCard = Color(surfR + 4, surfG + 4, surfB + 4, 0xD9)
        val outline = Color(
            (surfR + 20).coerceAtMost(255),
            (surfG + 20).coerceAtMost(255),
            (surfB + 20).coerceAtMost(255),
        )

        // Accent/secondary — brighten for readability
        val aR = ((accentRgb shr 16) and 0xFF)
        val aG = ((accentRgb shr 8) and 0xFF)
        val aB = (accentRgb and 0xFF)
        val secondary = Color(
            (aR * 0.7f + 76).toInt().coerceAtMost(255),
            (aG * 0.7f + 76).toInt().coerceAtMost(255),
            (aB * 0.7f + 76).toInt().coerceAtMost(255),
        )

        // Text colors — bright with a tint of the dominant hue
        val tintR = (r * 0.1f).toInt()
        val tintG = (g * 0.1f).toInt()
        val tintB = (b * 0.1f).toInt()
        val onSurface = Color(
            (230 + tintR).coerceAtMost(255),
            (230 + tintG).coerceAtMost(255),
            (230 + tintB).coerceAtMost(255),
        )
        val onSurfaceVariant = Color(
            (170 + tintR).coerceAtMost(255),
            (170 + tintG).coerceAtMost(255),
            (170 + tintB).coerceAtMost(255),
        )

        return buildImageTheme(
            surface = surface,
            surfaceVariant = surfaceVariant,
            surfaceCard = surfaceCard,
            outline = outline,
            secondary = secondary,
            onSurfaceVariant = onSurfaceVariant,
            onSurface = onSurface,
        )
    }
}
