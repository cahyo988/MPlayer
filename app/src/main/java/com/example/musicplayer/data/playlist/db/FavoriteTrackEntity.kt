package com.example.musicplayer.data.playlist.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
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
    val addedAt: Long = System.currentTimeMillis()
)
