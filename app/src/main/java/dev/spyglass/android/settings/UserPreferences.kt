package dev.spyglass.android.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

object PreferenceKeys {
    val DEFAULT_BROWSE_TAB      = intPreferencesKey("default_browse_tab")
    val DEFAULT_TOOL_TAB        = intPreferencesKey("default_tool_tab")
    val SHOW_TIP_OF_DAY         = booleanPreferencesKey("show_tip_of_day")
    val SHOW_FAVORITES_ON_HOME  = booleanPreferencesKey("show_favorites_on_home")
    val GAME_CLOCK_ENABLED      = booleanPreferencesKey("game_clock_enabled")
    val CLOCK_TICK_OFFSET       = longPreferencesKey("clock_tick_offset")
    val CLOCK_SYNC_TIME_MS      = longPreferencesKey("clock_sync_time_ms")
    val CLOCK_SYNC_METHOD       = stringPreferencesKey("clock_sync_method")
    val CLOCK_ACTIVE_EVENTS     = stringPreferencesKey("clock_active_events")
    val CLOCK_DAY_OFFSET        = longPreferencesKey("clock_day_offset")
    val BACKGROUND_THEME        = stringPreferencesKey("background_theme")
    val MINECRAFT_EDITION       = stringPreferencesKey("minecraft_edition")
    val MINECRAFT_VERSION       = stringPreferencesKey("minecraft_version")
    val VERSION_FILTER_MODE     = stringPreferencesKey("version_filter_mode")
    val ANALYTICS_CONSENT       = booleanPreferencesKey("analytics_consent")
    val CRASH_CONSENT           = booleanPreferencesKey("crash_consent")
    val CONSENT_SHOWN           = booleanPreferencesKey("consent_shown")
    val AD_PERSONALIZATION_CONSENT = booleanPreferencesKey("ad_personalization_consent")

    // Appearance & Accessibility
    val HAPTIC_FEEDBACK         = booleanPreferencesKey("haptic_feedback")
    val REDUCE_ANIMATIONS       = booleanPreferencesKey("reduce_animations")
    val DYNAMIC_COLOR           = booleanPreferencesKey("dynamic_color")
    val HIGH_CONTRAST           = booleanPreferencesKey("high_contrast")
    val DEFAULT_STARTUP_TAB     = intPreferencesKey("default_startup_tab")
    val FONT_SCALE              = intPreferencesKey("font_scale") // 0=Small, 1=Default, 2=Large, 3=Extra Large

    // Security
    val APP_LOCK_ENABLED        = booleanPreferencesKey("app_lock_enabled")

    // Content Filtering
    val HIDE_UNOBTAINABLE_BLOCKS = booleanPreferencesKey("hide_unobtainable_blocks")
    val SHOW_EXPERIMENTAL        = booleanPreferencesKey("show_experimental")

    // Data & Sync
    val SYNC_FREQUENCY_HOURS    = intPreferencesKey("sync_frequency_hours")
    val OFFLINE_MODE            = booleanPreferencesKey("offline_mode")

    // Language / i18n
    val APP_LANGUAGE            = stringPreferencesKey("app_language") // "system", "en", "es", "pt", "fr", "de", "ja"
    val TRANSLATE_GAME_DATA     = booleanPreferencesKey("translate_game_data")     // translate entity names/descriptions
    val SHOW_ORIGINAL_NAMES     = booleanPreferencesKey("show_original_names")     // show English name alongside translation

    // Spyglass Connect
    val CONNECT_PAIRED_DEVICE   = stringPreferencesKey("connect_paired_device")
    val CONNECT_AUTO_RECONNECT  = booleanPreferencesKey("connect_auto_reconnect")
    val PLAYER_IGN              = stringPreferencesKey("player_ign")
}
