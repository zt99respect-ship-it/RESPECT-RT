package com.respect.community.ui.viewmodel

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
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

data class VideoQualityOption(
    val id: String,
    val label: String,
    val maxWidth: Int,
    val maxHeight: Int,
    val bitrate: Int = Int.MAX_VALUE,
    val groupIndex: Int = -1,
    val trackIndex: Int = -1
) {
    companion object {
        val AUTO = VideoQualityOption(
            id = "auto",
            label = "تلقائي",
            maxWidth = Int.MAX_VALUE,
            maxHeight = Int.MAX_VALUE
        )

        val DEFAULT_PRESETS = listOf(
            AUTO,
            VideoQualityOption("1080p", "دقة عالية 1080", 1920, 1080),
            VideoQualityOption("720p", "دقة متوسطة 720", 1280, 720),
            VideoQualityOption("480p", "دقة قياسية 480", 854, 480),
            VideoQualityOption("360p", "توفير 360", 640, 360)
        )
    }
}

@OptIn(UnstableApi::class)
class PlayerViewModel(
    appContext: Context,
    private val slug: String,
    private val repository: CommunityRepository
) : ViewModel() {

    private val trackSelector = DefaultTrackSelector(appContext).apply {
        setParameters(
            buildUponParameters()
                // Remove subtitles completely
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED)
                .build()
        )
    }

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("RespectCommunityAndroid/2.0")
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(15_000)
        .setAllowCrossProtocolRedirects(true)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15_000, // minBufferMs
            50_000, // maxBufferMs
            1_500,  // bufferForPlaybackMs
            3_000   // bufferForPlaybackAfterRebufferMs
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(10_000, true)
        .build()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .build()

    val streamer: StateFlow<StreamerEntity?> = repository.observeStreamer(slug)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val kickChatMessages: StateFlow<List<ChatMessageEntity>> = repository.observeKickChat(slug)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatConnectionState: StateFlow<ChatConnectionState> = repository.kickChatConnectionState

    val kickAuthSession: StateFlow<KickAuthSession> = repository.kickAuthSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KickAuthSession())

    private val _availableQualities = MutableStateFlow(VideoQualityOption.DEFAULT_PRESETS)
    val availableQualities: StateFlow<List<VideoQualityOption>> = _availableQualities.asStateFlow()

    private val _selectedQuality = MutableStateFlow(VideoQualityOption.AUTO)
    val selectedQuality: StateFlow<VideoQualityOption> = _selectedQuality.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

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
                _isBuffering.value = false
                _playerError.value = "تعذر تشغيل البث (${error.errorCodeName})"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    _playerError.value = null
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateDetectedQualities(tracks)
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
        _isBuffering.value = true

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(3_000)
                    .setMinPlaybackSpeed(0.95f)
                    .setMaxPlaybackSpeed(1.05f)
                    .build()
            )
            .build()

        val hlsSource = HlsMediaSource.Factory(httpDataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(mediaItem)

        exoPlayer.setMediaSource(hlsSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun updateDetectedQualities(tracks: Tracks) {
        val detected = mutableListOf<VideoQualityOption>()
        tracks.groups.forEachIndexed { gIdx, group ->
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (tIdx in 0 until group.length) {
                    if (!group.isTrackSupported(tIdx)) continue
                    val format = group.getTrackFormat(tIdx)
                    val height = format.height
                    val width = format.width
                    if (height > 0 && width > 0) {
                        val label = when {
                            height >= 1080 -> "دقة عالية ${height}"
                            height >= 720 -> "دقة متوسطة ${height}"
                            height >= 480 -> "دقة قياسية ${height}"
                            else -> "توفير ${height}"
                        }
                        detected.add(
                            VideoQualityOption(
                                id = "${height}_${gIdx}_$tIdx",
                                label = label,
                                maxWidth = width,
                                maxHeight = height,
                                bitrate = if (format.bitrate > 0) format.bitrate else Int.MAX_VALUE,
                                groupIndex = gIdx,
                                trackIndex = tIdx
                            )
                        )
                    }
                }
            }
        }

        val unique = detected
            .sortedByDescending { it.maxHeight }
            .distinctBy { it.maxHeight }

        _availableQualities.value = if (unique.isNotEmpty()) {
            listOf(VideoQualityOption.AUTO) + unique
        } else {
            VideoQualityOption.DEFAULT_PRESETS
        }
    }

    fun setQuality(quality: VideoQualityOption) {
        _selectedQuality.value = quality
        val builder = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)

        if (quality.id == VideoQualityOption.AUTO.id) {
            builder.clearVideoSizeConstraints()
                .setMaxVideoBitrate(Int.MAX_VALUE)
        } else {
            val videoGroups = exoPlayer.currentTracks.groups
            val matchedGroup = quality.groupIndex
                .takeIf { it in videoGroups.indices }
                ?.let { videoGroups[it] }
                ?.takeIf { it.type == C.TRACK_TYPE_VIDEO && quality.trackIndex in 0 until it.length }

            if (matchedGroup != null) {
                builder.addOverride(
                    TrackSelectionOverride(matchedGroup.mediaTrackGroup, listOf(quality.trackIndex))
                )
            }
            builder.setMaxVideoSize(quality.maxWidth, quality.maxHeight)
            if (quality.bitrate != Int.MAX_VALUE) {
                builder.setMaxVideoBitrate(quality.bitrate)
            }
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
