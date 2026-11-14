package com.respect.community.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.communityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "respect_community_prefs"
)

data class UserProfileState(
    val uid: String = "",
    val displayName: String = "عضو RESPECT",
    val email: String = "",
    val bio: String = "متابع ومشجع لمجتمع ريسبكت على KICK",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val role: String = "USER",
    val isAdmin: Boolean = false,
    val isFirebaseSignedIn: Boolean = false,
    val isEmailVerified: Boolean = false
) {
    val activeIdentifier: String
        get() = uid.ifBlank { email.ifBlank { displayName } }
}

data class KickAuthSession(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAtMs: Long = 0L,
    val kickUsername: String? = null,
    val kickAvatar: String? = null,
    val kickUserId: Long? = null
) {
    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank()
}

data class NotificationPrefs(
    val monitoringEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val favoritesOnly: Boolean = false
)

data class RemoteSettingsState(
    val maintenanceMode: Boolean = false,
    val bannerText: String = "",
    val allowGuestPosts: Boolean = true,
    val pollIntervalSeconds: Int = 15
)

class SessionManager(private val context: Context) {

    private object Keys {
        val PROFILE_UID = stringPreferencesKey("profile_uid")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_EMAIL = stringPreferencesKey("profile_email")
        val PROFILE_BIO = stringPreferencesKey("profile_bio")
        val PROFILE_AVATAR = stringPreferencesKey("profile_avatar")
        val PROFILE_BANNER = stringPreferencesKey("profile_banner")
        val PROFILE_ROLE = stringPreferencesKey("profile_role")
        val IS_ADMIN = booleanPreferencesKey("is_admin")
        val IS_FIREBASE_SIGNED_IN = booleanPreferencesKey("is_firebase_signed_in")
        val IS_EMAIL_VERIFIED = booleanPreferencesKey("is_email_verified")

        val KICK_ACCESS_TOKEN = stringPreferencesKey("kick_access_token")
        val KICK_REFRESH_TOKEN = stringPreferencesKey("kick_refresh_token")
        val KICK_EXPIRES_AT = longPreferencesKey("kick_expires_at")
        val KICK_USERNAME = stringPreferencesKey("kick_username")
        val KICK_AVATAR = stringPreferencesKey("kick_avatar")
        val KICK_USER_ID = longPreferencesKey("kick_user_id")

        val APP_ACCESS_TOKEN = stringPreferencesKey("app_access_token")
        val APP_EXPIRES_AT = longPreferencesKey("app_expires_at")

        val PKCE_VERIFIER = stringPreferencesKey("pkce_verifier")
        val OAUTH_STATE = stringPreferencesKey("oauth_state")

        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val FAVORITES_ONLY = booleanPreferencesKey("favorites_only")

        val REMOTE_MAINTENANCE = booleanPreferencesKey("remote_maintenance")
        val REMOTE_BANNER = stringPreferencesKey("remote_banner")
        val REMOTE_ALLOW_GUEST = booleanPreferencesKey("remote_allow_guest")
        val REMOTE_POLL_SEC = intPreferencesKey("remote_poll_sec")
    }

    val userProfileFlow: Flow<UserProfileState> = context.communityDataStore.data.map { prefs ->
        UserProfileState(
            uid = prefs[Keys.PROFILE_UID].orEmpty(),
            displayName = prefs[Keys.PROFILE_NAME] ?: "عضو RESPECT",
            email = prefs[Keys.PROFILE_EMAIL].orEmpty(),
            bio = prefs[Keys.PROFILE_BIO] ?: "متابع ومشجع لمجتمع ريسبكت على KICK",
            avatarUrl = prefs[Keys.PROFILE_AVATAR].orEmpty(),
            bannerUrl = prefs[Keys.PROFILE_BANNER].orEmpty(),
            role = prefs[Keys.PROFILE_ROLE] ?: "USER",
            isAdmin = prefs[Keys.IS_ADMIN] ?: false,
            isFirebaseSignedIn = prefs[Keys.IS_FIREBASE_SIGNED_IN] ?: false,
            isEmailVerified = prefs[Keys.IS_EMAIL_VERIFIED] ?: false
        )
    }

    val kickAuthSessionFlow: Flow<KickAuthSession> = context.communityDataStore.data.map { prefs ->
        KickAuthSession(
            accessToken = prefs[Keys.KICK_ACCESS_TOKEN],
            refreshToken = prefs[Keys.KICK_REFRESH_TOKEN],
            expiresAtMs = prefs[Keys.KICK_EXPIRES_AT] ?: 0L,
            kickUsername = prefs[Keys.KICK_USERNAME],
            kickAvatar = prefs[Keys.KICK_AVATAR],
            kickUserId = prefs[Keys.KICK_USER_ID]
        )
    }

    val notificationPrefsFlow: Flow<NotificationPrefs> = context.communityDataStore.data.map { prefs ->
        NotificationPrefs(
            monitoringEnabled = prefs[Keys.MONITORING_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            favoritesOnly = prefs[Keys.FAVORITES_ONLY] ?: false
        )
    }

    val remoteSettingsFlow: Flow<RemoteSettingsState> = context.communityDataStore.data.map { prefs ->
        RemoteSettingsState(
            maintenanceMode = prefs[Keys.REMOTE_MAINTENANCE] ?: false,
            bannerText = prefs[Keys.REMOTE_BANNER].orEmpty(),
            allowGuestPosts = prefs[Keys.REMOTE_ALLOW_GUEST] ?: true,
            pollIntervalSeconds = (prefs[Keys.REMOTE_POLL_SEC] ?: 15).coerceIn(10, 120)
        )
    }

    suspend fun getProfileOnce(): UserProfileState = userProfileFlow.first()
    suspend fun getKickAuthOnce(): KickAuthSession = kickAuthSessionFlow.first()
    suspend fun getNotificationPrefsOnce(): NotificationPrefs = notificationPrefsFlow.first()
    suspend fun getRemoteSettingsOnce(): RemoteSettingsState = remoteSettingsFlow.first()

    suspend fun updateProfile(name: String, bio: String, avatarUrl: String, bannerUrl: String = "") {
        context.communityDataStore.edit { prefs ->
            prefs[Keys.PROFILE_NAME] = name.trim().ifBlank { "عضو RESPECT" }
            prefs[Keys.PROFILE_BIO] = bio.trim()
            prefs[Keys.PROFILE_AVATAR] = avatarUrl.trim()
            prefs[Keys.PROFILE_BANNER] = bannerUrl.trim()
        }
    }

    suspend fun setFirebaseUserSession(
        uid: String,
        email: String,
        displayName: String?,
        isAdmin: Boolean,
        isEmailVerified: Boolean,
        role: String = if (isAdmin) "ADMIN" else "USER"
    ) {
        context.communityDataStore.edit { prefs ->
            prefs[Keys.PROFILE_UID] = uid
            if (email.isNotBlank()) prefs[Keys.PROFILE_EMAIL] = email
            if (!displayName.isNullOrBlank()) prefs[Keys.PROFILE_NAME] = displayName
            prefs[Keys.PROFILE_ROLE] = role
            prefs[Keys.IS_ADMIN] = isAdmin
            prefs[Keys.IS_FIREBASE_SIGNED_IN] = uid.isNotBlank()
            prefs[Keys.IS_EMAIL_VERIFIED] = isEmailVerified
        }
    }

    suspend fun clearFirebaseSession() {
        context.communityDataStore.edit { prefs ->
            prefs.remove(Keys.PROFILE_UID)
            prefs.remove(Keys.PROFILE_EMAIL)
            prefs[Keys.PROFILE_ROLE] = "USER"
            prefs[Keys.IS_FIREBASE_SIGNED_IN] = false
            prefs[Keys.IS_EMAIL_VERIFIED] = false
            prefs[Keys.IS_ADMIN] = false
        }
    }

    suspend fun setAdminSession(isAdmin: Boolean, email: String? = null) {
        context.communityDataStore.edit { prefs ->
            prefs[Keys.IS_ADMIN] = isAdmin
            prefs[Keys.PROFILE_ROLE] = if (isAdmin) "ADMIN" else "USER"
            if (isAdmin && !email.isNullOrBlank()) {
                prefs[Keys.PROFILE_EMAIL] = email
                prefs[Keys.IS_EMAIL_VERIFIED] = true
                if (prefs[Keys.PROFILE_NAME].isNullOrBlank() || prefs[Keys.PROFILE_NAME] == "عضو RESPECT") {
                    prefs[Keys.PROFILE_NAME] = "إدارة RESPECT"
                }
            }
        }
    }

    suspend fun saveRemoteSettings(settings: RemoteSettingsState) {
        context.communityDataStore.edit { prefs ->
            prefs[Keys.REMOTE_MAINTENANCE] = settings.maintenanceMode
            prefs[Keys.REMOTE_BANNER] = settings.bannerText
            prefs[Keys.REMOTE_ALLOW_GUEST] = settings.allowGuestPosts
            prefs[Keys.REMOTE_POLL_SEC] = settings.pollIntervalSeconds.coerceIn(10, 120)
        }
    }

    suspend fun savePendingPkce(verifier: String, state: String) {
        context.communityDataStore.edit { prefs ->
            prefs[Keys.PKCE_VERIFIER] = verifier
            prefs[Keys.OAUTH_STATE] = state
        }
    }

    suspend fun consumePendingPkce(): Pair<String?, String?> {
        val prefs = context.communityDataStore.data.first()
        val pair = prefs[Keys.PKCE_VERIFIER] to prefs[Keys.OAUTH_STATE]
        context.communityDataStore.edit {
            it.remove(Keys.PKCE_VERIFIER)
            it.remove(Keys.OAUTH_STATE)
        }
        return pair
    }

    suspend fun saveKickTokens(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long,
        username: String?,
        avatarUrl: String?,
        userId: Long?
    ) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        context.communityDataStore.edit { prefs ->
            prefs[Keys.KICK_ACCESS_TOKEN] = accessToken
            if (refreshToken != null) prefs[Keys.KICK_REFRESH_TOKEN] = refreshToken
            prefs[Keys.KICK_EXPIRES_AT] = expiresAt
            if (username != null) {
                prefs[Keys.KICK_USERNAME] = username
                if (prefs[Keys.PROFILE_NAME].isNullOrBlank() || prefs[Keys.PROFILE_NAME] == "عضو RESPECT") {
                    prefs[Keys.PROFILE_NAME] = username
                }
            }
            if (avatarUrl != null) {
                prefs[Keys.KICK_AVATAR] = avatarUrl
                if (prefs[Keys.PROFILE_AVATAR].isNullOrBlank()) {
                    prefs[Keys.PROFILE_AVATAR] = avatarUrl
                }
            }
            if (userId != null) prefs[Keys.KICK_USER_ID] = userId
        }
    }

    suspend fun clearKickSession() {
        context.communityDataStore.edit { prefs ->
            prefs.remove(Keys.KICK_ACCESS_TOKEN)
            prefs.remove(Keys.KICK_REFRESH_TOKEN)
            prefs.remove(Keys.KICK_EXPIRES_AT)
            prefs.remove(Keys.KICK_USERNAME)
            prefs.remove(Keys.KICK_AVATAR)
            prefs.remove(Keys.KICK_USER_ID)
        }
    }

    suspend fun getCachedAppToken(): String? {
        val prefs = context.communityDataStore.data.first()
        val token = prefs[Keys.APP_ACCESS_TOKEN]
        val expiresAt = prefs[Keys.APP_EXPIRES_AT] ?: 0L
        return if (!token.isNullOrBlank() && System.currentTimeMillis() < expiresAt - 60_000L) {
            token
        } else {
            null
        }
    }

    suspend fun saveAppToken(token: String, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        context.communityDataStore.edit { prefs ->
            prefs[Keys.APP_ACCESS_TOKEN] = token
            prefs[Keys.APP_EXPIRES_AT] = expiresAt
        }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.communityDataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.communityDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.communityDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setFavoritesOnly(favoritesOnly: Boolean) {
        context.communityDataStore.edit { it[Keys.FAVORITES_ONLY] = favoritesOnly }
    }
}
