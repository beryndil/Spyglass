package dev.spyglass.android.core.module

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry

/**
 * A navigation route contributed by a module to the shell NavHost.
 */
data class ModuleRoute(
    val route: String,
    val content: @Composable (NavBackStackEntry, ModuleNavActions) -> Unit,
)

/**
 * Navigation actions available to module screens.
 */
interface ModuleNavActions {
    fun navigateTo(route: String)
    fun navigateBack()
    fun navigateToBrowseTab(tab: Int, id: String = "")
    fun navigateToCalcTab(tab: Int)
}
