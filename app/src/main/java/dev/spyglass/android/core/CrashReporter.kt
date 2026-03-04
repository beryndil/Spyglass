package dev.spyglass.android.core

import timber.log.Timber

/**
 * Thin wrapper around Firebase Crashlytics for non-fatal error recording
 * and breadcrumb logging. All methods no-op safely when Crashlytics
 * isn't available or consent hasn't been given.
 */
object CrashReporter {

    /** Log a breadcrumb message visible in crash reports. */
    fun log(message: String) {
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log(message)
        } catch (_: Exception) {
            // Crashlytics not available
        }
    }

    /** Set a custom key-value pair visible in crash reports. */
    fun setKey(key: String, value: String) {
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (_: Exception) {
            // Crashlytics not available
        }
    }

    /** Record a non-fatal exception. */
    fun recordException(e: Throwable) {
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
        } catch (_: Exception) {
            // Crashlytics not available
        }
    }

    /** Record a non-fatal exception with a breadcrumb message. */
    fun recordException(e: Throwable, message: String) {
        try {
            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            crashlytics.log(message)
            crashlytics.recordException(e)
        } catch (_: Exception) {
            // Crashlytics not available
        }
    }
}
