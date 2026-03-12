package com.example.musicplayer.data.playlist.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["lastPlayedAt"])]
)
data class PlaybackHistoryEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val source: String,
    val artworkUri: String? = null,
    val driveFileId: String? = null,
    val mimeType: String? = null,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
