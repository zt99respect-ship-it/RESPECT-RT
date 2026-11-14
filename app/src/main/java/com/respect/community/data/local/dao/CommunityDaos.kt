package com.respect.community.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.respect.community.data.local.entity.AnalysisEntity
import com.respect.community.data.local.entity.ArtEntity
import com.respect.community.data.local.entity.BannedUserEntity
import com.respect.community.data.local.entity.ChatMessageEntity
import com.respect.community.data.local.entity.ClipEntity
import com.respect.community.data.local.entity.CommunityUserEntity
import com.respect.community.data.local.entity.ScheduleEventEntity
import com.respect.community.data.local.entity.StreamSnapshotEntity
import com.respect.community.data.local.entity.StreamerEntity
import com.respect.community.data.local.entity.SuggestionEntity
import com.respect.community.data.local.entity.TimelinePostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamerDao {
    @Query("SELECT * FROM streamers ORDER BY isFeatured DESC, isLive DESC, viewerCount DESC, displayName ASC")
    fun observeAll(): Flow<List<StreamerEntity>>

    @Query("SELECT * FROM streamers WHERE slug = :slug LIMIT 1")
    fun observeBySlug(slug: String): Flow<StreamerEntity?>

    @Query("SELECT * FROM streamers WHERE displayName LIKE '%' || :q || '%' OR slug LIKE '%' || :q || '%' OR characterNames LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<StreamerEntity>>

    @Query("SELECT * FROM streamers")
    suspend fun getAllOnce(): List<StreamerEntity>

    @Query("SELECT * FROM streamers WHERE slug = :slug LIMIT 1")
    suspend fun getBySlugOnce(slug: String): StreamerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streamer: StreamerEntity)

    @Query("DELETE FROM streamers WHERE slug = :slug")
    suspend fun deleteBySlug(slug: String)

    @Query("UPDATE streamers SET isFavorite = :isFavorite WHERE slug = :slug")
    suspend fun updateFavorite(slug: String, isFavorite: Boolean)

    @Query("UPDATE streamers SET isFollowed = :isFollowed WHERE slug = :slug")
    suspend fun updateFollowed(slug: String, isFollowed: Boolean)

    @Query("UPDATE streamers SET isNotifying = :isNotifying WHERE slug = :slug")
    suspend fun updateNotifying(slug: String, isNotifying: Boolean)

    @Query("UPDATE streamers SET isFeatured = :isFeatured WHERE slug = :slug")
    suspend fun updateFeatured(slug: String, isFeatured: Boolean)
}

@Dao
interface StreamSnapshotDao {
    @Query("SELECT * FROM stream_snapshots ORDER BY recordedAtMs DESC LIMIT 2000")
    fun observeAll(): Flow<List<StreamSnapshotEntity>>

    @Query("SELECT * FROM stream_snapshots WHERE recordedAtMs >= :sinceMs ORDER BY recordedAtMs DESC")
    fun observeSince(sinceMs: Long): Flow<List<StreamSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: StreamSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<StreamSnapshotEntity>)

    @Query("SELECT COUNT(*) FROM stream_snapshots")
    suspend fun count(): Int
}

@Dao
interface SuggestionDao {
    @Query("SELECT * FROM streamer_suggestions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(suggestion: SuggestionEntity)

    @Query("UPDATE streamer_suggestions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM streamer_suggestions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM stream_schedule ORDER BY scheduledDate ASC, scheduledTime ASC")
    fun observeAll(): Flow<List<ScheduleEventEntity>>

    @Query("SELECT COUNT(*) FROM stream_schedule")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: ScheduleEventEntity)

    @Update
    suspend fun update(event: ScheduleEventEntity)

    @Query("DELETE FROM stream_schedule WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TimelineDao {
    @Query("SELECT * FROM timeline_posts ORDER BY isPinned DESC, isOfficialAnnouncement DESC, createdAt DESC")
    fun observeAll(): Flow<List<TimelinePostEntity>>

    @Query("SELECT * FROM timeline_posts WHERE content LIKE '%' || :q || '%' OR authorName LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<TimelinePostEntity>>

    @Query("SELECT COUNT(*) FROM timeline_posts")
    suspend fun count(): Int

    @Query("SELECT * FROM timeline_posts WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteIdOnce(remoteId: String): TimelinePostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: TimelinePostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<TimelinePostEntity>)

    @Query("UPDATE timeline_posts SET isLikedByMe = :liked, likesCount = CASE WHEN :liked THEN likesCount + 1 ELSE MAX(0, likesCount - 1) END WHERE id = :id")
    suspend fun toggleLike(id: Long, liked: Boolean)

    @Query("UPDATE timeline_posts SET isPinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM timeline_posts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM stream_analyses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AnalysisEntity>>

    @Query("SELECT COUNT(*) FROM stream_analyses")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: AnalysisEntity)

    @Query("DELETE FROM stream_analyses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ClipDao {
    @Query("SELECT * FROM community_clips ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM community_clips WHERE title LIKE '%' || :q || '%' OR streamerName LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<ClipEntity>>

    @Query("SELECT COUNT(*) FROM community_clips")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clip: ClipEntity)

    @Query("UPDATE community_clips SET isLikedByMe = :liked, likesCount = CASE WHEN :liked THEN likesCount + 1 ELSE MAX(0, likesCount - 1) END WHERE id = :id")
    suspend fun toggleLike(id: Long, liked: Boolean)

    @Query("DELETE FROM community_clips WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ArtDao {
    @Query("SELECT * FROM community_art ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ArtEntity>>

    @Query("SELECT * FROM community_art WHERE title LIKE '%' || :q || '%' OR artistName LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<ArtEntity>>

    @Query("SELECT COUNT(*) FROM community_art")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(art: ArtEntity)

    @Query("UPDATE community_art SET isLikedByMe = :liked, likesCount = CASE WHEN :liked THEN likesCount + 1 ELSE MAX(0, likesCount - 1) END WHERE id = :id")
    suspend fun toggleLike(id: Long, liked: Boolean)

    @Query("DELETE FROM community_art WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE roomKey = :roomKey ORDER BY createdAt DESC LIMIT :limit")
    fun observeRoom(roomKey: String, limit: Int = 150): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE roomKey = :roomKey")
    suspend fun countRoom(roomKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        "DELETE FROM chat_messages WHERE roomKey = :roomKey AND id NOT IN " +
            "(SELECT id FROM chat_messages WHERE roomKey = :roomKey ORDER BY createdAt DESC LIMIT 250)"
    )
    suspend fun trimRoom(roomKey: String)
}

@Dao
interface BannedUserDao {
    @Query("SELECT * FROM banned_users ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BannedUserEntity>>

    @Query("SELECT * FROM banned_users WHERE LOWER(identifier) = LOWER(:identifier) AND bannedUntilMs > :nowMs LIMIT 1")
    suspend fun getActiveBan(identifier: String, nowMs: Long = System.currentTimeMillis()): BannedUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ban(banned: BannedUserEntity)

    @Query("DELETE FROM banned_users WHERE identifier = :identifier")
    suspend fun unban(identifier: String)
}

@Dao
interface CommunityUserDao {
    @Query("SELECT * FROM community_users ORDER BY role ASC, updatedAt DESC")
    fun observeAll(): Flow<List<CommunityUserEntity>>

    @Query("SELECT COUNT(*) FROM community_users")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: CommunityUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<CommunityUserEntity>)

    @Query("UPDATE community_users SET role = :role, updatedAt = :updatedAt WHERE uid = :uid")
    suspend fun updateRole(uid: String, role: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE community_users SET banned = :banned, isMuted = :muted, mutedUntilMs = :untilMs, banReason = :reason, updatedAt = :updatedAt WHERE uid = :uid")
    suspend fun updateModeration(
        uid: String,
        banned: Boolean,
        muted: Boolean,
        untilMs: Long,
        reason: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM community_users WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}
