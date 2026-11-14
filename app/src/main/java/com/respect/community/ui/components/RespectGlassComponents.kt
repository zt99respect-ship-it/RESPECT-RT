package com.respect.community.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.respect.community.R
import com.respect.community.ui.theme.DeepObsidian
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.GlassPurpleBorder
import com.respect.community.ui.theme.LiveCrimson
import com.respect.community.ui.theme.RespectPurple
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.RespectSurface
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary
import com.respect.community.ui.theme.VoidBlack
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import java.util.Locale

const val RESPECT_LOGO_URL = "https://e.top4top.io/p_3970nqvyb0.png"

val LocalRespectHazeState = compositionLocalOf<HazeState?> { null }

@Composable
fun respectGlassStyle(isGlowing: Boolean = false): HazeStyle {
    return HazeStyle(
        backgroundColor = VoidBlack,
        tint = HazeTint(color = Color.White.copy(alpha = if (isGlowing) 0.08f else 0.05f)),
        blurRadius = 24.dp,
        noiseFactor = 0.04f
    )
}

@Composable
fun RespectHazeContainer(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    parallaxY: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DeepObsidian,
                                VoidBlack,
                                VoidBlack
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = parallaxY }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RespectPurple.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 900f
                        )
                    )
            )
        }
        content()
    }
}

@Composable
fun HazeGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    isGlowing: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isGlowing) GlassPurpleBorder else Color.White.copy(alpha = 0.1f)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .let {
                if (onClick != null) {
                    it.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    it
                }
            },
        shape = shape,
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun RespectBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = 12.dp
) {
    AsyncImage(
        model = RESPECT_LOGO_URL,
        placeholder = painterResource(id = R.drawable.respect_logo),
        error = painterResource(id = R.drawable.respect_logo),
        contentDescription = "RESPECT",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
    )
}

@Composable
fun ScaleBounceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = RespectPurple,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = TextPrimary
        ),
        modifier = modifier.scale(scale),
        content = content
    )
}

@Composable
fun GlassFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(13.dp)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = if (selected) RespectPurple.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (selected) RespectPurple else Color.White.copy(alpha = 0.1f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TextPrimary else TextSecondary,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.04f)
        ),
        start = Offset(translate - 300f, 0f),
        end = Offset(translate, 200f)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun ShimmerCardSkeleton(modifier: Modifier = Modifier) {
    HazeGlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(modifier = Modifier.size(48.dp), cornerRadius = 24.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp), cornerRadius = 8.dp)
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.35f).height(10.dp), cornerRadius = 8.dp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp), cornerRadius = 8.dp)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.75f).height(12.dp), cornerRadius = 8.dp)
    }
}

@Composable
fun ErrorRetryState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    HazeGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = LiveCrimson,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = message.ifBlank { "حدث خطأ في الاتصال بالشبكة" },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            ScaleBounceButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إعادة المحاولة")
            }
        }
    }
}

@Composable
fun TelegraphImagePickerButton(
    onUploadImage: suspend (Context, Uri) -> Result<String>,
    onUploadedUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "رفع صورة عبر Telegraph"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                onUploadImage(context, uri).onSuccess { url ->
                    onUploadedUrl(url)
                }
                isUploading = false
            }
        }
    }

    OutlinedButton(
        onClick = {
            if (!isUploading) {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        },
        enabled = !isUploading,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        if (isUploading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = RespectPurple
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("جاري الضغط والرفع...", color = TextSecondary, fontSize = 12.sp)
        } else {
            Icon(
                Icons.Rounded.CloudUpload,
                contentDescription = null,
                tint = RespectPurpleBright,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = TextPrimary, fontSize = 12.sp)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
fun VideoLinkPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false
) {
    val lower = videoUrl.lowercase(Locale.US)
    val isSocialWebEmbed = lower.contains("tiktok.com") ||
        lower.contains("vm.tiktok.com") ||
        lower.contains("twitter.com") ||
        lower.contains("x.com")

    if (isSocialWebEmbed) {
        val embedUrl = remember(videoUrl) { buildSocialEmbedUrl(videoUrl) }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    setBackgroundColor(android.graphics.Color.BLACK)
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            return false
                        }
                    }
                    loadUrl(embedUrl)
                }
            },
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        )
    } else {
        val context = LocalContext.current
        val exoPlayer = remember(videoUrl) {
            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                )
            }
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 50_000, 1_500, 3_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val httpFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent("RespectCommunityAndroid/3.0")

            ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpFactory))
                .build().apply {
                    val mediaItem = MediaItem.fromUri(videoUrl)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = autoPlay
                    repeatMode = Player.REPEAT_MODE_ONE
                }
        }

        DisposableEffect(exoPlayer) {
            onDispose { exoPlayer.release() }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    subtitleView?.visibility = View.GONE
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { pv -> pv.subtitleView?.visibility = View.GONE },
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        )
    }
}

private fun buildSocialEmbedUrl(rawUrl: String): String {
    val clean = rawUrl.trim()
    val lower = clean.lowercase(Locale.US)
    return when {
        lower.contains("tiktok.com") -> {
            val match = Regex("""/video/(\d+)""").find(clean)
            val id = match?.groupValues?.getOrNull(1)
            if (id != null) "https://www.tiktok.com/embed/v2/$id" else clean
        }
        lower.contains("x.com") || lower.contains("twitter.com") -> {
            val normalized = clean.replace("https://x.com/", "https://twitter.com/")
            "https://platform.twitter.com/embed/Tweet.html?dnt=true&theme=dark&url=${Uri.encode(normalized)}"
        }
        else -> clean
    }
}

@Composable
fun LivePulseBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = LiveCrimson.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, LiveCrimson.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(LiveCrimson)
            )
            Text(
                text = "مباشر",
                color = Color(0xFFFCA5A5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RespectImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val model: Any = when (url) {
        "res://respect_banner" -> R.drawable.respect_banner
        "res://respect_logo" -> RESPECT_LOGO_URL
        "res://respect_shapes" -> R.drawable.respect_bg_shapes
        "" -> RESPECT_LOGO_URL
        else -> url
    }
    AsyncImage(
        model = model,
        placeholder = painterResource(id = R.drawable.respect_logo),
        error = painterResource(id = R.drawable.respect_logo),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

fun formatCompactNumber(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
