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
import dev.spyglass.android.data.sync.DataSyncManager
import dev.spyglass.android.data.sync.DataSyncWorker
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
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

    val dynamicColor: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.DYNAMIC_COLOR] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val highContrast: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.HIGH_CONTRAST] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val defaultStartupTab: StateFlow<Int> = store.data
        .map { it[PreferenceKeys.DEFAULT_STARTUP_TAB] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.HAPTIC_FEEDBACK] = enabled }
    }

    fun setReduceAnimations(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.REDUCE_ANIMATIONS] = enabled }
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.DYNAMIC_COLOR] = enabled }
    }

    fun setHighContrast(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.HIGH_CONTRAST] = enabled }
    }

    fun setDefaultStartupTab(tab: Int) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.DEFAULT_STARTUP_TAB] = tab }
    }

    val fontScale: StateFlow<Int> = store.data
        .map { it[PreferenceKeys.FONT_SCALE] ?: 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    fun setFontScale(scale: Int) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.FONT_SCALE] = scale }
    }

    // Language / i18n
    val appLanguage: StateFlow<String> = store.data
        .map { it[PreferenceKeys.APP_LANGUAGE] ?: "system" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun setAppLanguage(language: String) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.APP_LANGUAGE] = language }
        val localeList = if (language == "system") {
            androidx.core.os.LocaleListCompat.getEmptyLocaleList()
        } else {
            androidx.core.os.LocaleListCompat.forLanguageTags(language)
        }
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
    }

    val translateGameData: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.TRANSLATE_GAME_DATA] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setTranslateGameData(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.TRANSLATE_GAME_DATA] = enabled }
    }

    val showOriginalNames: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.SHOW_ORIGINAL_NAMES] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setShowOriginalNames(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.SHOW_ORIGINAL_NAMES] = enabled }
    }

    // Content Filtering
    val hideUnobtainableBlocks: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.HIDE_UNOBTAINABLE_BLOCKS] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setHideUnobtainableBlocks(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.HIDE_UNOBTAINABLE_BLOCKS] = enabled }
    }

    val showExperimental: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.SHOW_EXPERIMENTAL] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setShowExperimental(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.SHOW_EXPERIMENTAL] = enabled }
    }

    // Security
    val appLockEnabled: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.APP_LOCK_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAppLockEnabled(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.APP_LOCK_ENABLED] = enabled }
    }

    // Data & Sync
    val syncFrequencyHours: StateFlow<Int> = store.data
        .map { it[PreferenceKeys.SYNC_FREQUENCY_HOURS] ?: 12 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12)

    val offlineMode: StateFlow<Boolean> = store.data
        .map { it[PreferenceKeys.OFFLINE_MODE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setSyncFrequencyHours(hours: Int) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.SYNC_FREQUENCY_HOURS] = hours }
        DataSyncWorker.enqueue(getApplication(), hours)
    }

    fun setOfflineMode(enabled: Boolean) = viewModelScope.launch {
        store.edit { it[PreferenceKeys.OFFLINE_MODE] = enabled }
        if (enabled) {
            DataSyncWorker.cancel(getApplication())
        } else {
            DataSyncWorker.enqueue(getApplication(), syncFrequencyHours.value)
        }
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

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    fun syncNow() = viewModelScope.launch(Dispatchers.IO) {
        _syncing.value = true
        try {
            DataSyncManager.sync(getApplication())
        } finally {
            _syncing.value = false
        }
    }
}
