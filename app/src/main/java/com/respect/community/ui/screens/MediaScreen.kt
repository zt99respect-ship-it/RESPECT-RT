package com.respect.community.ui.screens

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.respect.community.data.local.entity.ArtEntity
import com.respect.community.data.local.entity.ClipEntity
import com.respect.community.ui.components.HazeGlassCard
import com.respect.community.ui.components.RespectImage
import com.respect.community.ui.components.ScaleBounceButton
import com.respect.community.ui.components.TelegraphImagePickerButton
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.RespectSurface
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class)
@Composable
fun ClipsVerticalFeed(
    clips: List<ClipEntity>,
    isAdmin: Boolean,
    onToggleLike: (ClipEntity) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (clips.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            HazeGlassCard { Text("لا توجد كليبات مضافة بعد", style = MaterialTheme.typography.titleMedium) }
        }
        return
    }

    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { clips.size })
    var isMuted by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED)
                    .build()
            )
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 30_000, 1_000, 2_500)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(pagerState.currentPage, clips) {
        val clip = clips.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(clip.videoUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = modifier.padding(horizontal = 12.dp)
    ) { page ->
        val clip = clips[page]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
        ) {
            if (page == pagerState.currentPage) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            subtitleView?.visibility = View.GONE
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                RespectImage(
                    url = clip.thumbnailUrl,
                    contentDescription = clip.title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = CircleShape, color = Color(0x88090B10)) {
                    IconButton(onClick = { onToggleLike(clip) }) {
                        Icon(
                            imageVector = if (clip.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "إعجاب",
                            tint = if (clip.isLikedByMe) Color(0xFFF43F5E) else TextPrimary
                        )
                    }
                }
                Text("${clip.likesCount}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Surface(shape = CircleShape, color = Color(0x88090B10)) {
                    IconButton(onClick = { isMuted = !isMuted }) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "الصوت",
                            tint = TextPrimary
                        )
                    }
                }

                if (isAdmin) {
                    Surface(shape = CircleShape, color = Color(0x88090B10)) {
                        IconButton(onClick = { onDelete(clip.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFCA5A5))
                        }
                    }
                }
            }

            HazeGlassCard(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, end = 78.dp, bottom = 14.dp),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(12.dp)
            ) {
                Text(
                    text = "@${clip.streamerName}",
                    color = RespectPurpleBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(clip.title, color = TextPrimary, fontSize = 14.sp)
                Text("رفع بواسطة: ${clip.uploadedBy}", color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ArtPinterestGrid(
    artworks: List<ArtEntity>,
    isAdmin: Boolean,
    onToggleLike: (ArtEntity) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp
    ) {
        items(artworks, key = { it.id }) { art ->
            HazeGlassCard(cornerRadius = 20.dp, contentPadding = PaddingValues(10.dp)) {
                RespectImage(
                    url = art.imageUrl,
                    contentDescription = art.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(art.aspectRatio.coerceIn(0.65f, 1.55f))
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(art.title, style = MaterialTheme.typography.titleMedium, fontSize = 14.sp)
                Text("الفنان: ${art.artistName}", color = TextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onToggleLike(art) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = if (art.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "إعجاب",
                                tint = if (art.isLikedByMe) Color(0xFFF43F5E) else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text("${art.likesCount}", color = TextSecondary, fontSize = 12.sp)
                    }

                    if (isAdmin) {
                        IconButton(onClick = { onDelete(art.id) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "حذف",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddClipDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var streamerName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RespectSurface,
        title = { Text("نشر كليب جديد", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = streamerName, onValueChange = { streamerName = it }, label = { Text("اسم الستريمر") }, singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان اللحظة") }, singleLine = true)
                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("رابط الفيديو (MP4 / HLS)") }, singleLine = true)
            }
        },
        confirmButton = {
            ScaleBounceButton(onClick = { onAdd(streamerName, streamerName, title, videoUrl) }) {
                Text("نشر الكليب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}

@Composable
fun AddArtDialog(
    onUploadImage: suspend (Context, Uri) -> Result<String>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RespectSurface,
        title = { Text("إضافة رسمة لمعرض الفن", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان العمل الفني") }, singleLine = true)
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("اسم الرسام") }, singleLine = true)
                TelegraphImagePickerButton(
                    label = "رفع الرسمة عبر Telegraph",
                    onUploadImage = onUploadImage,
                    onUploadedUrl = { imageUrl = it }
                )
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("رابط الصورة (Telegraph / URL)") }, singleLine = true)
            }
        },
        confirmButton = {
            ScaleBounceButton(onClick = { onAdd(title, artist, imageUrl) }) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}
