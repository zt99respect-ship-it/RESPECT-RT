package com.respect.community.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.respect.community.BuildConfig
import com.respect.community.data.local.RemoteSettingsState
import com.respect.community.data.local.entity.CommunityUserEntity
import com.respect.community.data.local.entity.StreamSnapshotEntity
import com.respect.community.data.local.entity.StreamerEntity
import com.respect.community.data.local.entity.TimelinePostEntity
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.UUID

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

data class TimelinePageResult(
    val posts: List<TimelinePostEntity>,
    val lastSnapshot: DocumentSnapshot?,
    val hasMore: Boolean
)

data class AdminNotificationPayload(
    val id: String,
    val title: String,
    val body: String,
    val targetUid: String,
    val streamerSlug: String,
    val createdAt: Long
)

class FirebaseManager(context: Context) {

    var isConfigured: Boolean = false
        private set

    private var firebaseApp: FirebaseApp? = null
    var auth: FirebaseAuth? = null
        private set
    var firestore: FirebaseFirestore? = null
        private set
    var remoteConfig: FirebaseRemoteConfig? = null
        private set

    init {
        runCatching {
            val existingApp = FirebaseApp.getApps(context).firstOrNull()
            val hasAppletConfig = BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
                BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
                BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

            val app = existingApp ?: if (hasAppletConfig) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET.ifBlank { null })
                    .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID.ifBlank { null })
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.initializeApp(context)
            }
            if (app != null) {
                firebaseApp = app
                auth = if (existingApp != null) FirebaseAuth.getInstance() else FirebaseAuth.getInstance(app)
                val sameAppletProject = app.options.projectId == BuildConfig.FIREBASE_PROJECT_ID
                firestore = if (sameAppletProject && BuildConfig.FIREBASE_DATABASE_ID.isNotBlank()) {
                    FirebaseFirestore.getInstance(app, BuildConfig.FIREBASE_DATABASE_ID)
                } else {
                    FirebaseFirestore.getInstance(app)
                }
                remoteConfig = FirebaseRemoteConfig.getInstance(app).apply {
                    val settings = FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(300L)
                        .build()
                    setConfigSettingsAsync(settings)
                    setDefaultsAsync(
                        mapOf(
                            "announcement_banner" to "",
                            "maintenance_mode" to false,
                            "poll_interval_seconds" to 15L,
                            "allow_guest_posts" to true
                        )
                    )
                }
                isConfigured = auth != null && firestore != null
            }
        }.onFailure {
            Log.e(TAG, "Firebase init failed: ${it.message}")
        }
    }

    suspend fun testConnection() {
        val db = firestore ?: return
        try {
            db.collection("test").document("connection").get(Source.SERVER).await()
        } catch (error: Throwable) {
            if (error.message?.contains("offline", ignoreCase = true) == true) {
                Log.e(TAG, "Firestore client offline; check Firebase configuration.")
            }
        }
    }

    suspend fun fetchRemoteConfig(): RemoteSettingsState {
        val rc = remoteConfig ?: return RemoteSettingsState()
        runCatching { rc.fetchAndActivate().await() }
        return RemoteSettingsState(
            maintenanceMode = rc.getBoolean("maintenance_mode"),
            bannerText = rc.getString("announcement_banner"),
            allowGuestPosts = rc.getBoolean("allow_guest_posts"),
            pollIntervalSeconds = rc.getLong("poll_interval_seconds").toInt().coerceIn(10, 120)
        )
    }

    suspend fun fetchFcmToken(): String? = runCatching {
        FirebaseMessaging.getInstance().token.await()
    }.getOrNull()

    private fun resolveAuth(): FirebaseAuth? = auth ?: runCatching { FirebaseAuth.getInstance() }.getOrNull()?.also { auth = it }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val firebaseAuth = resolveAuth() ?: error("Firebase غير مفعّل")
        val res = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        res.user ?: error("تعذر تسجيل الدخول")
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> = runCatching {
        val firebaseAuth = resolveAuth() ?: error("Firebase غير مفعّل")
        val res = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = res.user ?: error("تعذر إنشاء الحساب")
        if (displayName.isNotBlank()) {
            val update = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
            user.updateProfile(update).await()
        }
        runCatching { user.sendEmailVerification().await() }
        syncUserProfile(
            CommunityUserEntity(
                uid = user.uid,
                displayName = displayName.trim().ifBlank { "عضو RESPECT" },
                email = user.email.orEmpty(),
                role = "USER",
                banned = false,
                isMuted = false
            )
        )
        user
    }

    suspend fun sendVerificationEmail(): Result<Unit> = runCatching {
        val user = resolveAuth()?.currentUser ?: error("لا يوجد مستخدم مسجل حالياً")
        user.sendEmailVerification().await()
    }

    suspend fun reloadCurrentUser(): FirebaseUser? {
        val firebaseAuth = resolveAuth() ?: return null
        val user = firebaseAuth.currentUser ?: return null
        runCatching { user.reload().await() }
        return firebaseAuth.currentUser
    }

    fun signOut() {
        resolveAuth()?.signOut()
    }

    suspend fun syncUserProfile(user: CommunityUserEntity, fcmToken: String? = null) {
        val db = firestore ?: return
        val path = "users/${user.uid}"
        val now = System.currentTimeMillis()
        val payload = hashMapOf<String, Any>(
            "uid" to user.uid,
            "displayName" to user.displayName.take(80),
            "email" to user.email.take(160),
            "avatarUrl" to user.avatarUrl.take(512),
            "bannerUrl" to user.bannerUrl.take(512),
            "bio" to user.bio.take(300),
            "role" to user.role,
            "banned" to user.banned,
            "isMuted" to user.isMuted,
            "mutedUntilMs" to user.mutedUntilMs,
            "banReason" to user.banReason.take(240),
            "createdAt" to now,
            "updatedAt" to now
        )
        if (!fcmToken.isNullOrBlank()) {
            payload["fcmToken"] = fcmToken.take(512)
        }
        try {
            db.collection("users").document(user.uid).set(payload).await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.WRITE, path)
        }
    }

    suspend fun updateUserModeration(
        uid: String,
        role: String? = null,
        banned: Boolean? = null,
        isMuted: Boolean? = null,
        mutedUntilMs: Long? = null,
        banReason: String? = null
    ) {
        val db = firestore ?: return
        val path = "users/$uid"
        val updates = mutableMapOf<String, Any>("updatedAt" to System.currentTimeMillis())
        if (role != null) updates["role"] = role
        if (banned != null) updates["banned"] = banned
        if (isMuted != null) updates["isMuted"] = isMuted
        if (mutedUntilMs != null) updates["mutedUntilMs"] = mutedUntilMs
        if (banReason != null) updates["banReason"] = banReason.take(240)
        try {
            db.collection("users").document(uid).update(updates).await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.UPDATE, path)
        }
    }

    suspend fun deleteUserDocument(uid: String) {
        val db = firestore ?: return
        val path = "users/$uid"
        try {
            db.collection("users").document(uid).delete().await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.DELETE, path)
        }
    }

    // --- Paginated Timeline Posts (10 posts per page) ---
    suspend fun fetchTimelinePage(
        pageSize: Long = 10L,
        afterSnapshot: DocumentSnapshot? = null
    ): Result<TimelinePageResult> = runCatching {
        val db = firestore ?: return@runCatching TimelinePageResult(emptyList(), null, false)
        val path = "timeline_posts"
        try {
            var query: Query = db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize)
            if (afterSnapshot != null) {
                query = query.startAfter(afterSnapshot)
            }
            val snap = query.get().await()
            val posts = snap.documents.map { doc -> doc.toTimelineEntity() }
            TimelinePageResult(
                posts = posts,
                lastSnapshot = snap.documents.lastOrNull(),
                hasMore = snap.documents.size >= pageSize
            )
        } catch (error: Throwable) {
            handleFirestoreError(error, OperationType.LIST, path)
        }
    }

    suspend fun publishTimelinePost(post: TimelinePostEntity, uid: String) {
        val db = firestore ?: return
        val docId = post.remoteId.ifBlank { "post_${post.createdAt}" }
        val path = "timeline_posts/$docId"
        val payload = hashMapOf(
            "id" to docId,
            "authorUid" to uid,
            "authorName" to post.authorName.take(80),
            "authorAvatarUrl" to post.authorAvatarUrl.take(512),
            "isAdminPost" to post.isAdminPost,
            "isOfficialAnnouncement" to post.isOfficialAnnouncement,
            "isPinned" to post.isPinned,
            "content" to post.content.take(1200),
            "mediaUrl" to post.mediaUrl.take(512),
            "mediaType" to post.mediaType,
            "likesCount" to post.likesCount,
            "createdAt" to post.createdAt
        )
        try {
            db.collection("timeline_posts").document(docId).set(payload).await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.CREATE, path)
        }
    }

    suspend fun setPostPinned(remoteId: String, isPinned: Boolean) {
        val db = firestore ?: return
        if (remoteId.isBlank()) return
        val path = "timeline_posts/$remoteId"
        try {
            db.collection("timeline_posts").document(remoteId).update("isPinned", isPinned).await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.UPDATE, path)
        }
    }

    suspend fun deleteTimelinePostRemote(remoteId: String) {
        val db = firestore ?: return
        if (remoteId.isBlank()) return
        val path = "timeline_posts/$remoteId"
        try {
            db.collection("timeline_posts").document(remoteId).delete().await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.DELETE, path)
        }
    }

    // --- Global & Featured Streamers ---
    suspend fun saveGlobalStreamer(streamer: StreamerEntity, addedByUid: String) {
        val db = firestore ?: return
        val path = "streamers/${streamer.slug}"
        val now = System.currentTimeMillis()
        val payload = hashMapOf(
            "slug" to streamer.slug,
            "displayName" to streamer.displayName.take(80),
            "characterNames" to streamer.characterNames.take(200),
            "broadcasterUserId" to streamer.broadcasterUserId,
            "isGlobal" to true,
            "isFeatured" to streamer.isFeatured,
            "addedByUid" to addedByUid,
            "createdAt" to now,
            "updatedAt" to now
        )
        try {
            db.collection("streamers").document(streamer.slug).set(payload).await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.WRITE, path)
        }
    }

    suspend fun setStreamerFeaturedRemote(slug: String, isFeatured: Boolean) {
        val db = firestore ?: return
        val path = "streamers/$slug"
        try {
            db.collection("streamers").document(slug).update(
                mapOf("isFeatured" to isFeatured, "updatedAt" to System.currentTimeMillis())
            ).await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.UPDATE, path)
        }
    }

    suspend fun deleteGlobalStreamerRemote(slug: String) {
        val db = firestore ?: return
        val path = "streamers/$slug"
        try {
            db.collection("streamers").document(slug).delete().await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.DELETE, path)
        }
    }

    // --- 10-Min KICK Analytics Snapshots ---
    suspend fun saveStreamSnapshots(snapshots: List<StreamSnapshotEntity>) {
        val db = firestore ?: return
        if (snapshots.isEmpty()) return
        for (snap in snapshots) {
            val docId = "snap_${snap.streamerSlug}_${snap.recordedAtMs}"
            val path = "stream_analytics/$docId"
            val payload = hashMapOf(
                "id" to docId,
                "streamerSlug" to snap.streamerSlug.take(80),
                "streamerName" to snap.streamerName.take(80),
                "isLive" to snap.isLive,
                "viewerCount" to snap.viewerCount,
                "followerCount" to snap.followerCount,
                "streamTitle" to snap.streamTitle.take(300),
                "recordedAtMs" to snap.recordedAtMs
            )
            try {
                db.collection("stream_analytics").document(docId).set(payload).await()
            } catch (error: Throwable) {
                logPermissionOrFirestoreError(error, OperationType.CREATE, path)
            }
        }
    }

    suspend fun fetchRecentSnapshots(sinceMs: Long): List<StreamSnapshotEntity> {
        val db = firestore ?: return emptyList()
        val path = "stream_analytics"
        return try {
            val snap = db.collection(path)
                .whereGreaterThanOrEqualTo("recordedAtMs", sinceMs)
                .orderBy("recordedAtMs", Query.Direction.DESCENDING)
                .limit(500)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                val slug = doc.getString("streamerSlug") ?: return@mapNotNull null
                StreamSnapshotEntity(
                    streamerSlug = slug,
                    streamerName = doc.getString("streamerName") ?: slug,
                    isLive = doc.getBoolean("isLive") ?: false,
                    viewerCount = (doc.getLong("viewerCount") ?: 0L).toInt(),
                    followerCount = (doc.getLong("followerCount") ?: 0L).toInt(),
                    streamTitle = doc.getString("streamTitle").orEmpty(),
                    recordedAtMs = doc.getLong("recordedAtMs") ?: 0L
                )
            }
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.LIST, path)
            emptyList()
        }
    }

    // --- Admin Notification Center ---
    suspend fun dispatchNotification(
        title: String,
        body: String,
        targetUid: String = "",
        streamerSlug: String = ""
    ): Result<Unit> = runCatching {
        val db = firestore ?: error("Firestore غير متاح")
        val docId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "notifications/$docId"
        val sender = auth?.currentUser?.uid ?: "admin"
        val payload = hashMapOf(
            "id" to docId,
            "title" to title.take(140),
            "body" to body.take(400),
            "targetUid" to targetUid.trim().take(128),
            "streamerSlug" to streamerSlug.trim().take(80),
            "senderUid" to sender,
            "createdAt" to System.currentTimeMillis()
        )
        try {
            db.collection("notifications").document(docId).set(payload).await()
        } catch (error: Throwable) {
            handleFirestoreError(error, OperationType.CREATE, path)
        }
    }

    suspend fun saveGlobalSettings(settings: RemoteSettingsState) {
        val db = firestore ?: return
        val path = "app_settings/global"
        val payload = hashMapOf(
            "maintenanceMode" to settings.maintenanceMode,
            "bannerText" to settings.bannerText.take(300),
            "allowGuestPosts" to settings.allowGuestPosts,
            "pollIntervalSeconds" to settings.pollIntervalSeconds,
            "updatedAt" to System.currentTimeMillis()
        )
        try {
            db.collection("app_settings").document("global").set(payload).await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.WRITE, path)
        }
    }

    fun listenUsers(onUsers: (List<CommunityUserEntity>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        val path = "users"
        return db.collection(path)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(150)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logPermissionOrFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty().map { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    CommunityUserEntity(
                        uid = uid,
                        displayName = doc.getString("displayName") ?: "عضو RESPECT",
                        email = doc.getString("email").orEmpty(),
                        avatarUrl = doc.getString("avatarUrl").orEmpty(),
                        bannerUrl = doc.getString("bannerUrl").orEmpty(),
                        bio = doc.getString("bio").orEmpty(),
                        role = doc.getString("role") ?: "USER",
                        banned = doc.getBoolean("banned") ?: false,
                        isMuted = doc.getBoolean("isMuted") ?: false,
                        mutedUntilMs = doc.getLong("mutedUntilMs") ?: 0L,
                        banReason = doc.getString("banReason").orEmpty(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                }
                onUsers(list)
            }
    }

    fun listenGlobalSettings(onSettings: (RemoteSettingsState) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        val path = "app_settings/global"
        return db.collection("app_settings").document("global")
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    logPermissionOrFirestoreError(error, OperationType.GET, path)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    onSettings(
                        RemoteSettingsState(
                            maintenanceMode = doc.getBoolean("maintenanceMode") ?: false,
                            bannerText = doc.getString("bannerText").orEmpty(),
                            allowGuestPosts = doc.getBoolean("allowGuestPosts") ?: true,
                            pollIntervalSeconds = (doc.getLong("pollIntervalSeconds") ?: 15L)
                                .toInt().coerceIn(10, 120)
                        )
                    )
                }
            }
    }

    private fun DocumentSnapshot.toTimelineEntity(): TimelinePostEntity {
        val remote = getString("id") ?: id
        return TimelinePostEntity(
            remoteId = remote,
            authorUid = getString("authorUid").orEmpty(),
            authorName = getString("authorName") ?: "عضو RESPECT",
            authorIdentifier = getString("authorUid") ?: "member",
            authorAvatarUrl = getString("authorAvatarUrl").orEmpty(),
            isAdminPost = getBoolean("isAdminPost") ?: false,
            isOfficialAnnouncement = getBoolean("isOfficialAnnouncement") ?: false,
            isPinned = getBoolean("isPinned") ?: false,
            content = getString("content").orEmpty(),
            mediaUrl = getString("mediaUrl").orEmpty(),
            mediaType = getString("mediaType") ?: "NONE",
            likesCount = (getLong("likesCount") ?: 0L).toInt(),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    fun handleFirestoreError(
        error: Throwable,
        operationType: OperationType,
        path: String?
    ): Nothing {
        val json = buildErrorJson(error, operationType, path)
        Log.e(TAG, "Firestore Error: $json")
        throw IllegalStateException(json, error)
    }

    private fun logPermissionOrFirestoreError(
        error: Throwable,
        operationType: OperationType,
        path: String?
    ) {
        val json = buildErrorJson(error, operationType, path)
        Log.w(TAG, "Firestore Op ($operationType @ $path): $json")
    }

    private fun buildErrorJson(
        error: Throwable,
        operationType: OperationType,
        path: String?
    ): String {
        val current = auth?.currentUser
        val authInfo = JSONObject().apply {
            put("userId", current?.uid)
            put("email", current?.email)
            put("emailVerified", current?.isEmailVerified)
            put("isAnonymous", current?.isAnonymous)
            put("tenantId", current?.tenantId)
        }
        return JSONObject().apply {
            put("error", error.message ?: error.toString())
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfo)
        }.toString()
    }

    companion object {
        private const val TAG = "RespectFirebase"
    }
}
