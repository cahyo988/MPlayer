package com.example.musicplayer.core.model

data class DriveNode(
    val name: String,
    val uri: String,
    val isFolder: Boolean,
    val track: Track? = null
)
