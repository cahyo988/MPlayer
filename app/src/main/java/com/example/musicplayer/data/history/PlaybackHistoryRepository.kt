package com.example.musicplayer.data.history

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import com.example.musicplayer.data.playlist.db.PlaybackHistoryDao
import com.example.musicplayer.data.playlist.db.PlaybackHistoryEntity
import com.example.musicplayer.data.settings.RecentsMaintenance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PlaybackHistoryRepository(
    private val dao: PlaybackHistoryDao,
    private val recentsLimitProvider: () -> Int = { MAX_RECENTS },
    private val writeScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : RecentsMaintenance {
    fun observeRecentTracks(): Flow<List<Track>> {
        return dao.observeRecent(recentsLimitProvider().coerceIn(10, MAX_RECENTS)).map { rows ->
            rows.map { it.toTrack() }
        }
    }

    fun recordPlayedTrack(track: Track) {
        writeScope.launch {
            val limit = recentsLimitProvider().coerceIn(10, MAX_RECENTS)
            dao.upsert(track.toEntity())
            dao.trimToLatest(limit)
        }
    }

    override suspend fun clearRecents() {
        dao.clear()
    }

    private fun Track.toEntity(): PlaybackHistoryEntity {
        return PlaybackHistoryEntity(
            trackId = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = uri,
            source = source.name,
            artworkUri = artworkUri,
            driveFileId = driveFileId,
            mimeType = mimeType,
            lastPlayedAt = nowProvider()
        )
    }

    private fun PlaybackHistoryEntity.toTrack(): Track {
        val trackSource = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.LOCAL)
        return Track(
            id = trackId,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = uri,
            source = trackSource,
            artworkUri = artworkUri,
            driveFileId = driveFileId,
            mimeType = mimeType
        )
    }

    companion object {
        private const val MAX_RECENTS = 50
    }
}
