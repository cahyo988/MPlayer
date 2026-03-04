package com.example.musicplayer.data.drive

import com.example.musicplayer.data.playlist.db.DriveSourceDao
import com.example.musicplayer.data.playlist.db.DriveSourceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DriveSourceRepository(
    private val dao: DriveSourceDao,
    private val driveRepository: DriveRepository
) : DriveSourcesDataSource {

    override suspend fun getSourceById(id: Long): DriveSource? {
        return dao.getById(id)?.toModel()
    }

    override fun observeSources(): Flow<List<DriveSource>> {
        return dao.observeSources().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun addSource(title: String, folderUrl: String): Long {
        val normalizedUrl = folderUrl.trim()
        val folderId = driveRepository.extractPublicFolderId(normalizedUrl)
            ?: throw IllegalArgumentException("Invalid URL. Example: https://drive.google.com/drive/folders/yourFolderId")
        val safeTitle = title.trim().ifBlank { "Drive Source" }
        return dao.insert(
            DriveSourceEntity(
                title = safeTitle,
                folderUrl = normalizedUrl,
                folderId = folderId
            )
        )
    }

    override suspend fun deleteSource(id: Long) {
        dao.deleteById(id)
    }

    private fun DriveSourceEntity.toModel(): DriveSource {
        return DriveSource(
            id = id,
            title = title,
            folderUrl = folderUrl,
            folderId = folderId,
            createdAt = createdAt
        )
    }
}
