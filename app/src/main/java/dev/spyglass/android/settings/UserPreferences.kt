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
    val PLAYER_USERNAME         = stringPreferencesKey("player_username")
    val DISMISS_USERNAME_DIALOG = booleanPreferencesKey("dismiss_username_dialog")
}
