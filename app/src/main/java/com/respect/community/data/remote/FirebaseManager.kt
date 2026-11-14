package com.respect.community.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.respect.community.BuildConfig
import com.respect.community.data.local.RemoteSettingsState
import com.respect.community.data.local.entity.CommunityUserEntity
import com.respect.community.data.local.entity.TimelinePostEntity
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

class FirebaseManager(context: Context) {

    val isConfigured: Boolean =
        BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

    private var firebaseApp: FirebaseApp? = null
    var auth: FirebaseAuth? = null
        private set
    var firestore: FirebaseFirestore? = null
        private set
    var remoteConfig: FirebaseRemoteConfig? = null
        private set

    init {
        if (isConfigured) {
            runCatching {
                val app = FirebaseApp.getApps(context).firstOrNull() ?: run {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET.ifBlank { null })
                        .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID.ifBlank { null })
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
                firebaseApp = app
                auth = FirebaseAuth.getInstance(app)
                firestore = if (BuildConfig.FIREBASE_DATABASE_ID.isNotBlank()) {
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
            }.onFailure {
                Log.e(TAG, "Firebase init failed: ${it.message}")
            }
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

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val firebaseAuth = auth ?: error("Firebase غير مفعّل")
        val res = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        res.user ?: error("تعذر تسجيل الدخول")
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> = runCatching {
        val firebaseAuth = auth ?: error("Firebase غير مفعّل")
        val res = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = res.user ?: error("تعذر إنشاء الحساب")
        if (displayName.isNotBlank()) {
            val update = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
            user.updateProfile(update).await()
        }
        syncUserProfile(
            CommunityUserEntity(
                uid = user.uid,
                displayName = displayName.trim().ifBlank { "عضو RESPECT" },
                email = user.email.orEmpty(),
                role = "USER",
                isMuted = false
            )
        )
        user
    }

    suspend fun ensureAnonymousSession(): FirebaseUser? {
        val firebaseAuth = auth ?: return null
        firebaseAuth.currentUser?.let { return it }
        return runCatching { firebaseAuth.signInAnonymously().await().user }.getOrNull()
    }

    fun signOut() {
        auth?.signOut()
    }

    suspend fun syncUserProfile(user: CommunityUserEntity) {
        val db = firestore ?: return
        val path = "users/${user.uid}"
        val now = System.currentTimeMillis()
        val payload = hashMapOf(
            "uid" to user.uid,
            "displayName" to user.displayName.take(80),
            "email" to user.email.take(160),
            "avatarUrl" to user.avatarUrl.take(512),
            "bio" to user.bio.take(300),
            "role" to user.role,
            "isMuted" to user.isMuted,
            "mutedUntilMs" to user.mutedUntilMs,
            "banReason" to user.banReason.take(240),
            "createdAt" to now,
            "updatedAt" to now
        )
        try {
            db.collection("users").document(user.uid).set(payload).await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.WRITE, path)
        }
    }

    suspend fun updateUserModeration(
        uid: String,
        role: String,
        isMuted: Boolean,
        mutedUntilMs: Long,
        banReason: String
    ) {
        val db = firestore ?: return
        val path = "users/$uid"
        val updates = mapOf(
            "role" to role,
            "isMuted" to isMuted,
            "mutedUntilMs" to mutedUntilMs,
            "banReason" to banReason.take(240),
            "updatedAt" to System.currentTimeMillis()
        )
        try {
            db.collection("users").document(uid).update(updates).await()
        } catch (error: Throwable) {
            logPermissionOrFirestoreError(error, OperationType.UPDATE, path)
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
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logPermissionOrFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    CommunityUserEntity(
                        uid = uid,
                        displayName = doc.getString("displayName") ?: "عضو RESPECT",
                        email = doc.getString("email").orEmpty(),
                        avatarUrl = doc.getString("avatarUrl").orEmpty(),
                        bio = doc.getString("bio").orEmpty(),
                        role = doc.getString("role") ?: "USER",
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
