package dev.spyglass.android.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.core.FirebaseHelper
import dev.spyglass.android.core.VersionFilterState
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

    // Game Version filter
    val minecraftEdition: StateFlow<String> = store.data
        .map { it[PreferenceKeys.MINECRAFT_EDITION] ?: "java" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "java")

    val minecraftVersion: StateFlow<String> = store.data
        .map { it[PreferenceKeys.MINECRAFT_VERSION] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val versionFilterMode: StateFlow<String> = store.data
        .map { it[PreferenceKeys.VERSION_FILTER_MODE] ?: "show_all" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "show_all")

    val versionFilter: StateFlow<VersionFilterState> = combine(
        minecraftEdition, minecraftVersion, versionFilterMode
    ) { edition, version, mode ->
        VersionFilterState(edition, version, mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VersionFilterState())

    fun setMinecraftEdition(edition: String) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.MINECRAFT_EDITION] = edition }
    }

    fun setMinecraftVersion(version: String) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.MINECRAFT_VERSION] = version }
    }

    fun setVersionFilterMode(mode: String) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.VERSION_FILTER_MODE] = mode }
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
        FirebaseHelper.applyConsent(crashConsent.value, enabled)
    }

    fun setCrashConsent(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.CRASH_CONSENT] = enabled }
        FirebaseHelper.applyConsent(enabled, analyticsConsent.value)
    }

    fun deleteAllUserData() = viewModelScope.launch {
        repo.deleteAllUserData()
    }
}
