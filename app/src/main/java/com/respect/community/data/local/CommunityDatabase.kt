package com.respect.community.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.respect.community.data.local.dao.AnalysisDao
import com.respect.community.data.local.dao.ArtDao
import com.respect.community.data.local.dao.BannedUserDao
import com.respect.community.data.local.dao.ChatMessageDao
import com.respect.community.data.local.dao.ClipDao
import com.respect.community.data.local.dao.CommunityUserDao
import com.respect.community.data.local.dao.ScheduleDao
import com.respect.community.data.local.dao.StreamerDao
import com.respect.community.data.local.dao.SuggestionDao
import com.respect.community.data.local.dao.TimelineDao
import com.respect.community.data.local.entity.AnalysisEntity
import com.respect.community.data.local.entity.ArtEntity
import com.respect.community.data.local.entity.BannedUserEntity
import com.respect.community.data.local.entity.ChatMessageEntity
import com.respect.community.data.local.entity.ClipEntity
import com.respect.community.data.local.entity.CommunityUserEntity
import com.respect.community.data.local.entity.ScheduleEventEntity
import com.respect.community.data.local.entity.StreamerEntity
import com.respect.community.data.local.entity.SuggestionEntity
import com.respect.community.data.local.entity.TimelinePostEntity

@Database(
    entities = [
        StreamerEntity::class,
        SuggestionEntity::class,
        ScheduleEventEntity::class,
        TimelinePostEntity::class,
        AnalysisEntity::class,
        ClipEntity::class,
        ArtEntity::class,
        ChatMessageEntity::class,
        BannedUserEntity::class,
        CommunityUserEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CommunityDatabase : RoomDatabase() {
    abstract fun streamerDao(): StreamerDao
    abstract fun suggestionDao(): SuggestionDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun timelineDao(): TimelineDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun clipDao(): ClipDao
    abstract fun artDao(): ArtDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun bannedUserDao(): BannedUserDao
    abstract fun communityUserDao(): CommunityUserDao

    companion object {
        fun build(context: Context): CommunityDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                CommunityDatabase::class.java,
                "respect_community.db"
            ).fallbackToDestructiveMigration().build()
    }
}
