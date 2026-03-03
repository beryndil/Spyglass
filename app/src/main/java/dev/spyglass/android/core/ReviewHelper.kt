package dev.spyglass.android.core

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import timber.log.Timber

/**
 * Tracks app opens and triggers the Play In-App Review flow once
 * after [REQUIRED_OPENS] launches. Uses SharedPreferences to persist
 * the open count and whether the prompt has already been shown.
 */
object ReviewHelper {

    private const val PREFS_NAME = "spyglass_review"
    private const val KEY_OPEN_COUNT = "open_count"
    private const val KEY_PROMPTED = "prompted"
    private const val REQUIRED_OPENS = 10

    /**
     * Call from Activity.onCreate(). Increments the open counter and
     * triggers the review flow if the threshold is reached (one-time).
     */
    fun trackOpenAndPrompt(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PROMPTED, false)) return

        val count = prefs.getInt(KEY_OPEN_COUNT, 0) + 1
        prefs.edit().putInt(KEY_OPEN_COUNT, count).apply()

        if (count >= REQUIRED_OPENS) {
            prefs.edit().putBoolean(KEY_PROMPTED, true).apply()
            launchReviewFlow(activity)
        }
    }

    private fun launchReviewFlow(activity: Activity) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(activity, task.result)
                    Timber.d("ReviewHelper: review flow launched")
                } else {
                    Timber.d("ReviewHelper: review request failed: %s", task.exception?.message)
                }
            }
        } catch (e: Exception) {
            Timber.d("ReviewHelper: not available: %s", e.message)
        }
    }
}
