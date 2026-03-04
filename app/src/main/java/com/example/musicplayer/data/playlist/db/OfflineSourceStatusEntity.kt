package com.example.musicplayer.data.playlist.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_source_status")
data class OfflineSourceStatusEntity(
    @PrimaryKey val sourceId: Long,
    val status: String,
    val totalTracks: Int,
    val downloadedTracks: Int,
    val failedTracks: Int,
    val downloadingTracks: Int,
    val queuedTracks: Int,
    val progressPercent: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
