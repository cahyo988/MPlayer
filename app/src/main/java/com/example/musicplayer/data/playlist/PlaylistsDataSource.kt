package com.example.musicplayer.data.playlist

import com.example.musicplayer.core.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistsDataSource {
    fun observePlaylists(): Flow<List<PlaylistSummary>>
    fun observePlaylist(playlistId: Long): Flow<PlaylistDetail?>
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addTrack(playlistId: Long, track: Track)
    suspend fun removeTrack(playlistId: Long, trackId: String)
}
