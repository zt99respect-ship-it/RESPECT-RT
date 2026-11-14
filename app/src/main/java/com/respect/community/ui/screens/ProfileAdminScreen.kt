package com.respect.community.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.ui.layout.ContentScale
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
import com.respect.community.ui.theme.LiveCrimson
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
    onUpdateProfile: (String, String, String, String) -> Unit,
    onFirebaseSignIn: (String, String) -> Unit,
    onFirebaseSignUp: (String, String, String) -> Unit,
    onResendVerification: () -> Unit,
    onCheckVerification: () -> Unit,
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
    onSetUserBannedOrMuted: (String, String, Boolean, String) -> Unit,
    onDeleteCommunityUser: (String) -> Unit,
    onAddGlobalStreamer: (String, String) -> Unit,
    onToggleFeaturedStreamer: (String, Boolean) -> Unit,
    onRemoveStreamer: (String) -> Unit,
    onApproveSuggestion: (SuggestionEntity) -> Unit,
    onRejectSuggestion: (Long) -> Unit,
    onDeleteSuggestion: (Long) -> Unit,
    onSaveSchedule: (Long, String, String, String, String, String, String) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onTogglePinPost: (TimelinePostEntity) -> Unit,
    onDeletePost: (TimelinePostEntity) -> Unit,
    onBanUserOneDay: (String, String, String) -> Unit,
    onUnbanUser: (String) -> Unit,
    onDeleteClip: (Long) -> Unit,
    onDeleteArt: (Long) -> Unit,
    onSaveGlobalSettings: (RemoteSettingsState) -> Unit,
    onRefreshRemoteConfig: () -> Unit,
    onSendBroadcast: (Context, String, String) -> Unit,
    onSendTargetedNotification: (Context, String, String, String) -> Unit,
    onSendTestKickNotification: (Context) -> Unit
) {
    val context = LocalContext.current
    var section by remember { mutableIntStateOf(0) } // 0: Profile, 1: Supercharged Admin Panel
    // 0: Users, 1: Moderation, 2: Settings, 3: Streamers, 4: Notifications, 5: Dashboard
    var adminTab by remember { mutableIntStateOf(5) }

    var nameEdit by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var bioEdit by remember(profile.bio) { mutableStateOf(profile.bio) }
    var avatarEdit by remember(profile.avatarUrl) { mutableStateOf(profile.avatarUrl) }
    var bannerEdit by remember(profile.bannerUrl) { mutableStateOf(profile.bannerUrl) }

    var authEmail by remember { mutableStateOf("") }
    var authPassword by remember { mutableStateOf("") }
    var authName by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var newStreamerSlug by remember { mutableStateOf("") }
    var newStreamerChars by remember { mutableStateOf("") }
    var showAddSchedule by remember { mutableStateOf(false) }

    var notifTitle by remember { mutableStateOf("") }
    var notifBody by remember { mutableStateOf("") }
    var notifTargetUid by remember { mutableStateOf("") }

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
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
                    GlassFilterChip("لوحة الإدارة", section == 1, { section = 1 })
                }
            }
        }

        if (section == 0) {
            item {
                HazeGlassCard(isGlowing = profile.isFirebaseSignedIn) {
                    if (profile.bannerUrl.isNotBlank()) {
                        RespectImage(
                            url = profile.bannerUrl,
                            contentDescription = "غلاف الملف الشخصي",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(RespectPurple.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("أضف غلاف (Banner) عبر Telegraph أو رابط", color = TextMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RespectImage(
                            url = profile.avatarUrl,
                            contentDescription = profile.displayName,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .border(2.dp, RespectPurpleBright, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                                if (profile.isAdmin) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = GoldAccent.copy(alpha = 0.22f)) {
                                        Text(
                                            "أدمن",
                                            color = GoldAccent,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (profile.isEmailVerified) {
                                    Icon(Icons.Rounded.VerifiedUser, contentDescription = "موثق", tint = KickEmerald, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(profile.email.ifBlank { "عضو ضيف" }, color = TextMuted, fontSize = 12.sp)
                            Text("المتابَعون: ${streamers.count { it.isFollowed }} ستريمر", color = CyanAccent, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TelegraphImagePickerButton(
                        label = "رفع الصورة الشخصية (Avatar) عبر Telegraph",
                        onUploadImage = onUploadImage,
                        onUploadedUrl = { uploaded -> avatarEdit = uploaded }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TelegraphImagePickerButton(
                        label = "رفع صورة الغلاف (Banner) عبر Telegraph",
                        onUploadImage = onUploadImage,
                        onUploadedUrl = { uploaded -> bannerEdit = uploaded }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                        label = { Text("رابط الصورة الشخصية (URL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bannerEdit,
                        onValueChange = { bannerEdit = it },
                        label = { Text("رابط الغلاف / Banner (URL)") },
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
                        onClick = { onUpdateProfile(nameEdit, bioEdit, avatarEdit, bannerEdit) },
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
                        if (!profile.isEmailVerified && !profile.isAdmin) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("البريد غير مفعل بعد", color = GoldAccent, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onResendVerification, modifier = Modifier.weight(1f)) {
                                    Text("إعادة إرسال التفعيل", fontSize = 11.sp)
                                }
                                ScaleBounceButton(onClick = onCheckVerification, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تحقق", fontSize = 11.sp)
                                }
                            }
                        }
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
                            "اربط حسابك عبر المتصفح الداخلي التلقائي للمشاركة مباشرة في دردشة KICK دون أي نسخ/لصق.",
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
                    ToggleRow("صوت الإشعار المخصص", notificationPrefs.soundEnabled, onSoundChanged)
                    ToggleRow("الاهتزاز", notificationPrefs.vibrationEnabled, onVibrationChanged)
                    ToggleRow("تنبيهات المفعّل جرسهم فقط", notificationPrefs.favoritesOnly, onFavoritesOnlyChanged)
                }
            }
        } else {
            // Supercharged Admin Panel
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
                            Text("فتح لوحة التحكم الكاملة")
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
                            Text("لوحة إدارة RESPECT (6 أقسام)", style = MaterialTheme.typography.titleMedium, color = GoldAccent)
                            OutlinedButton(onClick = onAdminLogout, shape = RoundedCornerShape(12.dp)) {
                                Text("خروج", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassFilterChip("لوحة البيانات", adminTab == 5, { adminTab = 5 })
                            GlassFilterChip("المستخدمون", adminTab == 0, { adminTab = 0 })
                            GlassFilterChip("الإشراف", adminTab == 1, { adminTab = 1 })
                            GlassFilterChip("الستريمرز", adminTab == 3, { adminTab = 3 })
                            GlassFilterChip("الإشعارات", adminTab == 4, { adminTab = 4 })
                            GlassFilterChip("الإعدادات", adminTab == 2, { adminTab = 2 })
                        }
                    }
                }

                when (adminTab) {
                    5 -> {
                        // 6. Live Dashboard Stats
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    DashboardMetricCard("إجمالي الأعضاء", "${communityUsers.size}", CyanAccent, Modifier.weight(1f))
                                    DashboardMetricCard("إجمالي المنشورات", "${timelinePosts.size}", RespectPurpleBright, Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    DashboardMetricCard("الستريمرز", "${streamers.size}", KickEmerald, Modifier.weight(1f))
                                    DashboardMetricCard("يبثون الآن", "${streamers.count { it.isLive }}", GoldAccent, Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    DashboardMetricCard("الاقتراحات", "${suggestions.size}", RespectPurpleBright, Modifier.weight(1f))
                                    DashboardMetricCard("المحظورون", "${bannedUsers.size + communityUsers.count { it.banned }}", LiveCrimson, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    0 -> {
                        // 1. User Management (Ban/Mute, Promote to MODERATOR, Delete Account)
                        item {
                            Text("إدارة الأعضاء (${communityUsers.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        items(communityUsers, key = { "usr_${it.uid}" }) { user ->
                            HazeGlassCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${user.displayName} • ${user.role}", color = TextPrimary, fontWeight = FontWeight.Bold)
                                            Text(user.email.ifBlank { user.uid }, color = TextMuted, fontSize = 12.sp)
                                            if (user.banned || user.isMuted) {
                                                Text("الحالة: محظور / مقيد (${user.banReason})", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                            }
                                        }
                                        IconButton(onClick = { onDeleteCommunityUser(user.uid) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف الحساب", tint = LiveCrimson)
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                        val isMod = user.role == "MODERATOR"
                                        OutlinedButton(onClick = { onUpdateUserRole(user.uid, if (isMod) "USER" else "MODERATOR") }) {
                                            Text(if (isMod) "إزالة الإشراف" else "ترقية لمشرف (MOD)", fontSize = 11.sp)
                                        }
                                        OutlinedButton(onClick = { onUpdateUserRole(user.uid, if (user.role == "ADMIN") "USER" else "ADMIN") }) {
                                            Text(if (user.role == "ADMIN") "تخفيض لأدنى" else "ترقية لأدمن", fontSize = 11.sp)
                                        }
                                        val isRestricted = user.banned || user.isMuted
                                        OutlinedButton(
                                            onClick = {
                                                onSetUserBannedOrMuted(
                                                    user.uid,
                                                    user.displayName,
                                                    !isRestricted,
                                                    if (isRestricted) "" else "حظر وكتم إداري"
                                                )
                                            }
                                        ) {
                                            Text(if (isRestricted) "فك الحظر" else "حظر / كتم", color = GoldAccent, fontSize = 11.sp)
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
                        // 2. Content Moderation (Pin/Unpin, Delete Posts, Suggestions, Clips, Art)
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
                        items(timelinePosts.take(20), key = { "mod_p_${it.id}" }) { post ->
                            HazeGlassCard(isGlowing = post.isPinned) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (post.isPinned) "${post.authorName} (مثبّت)" else post.authorName,
                                            color = if (post.isPinned) GoldAccent else RespectPurpleBright,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(post.content, color = TextPrimary, fontSize = 12.sp, maxLines = 2)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onTogglePinPost(post) }) {
                                            Icon(
                                                Icons.Rounded.PushPin,
                                                contentDescription = "تثبيت",
                                                tint = if (post.isPinned) GoldAccent else TextMuted
                                            )
                                        }
                                        if (!post.isAdminPost) {
                                            OutlinedButton(
                                                onClick = { onBanUserOneDay(post.authorIdentifier, post.authorName, "حظر إداري ليوم واحد") },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, GoldAccent)
                                            ) {
                                                Text("حظر", color = GoldAccent, fontSize = 11.sp)
                                            }
                                        }
                                        IconButton(onClick = { onDeletePost(post) }) {
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
                        // 3. App Settings & Remote Config
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
                                    label = { Text("نص شريط الإعلانات (Announcement Bar)") },
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

                    3 -> {
                        // 4. Streamer Management (Add/Delete Global Streamers, Feature Streamer, Schedule)
                        item {
                            HazeGlassCard {
                                Text("إضافة ستريمر عام للجميع (Global)", style = MaterialTheme.typography.titleMedium)
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
                                            onAddGlobalStreamer(newStreamerSlug, newStreamerChars)
                                            newStreamerSlug = ""
                                            newStreamerChars = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("إضافة ونشر للجميع")
                                }
                            }
                        }

                        items(streamers, key = { "adm_s_${it.slug}" }) { st ->
                            HazeGlassCard(isGlowing = st.isFeatured) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(st.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text("@${st.slug} • ${if (st.isGlobal) "عام" else "خاص"}", color = TextMuted, fontSize = 12.sp)
                                    }
                                    Row {
                                        IconButton(onClick = { onToggleFeaturedStreamer(st.slug, !st.isFeatured) }) {
                                            Icon(
                                                imageVector = if (st.isFeatured) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                                contentDescription = "تثبيت بالأعلى",
                                                tint = if (st.isFeatured) GoldAccent else TextMuted
                                            )
                                        }
                                        IconButton(onClick = { onRemoveStreamer(st.slug) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFCA5A5))
                                        }
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
                        // 5. Notification Center (Broadcast, Targeted, Test Notification)
                        item {
                            HazeGlassCard(isGlowing = true) {
                                Text("مركز الإشعارات المباشر", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = notifTitle,
                                    onValueChange = { notifTitle = it },
                                    label = { Text("عنوان الإشعار") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = notifBody,
                                    onValueChange = { notifBody = it },
                                    label = { Text("نص الإشعار") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = notifTargetUid,
                                    onValueChange = { notifTargetUid = it },
                                    label = { Text("UID المستخدم المحدد (اتركه فارغاً للإرسال للجميع)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    ScaleBounceButton(
                                        onClick = {
                                            if (notifTargetUid.isBlank()) {
                                                onSendBroadcast(context, notifTitle, notifBody)
                                            } else {
                                                onSendTargetedNotification(context, notifTargetUid.trim(), notifTitle, notifBody)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (notifTargetUid.isBlank()) "إرسال للجميع" else "إرسال للمستخدم", fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { onSendTestKickNotification(context) },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, RespectPurpleBright.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = RespectPurpleBright)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Test Notification (DRB7H بدأ بثه)", color = TextPrimary, fontSize = 12.sp)
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
private fun DashboardMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    HazeGlassCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
