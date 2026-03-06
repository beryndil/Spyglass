package dev.spyglass.android.core.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.core.content.ContextCompat
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import dev.spyglass.android.R
import java.io.File

sealed interface SpyglassIcon {
    data class Vector(val imageVector: ImageVector) : SpyglassIcon
    data class Drawable(@DrawableRes val resId: Int) : SpyglassIcon
    /** Potion bottle whose liquid is tinted with [color]. */
    data class Potion(val color: Color) : SpyglassIcon
    /** Bitmap loaded from a file on disk (downloaded textures). */
    data class FileBitmap(val file: File) : SpyglassIcon
    /** In-memory bitmap (e.g., player skin head). */
    data class BitmapIcon(val bitmap: Bitmap) : SpyglassIcon
    /** Material texture clipped to a shape mask with a frame overlay (e.g., button). */
    data class Overlay(val texture: SpyglassIcon, val mask: SpyglassIcon, val frame: SpyglassIcon) : SpyglassIcon
}

@Composable
fun SpyglassIconImage(
    icon: SpyglassIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    when (icon) {
        is SpyglassIcon.Vector -> Icon(
            imageVector = icon.imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        is SpyglassIcon.Drawable -> Icon(
            painter = painterResource(icon.resId),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = Color.Unspecified, // Colorful pixel art — don't override colors
        )
        is SpyglassIcon.Potion -> Box(modifier = modifier) {
            Icon(
                painter = painterResource(R.drawable.potion_bottle_liquid),
                contentDescription = null,
                modifier = modifier,
                tint = icon.color,
            )
            Icon(
                painter = painterResource(R.drawable.potion_bottle_base),
                contentDescription = contentDescription,
                modifier = modifier,
                tint = Color.Unspecified,
            )
        }
        is SpyglassIcon.FileBitmap -> {
            val bitmap = remember(icon.file.absolutePath) {
                TextureManager.getCachedBitmap(icon.file)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None, // Pixel art — nearest-neighbor
                )
            }
        }
        is SpyglassIcon.BitmapIcon -> {
            Image(
                bitmap = icon.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
            )
        }
        is SpyglassIcon.Overlay -> {
            val context = LocalContext.current
            val maskBitmap = remember(icon.mask) {
                when (val m = icon.mask) {
                    is SpyglassIcon.FileBitmap -> TextureManager.getCachedBitmap(m.file)
                    is SpyglassIcon.Drawable -> vectorToBitmap(context, m.resId)
                    else -> null
                }
            }
            val maskPaint = remember {
                android.graphics.Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
            }
            Box(modifier = modifier) {
                // Texture clipped to mask shape via DST_IN
                Box(
                    modifier = modifier.drawWithContent {
                        val nativeCanvas = drawContext.canvas.nativeCanvas
                        val checkpoint = nativeCanvas.saveLayer(null, null)
                        drawContent()
                        if (maskBitmap != null) {
                            nativeCanvas.drawBitmap(
                                maskBitmap,
                                null,
                                RectF(0f, 0f, size.width, size.height),
                                maskPaint,
                            )
                        }
                        nativeCanvas.restoreToCount(checkpoint)
                    }
                ) {
                    SpyglassIconImage(icon.texture, null, modifier, Color.Unspecified)
                }
                // Frame edges on top
                SpyglassIconImage(icon.frame, contentDescription, modifier, Color.Unspecified)
            }
        }
    }
}

/** Convert a vector drawable resource to a bitmap (BitmapFactory can't decode XML vectors). */
private fun vectorToBitmap(context: android.content.Context, @DrawableRes resId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val w = drawable.intrinsicWidth.coerceAtLeast(1)
    val h = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    return bitmap
}
