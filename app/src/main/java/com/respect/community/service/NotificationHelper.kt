package com.respect.community.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.respect.community.MainActivity
import com.respect.community.R
import com.respect.community.data.local.NotificationPrefs
import com.respect.community.data.local.entity.StreamerEntity

object NotificationHelper {

    const val CHANNEL_LIVE_ALERTS = "respect_live_custom_v2"
    const val CHANNEL_MONITOR_SERVICE = "respect_community_monitor"
    const val EXTRA_OPEN_SLUG = "extra_open_slug"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.respect_alert}")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val liveChannel = NotificationChannel(
            CHANNEL_LIVE_ALERTS,
            context.getString(R.string.channel_live_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_live_desc)
            enableLights(true)
            lightColor = 0xFF8B5CF6.toInt()
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 280L, 120L, 280L)
            setSound(soundUri, audioAttributes)
        }

        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR_SERVICE,
            context.getString(R.string.channel_monitor_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_monitor_desc)
            setShowBadge(false)
        }

        manager.createNotificationChannel(liveChannel)
        manager.createNotificationChannel(monitorChannel)
    }

    fun showKickLiveNotification(
        context: Context,
        streamer: StreamerEntity,
        prefs: NotificationPrefs = NotificationPrefs()
    ) {
        val title = "${streamer.displayName} بدأ بثه"
        val streamTitle = streamer.streamTitle.ifBlank { "بث مباشر الآن على منصة KICK!" }
        showCustomKickPush(
            context = context,
            title = title,
            body = "العنوان: $streamTitle",
            slug = streamer.slug,
            soundEnabled = prefs.soundEnabled,
            vibrationEnabled = prefs.vibrationEnabled,
            notificationId = 8000 + streamer.slug.hashCode()
        )
    }

    fun showTestKickNotification(context: Context) {
        showCustomKickPush(
            context = context,
            title = "DRB7H بدأ بثه",
            body = "العنوان: كافح المكافح عادد!",
            slug = "drb7h",
            soundEnabled = true,
            vibrationEnabled = true,
            notificationId = 8999
        )
    }

    fun showCustomKickPush(
        context: Context,
        title: String,
        body: String,
        slug: String = "",
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        notificationId: Int = (System.currentTimeMillis() % 100_000).toInt()
    ) {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (slug.isNotBlank()) putExtra(EXTRA_OPEN_SLUG, slug)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val kickBitmap = renderDrawableBitmap(context, R.drawable.ic_kick_logo)
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.respect_alert}")

        val builder = NotificationCompat.Builder(context, CHANNEL_LIVE_ALERTS)
            .setSmallIcon(R.drawable.ic_kick_logo)
            .setLargeIcon(kickBitmap)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setColor(0xFF8B5CF6.toInt())
            .setContentIntent(pendingIntent)

        if (soundEnabled) {
            builder.setSound(soundUri)
        } else {
            builder.setSilent(true)
        }
        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0L, 280L, 120L, 280L))
        }

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(notificationId, builder.build())
    }

    private fun renderDrawableBitmap(context: Context, drawableRes: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return null
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
