package dev.spyglass.android.core.module

import dev.spyglass.android.BuildConfig
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassIcon
import dev.spyglass.android.data.sync.DataManifest
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Core module — always enabled. Owns theme, preferences, privacy/consent,
 * texture management, news, app lock, and informational screens.
 *
 * Settings composables are defined in separate files within this package:
 * - [CoreSettingsAppearance.kt] — theme, font, wallpaper, motion
 * - [CoreSettingsGameplay.kt]   — edition, version filter, player profile, default tabs
 * - [CoreSettingsGeneral.kt]    — startup, tip-of-day, sync, storage, favorites
 * - [CoreSettingsPrivacy.kt]    — analytics, crash consent, app lock, about
 * - [CoreSettingsHelpers.kt]    — reusable toggle/link composables
 */
object CoreModule : SpyglassModule {

    override val id = "core"
    override val name = "Core"
    override val icon: SpyglassIcon = PixelIcons.Blocks
    override val priority = 0
    override val canDisable = false

    // ── Home sections ───────────────────────────────────────────────────────
    // Home content is rendered by HomeScreen.kt directly; the module system
    // no longer contributes home sections for Core.

    override fun homeSections(): List<HomeSection> = emptyList()

    // ── Settings sections ───────────────────────────────────────────────────
    // Each composable is defined in a dedicated CoreSettings*.kt file.

    override fun settingsSections(): List<SettingsSection> = listOf(
        // Tab 0: Appearance
        SettingsSection("appearance", "Appearance", 0, tab = 0) { AppearanceContent() },
        // Tab 1: Gameplay
        SettingsSection("player_profile", "Player", -10, tab = 1) { PlayerProfileContent() },
        SettingsSection("game_filters", "Game Filters", 0, tab = 1) { GameFiltersContent() },
        SettingsSection("default_tabs", "Default Tabs", 10, tab = 1) { DefaultTabsContent() },
        // Tab 2: General
        SettingsSection("app_behavior", "App Behavior", 0, tab = 2) { AppBehaviorContent() },
        SettingsSection("data_sync", "Data & Sync", 10, tab = 2) { DataSyncContent() },
        SettingsSection("favorites", "Favorites", 20, tab = 2) { FavoritesContent() },
        // Tab 3: About & Privacy
        SettingsSection("privacy_security", "Privacy & Security", 0, tab = 3) { PrivacySecurityContent() },
        SettingsSection("about", "About", 10, tab = 3) { scope -> AboutContent(scope) },
    )

    // ── Nav routes ──────────────────────────────────────────────────────────

    override fun navRoutes(): List<ModuleRoute> = listOf(
        ModuleRoute("about") { _, nav ->
            dev.spyglass.android.about.AboutScreen(
                onBack = { nav.navigateBack() },
                onLicense = { nav.navigateTo("license") },
                onDisclaimer = { nav.navigateTo("disclaimer") },
            )
        },
        ModuleRoute("license") { _, nav ->
            dev.spyglass.android.license.LicenseScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("disclaimer") { _, nav ->
            dev.spyglass.android.disclaimer.DisclaimerScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("changelog") { _, nav ->
            dev.spyglass.android.changelog.ChangelogScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("feedback") { _, nav ->
            dev.spyglass.android.feedback.FeedbackScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("help") { _, nav ->
            dev.spyglass.android.help.HelpScreen(onBack = { nav.navigateBack() })
        },
        ModuleRoute("news") { _, nav ->
            dev.spyglass.android.news.NewsScreen(onBack = { nav.navigateBack() })
        },
    )

    override fun bottomNavItems(): List<BottomNavItem> = emptyList()

    override fun searchProvider(): SearchProvider? = null

    // ── Utilities ───────────────────────────────────────────────────────────

    /**
     * Checks the data manifest CDN for a newer app release.
     *
     * Compares the remote `latestApp` field against the current build's
     * version name using zodiac-based version ordering.
     *
     * @return `true` if an update is available, `false` if current, `null` on error.
     */
    internal fun checkForUpdate(): Boolean? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("https://data.hardknocks.university/manifest.json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val manifest = DataManifest.fromJson(body)
                val latestApp = manifest.latestApp
                if (latestApp.isEmpty()) return null
                val currentVersion = BuildConfig.VERSION_NAME.removeSuffix("-a")
                DataManifest.compareVersions(latestApp, currentVersion) > 0
            }
        } catch (e: Exception) {
            Timber.d(e, "Update check failed")
            null
        }
    }
}
