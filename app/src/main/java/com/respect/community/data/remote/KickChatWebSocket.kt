package com.respect.community.data.remote

import com.google.gson.JsonParser
import com.respect.community.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID

enum class ChatConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    POLLING_FALLBACK
}

class KickChatWebSocket(
    private val okHttpClient: OkHttpClient,
    private val kickV2Api: KickV2Api
) {
    companion object {
        private const val PUSHER_URL =
            "wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679?protocol=7&client=js&version=7.6.0&flash=false"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var pollingJob: Job? = null

    private var activeSlug: String = ""
    private var activeChatroomId: Long = 0L
    private var activeChannelId: Long = 0L

    private val _connectionState = MutableStateFlow(ChatConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ChatConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ChatMessageEntity>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ChatMessageEntity> = _incomingMessages.asSharedFlow()

    fun connect(slug: String, chatroomId: Long, channelId: Long = 0L) {
        if (chatroomId <= 0L && channelId <= 0L) {
            _connectionState.value = ChatConnectionState.DISCONNECTED
            return
        }
        if (activeChatroomId == chatroomId && _connectionState.value == ChatConnectionState.CONNECTED) {
            return
        }

        disconnect()
        activeSlug = slug
        activeChatroomId = chatroomId
        activeChannelId = channelId
        _connectionState.value = ChatConnectionState.CONNECTING

        if (chatroomId > 0L) {
            val request = Request.Builder().url(PUSHER_URL).build()
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    subscribe(webSocket, chatroomId)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleFrame(webSocket, text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    startPollingFallback()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (activeChatroomId == chatroomId) {
                        _connectionState.value = ChatConnectionState.DISCONNECTED
                    }
                }
            })
        } else {
            startPollingFallback()
        }
    }

    private fun startPollingFallback() {
        val targetId = if (activeChannelId > 0L) activeChannelId else activeChatroomId
        if (targetId <= 0L || pollingJob?.isActive == true) return
        _connectionState.value = ChatConnectionState.POLLING_FALLBACK

        pollingJob = scope.launch {
            while (isActive) {
                runCatching {
                    val resp = kickV2Api.getChatMessages(targetId)
                    if (resp.isSuccessful) {
                        resp.body()?.data?.messages.orEmpty().forEach { item ->
                            val content = item.content?.trim().orEmpty()
                            if (content.isNotBlank()) {
                                _incomingMessages.tryEmit(
                                    ChatMessageEntity(
                                        id = item.id ?: UUID.randomUUID().toString(),
                                        roomKey = "kick:$activeSlug",
                                        senderName = item.sender?.username ?: "مستخدم",
                                        senderColor = item.sender?.identity?.color
                                            ?.takeIf { it.startsWith("#") } ?: "#A78BFA",
                                        isAdmin = false,
                                        content = content,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
                delay(5_000L)
            }
        }
    }

    private fun subscribe(ws: WebSocket, chatroomId: Long) {
        val payload = JSONObject().apply {
            put("event", "pusher:subscribe")
            put("data", JSONObject().apply {
                put("auth", "")
                put("channel", "chatrooms.$chatroomId.v2")
            })
        }
        ws.send(payload.toString())
    }

    private fun handleFrame(ws: WebSocket, raw: String) {
        try {
            val root = JsonParser.parseString(raw).asJsonObject
            val event = root.get("event")?.asString ?: return
            when {
                event == "pusher:connection_established" -> subscribe(ws, activeChatroomId)
                event == "pusher_internal:subscription_succeeded" -> {
                    _connectionState.value = ChatConnectionState.CONNECTED
                }
                event == "pusher:ping" -> {
                    ws.send(JSONObject().apply {
                        put("event", "pusher:pong")
                        put("data", JSONObject())
                    }.toString())
                }
                event.contains("ChatMessageEvent") -> {
                    val dataStr = root.get("data")?.asString ?: return
                    val msgObj = JsonParser.parseString(dataStr).asJsonObject
                    val content = msgObj.get("content")?.asString?.trim().orEmpty()
                    if (content.isBlank()) return
                    val msgId = msgObj.get("id")?.asString ?: UUID.randomUUID().toString()
                    val senderObj = msgObj.getAsJsonObject("sender")
                    val username = senderObj?.get("username")?.asString ?: "مستخدم"
                    val color = senderObj?.getAsJsonObject("identity")
                        ?.get("color")?.asString?.takeIf { it.startsWith("#") } ?: "#A78BFA"

                    _incomingMessages.tryEmit(
                        ChatMessageEntity(
                            id = msgId,
                            roomKey = "kick:$activeSlug",
                            senderName = username,
                            senderColor = color,
                            isAdmin = false,
                            content = content,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        try {
            webSocket?.close(1000, "Closed")
        } catch (_: Exception) {
        }
        webSocket = null
        activeChatroomId = 0L
        activeChannelId = 0L
        _connectionState.value = ChatConnectionState.DISCONNECTED
    }
}
