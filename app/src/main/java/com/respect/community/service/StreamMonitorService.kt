package com.respect.community.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.respect.community.MainActivity
import com.respect.community.R
import com.respect.community.RespectCommunityApp
import com.respect.community.data.local.NotificationPrefs
import com.respect.community.data.local.entity.StreamerEntity
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

        val notification = buildMonitorNotification("جاري مراقبة ستريمرز RESPECT كل 15 ثانية...")
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
                        if (!prefs.favoritesOnly || streamer.isFavorite || streamer.isFollowed) {
                            sendLiveNotification(streamer, prefs)
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

        return NotificationCompat.Builder(this, RespectCommunityApp.CHANNEL_MONITOR_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("مجتمع RESPECT • تنبيهات KICK")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(0xFF8B5CF6.toInt())
            .setContentIntent(openPending)
            .addAction(0, "إيقاف", stopPending)
            .build()
    }

    private fun sendLiveNotification(streamer: StreamerEntity, prefs: NotificationPrefs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SLUG, streamer.slug)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            streamer.slug.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, RespectCommunityApp.CHANNEL_LIVE_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${streamer.displayName} بدأ البث المباشر الآن!")
            .setContentText(streamer.streamTitle.ifBlank { "اضغط للمشاهدة عبر RESPECT Community" })
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${streamer.streamTitle}\n${streamer.categoryName} • ${streamer.viewerCount} مشاهد"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setColor(0xFF8B5CF6.toInt())
            .setContentIntent(pendingIntent)

        if (prefs.soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSilent(true)
        }
        if (prefs.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0L, 300L, 120L, 300L))
        }

        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(LIVE_NOTIFICATION_BASE_ID + streamer.slug.hashCode(), builder.build())
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
        const val EXTRA_OPEN_SLUG = "extra_open_slug"
        private const val MONITOR_NOTIFICATION_ID = 7001
        private const val LIVE_NOTIFICATION_BASE_ID = 8000
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
