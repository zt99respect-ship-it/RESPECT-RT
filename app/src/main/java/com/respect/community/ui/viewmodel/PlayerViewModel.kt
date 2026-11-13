package com.respect.community.ui.viewmodel

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.respect.community.data.local.KickAuthSession
import com.respect.community.data.local.entity.ChatMessageEntity
import com.respect.community.data.local.entity.StreamerEntity
import com.respect.community.data.remote.ChatConnectionState
import com.respect.community.data.repository.CommunityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class StreamQuality(val label: String, val maxWidth: Int, val maxHeight: Int) {
    AUTO("تلقائي (Auto)", Int.MAX_VALUE, Int.MAX_VALUE),
    Q1080P("1080p", 1920, 1080),
    Q720P("720p", 1280, 720),
    Q480P("480p", 854, 480)
}

@OptIn(UnstableApi::class)
class PlayerViewModel(
    appContext: Context,
    private val slug: String,
    private val repository: CommunityRepository
) : ViewModel() {

    private val trackSelector = DefaultTrackSelector(appContext)
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setTrackSelector(trackSelector)
        .build()

    val streamer: StateFlow<StreamerEntity?> = repository.observeStreamer(slug)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val kickChatMessages: StateFlow<List<ChatMessageEntity>> = repository.observeKickChat(slug)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatConnectionState: StateFlow<ChatConnectionState> = repository.kickChatConnectionState

    val kickAuthSession: StateFlow<KickAuthSession> = repository.kickAuthSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KickAuthSession())

    private val _selectedQuality = MutableStateFlow(StreamQuality.AUTO)
    val selectedQuality: StateFlow<StreamQuality> = _selectedQuality.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private val _chatSendError = MutableStateFlow<String?>(null)
    val chatSendError: StateFlow<String?> = _chatSendError.asStateFlow()

    private var loadedPlaybackUrl: String = ""
    private var connectedChatroomId: Long = 0L
    private var pollingJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                _playerError.value = "تعذر تشغيل البث (${error.errorCodeName})"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) _playerError.value = null
            }
        })

        viewModelScope.launch { repository.addOrRefreshStreamer(slug) }

        viewModelScope.launch {
            streamer.collect { entity ->
                if (entity != null) {
                    if (entity.isLive && entity.playbackUrl.isNotBlank() && entity.playbackUrl != loadedPlaybackUrl) {
                        loadStream(entity.playbackUrl)
                    }
                    if ((entity.chatroomId > 0L || entity.broadcasterUserId > 0L) &&
                        entity.chatroomId != connectedChatroomId
                    ) {
                        connectedChatroomId = entity.chatroomId
                        repository.connectKickChat(
                            slug = entity.slug,
                            chatroomId = entity.chatroomId,
                            channelId = entity.broadcasterUserId
                        )
                    }
                }
            }
        }

        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000L)
                runCatching { repository.addOrRefreshStreamer(slug) }
            }
        }
    }

    private fun loadStream(url: String) {
        loadedPlaybackUrl = url
        _playerError.value = null
        val item = MediaItem.Builder()
            .setUri(url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        exoPlayer.setMediaItem(item)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun setQuality(quality: StreamQuality) {
        _selectedQuality.value = quality
        val builder = trackSelector.buildUponParameters()
        if (quality == StreamQuality.AUTO) {
            builder.clearVideoSizeConstraints().setMaxVideoBitrate(Int.MAX_VALUE)
        } else {
            builder.setMaxVideoSize(quality.maxWidth, quality.maxHeight)
        }
        trackSelector.setParameters(builder.build())
    }

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        exoPlayer.volume = if (next) 0f else 1f
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun sendKickChat(content: String) {
        val current = streamer.value ?: return
        viewModelScope.launch {
            _chatSendError.value = null
            repository.sendKickChatMessage(current.broadcasterUserId, current.slug, content)
                .onFailure { _chatSendError.value = it.message ?: "تعذر إرسال الرسالة" }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        repository.disconnectKickChat()
        exoPlayer.release()
        super.onCleared()
    }

    class Factory(
        private val appContext: Context,
        private val slug: String,
        private val repository: CommunityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(appContext, slug, repository) as T
        }
    }
}
