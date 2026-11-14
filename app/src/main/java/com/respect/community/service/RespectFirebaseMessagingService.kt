package com.respect.community.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.respect.community.RespectCommunityApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RespectFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as? RespectCommunityApp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { app.container.repository.syncFcmToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "تنبيه مجتمع ريسبكت"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "تحديث جديد من البثوث المباشرة"
        val slug = message.data["slug"]

        NotificationHelper.showCustomKickPush(
            context = applicationContext,
            title = title,
            body = body,
            slug = slug,
            playSound = true,
            vibrate = true
        )
    }
}
