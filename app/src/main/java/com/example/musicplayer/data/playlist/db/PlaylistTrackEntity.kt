package com.example.musicplayer.data.playlist.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"])]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val source: String,
    val artworkUri: String? = null,
    val driveFileId: String? = null,
    val mimeType: String? = null,
    val position: Int
)
