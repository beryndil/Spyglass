package dev.spyglass.android.connect.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.spyglass.android.connect.ItemStack
import dev.spyglass.android.core.ui.ItemTextures
import dev.spyglass.android.core.ui.LocalSurfaceCard
import dev.spyglass.android.core.ui.SpyglassIconImage

/**
 * Single inventory slot composable: item icon + count badge.
 * Reuses the existing ItemTextures.get(itemId) system.
 */
@Composable
fun InventorySlotView(
    item: ItemStack?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(LocalSurfaceCard.current, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (item != null) {
            val icon = ItemTextures.get(item.id)
            if (icon != null) {
                SpyglassIconImage(
                    icon = icon,
                    contentDescription = item.id,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified,
                )
            }

            // Count badge (skip for count=1)
            if (item.count > 1) {
                Text(
                    text = "${item.count}",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
