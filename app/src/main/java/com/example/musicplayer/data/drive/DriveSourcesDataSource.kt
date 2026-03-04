package com.example.musicplayer.data.drive

import kotlinx.coroutines.flow.Flow

interface DriveSourcesDataSource {
    fun observeSources(): Flow<List<DriveSource>>
    suspend fun getSourceById(id: Long): DriveSource?
    suspend fun addSource(title: String, folderUrl: String): Long
    suspend fun deleteSource(id: Long)
}
