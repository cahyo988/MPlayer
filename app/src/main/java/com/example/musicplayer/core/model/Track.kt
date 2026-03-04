package com.example.musicplayer.core.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val source: TrackSource,
    val artworkUri: String? = null,
    val driveFileId: String? = null,
    val mimeType: String? = null
)
