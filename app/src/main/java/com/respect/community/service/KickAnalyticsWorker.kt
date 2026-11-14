package com.respect.community.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.respect.community.RespectCommunityApp
import java.util.concurrent.TimeUnit

class KickAnalyticsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RespectCommunityApp ?: return Result.success()
        val repository = app.container.repository
        val sessionManager = app.container.sessionManager

        return try {
            val newlyLive = repository.refreshAllStreamersAndRecordSnapshots()
            val prefs = sessionManager.getNotificationPrefsOnce()
            newlyLive.forEach { streamer ->
                if (streamer.isNotifying && (!prefs.favoritesOnly || streamer.isFavorite || streamer.isFollowed)) {
                    NotificationHelper.showKickLiveNotification(applicationContext, streamer, prefs)
                }
            }
            scheduleNext10MinRun(applicationContext)
            Result.success()
        } catch (_: Throwable) {
            scheduleNext10MinRun(applicationContext)
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_NAME = "respect_kick_analytics_periodic"
        private const val UNIQUE_10MIN_NAME = "respect_kick_analytics_10min"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodic = PeriodicWorkRequestBuilder<KickAnalyticsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic
            )

            scheduleNext10MinRun(context)
        }

        private fun scheduleNext10MinRun(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val nextRun = OneTimeWorkRequestBuilder<KickAnalyticsWorker>()
                .setInitialDelay(10, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_10MIN_NAME,
                ExistingWorkPolicy.REPLACE,
                nextRun
            )
        }
    }
}
