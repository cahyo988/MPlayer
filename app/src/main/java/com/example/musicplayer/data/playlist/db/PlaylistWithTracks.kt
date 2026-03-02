package com.example.musicplayer.data.playlist.db

import androidx.room.Embedded
import androidx.room.Relation

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val tracks: List<PlaylistTrackEntity>
)
