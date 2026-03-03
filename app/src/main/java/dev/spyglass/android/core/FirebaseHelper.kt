package dev.spyglass.android.core

import timber.log.Timber

/**
 * Consent-gated Firebase initialization. All Firebase SDKs are disabled
 * by default via AndroidManifest meta-data flags. This helper enables
 * or disables collection at runtime based on user consent.
 *
 * Safe to call even when google-services.json is absent — all calls are
 * wrapped in try/catch so the build works without Firebase configured.
 */
object FirebaseHelper {

    fun applyConsent(crashConsent: Boolean, analyticsConsent: Boolean) {
        try {
            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(crashConsent)
            Timber.d("Firebase Crashlytics collection: %s", crashConsent)
        } catch (e: Exception) {
            Timber.d("Firebase Crashlytics not available: %s", e.message)
        }

        try {
            val analytics = com.google.firebase.analytics.FirebaseAnalytics
                .getInstance(com.google.firebase.FirebaseApp.getInstance().applicationContext)
            analytics.setAnalyticsCollectionEnabled(analyticsConsent)
            Timber.d("Firebase Analytics collection: %s", analyticsConsent)
        } catch (e: Exception) {
            Timber.d("Firebase Analytics not available: %s", e.message)
        }

        try {
            val perf = com.google.firebase.perf.FirebasePerformance.getInstance()
            perf.isPerformanceCollectionEnabled = analyticsConsent
            Timber.d("Firebase Performance collection: %s", analyticsConsent)
        } catch (e: Exception) {
            Timber.d("Firebase Performance not available: %s", e.message)
        }
    }
}
