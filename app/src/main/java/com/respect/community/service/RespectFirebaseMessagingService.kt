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
        val app = application as? RespectCommunityApp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { app.container.repository.syncFcmToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val streamerName = message.data["streamerName"].orEmpty()
        val streamTitle = message.data["streamTitle"].orEmpty()
        val slug = message.data["slug"].orEmpty()

        val title = message.notification?.title
            ?: if (streamerName.isNotBlank()) "$streamerName بدأ بثه" else "إشعار RESPECT"
        val body = message.notification?.body
            ?: if (streamTitle.isNotBlank()) "العنوان: $streamTitle" else message.data["body"].orEmpty()

        if (title.isNotBlank() || body.isNotBlank()) {
            NotificationHelper.showCustomKickPush(
                context = applicationContext,
                title = title,
                body = body,
                slug = slug
            )
        }
    }
}
