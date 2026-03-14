package dev.spyglass.android.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Manages a forward navigation stack. Jetpack Navigation handles back stack
 * natively, but has no concept of "forward" — this class fills that gap so
 * swipe-left can re-navigate to a page the user just swiped away from.
 */
class NavigationHistory {

    private val forwardStack = ArrayDeque<String>()

    /** Observable — true when at least one forward entry exists. */
    var canGoForward by mutableStateOf(false)
        private set

    /** Called when the user navigates back. Pushes [currentRoute] onto the forward stack. */
    fun onNavigateBack(currentRoute: String) {
        forwardStack.addLast(currentRoute)
        canGoForward = true
    }

    /** Pops the next forward route, or null if none. */
    fun onNavigateForward(): String? {
        val route = forwardStack.removeLastOrNull()
        canGoForward = forwardStack.isNotEmpty()
        return route
    }

    /** Clears the entire forward stack (called on any new navigation). */
    fun onNewNavigation() {
        forwardStack.clear()
        canGoForward = false
    }
}
