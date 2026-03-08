package dev.spyglass.android.core.ui

import android.os.Build
import android.view.HapticFeedbackConstants
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
 * Uses FLAG_IGNORE_GLOBAL_SETTING to ensure feedback fires even if the
 * system haptic setting is off (our own preference controls this instead).
 */
@Composable
fun rememberHapticConfirm(): () -> Unit {
    val enabled = LocalHapticEnabled.current
    val view = LocalView.current
    return remember(enabled, view) {
        {
            if (enabled) {
                val constant = if (Build.VERSION.SDK_INT >= 30) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }
                @Suppress("DEPRECATION")
                view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            }
        }
    }
}

/**
 * Returns a callback that fires a light click haptic.
 * Good for filter chips, tabs, and general taps.
 * Uses FLAG_IGNORE_GLOBAL_SETTING to ensure feedback fires even if the
 * system haptic setting is off (our own preference controls this instead).
 */
@Composable
fun rememberHapticClick(): () -> Unit {
    val enabled = LocalHapticEnabled.current
    val view = LocalView.current
    return remember(enabled, view) {
        {
            if (enabled) {
                @Suppress("DEPRECATION")
                view.performHapticFeedback(
                    HapticFeedbackConstants.CONTEXT_CLICK,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
                )
            }
        }
    }
}
