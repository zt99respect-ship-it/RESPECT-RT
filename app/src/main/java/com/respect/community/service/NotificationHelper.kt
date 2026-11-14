package com.respect.community.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.respect.community.MainActivity
import com.respect.community.R
import com.respect.community.data.local.NotificationPrefs
import com.respect.community.data.local.entity.StreamerEntity

object NotificationHelper {

    const val CHANNEL_LIVE_ALERTS = "respect_live_alerts_v2"
    const val CHANNEL_MONITOR_SERVICE = "respect_monitor_service_v2"
    const val EXTRA_OPEN_SLUG = "open_streamer_slug"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val soundUri = Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.respect_alert}"
        )
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val liveChannel = NotificationChannel(
            CHANNEL_LIVE_ALERTS,
            context.getString(R.string.channel_live_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_live_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 220, 140, 260)
            setSound(soundUri, audioAttrs)
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
        prefs: NotificationPrefs
    ) {
        ensureChannels(context)
        val title = "${streamer.displayName} بدأ بثه"
        val streamTitle = streamer.streamTitle.ifBlank { "انضم الآن لمشاهدة البث المباشر على كيك!" }
        val body = "العنوان: $streamTitle"
        showCustomKickPush(
            context = context,
            title = title,
            body = body,
            slug = streamer.slug,
            playSound = prefs.soundEnabled,
            vibrate = prefs.vibrationEnabled
        )
    }

    fun showTestKickNotification(context: Context) {
        showCustomKickPush(
            context = context,
            title = "DRB7H بدأ بثه",
            body = "العنوان: كافح المكافح عادد!",
            slug = "drb7h",
            playSound = true,
            vibrate = true
        )
    }

    fun showCustomKickPush(
        context: Context,
        title: String,
        body: String,
        slug: String? = null,
        playSound: Boolean = true,
        vibrate: Boolean = true
    ) {
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!slug.isNullOrBlank()) {
                putExtra(EXTRA_OPEN_SLUG, slug)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (slug ?: title).hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val kickBitmap = ContextCompat.getDrawable(context, R.drawable.ic_kick_logo)?.let { drawable ->
            val width = drawable.intrinsicWidth.coerceAtLeast(96)
            val height = drawable.intrinsicHeight.coerceAtLeast(96)
            android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888).also { bmp ->
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        } ?: BitmapFactory.decodeResource(context.resources, R.drawable.respect_logo)

        val soundUri = Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.respect_alert}"
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_LIVE_ALERTS)
            .setSmallIcon(R.drawable.ic_kick_logo)
            .setLargeIcon(kickBitmap)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(0xFF53FC18.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (playSound) {
            builder.setSound(soundUri)
        } else {
            builder.setSilent(true)
        }

        if (vibrate) {
            builder.setDefaults(Notification.DEFAULT_LIGHTS)
                .setVibrate(longArrayOf(0, 220, 140, 260))
        }

        val notifId = ((slug ?: title).hashCode() and 0x0FFF) + 8100
        manager.notify(notifId, builder.build())
    }
}
