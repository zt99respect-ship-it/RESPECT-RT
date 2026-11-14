package com.respect.community.data.repository

import android.content.Context
import android.net.Uri
import com.respect.community.BuildConfig
import com.respect.community.auth.KickOAuthManager
import com.respect.community.data.local.CommunityDatabase
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
import com.respect.community.data.remote.ChatConnectionState
import com.respect.community.data.remote.FirebaseManager
import com.respect.community.data.remote.KickAuthApi
import com.respect.community.data.remote.KickChatWebSocket
import com.respect.community.data.remote.KickPublicApi
import com.respect.community.data.remote.KickV2Api
import com.respect.community.data.remote.SendChatRequest
import com.respect.community.data.remote.TelegraphUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class CommunityRepository(
    private val authApi: KickAuthApi,
    private val publicApi: KickPublicApi,
    private val v2Api: KickV2Api,
    private val db: CommunityDatabase,
    private val sessionManager: SessionManager,
    private val chatWebSocket: KickChatWebSocket,
    private val firebaseManager: FirebaseManager,
    private val telegraphUploader: TelegraphUploader
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appTokenMutex = Mutex()

    private val streamerDao = db.streamerDao()
    private val suggestionDao = db.suggestionDao()
    private val scheduleDao = db.scheduleDao()
    private val timelineDao = db.timelineDao()
    private val analysisDao = db.analysisDao()
    private val clipDao = db.clipDao()
    private val artDao = db.artDao()
    private val chatDao = db.chatMessageDao()
    private val bannedDao = db.bannedUserDao()
    private val userDao = db.communityUserDao()

    init {
        scope.launch {
            chatWebSocket.incomingMessages.collect { msg ->
                chatDao.insert(msg)
                chatDao.trimRoom(msg.roomKey)
            }
        }
        scope.launch {
            ensureInitialCommunityContent()
            firebaseManager.testConnection()
            val remote = firebaseManager.fetchRemoteConfig()
            sessionManager.saveRemoteSettings(remote)
            firebaseManager.listenGlobalSettings { cloudSettings ->
                scope.launch { sessionManager.saveRemoteSettings(cloudSettings) }
            }
            firebaseManager.listenUsers { cloudUsers ->
                if (cloudUsers.isNotEmpty()) {
                    scope.launch { userDao.upsertAll(cloudUsers) }
                }
            }
        }
    }

    val streamersFlow: Flow<List<StreamerEntity>> = streamerDao.observeAll()
    val suggestionsFlow: Flow<List<SuggestionEntity>> = suggestionDao.observeAll()
    val scheduleFlow: Flow<List<ScheduleEventEntity>> = scheduleDao.observeAll()
    val timelineFlow: Flow<List<TimelinePostEntity>> = timelineDao.observeAll()
    val analysesFlow: Flow<List<AnalysisEntity>> = analysisDao.observeAll()
    val clipsFlow: Flow<List<ClipEntity>> = clipDao.observeAll()
    val artFlow: Flow<List<ArtEntity>> = artDao.observeAll()
    val generalChatFlow: Flow<List<ChatMessageEntity>> = chatDao.observeRoom(GENERAL_CHAT_ROOM)
    val bannedUsersFlow: Flow<List<BannedUserEntity>> = bannedDao.observeAll()
    val communityUsersFlow: Flow<List<CommunityUserEntity>> = userDao.observeAll()

    val userProfileFlow: Flow<UserProfileState> = sessionManager.userProfileFlow
    val kickAuthSessionFlow: Flow<KickAuthSession> = sessionManager.kickAuthSessionFlow
    val notificationPrefsFlow: Flow<NotificationPrefs> = sessionManager.notificationPrefsFlow
    val remoteSettingsFlow: Flow<RemoteSettingsState> = sessionManager.remoteSettingsFlow
    val kickChatConnectionState: StateFlow<ChatConnectionState> = chatWebSocket.connectionState

    suspend fun getRemoteSettingsOnce(): RemoteSettingsState = sessionManager.getRemoteSettingsOnce()

    fun observeStreamer(slug: String): Flow<StreamerEntity?> = streamerDao.observeBySlug(slug)
    fun observeKickChat(slug: String): Flow<List<ChatMessageEntity>> = chatDao.observeRoom("kick:$slug")

    fun searchStreamers(query: String): Flow<List<StreamerEntity>> = streamerDao.search(query)
    fun searchTimeline(query: String): Flow<List<TimelinePostEntity>> = timelineDao.search(query)
    fun searchClips(query: String): Flow<List<ClipEntity>> = clipDao.search(query)
    fun searchArt(query: String): Flow<List<ArtEntity>> = artDao.search(query)

    private suspend fun ensureInitialCommunityContent() {
        if (userDao.count() == 0) {
            userDao.upsert(
                CommunityUserEntity(
                    uid = "admin_respect",
                    displayName = "إدارة RESPECT",
                    email = BuildConfig.ADMIN_EMAIL,
                    bio = "الحساب الإداري الرسمي لمجتمع RESPECT",
                    role = "ADMIN",
                    isMuted = false
                )
            )
        }
        if (timelineDao.count() == 0) {
            timelineDao.insert(
                TimelinePostEntity(
                    remoteId = "welcome_post_1",
                    authorName = "إدارة مجتمع RESPECT",
                    authorIdentifier = BuildConfig.ADMIN_EMAIL,
                    isAdminPost = true,
                    isOfficialAnnouncement = true,
                    content = "أهلاً بكم في تطبيق مجتمع RESPECT! واجهة زجاجية حديثة، بثوث KICK مباشرة بدون تقطيع، ورفع صور عبر Telegraph.",
                    likesCount = 12
                )
            )
        }
        if (chatDao.countRoom(GENERAL_CHAT_ROOM) == 0) {
            chatDao.insert(
                ChatMessageEntity(
                    id = "welcome_general_1",
                    roomKey = GENERAL_CHAT_ROOM,
                    senderName = "إدارة RESPECT",
                    senderColor = "#8B5CF6",
                    isAdmin = true,
                    content = "أهلاً بالجميع في شات السوالف! تقدر تسولف وتناقش فعاليات المجتمع هنا."
                )
            )
        }
        if (scheduleDao.count() == 0) {
            scheduleDao.upsert(
                ScheduleEventEntity(
                    streamerSlug = "respect",
                    streamerName = "بث مجتمع RESPECT",
                    scheduledDate = "اليوم",
                    scheduledTime = "21:00 بتوقيت السعودية",
                    gameCategory = "GTA RP / فعاليات المجتمع",
                    predictionNote = "توقعات اليوم: جولات RP جماعية وتحديات جديدة مع أعضاء السيرفر"
                )
            )
        }
        if (analysisDao.count() == 0) {
            analysisDao.insert(
                AnalysisEntity(
                    streamerSlug = "respect",
                    streamerName = "سيرفر RESPECT",
                    authorName = "محلل المجتمع",
                    title = "نظرة على مجريات البثوث الأخيرة",
                    analysisText = "تصاعد التفاعل في سيناريوهات الشرطة والعصابات، مع توقعات بأحداث قوية في الجدول القادم.",
                    rating = 9
                )
            )
        }
        if (artDao.count() == 0) {
            artDao.insert(
                ArtEntity(
                    title = "هوية RESPECT الرسمية",
                    artistName = "فريق التصميم",
                    imageUrl = "res://respect_banner",
                    aspectRatio = 1.45f,
                    likesCount = 24
                )
            )
            artDao.insert(
                ArtEntity(
                    title = "شعار RESPECT",
                    artistName = "مصمم المجتمع",
                    imageUrl = "res://respect_logo",
                    aspectRatio = 0.85f,
                    likesCount = 19
                )
            )
        }
        if (clipDao.count() == 0) {
            clipDao.insert(
                ClipEntity(
                    streamerSlug = "respect",
                    streamerName = "أبرز لحظات المجتمع",
                    title = "مقتطف تشويقي من فعاليات RESPECT",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    likesCount = 15,
                    uploadedBy = "إدارة RESPECT"
                )
            )
        }
    }

    // --- Telegraph Image Upload ---
    suspend fun uploadImageToTelegraph(context: Context, uri: Uri): Result<String> =
        telegraphUploader.uploadImage(context, uri)

    // --- Firebase Auth & Remote Config ---
    suspend fun signInFirebase(email: String, password: String): Result<UserProfileState> = runCatching {
        val cleanEmail = email.trim()
        require(cleanEmail.isNotBlank() && password.length >= 6) {
            "أدخل البريد الإلكتروني وكلمة المرور (6 أحرف على الأقل)"
        }
        val isHardcodedAdmin =
            cleanEmail.equals(BuildConfig.ADMIN_EMAIL, ignoreCase = true) &&
                password == BuildConfig.ADMIN_PASSWORD

        val firebaseResult = firebaseManager.signInWithEmail(cleanEmail, password)
        val user = firebaseResult.getOrNull()

        if (user != null) {
            val isAdmin = isHardcodedAdmin || cleanEmail.equals(BuildConfig.ADMIN_EMAIL, ignoreCase = true)
            sessionManager.setFirebaseUserSession(
                uid = user.uid,
                email = user.email ?: cleanEmail,
                displayName = user.displayName ?: if (isAdmin) "إدارة RESPECT" else null,
                isAdmin = isAdmin
            )
            val entity = CommunityUserEntity(
                uid = user.uid,
                displayName = user.displayName ?: sessionManager.getProfileOnce().displayName,
                email = user.email ?: cleanEmail,
                role = if (isAdmin) "ADMIN" else "USER"
            )
            userDao.upsert(entity)
            firebaseManager.syncUserProfile(entity)
            sessionManager.getProfileOnce()
        } else if (isHardcodedAdmin) {
            sessionManager.setAdminSession(isAdmin = true, email = BuildConfig.ADMIN_EMAIL)
            sessionManager.getProfileOnce()
        } else {
            throw firebaseResult.exceptionOrNull() ?: IllegalStateException("تعذر تسجيل الدخول في Firebase")
        }
    }

    suspend fun signUpFirebase(
        email: String,
        password: String,
        displayName: String
    ): Result<UserProfileState> = runCatching {
        val cleanEmail = email.trim()
        require(cleanEmail.isNotBlank() && password.length >= 6) {
            "يرجى إدخال بريد إلكتروني صحيح وكلمة مرور من 6 أحرف على الأقل"
        }
        val name = displayName.trim().ifBlank { "عضو RESPECT" }
        val user = firebaseManager.signUpWithEmail(cleanEmail, password, name).getOrThrow()
        val isAdmin = cleanEmail.equals(BuildConfig.ADMIN_EMAIL, ignoreCase = true)
        sessionManager.setFirebaseUserSession(
            uid = user.uid,
            email = user.email ?: cleanEmail,
            displayName = name,
            isAdmin = isAdmin
        )
        val entity = CommunityUserEntity(
            uid = user.uid,
            displayName = name,
            email = user.email ?: cleanEmail,
            role = if (isAdmin) "ADMIN" else "USER"
        )
        userDao.upsert(entity)
        sessionManager.getProfileOnce()
    }

    suspend fun signOutFirebase() {
        firebaseManager.signOut()
        sessionManager.clearFirebaseSession()
    }

    suspend fun refreshRemoteConfig(): RemoteSettingsState {
        val settings = firebaseManager.fetchRemoteConfig()
        sessionManager.saveRemoteSettings(settings)
        return settings
    }

    suspend fun saveGlobalSettings(settings: RemoteSettingsState) {
        sessionManager.saveRemoteSettings(settings)
        firebaseManager.saveGlobalSettings(settings)
    }

    // --- Admin User Management ---
    suspend fun updateUserRole(uid: String, role: String) {
        userDao.updateRole(uid, role)
        firebaseManager.updateUserModeration(uid, role, isMuted = false, mutedUntilMs = 0L, banReason = "")
    }

    suspend fun muteCommunityUser(uid: String, displayName: String, mute: Boolean, reason: String) {
        val until = if (mute) System.currentTimeMillis() + 24L * 60L * 60L * 1000L else 0L
        userDao.updateMute(uid, mute, until, reason)
        if (mute) {
            banUserOneDay(uid, displayName, reason)
        } else {
            unbanUser(uid)
        }
        firebaseManager.updateUserModeration(uid, role = "USER", isMuted = mute, mutedUntilMs = until, banReason = reason)
    }

    // --- 1. KICK API & Streamer Directory ---
    suspend fun getValidAppToken(): String = appTokenMutex.withLock {
        val cached = sessionManager.getCachedAppToken()
        if (!cached.isNullOrBlank()) return@withLock cached

        val response = authApi.getAppToken(
            clientId = BuildConfig.KICK_CLIENT_ID,
            clientSecret = BuildConfig.KICK_CLIENT_SECRET
        )
        sessionManager.saveAppToken(response.accessToken, response.expiresIn)
        response.accessToken
    }

    suspend fun addOrRefreshStreamer(
        rawInput: String,
        characterNames: String = ""
    ): Result<StreamerEntity> = runCatching {
        val cleanSlug = sanitizeSlug(rawInput)
        require(cleanSlug.isNotBlank()) { "يرجى إدخال اسم قناة صحيح على منصة KICK" }

        val appToken = getValidAppToken()
        val authHeader = "Bearer $appToken"
        val channelResp = publicApi.getChannelsBySlug(authHeader, cleanSlug)
        val channel = channelResp.data?.firstOrNull()
            ?: error("لم يتم العثور على القناة \"$cleanSlug\" في KICK")

        val userProfile = runCatching {
            publicApi.getUsersByIds(authHeader, listOf(channel.broadcasterUserId)).data?.firstOrNull()
        }.getOrNull()

        val v2Data = runCatching {
            val resp = v2Api.getV2Channel(channel.slug)
            if (resp.isSuccessful) resp.body() else null
        }.getOrNull()

        val existing = streamerDao.getBySlugOnce(channel.slug)
        val isLive = (channel.stream?.isLive == true) || (v2Data?.livestream?.isLive == true)
        val viewers = maxOf(
            channel.stream?.viewerCount ?: 0,
            v2Data?.livestream?.viewerCount ?: 0
        )
        val peak = maxOf(existing?.peakViewers ?: 0, viewers)

        val entity = StreamerEntity(
            slug = channel.slug,
            broadcasterUserId = channel.broadcasterUserId,
            displayName = userProfile?.name?.takeIf { it.isNotBlank() }
                ?: v2Data?.user?.username?.takeIf { it.isNotBlank() }
                ?: channel.slug,
            characterNames = characterNames.trim().ifBlank { existing?.characterNames.orEmpty() },
            avatarUrl = userProfile?.profilePicture?.takeIf { it.isNotBlank() }
                ?: v2Data?.user?.profilePic.orEmpty(),
            bannerUrl = channel.bannerPicture.orEmpty(),
            isLive = isLive,
            streamTitle = channel.stream?.streamTitle?.takeIf { it.isNotBlank() }
                ?: v2Data?.livestream?.sessionTitle
                ?: channel.channelDescription.orEmpty(),
            categoryName = channel.stream?.category?.name?.takeIf { it.isNotBlank() }
                ?: v2Data?.livestream?.categories?.firstOrNull()?.name
                ?: "بث مباشر",
            viewerCount = if (isLive) viewers else 0,
            peakViewers = peak,
            playbackUrl = v2Data?.playbackUrl.orEmpty(),
            thumbnailUrl = channel.stream?.thumbnail?.takeIf { it.isNotBlank() }
                ?: v2Data?.livestream?.thumbnail?.url.orEmpty(),
            chatroomId = v2Data?.chatroom?.id ?: existing?.chatroomId ?: 0L,
            startedAt = channel.stream?.startTime
                ?: v2Data?.livestream?.createdAt.orEmpty(),
            isFavorite = existing?.isFavorite ?: true,
            isFollowed = existing?.isFollowed ?: false,
            lastUpdatedAt = System.currentTimeMillis()
        )

        streamerDao.upsert(entity)
        entity
    }

    suspend fun refreshAllStreamers(): List<StreamerEntity> {
        val current = streamerDao.getAllOnce()
        val newlyLive = mutableListOf<StreamerEntity>()
        for (existing in current) {
            val updated = addOrRefreshStreamer(existing.slug).getOrNull() ?: continue
            if (!existing.isLive && updated.isLive) {
                newlyLive.add(updated)
            }
        }
        return newlyLive
    }

    suspend fun removeStreamer(slug: String) = streamerDao.deleteBySlug(slug)
    suspend fun toggleFavorite(slug: String, isFavorite: Boolean) =
        streamerDao.updateFavorite(slug, isFavorite)

    suspend fun toggleFollow(slug: String, isFollowed: Boolean) =
        streamerDao.updateFollowed(slug, isFollowed)

    // --- 1b. Suggestions ---
    suspend fun submitSuggestion(kickUsername: String, characterNames: String, reason: String): Result<Unit> =
        runCatching {
            val slug = sanitizeSlug(kickUsername)
            require(slug.isNotBlank()) { "يرجى إدخال اسم الستريمر في KICK" }
            val profile = sessionManager.getProfileOnce()
            suggestionDao.insert(
                SuggestionEntity(
                    kickUsername = slug,
                    characterNames = characterNames.trim(),
                    reason = reason.trim(),
                    suggestedBy = profile.displayName
                )
            )
        }

    suspend fun approveSuggestion(suggestion: SuggestionEntity): Result<StreamerEntity> {
        val result = addOrRefreshStreamer(suggestion.kickUsername, suggestion.characterNames)
        if (result.isSuccess) {
            suggestionDao.updateStatus(suggestion.id, "APPROVED")
        }
        return result
    }

    suspend fun rejectSuggestion(id: Long) = suggestionDao.updateStatus(id, "REJECTED")
    suspend fun deleteSuggestion(id: Long) = suggestionDao.deleteById(id)

    // --- 2. Schedule & Predictions ---
    suspend fun upsertSchedule(event: ScheduleEventEntity) = scheduleDao.upsert(event)
    suspend fun deleteSchedule(id: Long) = scheduleDao.deleteById(id)

    // --- 4. KICK OAuth2 & Live KICK Chat ---
    suspend fun exchangeOAuthCode(code: String, returnedState: String?): Result<KickAuthSession> =
        runCatching {
            val (verifier, expectedState) = sessionManager.consumePendingPkce()
            require(!verifier.isNullOrBlank()) { "انتهت صلاحية جلسة التحقق (PKCE)" }
            if (!expectedState.isNullOrBlank() && !returnedState.isNullOrBlank()) {
                require(expectedState == returnedState) { "فشل التحقق من حالة الأمان" }
            }

            val tokenResp = authApi.exchangeAuthCode(
                clientId = BuildConfig.KICK_CLIENT_ID,
                clientSecret = BuildConfig.KICK_CLIENT_SECRET,
                redirectUri = KickOAuthManager.REDIRECT_URI,
                code = code,
                codeVerifier = verifier
            )

            val userProfile = runCatching {
                publicApi.getCurrentUser("Bearer ${tokenResp.accessToken}").data?.firstOrNull()
            }.getOrNull()

            sessionManager.saveKickTokens(
                accessToken = tokenResp.accessToken,
                refreshToken = tokenResp.refreshToken,
                expiresInSeconds = tokenResp.expiresIn,
                username = userProfile?.name,
                avatarUrl = userProfile?.profilePicture,
                userId = userProfile?.userId
            )

            sessionManager.getKickAuthOnce()
        }

    suspend fun getValidKickUserToken(): String? {
        val session = sessionManager.getKickAuthOnce()
        val token = session.accessToken ?: return null
        if (System.currentTimeMillis() < session.expiresAtMs - 60_000L) return token
        val refresh = session.refreshToken ?: return token
        return runCatching {
            val refreshed = authApi.refreshUserToken(
                clientId = BuildConfig.KICK_CLIENT_ID,
                clientSecret = BuildConfig.KICK_CLIENT_SECRET,
                refreshToken = refresh
            )
            sessionManager.saveKickTokens(
                accessToken = refreshed.accessToken,
                refreshToken = refreshed.refreshToken ?: refresh,
                expiresInSeconds = refreshed.expiresIn,
                username = session.kickUsername,
                avatarUrl = session.kickAvatar,
                userId = session.kickUserId
            )
            refreshed.accessToken
        }.getOrDefault(token)
    }

    suspend fun logoutKick() = sessionManager.clearKickSession()

    fun connectKickChat(slug: String, chatroomId: Long, channelId: Long) {
        chatWebSocket.connect(slug, chatroomId, channelId)
    }

    fun disconnectKickChat() = chatWebSocket.disconnect()

    suspend fun sendKickChatMessage(
        broadcasterUserId: Long,
        slug: String,
        content: String
    ): Result<Unit> = runCatching {
        val trimmed = content.trim()
        require(trimmed.isNotBlank()) { "لا يمكن إرسال رسالة فارغة" }
        val token = getValidKickUserToken()
            ?: error("سجّل الدخول بحساب KICK لإرسال الرسائل في الشات")

        val resp = publicApi.sendChatMessage(
            authorization = "Bearer $token",
            request = SendChatRequest(
                broadcasterUserId = broadcasterUserId,
                content = trimmed,
                type = "user"
            )
        )
        if (!resp.isSuccessful) error("تعذر إرسال الرسالة (${resp.code()})")

        val session = sessionManager.getKickAuthOnce()
        chatDao.insert(
            ChatMessageEntity(
                id = resp.body()?.data?.messageId ?: "kick_${UUID.randomUUID()}",
                roomKey = "kick:$slug",
                senderName = session.kickUsername ?: "أنت",
                senderColor = "#8B5CF6",
                isAdmin = false,
                content = trimmed
            )
        )
    }

    // --- 5. Social Timeline & Moderation ---
    suspend fun createTimelinePost(
        content: String,
        mediaUrl: String,
        mediaType: String,
        isAnnouncement: Boolean
    ): Result<Unit> = runCatching {
        val profile = sessionManager.getProfileOnce()
        val settings = sessionManager.getRemoteSettingsOnce()
        if (!settings.allowGuestPosts && !profile.isFirebaseSignedIn && !profile.isAdmin) {
            error("النشر حالياً متاح للأعضاء المسجلين فقط")
        }
        checkNotBanned(profile)
        val trimmed = content.trim()
        require(trimmed.isNotBlank() || mediaUrl.isNotBlank()) { "اكتب محتوى المنشور أو أرفق صورة" }
        val announcementAllowed = isAnnouncement && profile.isAdmin
        val remoteId = "post_${UUID.randomUUID()}"

        val post = TimelinePostEntity(
            remoteId = remoteId,
            authorName = profile.displayName,
            authorIdentifier = profile.activeIdentifier,
            authorAvatarUrl = profile.avatarUrl,
            isAdminPost = profile.isAdmin,
            isOfficialAnnouncement = announcementAllowed,
            content = trimmed,
            mediaUrl = mediaUrl.trim(),
            mediaType = if (mediaUrl.isBlank()) "NONE" else mediaType
        )
        timelineDao.insert(post)
        val uid = firebaseManager.auth?.currentUser?.uid ?: profile.uid
        if (uid.isNotBlank()) {
            firebaseManager.publishTimelinePost(post, uid)
        }
    }

    suspend fun togglePostLike(post: TimelinePostEntity) =
        timelineDao.toggleLike(post.id, !post.isLikedByMe)

    suspend fun deleteTimelinePost(id: Long) = timelineDao.deleteById(id)

    suspend fun banUserOneDay(identifier: String, displayName: String, reason: String) {
        val oneDayMs = 24L * 60L * 60L * 1000L
        bannedDao.ban(
            BannedUserEntity(
                identifier = identifier,
                displayName = displayName,
                reason = reason.ifBlank { "مخالفة قوانين المجتمع" },
                bannedUntilMs = System.currentTimeMillis() + oneDayMs
            )
        )
    }

    suspend fun unbanUser(identifier: String) = bannedDao.unban(identifier)

    private suspend fun checkNotBanned(profile: UserProfileState) {
        if (profile.isAdmin) return
        val banById = bannedDao.getActiveBan(profile.activeIdentifier)
        val banByEmail = bannedDao.getActiveBan(profile.email)
        val banByName = bannedDao.getActiveBan(profile.displayName)
        val activeBan = banById ?: banByEmail ?: banByName ?: return
        val hoursLeft = ((activeBan.bannedUntilMs - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(1L)
        error("حسابك مقيد مؤقتاً لمدة $hoursLeft ساعة: ${activeBan.reason}")
    }

    // --- 6. General Chat Room ---
    suspend fun sendGeneralChatMessage(content: String): Result<Unit> = runCatching {
        val profile = sessionManager.getProfileOnce()
        checkNotBanned(profile)
        val trimmed = content.trim()
        require(trimmed.isNotBlank()) { "اكتب رسالة أولاً" }
        chatDao.insert(
            ChatMessageEntity(
                id = "gen_${UUID.randomUUID()}",
                roomKey = GENERAL_CHAT_ROOM,
                senderName = profile.displayName,
                senderColor = if (profile.isAdmin) "#8B5CF6" else "#38BDF8",
                isAdmin = profile.isAdmin,
                content = trimmed
            )
        )
        chatDao.trimRoom(GENERAL_CHAT_ROOM)
    }

    suspend fun deleteChatMessage(id: String) = chatDao.deleteById(id)

    // --- 7. Stream Analysis ---
    suspend fun addAnalysis(
        streamerSlug: String,
        streamerName: String,
        title: String,
        analysisText: String,
        rating: Int
    ): Result<Unit> = runCatching {
        val profile = sessionManager.getProfileOnce()
        checkNotBanned(profile)
        require(title.isNotBlank() && analysisText.isNotBlank()) { "أدخل عنوان التحليل والمحتوى" }
        analysisDao.insert(
            AnalysisEntity(
                streamerSlug = streamerSlug,
                streamerName = streamerName.ifBlank { "بث KICK" },
                authorName = profile.displayName,
                title = title.trim(),
                analysisText = analysisText.trim(),
                rating = rating.coerceIn(1, 10)
            )
        )
    }

    suspend fun deleteAnalysis(id: Long) = analysisDao.deleteById(id)

    // --- 8. Clips ---
    suspend fun addClip(streamerSlug: String, streamerName: String, title: String, videoUrl: String): Result<Unit> =
        runCatching {
            val profile = sessionManager.getProfileOnce()
            checkNotBanned(profile)
            require(title.isNotBlank() && videoUrl.isNotBlank()) { "أدخل عنوان المقطع ورابط الفيديو" }
            clipDao.insert(
                ClipEntity(
                    streamerSlug = sanitizeSlug(streamerSlug).ifBlank { "respect" },
                    streamerName = streamerName.trim().ifBlank { "ستريمر RESPECT" },
                    title = title.trim(),
                    videoUrl = videoUrl.trim(),
                    uploadedBy = profile.displayName
                )
            )
        }

    suspend fun toggleClipLike(clip: ClipEntity) = clipDao.toggleLike(clip.id, !clip.isLikedByMe)
    suspend fun deleteClip(id: Long) = clipDao.deleteById(id)

    // --- 9. Art Gallery ---
    suspend fun addArt(title: String, artistName: String, imageUrl: String): Result<Unit> = runCatching {
        val profile = sessionManager.getProfileOnce()
        checkNotBanned(profile)
        require(title.isNotBlank() && imageUrl.isNotBlank()) { "أدخل عنوان العمل ورابط الصورة" }
        artDao.insert(
            ArtEntity(
                title = title.trim(),
                artistName = artistName.trim().ifBlank { profile.displayName },
                imageUrl = imageUrl.trim(),
                aspectRatio = listOf(0.72f, 0.88f, 1.1f, 1.35f).random()
            )
        )
    }

    suspend fun toggleArtLike(art: ArtEntity) = artDao.toggleLike(art.id, !art.isLikedByMe)
    suspend fun deleteArt(id: Long) = artDao.deleteById(id)

    // --- 12. Profile & Admin Auth ---
    suspend fun updateProfile(name: String, bio: String, avatarUrl: String) {
        sessionManager.updateProfile(name, bio, avatarUrl)
        val profile = sessionManager.getProfileOnce()
        if (profile.uid.isNotBlank()) {
            val entity = CommunityUserEntity(
                uid = profile.uid,
                displayName = profile.displayName,
                email = profile.email,
                avatarUrl = profile.avatarUrl,
                bio = profile.bio,
                role = if (profile.isAdmin) "ADMIN" else "USER"
            )
            userDao.upsert(entity)
            firebaseManager.syncUserProfile(entity)
        }
    }

    suspend fun loginAdmin(email: String, password: String): Result<Unit> = runCatching {
        val validEmail = email.trim().equals(BuildConfig.ADMIN_EMAIL, ignoreCase = true)
        val validPass = password == BuildConfig.ADMIN_PASSWORD
        require(validEmail && validPass) { "بيانات دخول الإدارة غير صحيحة" }
        sessionManager.setAdminSession(isAdmin = true, email = BuildConfig.ADMIN_EMAIL)
        runCatching { firebaseManager.signInWithEmail(BuildConfig.ADMIN_EMAIL, BuildConfig.ADMIN_PASSWORD) }
    }

    suspend fun logoutAdmin() = sessionManager.setAdminSession(isAdmin = false)

    // --- Notification Preferences ---
    suspend fun setMonitoringEnabled(enabled: Boolean) = sessionManager.setMonitoringEnabled(enabled)
    suspend fun setSoundEnabled(enabled: Boolean) = sessionManager.setSoundEnabled(enabled)
    suspend fun setVibrationEnabled(enabled: Boolean) = sessionManager.setVibrationEnabled(enabled)
    suspend fun setFavoritesOnly(favoritesOnly: Boolean) = sessionManager.setFavoritesOnly(favoritesOnly)

    companion object {
        const val GENERAL_CHAT_ROOM = "general"

        fun sanitizeSlug(input: String): String {
            var cleaned = input.trim().lowercase()
            cleaned = cleaned.removePrefix("https://").removePrefix("http://")
            cleaned = cleaned.removePrefix("www.")
            if (cleaned.startsWith("kick.com/")) {
                cleaned = cleaned.removePrefix("kick.com/")
            }
            return cleaned.substringBefore("/").substringBefore("?").trim()
        }
    }
}
