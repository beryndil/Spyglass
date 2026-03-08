package dev.spyglass.android.core.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Returns a callback that fires haptic feedback when invoked,
 * respecting the user's haptic preference via [LocalHapticEnabled].
 */
@Composable
fun rememberHaptic(type: HapticFeedbackType = HapticFeedbackType.LongPress): () -> Unit {
    val enabled = LocalHapticEnabled.current
    val haptic = LocalHapticFeedback.current
    return remember(enabled, haptic, type) {
        { if (enabled) haptic.performHapticFeedback(type) }
    }
}

/**
 * Returns a callback that fires a confirm (tick) haptic via the View API,
 * respecting the user's haptic preference. Good for toggles and favorites.
 */
@Composable
fun rememberHapticConfirm(): () -> Unit {
    val enabled = LocalHapticEnabled.current
    val view = LocalView.current
    return remember(enabled, view) {
        {
            if (enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
        }
    }
}

/**
 * Returns a callback that fires a light click haptic.
 * Good for filter chips, tabs, and general taps.
 */
@Composable
fun rememberHapticClick(): () -> Unit {
    val enabled = LocalHapticEnabled.current
    val view = LocalView.current
    return remember(enabled, view) {
        {
            if (enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }
}
