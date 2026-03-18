package com.example.musicplayer.data.offline

import android.content.Context
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.offline.model.OfflineSourceStatus
import com.example.musicplayer.data.offline.model.OfflineTrackStatus
import com.example.musicplayer.data.offline.model.SourceOfflineSummary
import com.example.musicplayer.data.offline.model.computeSourceSummary
import com.example.musicplayer.data.playlist.db.DriveSourceDao
import com.example.musicplayer.data.playlist.db.OfflineSourceStatusEntity
import com.example.musicplayer.data.playlist.db.OfflineStatusDao
import com.example.musicplayer.data.playlist.db.OfflineTrackStatusEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

data class OfflineTrackState(
    val sourceId: Long,
    val trackId: String,
    val title: String,
    val remoteUri: String,
    val status: OfflineTrackStatus,
    val localFilePath: String?,
    val errorMessage: String?
)

interface OfflinePathLookup {
    suspend fun findAnyDownloadedPath(trackId: String): String?
}

interface DriveOfflineDataSource {
    fun observeSourceSummary(sourceId: Long): Flow<SourceOfflineSummary?>
    fun observeTrackStates(sourceId: Long): Flow<List<OfflineTrackState>>
    suspend fun seedSourceTracks(sourceId: Long, tracks: List<Track>)
    suspend fun clearSource(sourceId: Long)
    suspend fun getTracksByStatus(sourceId: Long, status: OfflineTrackStatus): List<Track>
}

class OfflineStatusRepository(
    context: Context,
    private val dao: OfflineStatusDao,
    private val driveSourceDao: DriveSourceDao
) : OfflinePathLookup, DriveOfflineDataSource {
    private val appContext = context.applicationContext

    override fun observeSourceSummary(sourceId: Long): Flow<SourceOfflineSummary?> {
        return dao.observeSourceStatus(sourceId).map { entity ->
            entity?.let {
                SourceOfflineSummary(
                    sourceId = it.sourceId,
                    status = parseSourceStatus(it.status),
                    totalTracks = it.totalTracks,
                    downloadedTracks = it.downloadedTracks,
                    failedTracks = it.failedTracks,
                    downloadingTracks = it.downloadingTracks,
                    queuedTracks = it.queuedTracks,
                    progressPercent = it.progressPercent
                )
            }
        }
    }

    override fun observeTrackStates(sourceId: Long): Flow<List<OfflineTrackState>> {
        return dao.observeTrackStatuses(sourceId).map { list -> list.map { it.toModel() } }
    }

    override suspend fun seedSourceTracks(sourceId: Long, tracks: List<Track>) {
        val rows = tracks.map { track ->
            val current = dao.getTrackStatus(sourceId, track.id)
            current?.copy(
                title = track.title,
                remoteUri = track.uri,
                mimeType = track.mimeType,
                driveFileId = track.driveFileId,
                updatedAt = System.currentTimeMillis()
            ) ?: OfflineTrackStatusEntity(
                sourceId = sourceId,
                trackId = track.id,
                title = track.title,
                remoteUri = track.uri,
                mimeType = track.mimeType,
                driveFileId = track.driveFileId,
                status = OfflineTrackStatus.NOT_DOWNLOADED.name
            )
        }
        dao.upsertTrackStatuses(rows)
        recomputeAndPersistSummary(sourceId)
    }

    suspend fun markTrackStatus(
        sourceId: Long,
        track: Track,
        status: OfflineTrackStatus,
        localFilePath: String? = null,
        errorMessage: String? = null
    ) {
        val existing = dao.getTrackStatus(sourceId, track.id)
        dao.upsertTrackStatus(
            (existing ?: OfflineTrackStatusEntity(
                sourceId = sourceId,
                trackId = track.id,
                title = track.title,
                remoteUri = track.uri,
                mimeType = track.mimeType,
                driveFileId = track.driveFileId,
                status = status.name
            )).copy(
                title = track.title,
                remoteUri = track.uri,
                mimeType = track.mimeType,
                driveFileId = track.driveFileId,
                status = status.name,
                localFilePath = localFilePath,
                errorMessage = errorMessage,
                updatedAt = System.currentTimeMillis()
            )
        )
        recomputeAndPersistSummary(sourceId)
    }

    suspend fun getLocalPath(sourceId: Long, trackId: String): String? {
        val row = dao.getTrackStatus(sourceId, trackId) ?: return null
        if (parseTrackStatus(row.status) != OfflineTrackStatus.DOWNLOADED) return null
        val path = row.localFilePath ?: return null
        return path.takeIf { isPathUnderOfflineRoot(it) && File(it).exists() }
    }

    override suspend fun findAnyDownloadedPath(trackId: String): String? {
        val row = dao.getLatestTrackByStatus(trackId, OfflineTrackStatus.DOWNLOADED.name) ?: return null
        val path = row.localFilePath ?: return null
        return path.takeIf { isPathUnderOfflineRoot(it) && File(it).exists() }
    }

    override suspend fun clearSource(sourceId: Long) {
        dao.getTrackStatuses(sourceId).forEach { row ->
            row.localFilePath?.takeIf(::isPathUnderOfflineRoot)?.let { File(it).delete() }
        }
        dao.deleteTrackStatuses(sourceId)
        dao.deleteSourceStatus(sourceId)
    }

    override suspend fun getTracksByStatus(sourceId: Long, status: OfflineTrackStatus): List<Track> {
        return dao.getTrackStatusesByStatus(sourceId, status.name).map { row ->
            Track(
                id = row.trackId,
                title = row.title,
                artist = "Drive",
                album = "Drive Public Folder",
                durationMs = 0L,
                uri = row.remoteUri,
                source = com.example.musicplayer.core.model.TrackSource.DRIVE,
                driveFileId = row.driveFileId,
                mimeType = row.mimeType
            )
        }
    }

    suspend fun markPendingAsFailed(sourceId: Long, reason: String) {
        dao.updateStatusesForSource(
            sourceId = sourceId,
            fromStatuses = listOf(
                OfflineTrackStatus.QUEUED.name,
                OfflineTrackStatus.DOWNLOADING.name
            ),
            toStatus = OfflineTrackStatus.FAILED.name,
            errorMessage = reason,
            updatedAt = System.currentTimeMillis()
        )
        recomputeAndPersistSummary(sourceId)
    }

    private fun isPathUnderOfflineRoot(path: String): Boolean {
        return runCatching {
            val root = File(appContext.filesDir, OFFLINE_ROOT_DIR).canonicalPath + File.separator
            val candidate = File(path).canonicalPath
            candidate.startsWith(root)
        }.getOrDefault(false)
    }

    suspend fun recomputeAndPersistSummary(sourceId: Long): SourceOfflineSummary {
        val statuses = dao.getTrackStatuses(sourceId)
        val summary = computeSourceSummary(
            sourceId = sourceId,
            totalTracks = statuses.size,
            downloadedTracks = statuses.count { parseTrackStatus(it.status) == OfflineTrackStatus.DOWNLOADED },
            failedTracks = statuses.count { parseTrackStatus(it.status) == OfflineTrackStatus.FAILED },
            downloadingTracks = statuses.count { parseTrackStatus(it.status) == OfflineTrackStatus.DOWNLOADING },
            queuedTracks = statuses.count { parseTrackStatus(it.status) == OfflineTrackStatus.QUEUED }
        )
        dao.upsertSourceStatus(
            OfflineSourceStatusEntity(
                sourceId = sourceId,
                status = summary.status.name,
                totalTracks = summary.totalTracks,
                downloadedTracks = summary.downloadedTracks,
                failedTracks = summary.failedTracks,
                downloadingTracks = summary.downloadingTracks,
                queuedTracks = summary.queuedTracks,
                progressPercent = summary.progressPercent,
                updatedAt = System.currentTimeMillis()
            )
        )
        return summary
    }

    suspend fun sourceExists(sourceId: Long): Boolean = driveSourceDao.getById(sourceId) != null

    private fun OfflineTrackStatusEntity.toModel(): OfflineTrackState {
        return OfflineTrackState(
            sourceId = sourceId,
            trackId = trackId,
            title = title,
            remoteUri = remoteUri,
            status = parseTrackStatus(status),
            localFilePath = localFilePath,
            errorMessage = errorMessage
        )
    }

    private fun parseTrackStatus(value: String): OfflineTrackStatus {
        return runCatching { OfflineTrackStatus.valueOf(value) }
            .getOrDefault(OfflineTrackStatus.NOT_DOWNLOADED)
    }

    private fun parseSourceStatus(value: String): OfflineSourceStatus {
        return runCatching { OfflineSourceStatus.valueOf(value) }
            .getOrDefault(OfflineSourceStatus.NOT_STARTED)
    }

    companion object {
        private const val OFFLINE_ROOT_DIR = "offline_drive"
    }
}
