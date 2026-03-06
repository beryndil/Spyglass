package dev.spyglass.android.core.module

import androidx.compose.runtime.Composable

/**
 * A composable section contributed by a module to the shell home screen.
 * Sections are sorted by [weight] (lower = higher on screen).
 */
data class HomeSection(
    val key: String,
    val weight: Int,
    val content: @Composable (HomeSectionScope) -> Unit,
)

/**
 * Scope provided to home sections, enabling navigation without
 * exposing the NavController directly.
 */
interface HomeSectionScope {
    fun navigateTo(route: String)
    fun navigateToBrowseTab(tab: Int, id: String = "")
    fun navigateToCalcTab(tab: Int)
    fun navigateToSearch()
    fun navigateToScanQr()
}
