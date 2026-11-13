package com.respect.community

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.respect.community.di.AppContainer

class RespectCommunityApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val liveChannel = NotificationChannel(
            CHANNEL_LIVE_ALERTS,
            getString(R.string.channel_live_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_live_desc)
            enableLights(true)
            lightColor = 0xFF8B5CF6.toInt()
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 280L, 120L, 280L)
            setSound(alertSound, audioAttributes)
        }

        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR_SERVICE,
            getString(R.string.channel_monitor_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_monitor_desc)
            setShowBadge(false)
        }

        manager.createNotificationChannel(liveChannel)
        manager.createNotificationChannel(monitorChannel)
    }

    companion object {
        const val CHANNEL_LIVE_ALERTS = "respect_community_live_alerts"
        const val CHANNEL_MONITOR_SERVICE = "respect_community_monitor"
    }
}
