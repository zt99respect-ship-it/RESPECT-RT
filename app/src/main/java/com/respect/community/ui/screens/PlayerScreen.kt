package com.respect.community.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.respect.community.data.remote.ChatConnectionState
import com.respect.community.ui.components.HazeGlassCard
import com.respect.community.ui.components.LivePulseBadge
import com.respect.community.ui.components.RespectImage
import com.respect.community.ui.components.ScaleBounceButton
import com.respect.community.ui.components.formatCompactNumber
import com.respect.community.ui.theme.CyanAccent
import com.respect.community.ui.theme.KickEmerald
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.RespectSurface
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary
import com.respect.community.ui.theme.VoidBlack
import com.respect.community.ui.viewmodel.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onRequestKickLogin: () -> Unit
) {
    val streamer by viewModel.streamer.collectAsStateWithLifecycle()
    val messages by viewModel.kickChatMessages.collectAsStateWithLifecycle()
    val chatState by viewModel.chatConnectionState.collectAsStateWithLifecycle()
    val kickAuth by viewModel.kickAuthSession.collectAsStateWithLifecycle()
    val availableQualities by viewModel.availableQualities.collectAsStateWithLifecycle()
    val selectedQuality by viewModel.selectedQuality.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val playerError by viewModel.playerError.collectAsStateWithLifecycle()
    val chatSendError by viewModel.chatSendError.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity

    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }

    DisposableEffect(viewModel.exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        viewModel.exoPlayer.addListener(listener)
        onDispose { viewModel.exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val insets = WindowCompat.getInsetsController(window, view)
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insets.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insets.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let {
                WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler(enabled = isFullscreen) { viewModel.setFullscreen(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Box(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            }
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showControls = !showControls }
        ) {
            val current = streamer
            if (current?.isLive == true && current.playbackUrl.isNotBlank()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = viewModel.exoPlayer
                            useController = false
                            // Remove subtitle overlay completely
                            subtitleView?.visibility = View.GONE
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { pv ->
                        pv.subtitleView?.visibility = View.GONE
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RespectSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.LiveTv, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = current?.let { "${it.displayName} غير متصل بالبث حالياً" } ?: "جاري التحميل...",
                            color = TextSecondary
                        )
                    }
                }
            }

            if (isBuffering && current?.isLive == true) {
                CircularProgressIndicator(
                    color = RespectPurpleBright,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                )
            }

            if (playerError != null) {
                Surface(
                    color = Color(0xCC090B10),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Text(playerError.orEmpty(), color = Color(0xFFFCA5A5), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x4D000000))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { if (isFullscreen) viewModel.setFullscreen(false) else onBack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                        }
                        if (current?.isLive == true) LivePulseBadge()
                    }

                    if (current?.isLive == true && !isBuffering) {
                        IconButton(
                            onClick = {
                                if (viewModel.exoPlayer.isPlaying) viewModel.exoPlayer.pause() else viewModel.exoPlayer.play()
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0x99151924))
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = viewModel::toggleMute) {
                                Icon(
                                    if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "الصوت",
                                    tint = TextPrimary
                                )
                            }

                            Box {
                                Surface(
                                    onClick = { showQualityMenu = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xCC151924)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.HighQuality, contentDescription = "الجودة", tint = RespectPurpleBright, modifier = Modifier.size(18.dp))
                                        Text(selectedQuality.label, color = TextPrimary, fontSize = 12.sp)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false },
                                    containerColor = RespectSurface
                                ) {
                                    availableQualities.forEach { q ->
                                        val isSelected = q.id == selectedQuality.id
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    q.label,
                                                    color = if (isSelected) RespectPurpleBright else TextPrimary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setQuality(q)
                                                showQualityMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = viewModel::toggleFullscreen) {
                            Icon(
                                if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = "ملء الشاشة",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        if (!isFullscreen) {
            val current = streamer
            Surface(color = RespectSurface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RespectImage(
                        url = current?.avatarUrl.orEmpty(),
                        contentDescription = current?.displayName,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, RespectPurpleBright, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(current?.displayName ?: "بث KICK", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(current?.streamTitle.orEmpty().ifBlank { current?.categoryName ?: "" }, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(16.dp))
                        Text(formatCompactNumber(current?.viewerCount ?: 0), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("دردشة KICK المباشرة", style = MaterialTheme.typography.titleMedium)
                val (label, col) = when (chatState) {
                    ChatConnectionState.CONNECTED -> "WebSocket متصل" to KickEmerald
                    ChatConnectionState.POLLING_FALLBACK -> "تحديث تلقائي" to CyanAccent
                    ChatConnectionState.CONNECTING -> "جاري الاتصال..." to RespectPurpleBright
                    ChatConnectionState.DISCONNECTED -> "غير متصل" to TextMuted
                }
                Text(label, color = col, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val nameColor = remember(msg.senderColor) {
                        try {
                            Color(android.graphics.Color.parseColor(msg.senderColor))
                        } catch (_: Exception) {
                            RespectPurpleBright
                        }
                    }
                    HazeGlassCard(contentPadding = PaddingValues(10.dp), cornerRadius = 14.dp) {
                        Row {
                            Text("${msg.senderName}: ", color = nameColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(msg.content, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (chatSendError != null) {
                Text(chatSendError.orEmpty(), color = Color(0xFFFCA5A5), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }

            HazeGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(10.dp)
            ) {
                if (kickAuth.isLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("اكتب في دردشة KICK...", color = TextMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (chatInput.isNotBlank()) {
                                    viewModel.sendKickChat(chatInput)
                                    chatInput = ""
                                }
                            }),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (chatInput.isNotBlank()) {
                                viewModel.sendKickChat(chatInput)
                                chatInput = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "إرسال", tint = RespectPurpleBright)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("سجّل بحساب KICK لإرسال الرسائل", color = TextSecondary, fontSize = 13.sp)
                        ScaleBounceButton(onClick = onRequestKickLogin) {
                            Text("دخول KICK", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
