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
    val ANALYTICS_CONSENT       = booleanPreferencesKey("analytics_consent")
    val CRASH_CONSENT           = booleanPreferencesKey("crash_consent")
    val CONSENT_SHOWN           = booleanPreferencesKey("consent_shown")
}
