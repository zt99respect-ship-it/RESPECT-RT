package com.respect.community.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respect.community.data.local.entity.AnalysisEntity
import com.respect.community.data.local.entity.ChatMessageEntity
import com.respect.community.data.local.entity.TimelinePostEntity
import com.respect.community.ui.components.GlassFilterChip
import com.respect.community.ui.components.HazeGlassCard
import com.respect.community.ui.components.RespectImage
import com.respect.community.ui.components.ScaleBounceButton
import com.respect.community.ui.components.TelegraphImagePickerButton
import com.respect.community.ui.theme.CyanAccent
import com.respect.community.ui.theme.GoldAccent
import com.respect.community.ui.theme.RespectPurple
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.RespectSurface
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun TimelineFeedContent(
    posts: List<TimelinePostEntity>,
    isAdmin: Boolean,
    onTogglePostLike: (TimelinePostEntity) -> Unit,
    onDeletePost: (Long) -> Unit,
    onBanUserOneDay: (String, String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(posts, key = { it.id }) { post ->
            TimelinePostCard(
                post = post,
                isAdmin = isAdmin,
                onLike = { onTogglePostLike(post) },
                onDelete = { onDeletePost(post.id) },
                onBanOneDay = {
                    onBanUserOneDay(
                        post.authorIdentifier,
                        post.authorName,
                        "تقييد من الإدارة على منشور التايملاين"
                    )
                }
            )
        }
    }
}

@Composable
fun GeneralChatContent(
    messages: List<ChatMessageEntity>,
    isAdmin: Boolean,
    onSendGeneralChat: (String) -> Unit,
    onDeleteChatMessage: (String) -> Unit
) {
    var chatInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val nameColor = remember(msg.senderColor) {
                    try {
                        Color(android.graphics.Color.parseColor(msg.senderColor))
                    } catch (_: Exception) {
                        RespectPurpleBright
                    }
                }
                HazeGlassCard(contentPadding = PaddingValues(12.dp), cornerRadius = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = msg.senderName,
                                color = nameColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (msg.isAdmin) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RespectPurple.copy(alpha = 0.24f)
                                ) {
                                    Text(
                                        "إدارة",
                                        color = RespectPurpleBright,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (isAdmin) {
                            IconButton(
                                onClick = { onDeleteChatMessage(msg.id) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(msg.content, color = TextPrimary, fontSize = 14.sp)
                }
            }
        }

        HazeGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("سولف مع مجتمع RESPECT...", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (chatInput.isNotBlank()) {
                            onSendGeneralChat(chatInput)
                            chatInput = ""
                        }
                    }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (chatInput.isNotBlank()) {
                        onSendGeneralChat(chatInput)
                        chatInput = ""
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "إرسال",
                        tint = RespectPurpleBright
                    )
                }
            }
        }
    }
}

@Composable
fun StreamAnalysisContent(
    analyses: List<AnalysisEntity>,
    isAdmin: Boolean,
    onDeleteAnalysis: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(analyses, key = { it.id }) { item ->
            HazeGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.streamerName} • ${item.authorName}",
                        color = RespectPurpleBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            " ${item.rating}/10",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (isAdmin) {
                            IconButton(
                                onClick = { onDeleteAnalysis(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.analysisText, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun TimelinePostCard(
    post: TimelinePostEntity,
    isAdmin: Boolean,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    onBanOneDay: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    HazeGlassCard(isGlowing = post.isOfficialAnnouncement) {
        if (post.isOfficialAnnouncement) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = RespectPurple.copy(alpha = 0.20f),
                border = BorderStroke(1.dp, RespectPurpleBright.copy(alpha = 0.55f)),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Campaign, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(16.dp))
                    Text("إعلان رسمي من RESPECT", color = RespectPurpleBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RespectImage(
                    url = post.authorAvatarUrl,
                    contentDescription = post.authorName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Column {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (post.isAdminPost) RespectPurpleBright else TextPrimary
                    )
                    Text(
                        text = post.authorIdentifier,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            if (isAdmin) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "إشراف", tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = RespectSurface
                    ) {
                        DropdownMenuItem(
                            text = { Text("حذف المنشور", color = Color(0xFFFCA5A5)) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFFCA5A5)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                        if (!post.isAdminPost) {
                            DropdownMenuItem(
                                text = { Text("حظر العضو ليوم واحد", color = GoldAccent) },
                                leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null, tint = GoldAccent) },
                                onClick = {
                                    menuExpanded = false
                                    onBanOneDay()
                                }
                            )
                        }
                    }
                }
            }
        }

        if (post.content.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(post.content, style = MaterialTheme.typography.bodyLarge)
        }

        if (post.mediaUrl.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            if (post.mediaType == "VIDEO") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RespectSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = CyanAccent)
                        Text(post.mediaUrl, color = CyanAccent, fontSize = 12.sp, maxLines = 1)
                    }
                }
            } else {
                RespectImage(
                    url = post.mediaUrl,
                    contentDescription = "صورة المنشور",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLike, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = if (post.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "إعجاب",
                    tint = if (post.isLikedByMe) Color(0xFFF43F5E) else TextMuted
                )
            }
            Text("${post.likesCount}", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun ComposeTimelineDialog(
    isAdmin: Boolean,
    onUploadImage: suspend (Context, Uri) -> Result<String>,
    onDismiss: () -> Unit,
    onPublish: (String, String, String, Boolean) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("IMAGE") }
    var isAnnouncement by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RespectSurface,
        title = { Text("منشور جديد في التايملاين", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("اكتب تغريدتك للمجتمع...") },
                    minLines = 3
                )
                TelegraphImagePickerButton(
                    label = "اختر وارفع صورة via Telegraph",
                    onUploadImage = onUploadImage,
                    onUploadedUrl = { url ->
                        mediaUrl = url
                        mediaType = "IMAGE"
                    }
                )
                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = { Text("رابط صورة Telegraph أو فيديو (اختياري)") },
                    singleLine = true
                )
                if (mediaUrl.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassFilterChip("صورة", mediaType == "IMAGE", { mediaType = "IMAGE" })
                        GlassFilterChip("فيديو", mediaType == "VIDEO", { mediaType = "VIDEO" })
                    }
                }
                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isAnnouncement, onCheckedChange = { isAnnouncement = it })
                        Text("نشر كإعلان رسمي مثبت", color = RespectPurpleBright)
                    }
                }
            }
        },
        confirmButton = {
            ScaleBounceButton(onClick = { onPublish(content, mediaUrl, mediaType, isAnnouncement) }) {
                Text("نشر الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}

@Composable
fun AddAnalysisDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int) -> Unit
) {
    var streamerName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var rating by remember { mutableFloatStateOf(8f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RespectSurface,
        title = { Text("إضافة تحليل لبث", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = streamerName, onValueChange = { streamerName = it }, label = { Text("اسم الستريمر") }, singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان التحليل") }, singleLine = true)
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("رأيك وتحليلك للبث") }, minLines = 3)
                Text("التقييم: ${rating.roundToInt()}/10", color = GoldAccent, fontWeight = FontWeight.Bold)
                Slider(value = rating, onValueChange = { rating = it }, valueRange = 1f..10f, steps = 8)
            }
        },
        confirmButton = {
            ScaleBounceButton(onClick = { onSave(streamerName, streamerName, title, body, rating.roundToInt()) }) {
                Text("نشر التحليل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}
