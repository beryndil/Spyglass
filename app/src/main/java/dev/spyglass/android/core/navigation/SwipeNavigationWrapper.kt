package dev.spyglass.android.core.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val SWIPE_THRESHOLD = 100.dp
private val MAX_VISUAL_OFFSET = 40.dp

/**
 * Wraps content with horizontal swipe gesture detection for back/forward navigation.
 *
 * - Swipe right -> navigate back (if [canGoBack])
 * - Swipe left  -> navigate forward (if [canGoForward])
 *
 * Inner composables that consume horizontal drags (HorizontalPager, ScrollableTabRow,
 * MapScreen canvas) naturally take priority via Compose's nested scroll / pointer
 * dispatch — their `pointerInput` blocks consume the drag before it reaches this wrapper.
 */
@Composable
fun SwipeNavigationWrapper(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onSwipeBack: () -> Unit,
    onSwipeForward: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD.toPx() }
    val maxOffsetPx = with(density) { MAX_VISUAL_OFFSET.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Keep fresh references so the pointerInput block never uses stale captures
    val currentCanGoBack = rememberUpdatedState(canGoBack)
    val currentCanGoForward = rememberUpdatedState(canGoForward)
    val currentOnSwipeBack = rememberUpdatedState(onSwipeBack)
    val currentOnSwipeForward = rememberUpdatedState(onSwipeForward)

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        val triggered = totalDrag.absoluteValue >= thresholdPx
                        if (triggered && totalDrag > 0 && currentCanGoBack.value) {
                            currentOnSwipeBack.value()
                        } else if (triggered && totalDrag < 0 && currentCanGoForward.value) {
                            currentOnSwipeForward.value()
                        }
                        scope.launch { offsetX.animateTo(0f, tween(150)) }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, tween(150)) }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        val canAct = (totalDrag > 0 && currentCanGoBack.value) || (totalDrag < 0 && currentCanGoForward.value)
                        val target = if (canAct) {
                            totalDrag.coerceIn(-maxOffsetPx, maxOffsetPx)
                        } else {
                            0f
                        }
                        scope.launch { offsetX.snapTo(target) }
                    },
                )
            }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) },
    ) {
        content()
    }
}
