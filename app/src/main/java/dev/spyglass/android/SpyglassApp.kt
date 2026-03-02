package dev.spyglass.android

import android.app.Application
import android.os.StrictMode
import android.os.Trace
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.data.seed.DataSeeder
import dev.spyglass.android.data.sync.DataSyncManager
import dev.spyglass.android.data.sync.DataSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

            // Pre-warm database and seed game data on IO
            appScope.launch(Dispatchers.IO) {
                Trace.beginSection("SpyglassApp.seedAndWarm")
                try {
                    // Pre-warm database singleton so it's ready before HomeScreen composes
                    GameDataRepository.get(this@SpyglassApp)
                    // Seed game data from bundled JSON assets (no-op after first install)
                    DataSeeder.seedIfNeeded(this@SpyglassApp)
                } finally {
                    Trace.endSection()
                }

                // Sync with GitHub for data updates (fire-and-forget)
                try {
                    DataSyncManager.sync(this@SpyglassApp)
                } catch (e: Exception) {
                    Timber.w(e, "Initial data sync failed")
                }
            }

            // Enroll periodic sync worker (every 12 hours, requires network)
            DataSyncWorker.enqueue(this)
        } finally {
            Trace.endSection()
        }
    }

}
