package com.respect.community.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respect.community.data.local.entity.AnalysisEntity
import com.respect.community.data.local.entity.ArtEntity
import com.respect.community.data.local.entity.ChatMessageEntity
import com.respect.community.data.local.entity.ClipEntity
import com.respect.community.data.local.entity.TimelinePostEntity
import com.respect.community.ui.components.GlassFilterChip
import com.respect.community.ui.components.ScaleBounceButton
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary

@Composable
fun CommunityHubScreen(
    timelinePosts: List<TimelinePostEntity>,
    generalChat: List<ChatMessageEntity>,
    analyses: List<AnalysisEntity>,
    clips: List<ClipEntity>,
    artworks: List<ArtEntity>,
    isAdmin: Boolean,
    onUploadImage: suspend (Context, Uri) -> Result<String>,
    onCreatePost: (String, String, String, Boolean) -> Unit,
    onTogglePostLike: (TimelinePostEntity) -> Unit,
    onDeletePost: (Long) -> Unit,
    onBanUserOneDay: (String, String, String) -> Unit,
    onSendGeneralChat: (String) -> Unit,
    onDeleteChatMessage: (String) -> Unit,
    onAddAnalysis: (String, String, String, String, Int) -> Unit,
    onDeleteAnalysis: (Long) -> Unit,
    onAddClip: (String, String, String, String) -> Unit,
    onToggleClipLike: (ClipEntity) -> Unit,
    onDeleteClip: (Long) -> Unit,
    onAddArt: (String, String, String) -> Unit,
    onToggleArtLike: (ArtEntity) -> Unit,
    onDeleteArt: (Long) -> Unit
) {
    // 0: Timeline, 1: Chat, 2: Analysis, 3: Clips, 4: Art
    var activeSection by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var showComposePost by remember { mutableStateOf(false) }
    var showAddAnalysis by remember { mutableStateOf(false) }
    var showAddClip by remember { mutableStateOf(false) }
    var showAddArt by remember { mutableStateOf(false) }

    val q = searchQuery.trim()
    val filteredPosts = remember(timelinePosts, q) {
        if (q.isBlank()) timelinePosts else timelinePosts.filter {
            it.content.contains(q, ignoreCase = true) || it.authorName.contains(q, ignoreCase = true)
        }
    }
    val filteredClips = remember(clips, q) {
        if (q.isBlank()) clips else clips.filter {
            it.title.contains(q, ignoreCase = true) || it.streamerName.contains(q, ignoreCase = true)
        }
    }
    val filteredArt = remember(artworks, q) {
        if (q.isBlank()) artworks else artworks.filter {
            it.title.contains(q, ignoreCase = true) || it.artistName.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 92.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("مجتمع RESPECT", style = MaterialTheme.typography.headlineMedium)

                when (activeSection) {
                    0 -> ScaleBounceButton(onClick = { showComposePost = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("منشور", fontSize = 12.sp)
                    }
                    2 -> ScaleBounceButton(onClick = { showAddAnalysis = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحليل", fontSize = 12.sp)
                    }
                    3 -> ScaleBounceButton(onClick = { showAddClip = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("كليب", fontSize = 12.sp)
                    }
                    4 -> ScaleBounceButton(onClick = { showAddArt = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رسمة", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeSection != 1) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في التايملاين والكليبات والفن...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RespectPurpleBright,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassFilterChip("التايملاين", activeSection == 0, { activeSection = 0 })
                GlassFilterChip("شات السوالف", activeSection == 1, { activeSection = 1 })
                GlassFilterChip("تحليل البث", activeSection == 2, { activeSection = 2 })
                GlassFilterChip("الكليبات", activeSection == 3, { activeSection = 3 })
                GlassFilterChip("معرض الفن", activeSection == 4, { activeSection = 4 })
            }
        }

        when (activeSection) {
            0 -> TimelineFeedContent(
                posts = filteredPosts,
                isAdmin = isAdmin,
                onTogglePostLike = onTogglePostLike,
                onDeletePost = onDeletePost,
                onBanUserOneDay = onBanUserOneDay
            )
            1 -> GeneralChatContent(
                messages = generalChat,
                isAdmin = isAdmin,
                onSendGeneralChat = onSendGeneralChat,
                onDeleteChatMessage = onDeleteChatMessage
            )
            2 -> StreamAnalysisContent(
                analyses = analyses,
                isAdmin = isAdmin,
                onDeleteAnalysis = onDeleteAnalysis
            )
            3 -> ClipsVerticalFeed(
                clips = filteredClips,
                isAdmin = isAdmin,
                onToggleLike = onToggleClipLike,
                onDelete = onDeleteClip,
                modifier = Modifier.fillMaxSize()
            )
            else -> ArtPinterestGrid(
                artworks = filteredArt,
                isAdmin = isAdmin,
                onToggleLike = onToggleArtLike,
                onDelete = onDeleteArt,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showComposePost) {
        ComposeTimelineDialog(
            isAdmin = isAdmin,
            onUploadImage = onUploadImage,
            onDismiss = { showComposePost = false },
            onPublish = { text, mediaUrl, mediaType, announce ->
                onCreatePost(text, mediaUrl, mediaType, announce)
                showComposePost = false
            }
        )
    }

    if (showAddAnalysis) {
        AddAnalysisDialog(
            onDismiss = { showAddAnalysis = false },
            onSave = { slug, name, title, body, rating ->
                onAddAnalysis(slug, name, title, body, rating)
                showAddAnalysis = false
            }
        )
    }

    if (showAddClip) {
        AddClipDialog(
            onDismiss = { showAddClip = false },
            onAdd = { slug, name, title, url ->
                onAddClip(slug, name, title, url)
                showAddClip = false
            }
        )
    }

    if (showAddArt) {
        AddArtDialog(
            onUploadImage = onUploadImage,
            onDismiss = { showAddArt = false },
            onAdd = { title, artist, url ->
                onAddArt(title, artist, url)
                showAddArt = false
            }
        )
    }
}
