package dev.spyglass.android.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import dev.spyglass.android.R

sealed interface SpyglassIcon {
    data class Vector(val imageVector: ImageVector) : SpyglassIcon
    data class Drawable(@DrawableRes val resId: Int) : SpyglassIcon
    /** Potion bottle whose liquid is tinted with [color]. */
    data class Potion(val color: Color) : SpyglassIcon
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
    }
}
