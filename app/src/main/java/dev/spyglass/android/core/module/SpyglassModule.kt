package dev.spyglass.android.core.module

import android.content.Context
import dev.spyglass.android.core.ui.SpyglassIcon

/**
 * A feature module that contributes UI sections, navigation routes, and search
 * to the Spyglass shell. Modules register themselves via [ModuleRegistry].
 */
interface SpyglassModule {
    /** Unique identifier, e.g. "core", "database", "tools", "connect". */
    val id: String

    /** Human-readable display name. */
    val name: String

    /** Icon shown in module management UI. */
    val icon: SpyglassIcon

    /** Sort priority — lower values appear first. */
    val priority: Int

    /** Whether the user can toggle this module off. CoreModule returns false. */
    val canDisable: Boolean

    // ── Contributions to the shell ──────────────────────────────────────────

    /** Home screen sections this module provides, sorted by weight. */
    fun homeSections(): List<HomeSection>

    /** Settings screen sections this module provides, sorted by weight. */
    fun settingsSections(): List<SettingsSection>

    /** Navigation routes this module registers in the NavHost. */
    fun navRoutes(): List<ModuleRoute>

    /** Bottom navigation items (e.g. Browse, Tools tabs). */
    fun bottomNavItems(): List<BottomNavItem>

    /** Optional search provider for global search. */
    fun searchProvider(): SearchProvider?

    // ── Lifecycle ───────────────────────────────────────────────────────────

    /** Called once at app startup, before any composable renders. */
    suspend fun onInit(context: Context) {}

    /** Called when the user enables this module. */
    suspend fun onEnabled() {}

    /** Called when the user disables this module. */
    suspend fun onDisabled() {}
}
