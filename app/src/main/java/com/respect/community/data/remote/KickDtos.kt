package com.respect.community.data.remote

import com.google.gson.annotations.SerializedName

data class OAuthTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Long = 3600L,
    @SerializedName("scope") val scope: String? = null
)

data class KickChannelsResponse(
    @SerializedName("data") val data: List<KickChannelData>? = null
)

data class KickChannelData(
    @SerializedName("broadcaster_user_id") val broadcasterUserId: Long,
    @SerializedName("slug") val slug: String,
    @SerializedName("channel_description") val channelDescription: String? = null,
    @SerializedName("banner_picture") val bannerPicture: String? = null,
    @SerializedName("stream") val stream: KickStreamInfo? = null
)

data class KickStreamInfo(
    @SerializedName("is_live") val isLive: Boolean? = false,
    @SerializedName("viewer_count") val viewerCount: Int? = 0,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("stream_title") val streamTitle: String? = null,
    @SerializedName("category") val category: KickCategoryInfo? = null
)

data class KickCategoryInfo(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String? = null
)

data class KickUsersResponse(
    @SerializedName("data") val data: List<KickUserData>? = null
)

data class KickUserData(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

data class KickV2ChannelResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("playback_url") val playbackUrl: String? = null,
    @SerializedName("followers_count") val followersCount: Int? = null,
    @SerializedName("user") val user: KickV2User? = null,
    @SerializedName("livestream") val livestream: KickV2Livestream? = null,
    @SerializedName("chatroom") val chatroom: KickV2Chatroom? = null
)

data class KickV2User(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("profile_pic") val profilePic: String? = null
)

data class KickV2Livestream(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("session_title") val sessionTitle: String? = null,
    @SerializedName("is_live") val isLive: Boolean? = null,
    @SerializedName("viewer_count") val viewerCount: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("thumbnail") val thumbnail: KickV2Thumbnail? = null,
    @SerializedName("categories") val categories: List<KickCategoryInfo>? = null
)

data class KickV2Thumbnail(
    @SerializedName("url") val url: String? = null
)

data class KickV2Chatroom(
    @SerializedName("id") val id: Long? = null
)

data class KickV2MessagesResponse(
    @SerializedName("data") val data: KickV2MessagesData? = null
)

data class KickV2MessagesData(
    @SerializedName("messages") val messages: List<KickV2MessageItem>? = null
)

data class KickV2MessageItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("sender") val sender: KickV2MessageSender? = null
)

data class KickV2MessageSender(
    @SerializedName("username") val username: String? = null,
    @SerializedName("identity") val identity: KickV2SenderIdentity? = null
)

data class KickV2SenderIdentity(
    @SerializedName("color") val color: String? = null
)

data class SendChatRequest(
    @SerializedName("broadcaster_user_id") val broadcasterUserId: Long,
    @SerializedName("content") val content: String,
    @SerializedName("type") val type: String = "user"
)

data class SendChatResponse(
    @SerializedName("data") val data: SendChatResult? = null
)

data class SendChatResult(
    @SerializedName("is_sent") val isSent: Boolean? = null,
    @SerializedName("message_id") val messageId: String? = null
)
