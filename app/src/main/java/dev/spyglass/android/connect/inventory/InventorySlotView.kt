package dev.spyglass.android.connect.inventory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import dev.spyglass.android.connect.ItemStack
import dev.spyglass.android.core.ui.ItemTextures
import dev.spyglass.android.core.ui.LocalSurfaceCard
import dev.spyglass.android.core.ui.SpyglassIconImage
import dev.spyglass.android.core.ui.rememberHaptic
import kotlinx.coroutines.delay

/**
 * Single inventory slot: item icon + count badge + tap tooltip + selected highlight.
 * Tap shows tooltip popup, long-press opens item card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventorySlotView(
    item: ItemStack?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onLongPress: ((ItemStack) -> Unit)? = null,
) {
    var showTooltip by remember { mutableStateOf(false) }
    val haptic = rememberHaptic()

    // Auto-dismiss tooltip after 2 seconds
    if (showTooltip) {
        LaunchedEffect(Unit) {
            delay(2000)
            showTooltip = false
        }
    }

    val density = LocalDensity.current
    val tooltipOffsetY = with(density) { (-32).dp.roundToPx() }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(LocalSurfaceCard.current, RoundedCornerShape(4.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp),
            )
            .then(
                if (item != null) {
                    Modifier.combinedClickable(
                        onClick = { showTooltip = !showTooltip },
                        onLongClick = { haptic(); onLongPress?.invoke(item) },
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

            // Count badge
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

            // Tooltip popup
            if (showTooltip) {
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(0, tooltipOffsetY),
                    onDismissRequest = { showTooltip = false },
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shadowElevation = 4.dp,
                    ) {
                        val name = item.customName ?: formatItemName(item.id)
                        val countText = if (item.count > 1) " x${item.count}" else ""
                        Text(
                            "$name$countText",
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .widthIn(max = 200.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun formatItemName(id: String): String =
    id.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
