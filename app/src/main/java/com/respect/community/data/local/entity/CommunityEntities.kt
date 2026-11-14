package com.respect.community.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streamers")
data class StreamerEntity(
    @PrimaryKey
    val slug: String,
    val broadcasterUserId: Long,
    val displayName: String,
    val characterNames: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val isLive: Boolean = false,
    val streamTitle: String = "",
    val categoryName: String = "بث مباشر",
    val viewerCount: Int = 0,
    val peakViewers: Int = 0,
    val followerCount: Int = 0,
    val playbackUrl: String = "",
    val thumbnailUrl: String = "",
    val chatroomId: Long = 0L,
    val startedAt: String = "",
    val isGlobal: Boolean = false,
    val isFeatured: Boolean = false,
    val isNotifying: Boolean = true,
    val isFavorite: Boolean = true,
    val isFollowed: Boolean = false,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stream_snapshots")
data class StreamSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val streamerSlug: String,
    val streamerName: String,
    val isLive: Boolean,
    val viewerCount: Int,
    val followerCount: Int,
    val streamTitle: String = "",
    val recordedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "streamer_suggestions")
data class SuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kickUsername: String,
    val characterNames: String,
    val reason: String,
    val suggestedBy: String,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stream_schedule")
data class ScheduleEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val streamerSlug: String,
    val streamerName: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val gameCategory: String,
    val predictionNote: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "timeline_posts")
data class TimelinePostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String = "",
    val authorUid: String = "",
    val authorName: String,
    val authorIdentifier: String,
    val authorAvatarUrl: String = "",
    val isAdminPost: Boolean = false,
    val isOfficialAnnouncement: Boolean = false,
    val isPinned: Boolean = false,
    val content: String,
    val mediaUrl: String = "",
    val mediaType: String = "NONE", // NONE, IMAGE, VIDEO
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stream_analyses")
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val streamerSlug: String,
    val streamerName: String,
    val authorName: String,
    val title: String,
    val analysisText: String,
    val rating: Int = 8,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_clips")
data class ClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val streamerSlug: String,
    val streamerName: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String = "",
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val uploadedBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_art")
data class ArtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artistName: String,
    val imageUrl: String,
    val aspectRatio: Float = 0.8f,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val roomKey: String,
    val senderName: String,
    val senderColor: String = "#A78BFA",
    val isAdmin: Boolean = false,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "banned_users")
data class BannedUserEntity(
    @PrimaryKey
    val identifier: String,
    val displayName: String,
    val reason: String,
    val bannedUntilMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_users")
data class CommunityUserEntity(
    @PrimaryKey
    val uid: String,
    val displayName: String,
    val email: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val bio: String = "",
    val role: String = "USER", // USER, MODERATOR, ADMIN
    val banned: Boolean = false,
    val isMuted: Boolean = false,
    val mutedUntilMs: Long = 0L,
    val banReason: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
