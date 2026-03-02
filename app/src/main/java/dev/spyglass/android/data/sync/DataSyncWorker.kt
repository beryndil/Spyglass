package dev.spyglass.android.data.sync

import android.content.Context
import androidx.work.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that syncs game data from GitHub.
 * Runs every 12 hours when network is available.
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

        /** Enqueues a periodic sync worker (every 12 hours, requires network). */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DataSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)

            Timber.d("DataSyncWorker: periodic sync enrolled (every 12h)")
        }
    }
}
