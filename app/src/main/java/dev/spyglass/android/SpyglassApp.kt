package dev.spyglass.android

import android.app.Application
import android.os.StrictMode
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

        // Migrate player data from DataStore to SecurePreferences (one-time)
        appScope.launch(Dispatchers.IO) {
            migratePlayerDataToSecurePrefs()
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
