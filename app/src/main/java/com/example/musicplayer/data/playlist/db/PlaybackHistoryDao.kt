package com.example.musicplayer.data.playlist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE trackId NOT IN (SELECT trackId FROM playback_history ORDER BY lastPlayedAt DESC LIMIT :keep)")
    suspend fun trimToLatest(keep: Int)

    @Query("DELETE FROM playback_history")
    suspend fun clear()
}
