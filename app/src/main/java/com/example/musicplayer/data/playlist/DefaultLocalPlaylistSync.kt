package com.example.musicplayer.data.playlist

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.playlist.db.PlaylistTrackEntity

internal fun mapLocalTracksForDefaultPlaylist(
    playlistId: Long,
    tracks: List<Track>
): List<PlaylistTrackEntity> {
    return tracks
        .distinctBy { it.id }
        .mapIndexed { index, track ->
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
                position = index
            )
        }
}
