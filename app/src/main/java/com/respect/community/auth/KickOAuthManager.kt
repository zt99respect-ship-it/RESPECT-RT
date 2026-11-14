package com.respect.community.auth

import android.net.Uri
import android.util.Base64
import com.respect.community.BuildConfig
import com.respect.community.data.local.SessionManager
import java.security.MessageDigest
import java.security.SecureRandom

object KickOAuthManager {

    private const val AUTHORIZE_ENDPOINT = "https://id.kick.com/oauth/authorize"
    private const val SCOPES = "user:read channel:read chat:write"
    // تم تثبيت الرابط هنا ليتطابق مع لوحة تحكم KICK
    const val REDIRECT_URI = "https://docs.kick.com"

    suspend fun prepareAuthorizeUrl(sessionManager: SessionManager): String {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)
        val state = generateRandomState()

        sessionManager.savePendingPkce(verifier = verifier, state = state)

        return Uri.parse(AUTHORIZE_ENDPOINT).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", BuildConfig.KICK_CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateRandomState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
