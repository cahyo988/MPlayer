package com.example.musicplayer.data.drive

import com.example.musicplayer.core.model.DriveNode

interface DriveDataSource {
    suspend fun listPublicFolder(publicFolderUrl: String): List<DriveNode>
    fun isValidPublicFolderUrl(url: String): Boolean
}
