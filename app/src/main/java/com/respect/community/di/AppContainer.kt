package com.respect.community.di

import android.content.Context
import com.respect.community.data.local.CommunityDatabase
import com.respect.community.data.local.SessionManager
import com.respect.community.data.remote.KickAuthApi
import com.respect.community.data.remote.KickChatWebSocket
import com.respect.community.data.remote.KickPublicApi
import com.respect.community.data.remote.KickV2Api
import com.respect.community.data.repository.CommunityRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "RespectCommunityAndroid/1.0")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
    }

    private val authApi: KickAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://id.kick.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KickAuthApi::class.java)
    }

    private val publicApi: KickPublicApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.kick.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KickPublicApi::class.java)
    }

    private val v2Api: KickV2Api by lazy {
        Retrofit.Builder()
            .baseUrl("https://kick.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KickV2Api::class.java)
    }

    val database: CommunityDatabase by lazy {
        CommunityDatabase.build(appContext)
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(appContext)
    }

    val chatWebSocket: KickChatWebSocket by lazy {
        KickChatWebSocket(okHttpClient, v2Api)
    }

    val repository: CommunityRepository by lazy {
        CommunityRepository(
            authApi = authApi,
            publicApi = publicApi,
            v2Api = v2Api,
            db = database,
            sessionManager = sessionManager,
            chatWebSocket = chatWebSocket
        )
    }
}
