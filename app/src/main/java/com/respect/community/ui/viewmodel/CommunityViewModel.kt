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
import com.respect.community.data.local.entity.StreamerEntity
import com.respect.community.data.local.entity.SuggestionEntity
import com.respect.community.data.local.entity.TimelinePostEntity
import com.respect.community.data.repository.CommunityRepository
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

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        refreshAllStreamers(silent = true)
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
        result.onSuccess { _snackbarMessage.value = "تم رفع الصورة عبر Telegraph بنجاح" }
        return result
    }

    // --- Firebase Auth & Settings ---
    fun signInFirebase(email: String, password: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.signInFirebase(email, password)
                .onSuccess { _snackbarMessage.value = "تم تسجيل الدخول بنجاح (${it.displayName})" }
                .onFailure { _snackbarMessage.value = it.message ?: "فشل تسجيل الدخول" }
            _isBusy.value = false
        }
    }

    fun signUpFirebase(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.signUpFirebase(email, password, displayName)
                .onSuccess { _snackbarMessage.value = "تم إنشاء الحساب بنجاح!" }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر إنشاء الحساب" }
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
            _snackbarMessage.value = "تم تحديث إعدادات Remote Config"
            _isBusy.value = false
        }
    }

    fun saveGlobalSettings(settings: RemoteSettingsState) {
        viewModelScope.launch {
            repository.saveGlobalSettings(settings)
            _snackbarMessage.value = "تم حفظ الإعدادات العامة في Firestore"
        }
    }

    fun updateUserRole(uid: String, role: String) {
        viewModelScope.launch {
            repository.updateUserRole(uid, role)
            _snackbarMessage.value = "تم تحديث صلاحية العضو إلى $role"
        }
    }

    fun muteCommunityUser(uid: String, displayName: String, mute: Boolean, reason: String) {
        viewModelScope.launch {
            repository.muteCommunityUser(uid, displayName, mute, reason)
            _snackbarMessage.value = if (mute) "تم تقييد $displayName لمدة يوم" else "تم فك التقييد عن $displayName"
        }
    }

    // --- Streamers & Suggestions ---
    fun addStreamer(slug: String, characterNames: String = "") {
        viewModelScope.launch {
            _isBusy.value = true
            repository.addOrRefreshStreamer(slug, characterNames)
                .onSuccess { _snackbarMessage.value = "تمت إضافة ${it.displayName} بنجاح" }
                .onFailure { _snackbarMessage.value = it.message ?: "تعذر العثور على القناة في KICK" }
            _isBusy.value = false
        }
    }

    fun refreshAllStreamers(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _isBusy.value = true
            runCatching { repository.refreshAllStreamers() }
                .onSuccess { live ->
                    if (!silent && live.isNotEmpty()) {
                        _snackbarMessage.value = "${live.first().displayName} بدأ البث المباشر!"
                    }
                }
                .onFailure {
                    if (!silent) _snackbarMessage.value = "تعذر تحديث القنوات من KICK"
                }
            if (!silent) _isBusy.value = false
        }
    }

    fun removeStreamer(slug: String) = viewModelScope.launch { repository.removeStreamer(slug) }
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

    // --- Schedule & Predictions ---
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

    // --- Social Timeline & Moderation ---
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
    fun deleteTimelinePost(id: Long) = viewModelScope.launch { repository.deleteTimelinePost(id) }

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

    // --- General Chat Room ---
    fun sendGeneralChat(text: String) {
        viewModelScope.launch {
            repository.sendGeneralChatMessage(text)
                .onFailure { _snackbarMessage.value = it.message }
        }
    }

    fun deleteChatMessage(id: String) = viewModelScope.launch { repository.deleteChatMessage(id) }

    // --- Stream Analysis ---
    fun addAnalysis(streamerSlug: String, streamerName: String, title: String, text: String, rating: Int) {
        viewModelScope.launch {
            repository.addAnalysis(streamerSlug, streamerName, title, text, rating)
                .onSuccess { _snackbarMessage.value = "تم نشر تحليلك للبث" }
                .onFailure { _snackbarMessage.value = it.message }
        }
    }

    fun deleteAnalysis(id: Long) = viewModelScope.launch { repository.deleteAnalysis(id) }

    // --- Clips & Art ---
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

    // --- Profile & Admin ---
    fun updateProfile(name: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            repository.updateProfile(name, bio, avatarUrl)
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

    // --- KICK OAuth2 ---
    fun launchKickOAuth(context: Context) {
        viewModelScope.launch {
            runCatching { KickOAuthManager.launchOAuthLogin(context, sessionManager) }
                .onFailure { _snackbarMessage.value = "تعذر فتح متصفح KICK" }
        }
    }

    fun handleOAuthRedirect(uri: Uri) {
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            _snackbarMessage.value = "تم إلغاء الربط: $error"
            return
        }
        if (code.isNullOrBlank()) return
        viewModelScope.launch {
            repository.exchangeOAuthCode(code, state)
                .onSuccess { _snackbarMessage.value = "تم ربط حساب KICK (${it.kickUsername ?: "بنجاح"})" }
                .onFailure { _snackbarMessage.value = it.message ?: "فشل ربط KICK" }
        }
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
