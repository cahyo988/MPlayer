package com.example.musicplayer.data.playlist

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import com.example.musicplayer.data.playlist.db.PlaylistDao
import com.example.musicplayer.data.playlist.db.PlaylistEntity
import com.example.musicplayer.data.playlist.db.PlaylistTrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

data class PlaylistSummary(
    val id: Long,
    val name: String
)

data class PlaylistDetail(
    val id: Long,
    val name: String,
    val tracks: List<Track>
)

class PlaylistRepository(
    private val dao: PlaylistDao
) : PlaylistsDataSource {
    private val playlistLocks = ConcurrentHashMap<Long, Mutex>()

    override suspend fun ensureDefaultLocalPlaylist(): Long {
        val existing = dao.getSystemDefaultPlaylist()
        if (existing != null) return existing.id
        return dao.insertPlaylist(
            PlaylistEntity(
                name = DEFAULT_LOCAL_PLAYLIST_NAME,
                isSystemDefault = true
            )
        )
    }

    override suspend fun syncDefaultLocalPlaylist(localTracks: List<Track>) {
        val playlistId = ensureDefaultLocalPlaylist()
        val lock = playlistLocks.getOrPut(playlistId) { Mutex() }
        lock.withLock {
            dao.deleteTracksBySource(playlistId, TrackSource.LOCAL.name)
            if (localTracks.isEmpty()) return
            val deduped = mapLocalTracksForDefaultPlaylist(playlistId, localTracks)
            dao.upsertPlaylistTracks(deduped)
        }
    }

    override fun observePlaylists(): Flow<List<PlaylistSummary>> =
        dao.observePlaylists().map { entities ->
            entities.map { PlaylistSummary(id = it.id, name = it.name) }
        }

    override fun observePlaylist(playlistId: Long): Flow<PlaylistDetail?> =
        dao.observePlaylistWithTracks(playlistId).map { relation ->
            relation?.let {
                PlaylistDetail(
                    id = it.playlist.id,
                    name = it.playlist.name,
                    tracks = it.tracks.sortedBy { row -> row.position }.map { row ->
                        val source = runCatching { TrackSource.valueOf(row.source) }
                            .getOrDefault(TrackSource.LOCAL)
                        Track(
                            id = row.trackId,
                            title = row.title,
                            artist = row.artist,
                            album = row.album,
                            durationMs = row.durationMs,
                            uri = row.uri,
                            source = source,
                            artworkUri = row.artworkUri,
                            driveFileId = row.driveFileId,
                            mimeType = row.mimeType
                        )
                    }
                )
            }
        }

    override suspend fun createPlaylist(name: String): Long {
        return dao.insertPlaylist(PlaylistEntity(name = name.trim().ifBlank { "New Playlist" }))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        val playlist = dao.getPlaylistById(playlistId) ?: return
        if (playlist.isSystemDefault) return
        dao.deletePlaylist(playlistId)
    }

    override suspend fun addTrack(playlistId: Long, track: Track) {
        val lock = playlistLocks.getOrPut(playlistId) { Mutex() }
        lock.withLock {
            dao.addTrackAtEnd(
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    uri = track.uri,
                    source = track.source.name,
                    artworkUri = track.artworkUri,
                    driveFileId = track.driveFileId,
                    mimeType = track.mimeType,
                    position = 0
                )
            )
        }
    }

    override suspend fun removeTrack(playlistId: Long, trackId: String) {
        dao.deleteTrack(playlistId, trackId)
    }

    companion object {
        const val DEFAULT_LOCAL_PLAYLIST_NAME = "All Local Songs"
    }
}
