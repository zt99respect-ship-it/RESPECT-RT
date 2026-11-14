package com.respect.community.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.respect.community.BuildConfig
import com.respect.community.data.local.SessionManager
import java.security.MessageDigest
import java.security.SecureRandom

object KickOAuthManager {

    private const val AUTHORIZE_ENDPOINT = "https://id.kick.com/oauth/authorize"
    private const val SCOPES = "user:read channel:read chat:write"
    const val REDIRECT_URI = "respect://oauth/callback"

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

    suspend fun launchOAuthFlow(context: Context, sessionManager: SessionManager) {
        val url = prepareAuthorizeUrl(sessionManager)
        val authUri = Uri.parse(url)
        val colorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(0xFF09090B.toInt())
            .setNavigationBarColor(0xFF09090B.toInt())
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(colorParams)
            .build()

        runCatching {
            customTabsIntent.launchUrl(context, authUri)
        }.onFailure {
            val fallback = Intent(Intent.ACTION_VIEW, authUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
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
