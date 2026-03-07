package dev.spyglass.android

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.StrictMode
import android.os.Trace
import com.google.android.gms.ads.MobileAds
import dev.spyglass.android.core.FirebaseHelper
import dev.spyglass.android.core.module.ConnectModule
import dev.spyglass.android.core.module.CoreModule
import dev.spyglass.android.core.module.BrowseModule
import dev.spyglass.android.core.module.ModuleRegistry
import dev.spyglass.android.core.module.ToolsModule
import dev.spyglass.android.core.ui.TextureManager
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.data.seed.DataSeeder
import dev.spyglass.android.data.sync.DataSyncManager
import dev.spyglass.android.data.sync.DataSyncWorker
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

                // Note: StrictMode detectAll() is intentionally NOT used here.
                // AdMob SDK repeatedly accesses WindowManager from Application context,
                // which generates hundreds of IncorrectContextUseViolation stack traces
                // per second with penaltyLog(), burning CPU and flooding logcat.
                // Note: detectDiskReads/Writes disabled — penaltyLog() floods logcat
                // and burns CPU during startup (AdMob, DataStore, Room all do disk I/O).
                // Use Android Studio profiler for targeted disk-access audits instead.
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                        .detectNetwork()
                        .penaltyLog()
                        .build()
                )
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectLeakedClosableObjects()
                        .detectLeakedRegistrationObjects()
                        .detectLeakedSqlLiteObjects()
                        .penaltyLog()
                        .build()
                )
            }

            // Global uncaught exception handler — logs via Timber then delegates
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                // Save crash to disk so it can be sent to desktop on next connect
                dev.spyglass.android.connect.client.CrashLogStore.saveCrash(
                    this@SpyglassApp, thread.name, throwable
                )
                Timber.e(throwable, "Uncaught exception on thread %s", thread.name)
                defaultHandler?.uncaughtException(thread, throwable)
            }

            // Register all modules (order determines priority)
            ModuleRegistry.register(CoreModule)
            ModuleRegistry.register(ConnectModule)
            ModuleRegistry.register(BrowseModule)
            ModuleRegistry.register(ToolsModule)

            // Pre-warm database, seed data, init textures — all on IO to avoid ANR
            appScope.launch(Dispatchers.IO) {
                // Init TextureManager (checks if textures are already downloaded)
                TextureManager.init(this@SpyglassApp)

                // Load texture mappings (block/item ID -> filename) from JSON
                TextureManager.loadTextureMaps(this@SpyglassApp)

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

            // Initialize Firebase manually (ContentProvider auto-init removed for faster startup)
            // then read consent and conditionally enable collection
            appScope.launch(Dispatchers.IO) {
                try {
                    com.google.firebase.FirebaseApp.initializeApp(this@SpyglassApp)
                } catch (e: Exception) {
                    Timber.w(e, "Firebase init failed")
                }
                try {
                    val crashConsent = dataStore.data
                        .map { it[PreferenceKeys.CRASH_CONSENT] ?: false }
                        .first()
                    val analyticsConsent = dataStore.data
                        .map { it[PreferenceKeys.ANALYTICS_CONSENT] ?: false }
                        .first()
                    FirebaseHelper.applyConsent(crashConsent, analyticsConsent)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to read consent for Firebase init")
                }
            }

            // Initialize AdMob SDK on IO — disk-heavy init must not block main thread
            appScope.launch(Dispatchers.IO) {
                try {
                    MobileAds.initialize(this@SpyglassApp) {
                        Timber.d("MobileAds init complete: %s", it.adapterStatusMap)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "MobileAds init failed")
                }
            }

            // Enroll periodic sync worker — read prefs on IO to avoid blocking main thread
            appScope.launch(Dispatchers.IO) {
                try {
                    val syncPrefs = dataStore.data.first()
                    val offlineMode = syncPrefs[PreferenceKeys.OFFLINE_MODE] ?: false
                    if (!offlineMode) {
                        val hours = syncPrefs[PreferenceKeys.SYNC_FREQUENCY_HOURS] ?: 12
                        DataSyncWorker.enqueue(this@SpyglassApp, hours)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to read sync preferences")
                }
            }
        } finally {
            Trace.endSection()
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Timber.d("onTrimMemory CRITICAL — evicting all bitmaps")
                TextureManager.evictAllBitmaps()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                Timber.d("onTrimMemory MODERATE — trimming bitmap cache")
                TextureManager.trimBitmapCache()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Timber.d("onTrimMemory LOW — trimming bitmap cache")
                TextureManager.trimBitmapCache()
            }
        }
    }
}
