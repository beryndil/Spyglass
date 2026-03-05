package dev.spyglass.android.data.sync

import android.content.Context
import androidx.work.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that syncs game data from GitHub.
 * Default interval is 12 hours; configurable via [enqueue].
 */
class DataSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            DataSyncManager.sync(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Timber.w(e, "DataSyncWorker failed")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "data_sync"

        /** Enqueues a periodic sync worker with the given interval (requires network).
         *  Random 0–60 min initial delay spreads server load across installs. */
        fun enqueue(context: Context, intervalHours: Int = 12) {
            val jitterMinutes = (0L..60L).random()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DataSyncWorker>(
                intervalHours.toLong().coerceAtLeast(1), TimeUnit.HOURS,
            )
                .setInitialDelay(jitterMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)

            Timber.d("DataSyncWorker: periodic sync enrolled (every %dh)", intervalHours)
        }
    }
}
