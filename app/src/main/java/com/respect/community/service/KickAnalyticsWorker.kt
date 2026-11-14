package com.respect.community.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
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
        val prefs = app.container.sessionManager.getNotificationPrefsOnce()

        return runCatching {
            val newlyLive = repository.refreshAllStreamersAndRecordSnapshots()
            newlyLive.forEach { streamer ->
                if (streamer.isNotifying && (!prefs.favoritesOnly || streamer.isFavorite || streamer.isFollowed)) {
                    NotificationHelper.showKickLiveNotification(applicationContext, streamer, prefs)
                }
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "respect_kick_analytics_10m"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<KickAnalyticsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
