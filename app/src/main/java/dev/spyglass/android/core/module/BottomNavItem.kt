package dev.spyglass.android.core.module

import dev.spyglass.android.core.ui.SpyglassIcon

/**
 * A bottom navigation tab contributed by a module.
 */
data class BottomNavItem(
    val route: String,
    val labelResId: Int,
    val icon: SpyglassIcon,
    val priority: Int,
)
