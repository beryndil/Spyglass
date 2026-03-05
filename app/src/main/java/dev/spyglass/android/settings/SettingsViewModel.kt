package dev.spyglass.android.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.core.FirebaseHelper
import dev.spyglass.android.core.VersionFilterState
import dev.spyglass.android.core.ui.DEFAULT_THEME
import dev.spyglass.android.core.ui.TextureManager
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.data.sync.DataSyncWorker
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

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

    val adPersonalizationConsent: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.AD_PERSONALIZATION_CONSENT] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAdPersonalizationConsent(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.AD_PERSONALIZATION_CONSENT] = enabled }
    }

    fun deleteAllUserData() = viewModelScope.launch {
        repo.deleteAllUserData()
    }

    // Appearance & Accessibility
    val hapticFeedback: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.HAPTIC_FEEDBACK] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reduceAnimations: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.REDUCE_ANIMATIONS] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.HAPTIC_FEEDBACK] = enabled }
    }

    fun setReduceAnimations(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.REDUCE_ANIMATIONS] = enabled }
    }

    // Data & Sync
    val syncFrequencyHours: StateFlow<Int> = store.data
        .map { it[PreferenceKeys.SYNC_FREQUENCY_HOURS] ?: 12 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12)

    fun setSyncFrequencyHours(hours: Int) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.SYNC_FREQUENCY_HOURS] = hours }
        DataSyncWorker.enqueue(getApplication(), hours)
    }

    /** Returns total size of downloaded textures in bytes. */
    fun getTextureStorageBytes(): Long {
        val dir = File(getApplication<Application>().filesDir, "textures")
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearTextureCache() = viewModelScope.launch {
        TextureManager.delete(getApplication())
    }
}
