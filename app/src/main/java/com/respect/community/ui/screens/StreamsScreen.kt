package com.respect.community.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respect.community.R
import com.respect.community.data.local.entity.ScheduleEventEntity
import com.respect.community.data.local.entity.StreamerEntity
import com.respect.community.ui.components.GlassFilterChip
import com.respect.community.ui.components.HazeGlassCard
import com.respect.community.ui.components.LivePulseBadge
import com.respect.community.ui.components.RespectImage
import com.respect.community.ui.components.ScaleBounceButton
import com.respect.community.ui.components.formatCompactNumber
import com.respect.community.ui.theme.CyanAccent
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.GoldAccent
import com.respect.community.ui.theme.KickEmerald
import com.respect.community.ui.theme.RespectPurple
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.RespectSurface
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary

@Composable
fun StreamsScreen(
    streamers: List<StreamerEntity>,
    scheduleEvents: List<ScheduleEventEntity>,
    bannerText: String,
    isAdmin: Boolean,
    isBusy: Boolean,
    onAddStreamer: (String, String) -> Unit,
    onRefreshAll: () -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onToggleFollow: (String, Boolean) -> Unit,
    onRemoveStreamer: (String) -> Unit,
    onWatchStreamer: (String) -> Unit,
    onSubmitSuggestion: (String, String, String) -> Unit,
    onSaveSchedule: (Long, String, String, String, String, String, String) -> Unit,
    onDeleteSchedule: (Long) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val parallaxOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                -(listState.firstVisibleItemScrollOffset * 0.25f)
            } else {
                -60f
            }
        }
    }

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: All, 1: Live only, 2: Schedule
    var searchFilter by remember { mutableStateOf("") }
    var addSlugInput by remember { mutableStateOf("") }
    var showSuggestDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleEventEntity?>(null) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    val liveCount = streamers.count { it.isLive }
    val filteredStreamers = remember(streamers, searchFilter, selectedSubTab) {
        streamers.filter { s ->
            val matchesTab = selectedSubTab != 1 || s.isLive
            val q = searchFilter.trim()
            val matchesQuery = q.isBlank() ||
                s.displayName.contains(q, ignoreCase = true) ||
                s.slug.contains(q, ignoreCase = true) ||
                s.characterNames.contains(q, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HazeGlassCard(isGlowing = false, cornerRadius = 24.dp) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.respect_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { translationY = parallaxOffset }
                            .clip(RoundedCornerShape(16.dp))
                            .alpha(0.16f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.respect_logo),
                                contentDescription = "RESPECT",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                            )
                            Column {
                                Text("مجتمع RESPECT", style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    "متابعة بثوث KICK والجدول",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = KickEmerald.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, KickEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "$liveCount مباشر",
                                color = KickEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        if (bannerText.isNotBlank()) {
            item {
                HazeGlassCard(isGlowing = true, cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Campaign, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(18.dp))
                        Text(bannerText, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassFilterChip("الكل (${streamers.size})", selectedSubTab == 0, { selectedSubTab = 0 })
                    GlassFilterChip("مباشر ($liveCount)", selectedSubTab == 1, { selectedSubTab = 1 })
                    GlassFilterChip("الجدول (${scheduleEvents.size})", selectedSubTab == 2, { selectedSubTab = 2 })
                }

                IconButton(onClick = onRefreshAll, enabled = !isBusy) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = RespectPurpleBright,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "تحديث", tint = TextSecondary)
                    }
                }
            }
        }

        if (selectedSubTab != 2) {
            item {
                HazeGlassCard(cornerRadius = 20.dp, contentPadding = PaddingValues(14.dp)) {
                    OutlinedTextField(
                        value = searchFilter,
                        onValueChange = { searchFilter = it },
                        placeholder = { Text("ابحث باسم الستريمر أو الشخصية...", color = TextMuted) },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = addSlugInput,
                            onValueChange = { addSlugInput = it },
                            placeholder = { Text("أضف قناة KICK (مثال: xqc)", color = TextMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (addSlugInput.isNotBlank()) {
                                    onAddStreamer(addSlugInput, "")
                                    addSlugInput = ""
                                }
                            }),
                            shape = RoundedCornerShape(13.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RespectPurpleBright,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        ScaleBounceButton(
                            onClick = {
                                if (addSlugInput.isNotBlank()) {
                                    onAddStreamer(addSlugInput, "")
                                    addSlugInput = ""
                                }
                            },
                            enabled = !isBusy && addSlugInput.isNotBlank(),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "إضافة")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showSuggestDialog = true },
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = RespectPurpleBright)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اقترح ستريمر جديد للمجتمع", color = TextPrimary)
                    }
                }
            }

            if (filteredStreamers.isEmpty()) {
                item {
                    HazeGlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.respect_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .alpha(0.75f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("لا يوجد ستريمرز مطابقون", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "أضف اسم قناة KICK بالأعلى أو عدّل البحث",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(filteredStreamers, key = { it.slug }) { streamer ->
                    HazeGlassCard(
                        isGlowing = streamer.isLive,
                        onClick = { onWatchStreamer(streamer.slug) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RespectImage(
                                url = streamer.avatarUrl,
                                contentDescription = streamer.displayName,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (streamer.isLive) KickEmerald else GlassBorder,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = streamer.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (streamer.isLive) LivePulseBadge()
                                }
                                Text(
                                    text = "@${streamer.slug} • ${streamer.categoryName}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                if (streamer.characterNames.isNotBlank()) {
                                    Text(
                                        text = "الشخصيات: ${streamer.characterNames}",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(onClick = { onToggleFavorite(streamer.slug, !streamer.isFavorite) }) {
                                Icon(
                                    imageVector = if (streamer.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "تنبيهات",
                                    tint = if (streamer.isFavorite) GoldAccent else TextMuted
                                )
                            }
                            IconButton(onClick = { onToggleFollow(streamer.slug, !streamer.isFollowed) }) {
                                Icon(
                                    imageVector = if (streamer.isFollowed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "متابعة",
                                    tint = if (streamer.isFollowed) Color(0xFFF43F5E) else TextMuted
                                )
                            }
                        }

                        if (streamer.streamTitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = streamer.streamTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = if (streamer.isLive) KickEmerald else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (streamer.isLive) {
                                        "${formatCompactNumber(streamer.viewerCount)} مشاهد"
                                    } else {
                                        "غير متصل"
                                    },
                                    fontSize = 12.sp,
                                    color = if (streamer.isLive) TextPrimary else TextMuted
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://kick.com/${streamer.slug}"))
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "رابط KICK", tint = TextSecondary)
                                }

                                if (isAdmin) {
                                    IconButton(
                                        onClick = { onRemoveStreamer(streamer.slug) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "حذف",
                                            tint = Color(0xFFFCA5A5)
                                        )
                                    }
                                }

                                ScaleBounceButton(onClick = { onWatchStreamer(streamer.slug) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (streamer.isLive) "مشاهدة" else "الشات", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (isAdmin) {
                item {
                    ScaleBounceButton(
                        onClick = { showAddScheduleDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة موعد بث جديد")
                    }
                }
            }

            items(scheduleEvents, key = { it.id }) { event ->
                HazeGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = RespectPurpleBright)
                            Text(event.streamerName, style = MaterialTheme.typography.titleMedium)
                        }
                        if (isAdmin) {
                            Row {
                                IconButton(onClick = { editingSchedule = event }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "تعديل", tint = TextSecondary)
                                }
                                IconButton(onClick = { onDeleteSchedule(event.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFCA5A5))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${event.scheduledDate} • ${event.scheduledTime} • ${event.gameCategory}",
                            color = CyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (event.predictionNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RespectSurface.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = "التوقعات: ${event.predictionNote}",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSuggestDialog) {
        SuggestStreamerDialog(
            onDismiss = { showSuggestDialog = false },
            onSubmit = { user, chars, reason ->
                onSubmitSuggestion(user, chars, reason)
                showSuggestDialog = false
            }
        )
    }

    if (showAddScheduleDialog || editingSchedule != null) {
        ScheduleEditorDialog(
            existing = editingSchedule,
            onDismiss = {
                showAddScheduleDialog = false
                editingSchedule = null
            },
            onSave = { id, slug, name, date, time, cat, pred ->
                onSaveSchedule(id, slug, name, date, time, cat, pred)
                showAddScheduleDialog = false
                editingSchedule = null
            }
        )
    }
}

@Composable
private fun SuggestStreamerDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var kickUser by remember { mutableStateOf("") }
    var characters by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RespectSurface,
        title = { Text("اقتراح ستريمر جديد", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = kickUser,
                    onValueChange = { kickUser = it },
                    label = { Text("اسم المستخدم في KICK") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = characters,
                    onValueChange = { characters = it },
                    label = { Text("أسماء الشخصيات في السيرفر") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("ملاحظة أو سبب الاقتراح") }
                )
            }
        },
        confirmButton = {
            ScaleBounceButton(onClick = { onSubmit(kickUser, characters, reason) }) {
                Text("إرسال الاقتراح")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}

@Composable
fun ScheduleEditorDialog(
    existing: ScheduleEventEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, String, String, String) -> Unit
) {
    var slug by remember { mutableStateOf(existing?.streamerSlug.orEmpty()) }
    var name by remember { mutableStateOf(existing?.streamerName.orEmpty()) }
    var date by remember { mutableStateOf(existing?.scheduledDate ?: "غداً") }
    var time by remember { mutableStateOf(existing?.scheduledTime ?: "22:00") }
    var category by remember { mutableStateOf(existing?.gameCategory ?: "GTA RP") }
    var prediction by remember { mutableStateOf(existing?.predictionNote.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RespectSurface,
        title = { Text(if (existing == null) "إضافة موعد بث" else "تعديل موعد البث", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الستريمر") }, singleLine = true)
                OutlinedTextField(value = slug, onValueChange = { slug = it }, label = { Text("معرّف KICK") }, singleLine = true)
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("التاريخ") }, singleLine = true)
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("الوقت") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("اللعبة / التصنيف") }, singleLine = true)
                OutlinedTextField(value = prediction, onValueChange = { prediction = it }, label = { Text("التوقعات قبل البث") })
            }
        },
        confirmButton = {
            ScaleBounceButton(onClick = { onSave(existing?.id ?: 0L, slug, name, date, time, category, prediction) }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}
