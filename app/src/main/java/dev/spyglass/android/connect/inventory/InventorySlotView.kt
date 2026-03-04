package dev.spyglass.android.connect.inventory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.spyglass.android.connect.ItemStack
import dev.spyglass.android.core.ui.ItemTextures
import dev.spyglass.android.core.ui.LocalSurfaceCard
import dev.spyglass.android.core.ui.SpyglassIconImage

/**
 * Single inventory slot composable: item icon + count badge.
 * Tap shows item name, long-press opens item card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventorySlotView(
    item: ItemStack?,
    modifier: Modifier = Modifier,
    onTap: ((ItemStack) -> Unit)? = null,
    onLongPress: ((ItemStack) -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(LocalSurfaceCard.current, RoundedCornerShape(4.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp),
            )
            .then(
                if (item != null && (onTap != null || onLongPress != null)) {
                    Modifier.combinedClickable(
                        onClick = { onTap?.invoke(item) },
                        onLongClick = { onLongPress?.invoke(item) },
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (item != null) {
            val icon = ItemTextures.get(item.id)
            if (icon != null) {
                SpyglassIconImage(
                    icon = icon,
                    contentDescription = item.id,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    tint = Color.Unspecified,
                )
            }

            // Count badge (skip for count=1)
            if (item.count > 1) {
                Text(
                    text = "${item.count}",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 8.sp,
                        textAlign = TextAlign.End,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
