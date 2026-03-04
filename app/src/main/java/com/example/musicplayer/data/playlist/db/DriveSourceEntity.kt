package com.example.musicplayer.data.playlist.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drive_sources")
data class DriveSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val folderUrl: String,
    val folderId: String,
    val createdAt: Long = System.currentTimeMillis()
)
