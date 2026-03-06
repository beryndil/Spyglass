package dev.spyglass.android.core.module

import androidx.compose.runtime.Composable

/**
 * A composable section contributed by a module to the shell settings screen.
 * Sections are sorted by [weight] (lower = higher on screen).
 */
data class SettingsSection(
    val key: String,
    val title: String,
    val weight: Int,
    val content: @Composable (SettingsSectionScope) -> Unit,
)

/**
 * Scope provided to settings sections, enabling navigation without
 * exposing the NavController directly.
 */
interface SettingsSectionScope {
    fun navigateTo(route: String)
    fun navigateToCalcTab(tab: Int)
}
