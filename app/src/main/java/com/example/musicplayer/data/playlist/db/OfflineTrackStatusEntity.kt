package com.example.musicplayer.data.playlist.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "offline_track_status",
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["sourceId", "trackId"], unique = true)
    ]
)
data class OfflineTrackStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val trackId: String,
    val title: String,
    val remoteUri: String,
    val mimeType: String?,
    val driveFileId: String?,
    val status: String,
    val localFilePath: String? = null,
    val errorMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
