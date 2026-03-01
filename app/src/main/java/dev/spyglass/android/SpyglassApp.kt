package dev.spyglass.android

import android.app.Application
import android.os.StrictMode
import android.os.Trace
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.data.seed.DataSeeder
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.SecurePreferences
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class SpyglassApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        Trace.beginSection("SpyglassApp.onCreate")
        try {
            super.onCreate()

            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())

                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build()
                )
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build()
                )
            }

            // Global uncaught exception handler — logs via Timber then delegates
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Timber.e(throwable, "Uncaught exception on thread %s", thread.name)
                defaultHandler?.uncaughtException(thread, throwable)
            }

            // Pre-warm database, seed game data, and migrate player prefs — all on IO
            appScope.launch(Dispatchers.IO) {
                Trace.beginSection("SpyglassApp.seedAndWarm")
                try {
                    // Pre-warm database singleton so it's ready before HomeScreen composes
                    GameDataRepository.get(this@SpyglassApp)
                    // Seed game data from bundled JSON assets (no-op after first install)
                    DataSeeder.seedIfNeeded(this@SpyglassApp)
                    // Migrate player data from DataStore to SecurePreferences (one-time)
                    migratePlayerDataToSecurePrefs()
                } finally {
                    Trace.endSection()
                }
            }
        } finally {
            Trace.endSection()
        }
    }

    private suspend fun migratePlayerDataToSecurePrefs() {
        try {
            val prefs = dataStore.data.first()
            val username = prefs[PreferenceKeys.PLAYER_USERNAME] ?: return
            if (username.isBlank()) return

            val secure = SecurePreferences.get(this)
            // Only migrate if SecurePreferences doesn't already have the data
            if (SecurePreferences.getPlayerUsername(this).isNotBlank()) return

            SecurePreferences.setPlayerUsername(this, username)
            val uuid = prefs[PreferenceKeys.PLAYER_UUID] ?: ""
            if (uuid.isNotBlank()) {
                SecurePreferences.setPlayerUuid(this, uuid)
            }
            Timber.d("Migrated player data to SecurePreferences")
        } catch (e: Exception) {
            Timber.w(e, "Failed to migrate player data to SecurePreferences")
        }
    }
}
