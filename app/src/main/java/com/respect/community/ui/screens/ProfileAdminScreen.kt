package com.respect.community.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.respect.community.data.local.KickAuthSession
import com.respect.community.data.local.NotificationPrefs
import com.respect.community.data.local.RemoteSettingsState
import com.respect.community.data.local.UserProfileState
import com.respect.community.data.local.entity.ArtEntity
import com.respect.community.data.local.entity.BannedUserEntity
import com.respect.community.data.local.entity.ClipEntity
import com.respect.community.data.local.entity.CommunityUserEntity
import com.respect.community.data.local.entity.ScheduleEventEntity
import com.respect.community.data.local.entity.StreamerEntity
import com.respect.community.data.local.entity.SuggestionEntity
import com.respect.community.data.local.entity.TimelinePostEntity
import com.respect.community.ui.components.GlassFilterChip
import com.respect.community.ui.components.HazeGlassCard
import com.respect.community.ui.components.RespectImage
import com.respect.community.ui.components.ScaleBounceButton
import com.respect.community.ui.components.TelegraphImagePickerButton
import com.respect.community.ui.theme.CyanAccent
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.GoldAccent
import com.respect.community.ui.theme.KickEmerald
import com.respect.community.ui.theme.RespectPurple
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary

@Composable
fun ProfileAdminScreen(
    profile: UserProfileState,
    kickSession: KickAuthSession,
    notificationPrefs: NotificationPrefs,
    remoteSettings: RemoteSettingsState,
    communityUsers: List<CommunityUserEntity>,
    streamers: List<StreamerEntity>,
    suggestions: List<SuggestionEntity>,
    scheduleEvents: List<ScheduleEventEntity>,
    timelinePosts: List<TimelinePostEntity>,
    clips: List<ClipEntity>,
    artworks: List<ArtEntity>,
    bannedUsers: List<BannedUserEntity>,
    onUploadImage: suspend (Context, Uri) -> Result<String>,
    onUpdateProfile: (String, String, String) -> Unit,
    onFirebaseSignIn: (String, String) -> Unit,
    onFirebaseSignUp: (String, String, String) -> Unit,
    onFirebaseSignOut: () -> Unit,
    onKickOAuthLogin: () -> Unit,
    onKickLogout: () -> Unit,
    onMonitoringChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onFavoritesOnlyChanged: (Boolean) -> Unit,
    onAdminLogin: (String, String) -> Unit,
    onAdminLogout: () -> Unit,
    onUpdateUserRole: (String, String) -> Unit,
    onMuteCommunityUser: (String, String, Boolean, String) -> Unit,
    onAddStreamer: (String, String) -> Unit,
    onRemoveStreamer: (String) -> Unit,
    onApproveSuggestion: (SuggestionEntity) -> Unit,
    onRejectSuggestion: (Long) -> Unit,
    onDeleteSuggestion: (Long) -> Unit,
    onSaveSchedule: (Long, String, String, String, String, String, String) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onDeletePost: (Long) -> Unit,
    onBanUserOneDay: (String, String, String) -> Unit,
    onUnbanUser: (String) -> Unit,
    onDeleteClip: (Long) -> Unit,
    onDeleteArt: (Long) -> Unit,
    onSaveGlobalSettings: (RemoteSettingsState) -> Unit,
    onRefreshRemoteConfig: () -> Unit
) {
    val context = LocalContext.current
    var section by remember { mutableIntStateOf(0) } // 0: Profile & Auth, 1: Admin Panel
    var adminTab by remember { mutableIntStateOf(0) } // 0: Users, 1: Moderation, 2: Streamers, 3: Settings

    var nameEdit by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var bioEdit by remember(profile.bio) { mutableStateOf(profile.bio) }
    var avatarEdit by remember(profile.avatarUrl) { mutableStateOf(profile.avatarUrl) }

    var authEmail by remember { mutableStateOf("") }
    var authPassword by remember { mutableStateOf("") }
    var authName by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var newStreamerSlug by remember { mutableStateOf("") }
    var newStreamerChars by remember { mutableStateOf("") }
    var showAddSchedule by remember { mutableStateOf(false) }

    var maintenanceDraft by remember(remoteSettings.maintenanceMode) { mutableStateOf(remoteSettings.maintenanceMode) }
    var bannerDraft by remember(remoteSettings.bannerText) { mutableStateOf(remoteSettings.bannerText) }
    var guestPostsDraft by remember(remoteSettings.allowGuestPosts) { mutableStateOf(remoteSettings.allowGuestPosts) }
    var pollIntervalDraft by remember(remoteSettings.pollIntervalSeconds) {
        mutableStateOf(remoteSettings.pollIntervalSeconds.toString())
    }

    var hasNotifPerm by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasNotifPerm = it
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("حسابي والإدارة", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassFilterChip("ملفي", section == 0, { section = 0 })
                    GlassFilterChip("الإدارة", section == 1, { section = 1 })
                }
            }
        }

        if (section == 0) {
            item {
                HazeGlassCard(isGlowing = profile.isFirebaseSignedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RespectImage(
                            url = profile.avatarUrl,
                            contentDescription = profile.displayName,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(2.dp, RespectPurpleBright, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                                if (profile.isAdmin) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = GoldAccent.copy(alpha = 0.22f)) {
                                        Text("أدمن", color = GoldAccent, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(profile.email, color = TextMuted, fontSize = 12.sp)
                            Text("المتابَعون: ${streamers.count { it.isFollowed }} ستريمر", color = CyanAccent, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TelegraphImagePickerButton(
                        label = "رفع صورة شخصية عبر Telegraph",
                        onUploadImage = onUploadImage,
                        onUploadedUrl = { uploaded -> avatarEdit = uploaded }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameEdit,
                        onValueChange = { nameEdit = it },
                        label = { Text("الاسم المعروض") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = avatarEdit,
                        onValueChange = { avatarEdit = it },
                        label = { Text("رابط الصورة الشخصية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bioEdit,
                        onValueChange = { bioEdit = it },
                        label = { Text("النبذة (Bio)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ScaleBounceButton(
                        onClick = { onUpdateProfile(nameEdit, bioEdit, avatarEdit) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ الملف الشخصي")
                    }
                }
            }

            item {
                HazeGlassCard {
                    Text("حساب Firebase Cloud", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (profile.isFirebaseSignedIn) {
                        Text("متصل بحساب Firebase: ${profile.email}", color = KickEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = onFirebaseSignOut, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الخروج من Firebase")
                        }
                    } else {
                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = authName,
                                onValueChange = { authName = it },
                                label = { Text("الاسم") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = authEmail,
                            onValueChange = { authEmail = it },
                            label = { Text("البريد الإلكتروني") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = authPassword,
                            onValueChange = { authPassword = it },
                            label = { Text("كلمة المرور") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ScaleBounceButton(
                            onClick = {
                                if (isRegisterMode) {
                                    onFirebaseSignUp(authEmail, authPassword, authName)
                                } else {
                                    onFirebaseSignIn(authEmail, authPassword)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isRegisterMode) "إنشاء حساب Firebase" else "دخول بحساب Firebase")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { isRegisterMode = !isRegisterMode },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isRegisterMode) "لديك حساب؟ تسجيل الدخول" else "عضو جديد؟ إنشاء حساب")
                        }
                    }
                }
            }

            item {
                HazeGlassCard(isGlowing = kickSession.isLoggedIn) {
                    Text("ربط حساب KICK OAuth2", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (kickSession.isLoggedIn) {
                        Text(
                            text = "متصل بالحساب: ${kickSession.kickUsername ?: "عضو KICK"}",
                            color = KickEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onKickLogout,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فصل حساب KICK")
                        }
                    } else {
                        Text(
                            "اربط حسابك عبر Chrome Custom Tabs للمشاركة مباشرة في دردشة البثوث.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ScaleBounceButton(
                            onClick = onKickOAuthLogin,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الدخول عبر KICK")
                        }
                    }
                }
            }

            item {
                HazeGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = RespectPurpleBright)
                        Text("الإشعارات الفورية (${remoteSettings.pollIntervalSeconds} ثانية)", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ScaleBounceButton(
                            onClick = { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تفعيل إذن الإشعارات")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ToggleRow("مراقبة البث بالخلفية (Foreground Service)", notificationPrefs.monitoringEnabled, onMonitoringChanged)
                    ToggleRow("صوت الإشعار", notificationPrefs.soundEnabled, onSoundChanged)
                    ToggleRow("الاهتزاز", notificationPrefs.vibrationEnabled, onVibrationChanged)
                    ToggleRow("تنبيهات المفضلة فقط", notificationPrefs.favoritesOnly, onFavoritesOnlyChanged)
                }
            }
        } else {
            // Admin Section
            if (!profile.isAdmin) {
                item {
                    HazeGlassCard(isGlowing = true) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = GoldAccent)
                            Text("دخول لوحة إدارة RESPECT", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = adminEmail,
                            onValueChange = { adminEmail = it },
                            label = { Text("بريد الإدارة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it },
                            label = { Text("كلمة المرور") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ScaleBounceButton(
                            onClick = { onAdminLogin(adminEmail, adminPassword) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل دخول الإدارة")
                        }
                    }
                }
            } else {
                item {
                    HazeGlassCard(isGlowing = true) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("لوحة إدارة RESPECT", style = MaterialTheme.typography.titleMedium, color = GoldAccent)
                            OutlinedButton(onClick = onAdminLogout, shape = RoundedCornerShape(12.dp)) {
                                Text("خروج", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassFilterChip("المستخدمون", adminTab == 0, { adminTab = 0 })
                            GlassFilterChip("الإشراف", adminTab == 1, { adminTab = 1 })
                            GlassFilterChip("الستريمرز", adminTab == 2, { adminTab = 2 })
                            GlassFilterChip("الإعدادات", adminTab == 3, { adminTab = 3 })
                        }
                    }
                }

                when (adminTab) {
                    0 -> {
                        // 1. Admin Users Tab
                        item {
                            Text("الأعضاء المسجلون (${communityUsers.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        items(communityUsers, key = { "usr_${it.uid}" }) { user ->
                            HazeGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${user.displayName} (${user.role})", color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(user.email.ifBlank { user.uid }, color = TextMuted, fontSize = 12.sp)
                                        if (user.isMuted) {
                                            Text("مقيد: ${user.banReason}", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val nextRole = if (user.role == "ADMIN") "USER" else "ADMIN"
                                        OutlinedButton(onClick = { onUpdateUserRole(user.uid, nextRole) }) {
                                            Text(if (user.role == "ADMIN") "تخفيض" else "ترقية", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onMuteCommunityUser(
                                                    user.uid,
                                                    user.displayName,
                                                    !user.isMuted,
                                                    if (user.isMuted) "" else "تقييد إداري ليوم واحد"
                                                )
                                            }
                                        ) {
                                            Text(if (user.isMuted) "فك الكتم" else "كتم", color = GoldAccent, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (bannedUsers.isNotEmpty()) {
                            item { Text("سجل الحظر المؤقت (${bannedUsers.size})", style = MaterialTheme.typography.titleMedium) }
                            items(bannedUsers, key = { "ban_${it.identifier}" }) { banned ->
                                HazeGlassCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${banned.displayName} (${banned.identifier})", color = Color(0xFFFCA5A5), fontSize = 13.sp)
                                            Text(banned.reason, color = TextMuted, fontSize = 11.sp)
                                        }
                                        OutlinedButton(onClick = { onUnbanUser(banned.identifier) }) {
                                            Text("رفع الحظر", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // 2. Admin Moderation Tab
                        item { Text("اقتراحات الأعضاء (${suggestions.size})", style = MaterialTheme.typography.titleMedium) }
                        items(suggestions, key = { "sug_${it.id}" }) { sug ->
                            HazeGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("@${sug.kickUsername} (${sug.status})", fontWeight = FontWeight.Bold, color = TextPrimary)
                                        if (sug.characterNames.isNotBlank()) {
                                            Text("الشخصيات: ${sug.characterNames}", color = TextSecondary, fontSize = 12.sp)
                                        }
                                        Text("مقترح من: ${sug.suggestedBy} • ${sug.reason}", color = TextMuted, fontSize = 12.sp)
                                    }
                                    Row {
                                        IconButton(onClick = { onApproveSuggestion(sug) }) {
                                            Icon(Icons.Rounded.Check, contentDescription = "قبول", tint = KickEmerald)
                                        }
                                        IconButton(onClick = { onRejectSuggestion(sug.id) }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "رفض", tint = GoldAccent)
                                        }
                                        IconButton(onClick = { onDeleteSuggestion(sug.id) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFCA5A5))
                                        }
                                    }
                                }
                            }
                        }

                        item { Text("إشراف التايملاين (${timelinePosts.size})", style = MaterialTheme.typography.titleMedium) }
                        items(timelinePosts.take(15), key = { "mod_p_${it.id}" }) { post ->
                            HazeGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(post.authorName, color = RespectPurpleBright, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(post.content, color = TextPrimary, fontSize = 12.sp, maxLines = 2)
                                    }
                                    Row {
                                        if (!post.isAdminPost) {
                                            OutlinedButton(
                                                onClick = { onBanUserOneDay(post.authorIdentifier, post.authorName, "حظر إداري ليوم واحد") },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, GoldAccent)
                                            ) {
                                                Text("حظر يوم", color = GoldAccent, fontSize = 11.sp)
                                            }
                                        }
                                        IconButton(onClick = { onDeletePost(post.id) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFCA5A5))
                                        }
                                    }
                                }
                            }
                        }

                        item { Text("إدارة الكليبات (${clips.size}) والفن (${artworks.size})", style = MaterialTheme.typography.titleMedium) }
                        items(clips, key = { "mod_c_${it.id}" }) { clip ->
                            HazeGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("كليب: ${clip.title}", color = TextPrimary, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onDeleteClip(clip.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFCA5A5))
                                    }
                                }
                            }
                        }
                        items(artworks, key = { "mod_a_${it.id}" }) { art ->
                            HazeGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("رسمة: ${art.title} (${art.artistName})", color = TextPrimary, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onDeleteArt(art.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFCA5A5))
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // 3. Admin Streamers & Schedule Tab
                        item {
                            HazeGlassCard {
                                Text("إضافة ستريمر مع الشخصيات", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newStreamerSlug,
                                    onValueChange = { newStreamerSlug = it },
                                    label = { Text("اسم القناة في KICK") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = newStreamerChars,
                                    onValueChange = { newStreamerChars = it },
                                    label = { Text("أسماء الشخصيات") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ScaleBounceButton(
                                    onClick = {
                                        if (newStreamerSlug.isNotBlank()) {
                                            onAddStreamer(newStreamerSlug, newStreamerChars)
                                            newStreamerSlug = ""
                                            newStreamerChars = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("إضافة للدليل")
                                }
                            }
                        }

                        items(streamers, key = { "adm_s_${it.slug}" }) { st ->
                            HazeGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(st.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text("@${st.slug} • ${st.characterNames}", color = TextMuted, fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { onRemoveStreamer(st.slug) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFCA5A5))
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
                                Text("جدول البثوث (${scheduleEvents.size})", style = MaterialTheme.typography.titleMedium)
                                ScaleBounceButton(onClick = { showAddSchedule = true }) {
                                    Text("+ موعد", fontSize = 12.sp)
                                }
                            }
                        }
                        items(scheduleEvents, key = { "sch_${it.id}" }) { ev ->
                            HazeGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${ev.streamerName} • ${ev.scheduledDate} ${ev.scheduledTime}", color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(ev.predictionNote, color = TextMuted, fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { onDeleteSchedule(ev.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFCA5A5))
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // 4. Admin Settings & Remote Config Tab
                        item {
                            HazeGlassCard {
                                Text("إعدادات التطبيق و Remote Config", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                ToggleRow("وضع الصيانة (Maintenance Mode)", maintenanceDraft) { maintenanceDraft = it }
                                ToggleRow("السماح بالنشر للضيوف", guestPostsDraft) { guestPostsDraft = it }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = bannerDraft,
                                    onValueChange = { bannerDraft = it },
                                    label = { Text("شريط الإعلان العام (Banner)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = pollIntervalDraft,
                                    onValueChange = { pollIntervalDraft = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("فترة فحص البثوث (بالثواني 10-120)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                ScaleBounceButton(
                                    onClick = {
                                        val sec = (pollIntervalDraft.toIntOrNull() ?: 15).coerceIn(10, 120)
                                        onSaveGlobalSettings(
                                            RemoteSettingsState(
                                                maintenanceMode = maintenanceDraft,
                                                bannerText = bannerDraft.trim(),
                                                allowGuestPosts = guestPostsDraft,
                                                pollIntervalSeconds = sec
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("حفظ ونشر الإعدادات (Firestore)")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onRefreshRemoteConfig,
                                    shape = RoundedCornerShape(13.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.CloudSync, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مزامنة Firebase Remote Config الآن")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSchedule) {
        ScheduleEditorDialog(
            existing = null,
            onDismiss = { showAddSchedule = false },
            onSave = { id, slug, name, date, time, cat, pred ->
                onSaveSchedule(id, slug, name, date, time, cat, pred)
                showAddSchedule = false
            }
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = RespectPurple,
                uncheckedBorderColor = GlassBorder
            )
        )
    }
}
