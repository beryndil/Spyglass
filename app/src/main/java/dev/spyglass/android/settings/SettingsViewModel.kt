package dev.spyglass.android.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.core.ui.DEFAULT_THEME
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.repository.GameDataRepository
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = app.dataStore
    private val repo  = GameDataRepository.get(app)

    val defaultBrowseTab: StateFlow<Int> = store.data
        .map { it[PreferenceKeys.DEFAULT_BROWSE_TAB] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val defaultToolTab: StateFlow<Int> = store.data
        .map { it[PreferenceKeys.DEFAULT_TOOL_TAB] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val showTipOfDay: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.SHOW_TIP_OF_DAY] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showFavoritesOnHome: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.SHOW_FAVORITES_ON_HOME] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val playerUsername: StateFlow<String> = store.data
        .map { it[PreferenceKeys.PLAYER_USERNAME] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val playerUuid: StateFlow<String> = store.data
        .map { it[PreferenceKeys.PLAYER_UUID] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val allFavorites: StateFlow<List<FavoriteEntity>> = repo.allFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDefaultBrowseTab(tab: Int) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.DEFAULT_BROWSE_TAB] = tab }
    }

    fun setDefaultToolTab(tab: Int) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.DEFAULT_TOOL_TAB] = tab }
    }

    fun setShowTipOfDay(show: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.SHOW_TIP_OF_DAY] = show }
    }

    fun setShowFavoritesOnHome(show: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.SHOW_FAVORITES_ON_HOME] = show }
    }

    fun setPlayerUsername(name: String) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.PLAYER_USERNAME] = name }
    }

    fun clearPlayerUsername() = viewModelScope.launch {
        store.edit {
            it.remove(PreferenceKeys.PLAYER_USERNAME)
            it.remove(PreferenceKeys.PLAYER_UUID)
            it.remove(PreferenceKeys.DISMISS_USERNAME_DIALOG)
        }
    }

    val gameClockEnabled: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.GAME_CLOCK_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setGameClockEnabled(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.GAME_CLOCK_ENABLED] = enabled }
    }

    val backgroundTheme: StateFlow<String> = store.data
        .map { it[PreferenceKeys.BACKGROUND_THEME] ?: DEFAULT_THEME }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_THEME)

    fun setBackgroundTheme(theme: String) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.BACKGROUND_THEME] = theme }
    }

    fun clearAllFavorites() = viewModelScope.launch {
        repo.deleteAllFavorites()
    }

    // Privacy & Consent
    val analyticsConsent: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.ANALYTICS_CONSENT] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val crashConsent: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.CRASH_CONSENT] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAnalyticsConsent(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.ANALYTICS_CONSENT] = enabled }
    }

    fun setCrashConsent(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.CRASH_CONSENT] = enabled }
    }

    fun deleteAllUserData() = viewModelScope.launch {
        repo.deleteAllUserData()
        // Clear player data from DataStore
        store.edit {
            it.remove(PreferenceKeys.PLAYER_USERNAME)
            it.remove(PreferenceKeys.PLAYER_UUID)
            it.remove(PreferenceKeys.DISMISS_USERNAME_DIALOG)
        }
        // Clear secure preferences
        try {
            SecurePreferences.clearAll(getApplication())
        } catch (_: Exception) { /* SecurePreferences may not be initialized yet */ }
    }
}
