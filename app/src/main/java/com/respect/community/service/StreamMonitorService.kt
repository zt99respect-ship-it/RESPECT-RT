package com.respect.community.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.respect.community.MainActivity
import com.respect.community.R
import com.respect.community.RespectCommunityApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StreamMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        NotificationHelper.ensureChannels(this)
        val notification = buildMonitorNotification("جاري مراقبة ستريمرز RESPECT...")
        ServiceCompat.startForeground(
            this,
            MONITOR_NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )

        startPollingLoop()
        return START_STICKY
    }

    private fun startPollingLoop() {
        if (pollingJob?.isActive == true) return
        val app = application as RespectCommunityApp
        val repository = app.container.repository
        val sessionManager = app.container.sessionManager

        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val prefs = sessionManager.getNotificationPrefsOnce()
                    if (!prefs.monitoringEnabled) {
                        stopMonitoring()
                        break
                    }

                    val remoteSettings = repository.getRemoteSettingsOnce()
                    val pollMs = remoteSettings.pollIntervalSeconds.coerceIn(10, 120) * 1000L

                    val newlyLive = repository.refreshAllStreamers()
                    val allTracked = app.container.database.streamerDao().getAllOnce()
                    val liveCount = allTracked.count { it.isLive }

                    updateMonitorNotification(
                        "مراقبة ${allTracked.size} ستريمر • $liveCount مباشر الآن"
                    )

                    newlyLive.forEach { streamer ->
                        if (streamer.isNotifying && (!prefs.favoritesOnly || streamer.isFavorite || streamer.isFollowed)) {
                            NotificationHelper.showKickLiveNotification(this@StreamMonitorService, streamer, prefs)
                        }
                    }
                    delay(pollMs)
                } catch (_: Exception) {
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }

    private fun updateMonitorNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(MONITOR_NOTIFICATION_ID, buildMonitorNotification(text))
    }

    private fun buildMonitorNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 100, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, StreamMonitorService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 101, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_MONITOR_SERVICE)
            .setSmallIcon(R.drawable.ic_kick_logo)
            .setContentTitle("مجتمع RESPECT • تنبيهات KICK")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(0xFF8B5CF6.toInt())
            .setContentIntent(openPending)
            .addAction(0, "إيقاف", stopPending)
            .build()
    }

    private fun stopMonitoring() {
        pollingJob?.cancel()
        pollingJob = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.respect.community.action.START_MONITOR"
        const val ACTION_STOP = "com.respect.community.action.STOP_MONITOR"
        const val EXTRA_OPEN_SLUG = NotificationHelper.EXTRA_OPEN_SLUG
        private const val MONITOR_NOTIFICATION_ID = 7001
        private const val POLL_INTERVAL_MS = 15_000L

        fun start(context: Context) {
            val intent = Intent(context, StreamMonitorService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StreamMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
