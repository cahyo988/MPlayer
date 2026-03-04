package com.example.musicplayer.data.playlist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineStatusDao {
    @Query("SELECT * FROM offline_source_status WHERE sourceId = :sourceId")
    fun observeSourceStatus(sourceId: Long): Flow<OfflineSourceStatusEntity?>

    @Query("SELECT * FROM offline_track_status WHERE sourceId = :sourceId ORDER BY title COLLATE NOCASE ASC")
    fun observeTrackStatuses(sourceId: Long): Flow<List<OfflineTrackStatusEntity>>

    @Query("SELECT * FROM offline_track_status WHERE sourceId = :sourceId")
    suspend fun getTrackStatuses(sourceId: Long): List<OfflineTrackStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSourceStatus(status: OfflineSourceStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackStatus(track: OfflineTrackStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackStatuses(tracks: List<OfflineTrackStatusEntity>)

    @Query("DELETE FROM offline_track_status WHERE sourceId = :sourceId")
    suspend fun deleteTrackStatuses(sourceId: Long)

    @Query("DELETE FROM offline_source_status WHERE sourceId = :sourceId")
    suspend fun deleteSourceStatus(sourceId: Long)

    @Query("DELETE FROM offline_track_status WHERE sourceId = :sourceId AND trackId = :trackId")
    suspend fun deleteTrackStatus(sourceId: Long, trackId: String)

    @Query("SELECT * FROM offline_track_status WHERE sourceId = :sourceId AND trackId = :trackId LIMIT 1")
    suspend fun getTrackStatus(sourceId: Long, trackId: String): OfflineTrackStatusEntity?

    @Query("SELECT * FROM offline_track_status WHERE trackId = :trackId AND status = :status ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestTrackByStatus(trackId: String, status: String): OfflineTrackStatusEntity?
}
