package com.respect.community.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface KickAuthApi {
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun getAppToken(
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): OAuthTokenResponse

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun exchangeAuthCode(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String
    ): OAuthTokenResponse

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun refreshUserToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String
    ): OAuthTokenResponse
}

interface KickPublicApi {
    @GET("public/v1/channels")
    suspend fun getChannelsBySlug(
        @Header("Authorization") authorization: String,
        @Query("slug") slug: String
    ): KickChannelsResponse

    @GET("public/v1/users")
    suspend fun getUsersByIds(
        @Header("Authorization") authorization: String,
        @Query("id") ids: List<Long>
    ): KickUsersResponse

    @GET("public/v1/users")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): KickUsersResponse

    @POST("public/v1/chat")
    suspend fun sendChatMessage(
        @Header("Authorization") authorization: String,
        @Body request: SendChatRequest
    ): Response<SendChatResponse>
}

interface KickV2Api {
    @GET("api/v2/channels/{slug}")
    suspend fun getV2Channel(
        @Path("slug") slug: String
    ): Response<KickV2ChannelResponse>

    @GET("api/v2/channels/{channelId}/messages")
    suspend fun getChatMessages(
        @Path("channelId") channelId: Long
    ): Response<KickV2MessagesResponse>
}
