package com.example.musicplayer.data.favorites

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import com.example.musicplayer.data.playlist.db.FavoriteTrackDao
import com.example.musicplayer.data.playlist.db.FavoriteTrackEntity
import com.example.musicplayer.data.settings.FavoritesMaintenance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(
    private val dao: FavoriteTrackDao
) : FavoritesMaintenance {
    fun observeFavorites(): Flow<List<Track>> {
        return dao.observeFavorites().map { rows ->
            rows.map { row ->
                Track(
                    id = row.trackId,
                    title = row.title,
                    artist = row.artist,
                    album = row.album,
                    durationMs = row.durationMs,
                    uri = row.uri,
                    source = runCatching { TrackSource.valueOf(row.source) }
                        .getOrDefault(TrackSource.LOCAL),
                    artworkUri = row.artworkUri,
                    driveFileId = row.driveFileId,
                    mimeType = row.mimeType
                )
            }
        }
    }

    fun observeIsFavorite(trackId: String): Flow<Boolean> {
        return dao.observeIsFavorite(trackId)
    }

    suspend fun isFavorite(trackId: String): Boolean {
        return dao.isFavorite(trackId)
    }

    suspend fun toggle(track: Track, shouldFavorite: Boolean) {
        if (shouldFavorite) {
            dao.upsert(
                FavoriteTrackEntity(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    uri = track.uri,
                    source = track.source.name,
                    artworkUri = track.artworkUri,
                    driveFileId = track.driveFileId,
                    mimeType = track.mimeType
                )
            )
        } else {
            dao.delete(track.id)
        }
    }

    override suspend fun clearFavorites() {
        dao.clearAll()
    }
}
