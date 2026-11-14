package com.respect.community.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.respect.community.auth.KickOAuthManager
import com.respect.community.data.local.KickAuthSession
import com.respect.community.data.local.NotificationPrefs
import com.respect.community.data.local.RemoteSettingsState
import com.respect.community.data.local.SessionManager
import com.respect.community.data.local.UserProfileState
import com.respect.community.data.local.entity.AnalysisEntity
import com.respect.community.data.local.entity.ArtEntity
import com.respect.community.data.local.entity.BannedUserEntity
import com.respect.community.data.local.entity.ChatMessageEntity
import com.respect.community.data.local.entity.ClipEntity
import com.respect.community.data.local.entity.CommunityUserEntity
import com.respect.community.data.local.entity.ScheduleEventEntity
import com.respect.community.data.local.entity.StreamSnapshotEntity
import com.respect.community.data.local.entity.StreamerEntity
import com.respect.community.data.local.entity.SuggestionEntity
import com.respect.community.data.local.entity.TimelinePostEntity
import com.respect.community.data.repository.CommunityRepository
import com.respect.community.service.NotificationHelper
import com.respect.community.service.StreamMonitorService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatsPeriod(val label: String, val durationMs: Long) {
    TODAY("اليوم", 24L * 60L * 60L * 1000L),
    WEEK("هذا الأسبوع", 7L * 24L * 60L * 60L * 1000L),
    MONTH("هذا الشهر", 30L * 24L * 60L * 60L * 1000L)
}

data class StreamerChartStat(
    val slug: String,
    val displayName: String,
    val avatarUrl: String,
    val peakViewers: Int,
    val avgViewers: Int,
    val streamedHours: Float,
    val followerCount: Int,
    val isLiveNow: Boolean
)

data class CommunitySearchResults(
    val streamers: List<StreamerEntity> = emptyList(),
    val posts: List<TimelinePostEntity> = emptyList(),
    val clips: List<ClipEntity> = emptyList(),
    val arts: List<ArtEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityViewModel(
    private val repository: CommunityRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val streamers: StateFlow<List<StreamerEntity>> = repository.streamersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streamSnapshots: StateFlow<List<StreamSnapshotEntity>> = repository.snapshotsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestions: StateFlow<List<SuggestionEntity>> = repository.suggestionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleEvents: StateFlow<List<ScheduleEventEntity>> = repository.scheduleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timelinePosts: StateFlow<List<TimelinePostEntity>> = repository.timelineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analyses: StateFlow<List<AnalysisEntity>> = repository.analysesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clips: StateFlow<List<ClipEntity>> = repository.clipsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artworks: StateFlow<List<ArtEntity>> = repository.artFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generalChat: StateFlow<List<ChatMessageEntity>> = repository.generalChatFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bannedUsers: StateFlow<List<BannedUserEntity>> = repository.bannedUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityUsers: StateFlow<List<CommunityUserEntity>> = repository.communityUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileState> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileState())

    val kickAuthSession: StateFlow<KickAuthSession> = repository.kickAuthSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KickAuthSession())

    val notificationPrefs: StateFlow<NotificationPrefs> = repository.notificationPrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationPrefs())

    val remoteSettings: StateFlow<RemoteSettingsState> = repository.remoteSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RemoteSettingsState())

    val hasMoreTimeline: StateFlow<Boolean> = repository.hasMoreTimeline

    private val _selectedStatsPeriod = MutableStateFlow(StatsPeriod.TODAY)
    val selectedStatsPeriod: StateFlow<StatsPeriod> = _selectedStatsPeriod.asStateFlow()

    val statsRankings: StateFlow<List<StreamerChartStat>> = combine(
        streamers,
        streamSnapshots,
        _selectedStatsPeriod
    ) { streamerList, snapList, period ->
        val cutoff = System.currentTimeMillis() - period.durationMs
        val grouped = snapList.filter { it.recordedAtMs >= cutoff }.groupBy { it.streamerSlug }
        val allSlugs = (streamerList.map { it.slug } + grouped.keys).distinct()

        allSlugs.map { slug ->
            val st = streamerList.firstOrNull { it.slug == slug }
            val snaps = grouped[slug].orEmpty()
            val liveSnaps = snaps.filter { it.isLive || it.viewerCount > 0 }
            val peakFromSnaps = snaps.maxOfOrNull { it.viewerCount } ?: 0
            val peak = maxOf(peakFromSnaps, st?.peakViewers ?: 0, st?.viewerCount ?: 0)
            val avg = if (liveSnaps.isNotEmpty()) {
                liveSnaps.map { it.viewerCount }.average().toInt()
            } else {
                st?.viewerCount ?: 0
            }
            val hours = (liveSnaps.size * 10f) / 60f
            val followers = maxOf(
                st?.followerCount ?: 0,
                snaps.maxOfOrNull { it.followerCount } ?: 0
            )
            StreamerChartStat(
                slug = slug,
                displayName = st?.displayName ?: snaps.firstOrNull()?.streamerName ?: slug,
                avatarUrl = st?.avatarUrl.orEmpty(),
                peakViewers = peak,
                avgViewers = avg,
                streamedHours = hours,
                followerCount = followers,
                isLiveNow = st?.isLive == true
            )
        }.sortedByDescending { it.peakViewers }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<CommunitySearchResults> = _searchQuery
        .flatMapLatest { q ->
            val trimmed = q.trim()
            if (trimmed.isBlank()) {
                flowOf(CommunitySearchResults())
            } else {
                combine(
                    repository.searchStreamers(trimmed),
                    repository.searchTimeline(trimmed),
                    repository.searchClips(trimmed),
                    repository.searchArt(trimmed)
                ) { st, po, cl, ar ->
                    CommunitySearchResults(st, po, cl, ar)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CommunitySearchResults())

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isLoadingMorePosts = MutableStateFlow(false)
    val isLoadingMorePosts: StateFlow<Boolean> = _isLoadingMorePosts.asStateFlow()

    private val _networkError = MutableStateFlow<String?>(null)
    val networkError: StateFlow<String?> = _networkError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        initialBootSync()
    }

    fun initialBootSync() {
        viewModelScope.launch {
            _isInitialLoading.value = true
            _networkError.value = null
            val refreshRes = runCatching { repository.refreshAllStreamersAndRecordSnapshots() }
            if (refreshRes.isFailure && streamers.value.isEmpty()) {
                _networkError.value = "تعذر الاتصال بـ KICK API. تحقق من الإنترنت وحاول مجدداً."
            }
            repository.loadTimelineFirstPage()
            _isInitialLoading.value = false
        }
    }

    fun setStatsPeriod(period: StatsPeriod) {
        _selectedStatsPeriod.value = period
    }

    fun refreshStatsFromKICK() {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                repository.refreshAllStreamersAndRecordSnapshots()
                repository.syncRecentSnapshotsFromCloud()
            }.onFailure {
                _snackbarMessage.value = "تعذر تحديث إحصائيات KICK"
            }
            _isBusy.value = false
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Telegraph Image Uploader ---
    suspend fun uploadImageToTelegraph(context: Context, uri: Uri): Result<String> {
        val result = repository.uploadImageToTelegraph(context, uri)
        result.onFailure { _snackbarMessage.value = it.message ?: "فشل رفع الصورة إلى Telegraph" }
        result.onSuccess { _snackbarMessage.value = "تم ضغط ورفع الصورة عبر Telegraph بنجاح" }
        return result
    }

    // --- Firebase Auth & Email Verification ---
    fun signInFirebase(email: String, password: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.signInFirebase(email, password)
                .onSuccess {
                    _snackbarMessage.value = if (it.isEmailVerified) {
                        "تم تسجيل الدخول بنجاح (${it.displayName})"
                    } else {
                        "يرجى تفعيل بريدك الإلكتروني أولاً عبر الرابط المرسل"
                    }
                }
                .onFailure { _snackbarMessage.value = it.message ?: "فشل تسجيل الدخول" }
            _isBusy.value = false
        }
    }

    fun signUpFirebase(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.signUpFirebase(email, password, displayName)
                .onSuccess {
                    _snackbarMessage.value = "تم إنشاء الحساب وإرسال رابط التفعيل إلى بريدك الإلكتروني"
                }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر إنشاء الحساب" }
            _isBusy.value = false
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.resendVerificationEmail()
                .onSuccess { _snackbarMessage.value = "تم إعادة إرسال رابط التفعيل لبريدك" }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر إرسال رابط التفعيل" }
            _isBusy.value = false
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.refreshVerificationStatus()
                .onSuccess { verified ->
                    _snackbarMessage.value = if (verified) {
                        "تم تأكيد البريد الإلكتروني! أهلاً بك في RESPECT"
                    } else {
                        "لم يتم تأكيد البريد بعد. افحص صندوق الوارد أو الرسائل غير المرغوب فيها."
                    }
                }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر التحقق" }
            _isBusy.value = false
        }
    }

    fun signOutFirebase() {
        viewModelScope.launch {
            repository.signOutFirebase()
            _snackbarMessage.value = "تم تسجيل الخروج"
        }
    }

    fun refreshRemoteConfig() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.refreshRemoteConfig()
            _snackbarMessage.value = "تمت مزامنة Remote Config"
            _isBusy.value = false
        }
    }

    fun saveGlobalSettings(settings: RemoteSettingsState) {
        viewModelScope.launch {
            repository.saveGlobalSettings(settings)
            _snackbarMessage.value = "تم حفظ ونشر إعدادات التطبيق"
        }
    }

    // --- Admin User Management ---
    fun updateUserRole(uid: String, role: String) {
        viewModelScope.launch {
            repository.updateUserRole(uid, role)
            _snackbarMessage.value = "تم تحديث الصلاحية إلى $role"
        }
    }

    fun setUserBannedOrMuted(uid: String, displayName: String, banned: Boolean, reason: String) {
        viewModelScope.launch {
            repository.setUserBannedOrMuted(uid, displayName, banned, reason)
            _snackbarMessage.value = if (banned) "تم حظر/كتم $displayName" else "تم فك الحظر عن $displayName"
        }
    }

    fun deleteCommunityUser(uid: String) {
        viewModelScope.launch {
            repository.deleteCommunityUser(uid)
            _snackbarMessage.value = "تم حذف حساب العضو من المجتمع"
        }
    }

    // --- Admin Notification Center ---
    fun sendBroadcastNotification(context: Context, title: String, body: String) {
        if (title.isBlank() || body.isBlank()) {
            _snackbarMessage.value = "أدخل عنوان ونص الإشعار"
            return
        }
        viewModelScope.launch {
            repository.sendBroadcastNotification(title, body)
                .onSuccess {
                    NotificationHelper.showCustomKickPush(context, title, body)
                    _snackbarMessage.value = "تم إرسال الإشعار العام للجميع"
                }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر إرسال الإشعار" }
        }
    }

    fun sendTargetedNotification(context: Context, targetUid: String, title: String, body: String) {
        if (targetUid.isBlank() || title.isBlank() || body.isBlank()) {
            _snackbarMessage.value = "اختر المستخدم وأدخل العنوان والنص"
            return
        }
        viewModelScope.launch {
            repository.sendTargetedNotification(targetUid, title, body)
                .onSuccess {
                    NotificationHelper.showCustomKickPush(context, title, body)
                    _snackbarMessage.value = "تم إرسال الإشعار للمستخدم المحدد"
                }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر إرسال الإشعار" }
        }
    }

    fun sendTestKickNotification(context: Context) {
        NotificationHelper.showTestKickNotification(context)
        _snackbarMessage.value = "تم إرسال إشعار KICK التجريبي!"
    }

    // --- Streamers & Suggestions ---
    fun addStreamer(slug: String, characterNames: String = "", isGlobal: Boolean = false) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.addOrRefreshStreamer(slug, characterNames, isGlobal)
                .onSuccess {
                    _networkError.value = null
                    _snackbarMessage.value = "تمت إضافة ${it.displayName} بنجاح"
                }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر العثور على القناة في KICK" }
            _isBusy.value = false
        }
    }

    fun refreshAllStreamers(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _isBusy.value = true
            _networkError.value = null
            runCatching { repository.refreshAllStreamersAndRecordSnapshots() }
                .onSuccess { live ->
                    if (!silent && live.isNotEmpty()) {
                        _snackbarMessage.value = "${live.first().displayName} بدأ بثه!"
                    }
                }
                .onFailure {
                    if (!silent) {
                        _networkError.value = "تعذر تحديث البثوث من KICK"
                        _snackbarMessage.value = "تعذر تحديث القنوات من KICK"
                    }
                }
            if (!silent) _isBusy.value = false
        }
    }

    fun removeStreamer(slug: String) = viewModelScope.launch { repository.removeStreamer(slug) }
    fun toggleNotifying(slug: String, notify: Boolean) = viewModelScope.launch {
        repository.toggleNotifying(slug, notify)
        _snackbarMessage.value = if (notify) "تم تفعيل تنبيهات البث" else "تم كتم تنبيهات البث"
    }
    fun toggleFeatured(slug: String, featured: Boolean) = viewModelScope.launch {
        repository.toggleFeatured(slug, featured)
    }
    fun toggleFavorite(slug: String, fav: Boolean) = viewModelScope.launch { repository.toggleFavorite(slug, fav) }
    fun toggleFollow(slug: String, follow: Boolean) = viewModelScope.launch { repository.toggleFollow(slug, follow) }

    fun submitSuggestion(kickUsername: String, characterNames: String, reason: String) {
        viewModelScope.launch {
            repository.submitSuggestion(kickUsername, characterNames, reason)
                .onSuccess { _snackbarMessage.value = "تم إرسال اقتراحك للإدارة بنجاح" }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر إرسال الاقتراح" }
        }
    }

    fun approveSuggestion(suggestion: SuggestionEntity) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.approveSuggestion(suggestion)
                .onSuccess { _snackbarMessage.value = "تم قبول وإضافة ${it.displayName} من KICK" }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر جلب القناة من KICK" }
            _isBusy.value = false
        }
    }

    fun rejectSuggestion(id: Long) = viewModelScope.launch { repository.rejectSuggestion(id) }
    fun deleteSuggestion(id: Long) = viewModelScope.launch { repository.deleteSuggestion(id) }

    // --- Schedule ---
    fun saveScheduleEvent(
        id: Long = 0,
        streamerSlug: String,
        streamerName: String,
        date: String,
        time: String,
        category: String,
        prediction: String
    ) {
        viewModelScope.launch {
            if (streamerName.isBlank() || date.isBlank() || time.isBlank()) {
                _snackbarMessage.value = "يرجى إدخال اسم الستريمر والتاريخ والوقت"
                return@launch
            }
            repository.upsertSchedule(
                ScheduleEventEntity(
                    id = id,
                    streamerSlug = CommunityRepository.sanitizeSlug(streamerSlug).ifBlank { "respect" },
                    streamerName = streamerName.trim(),
                    scheduledDate = date.trim(),
                    scheduledTime = time.trim(),
                    gameCategory = category.trim().ifBlank { "GTA RP" },
                    predictionNote = prediction.trim()
                )
            )
            _snackbarMessage.value = "تم حفظ الموعد في جدول البثوث"
        }
    }

    fun deleteScheduleEvent(id: Long) = viewModelScope.launch { repository.deleteSchedule(id) }

    // --- Timeline Pagination (10 posts per load) & Moderation ---
    fun loadNextTimelinePage() {
        if (_isLoadingMorePosts.value || !hasMoreTimeline.value) return
        viewModelScope.launch {
            _isLoadingMorePosts.value = true
            repository.loadNextTimelinePage()
            _isLoadingMorePosts.value = false
        }
    }

    fun refreshTimeline() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.loadTimelineFirstPage()
            _isBusy.value = false
        }
    }

    fun createTimelinePost(
        content: String,
        mediaUrl: String,
        mediaType: String,
        isAnnouncement: Boolean
    ) {
        viewModelScope.launch {
            repository.createTimelinePost(content, mediaUrl, mediaType, isAnnouncement)
                .onSuccess { _snackbarMessage.value = "تم النشر في التايملاين" }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر النشر" }
        }
    }

    fun togglePostLike(post: TimelinePostEntity) = viewModelScope.launch { repository.togglePostLike(post) }
    fun togglePinPost(post: TimelinePostEntity) = viewModelScope.launch {
        repository.togglePinPost(post)
        _snackbarMessage.value = if (!post.isPinned) "تم تثبيت المنشور في الأعلى" else "تم إلغاء تثبيت المنشور"
    }
    fun deleteTimelinePost(post: TimelinePostEntity) = viewModelScope.launch { repository.deleteTimelinePost(post) }

    fun banUserOneDay(identifier: String, displayName: String, reason: String) {
        viewModelScope.launch {
            repository.banUserOneDay(identifier, displayName, reason)
            _snackbarMessage.value = "تم حظر $displayName مؤقتاً لمدة يوم واحد"
        }
    }

    fun unbanUser(identifier: String) {
        viewModelScope.launch {
            repository.unbanUser(identifier)
            _snackbarMessage.value = "تم رفع الحظر عن $identifier"
        }
    }

    // --- General Chat, Analysis, Clips, Art ---
    fun sendGeneralChat(text: String) {
        viewModelScope.launch {
            repository.sendGeneralChatMessage(text)
                .onFailure { _snackbarMessage.value = it.message }
        }
    }

    fun deleteChatMessage(id: String) = viewModelScope.launch { repository.deleteChatMessage(id) }

    fun addAnalysis(streamerSlug: String, streamerName: String, title: String, text: String, rating: Int) {
        viewModelScope.launch {
            repository.addAnalysis(streamerSlug, streamerName, title, text, rating)
                .onSuccess { _snackbarMessage.value = "تم نشر تحليلك للبث" }
                .onFailure { _snackbarMessage.value = it.message }
        }
    }

    fun deleteAnalysis(id: Long) = viewModelScope.launch { repository.deleteAnalysis(id) }

    fun addClip(streamerSlug: String, streamerName: String, title: String, videoUrl: String) {
        viewModelScope.launch {
            repository.addClip(streamerSlug, streamerName, title, videoUrl)
                .onSuccess { _snackbarMessage.value = "تم نشر الكليب بنجاح" }
                .onFailure { _snackbarMessage.value = it.message }
        }
    }

    fun toggleClipLike(clip: ClipEntity) = viewModelScope.launch { repository.toggleClipLike(clip) }
    fun deleteClip(id: Long) = viewModelScope.launch { repository.deleteClip(id) }

    fun addArt(title: String, artistName: String, imageUrl: String) {
        viewModelScope.launch {
            repository.addArt(title, artistName, imageUrl)
                .onSuccess { _snackbarMessage.value = "تمت إضافة العمل إلى معرض الفن" }
                .onFailure { _snackbarMessage.value = it.message }
        }
    }

    fun toggleArtLike(art: ArtEntity) = viewModelScope.launch { repository.toggleArtLike(art) }
    fun deleteArt(id: Long) = viewModelScope.launch { repository.deleteArt(id) }

    // --- Profile & Admin Auth ---
    fun updateProfile(name: String, bio: String, avatarUrl: String, bannerUrl: String) {
        viewModelScope.launch {
            repository.updateProfile(name, bio, avatarUrl, bannerUrl)
            _snackbarMessage.value = "تم تحديث الملف الشخصي"
        }
    }

    fun loginAdmin(email: String, password: String) {
        viewModelScope.launch {
            repository.loginAdmin(email, password)
                .onSuccess { _snackbarMessage.value = "تم تسجيل دخول الإدارة بنجاح!" }
                .onFailure { _snackbarMessage.value = it.message ?: "بيانات الإدارة غير صحيحة" }
        }
    }

    fun logoutAdmin() {
        viewModelScope.launch {
            repository.logoutAdmin()
            _snackbarMessage.value = "تم الخروج من وضع الإدارة"
        }
    }

    // --- KICK OAuth2 (Chrome Custom Tabs + Deep Link) ---
    fun launchKickOAuth(context: Context) {
        viewModelScope.launch {
            runCatching { KickOAuthManager.launchOAuthFlow(context, sessionManager) }
                .onFailure { _snackbarMessage.value = "تعذر فتح صفحة تسجيل دخول كيك" }
        }
    }

    fun handleOAuthRedirect(uri: Uri) {
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            _snackbarMessage.value = "تم إلغاء تسجيل دخول كيك: $error"
            return
        }
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        if (code.isNullOrBlank()) {
            _snackbarMessage.value = "لم يتم استلام رمز التفويض من كيك"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.exchangeOAuthCode(code, state)
                .onSuccess { _snackbarMessage.value = "تم ربط حساب كيك (${it.kickUsername ?: "بنجاح"})" }
                .onFailure { _snackbarMessage.value = it.message ?: "فشل ربط حساب كيك" }
            _isBusy.value = false
        }
    }

    fun showErrorSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun logoutKick() {
        viewModelScope.launch {
            repository.logoutKick()
            _snackbarMessage.value = "تم فصل حساب KICK"
        }
    }

    // --- Notifications ---
    fun setMonitoring(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            repository.setMonitoringEnabled(enabled)
            if (enabled) StreamMonitorService.start(context) else StreamMonitorService.stop(context)
        }
    }

    fun setSound(enabled: Boolean) = viewModelScope.launch { repository.setSoundEnabled(enabled) }
    fun setVibration(enabled: Boolean) = viewModelScope.launch { repository.setVibrationEnabled(enabled) }
    fun setFavoritesOnly(favOnly: Boolean) = viewModelScope.launch { repository.setFavoritesOnly(favOnly) }

    class Factory(
        private val repository: CommunityRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityViewModel(repository, sessionManager) as T
        }
    }
}
