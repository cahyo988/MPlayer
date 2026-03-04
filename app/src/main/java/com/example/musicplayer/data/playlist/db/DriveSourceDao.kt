package com.example.musicplayer.data.playlist.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveSourceDao {
    @Query("SELECT * FROM drive_sources ORDER BY createdAt DESC")
    fun observeSources(): Flow<List<DriveSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: DriveSourceEntity): Long

    @Query("SELECT * FROM drive_sources WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DriveSourceEntity?

    @Delete
    suspend fun delete(source: DriveSourceEntity)

    @Query("DELETE FROM drive_sources WHERE id = :id")
    suspend fun deleteById(id: Long)
}
