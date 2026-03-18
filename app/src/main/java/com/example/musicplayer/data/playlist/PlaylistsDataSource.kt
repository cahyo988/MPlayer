package com.example.musicplayer.data.playlist

import com.example.musicplayer.core.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistsDataSource {
    fun observePlaylists(): Flow<List<PlaylistSummary>>
    fun observePlaylist(playlistId: Long): Flow<PlaylistDetail?>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, name: String)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addTrack(playlistId: Long, track: Track)
    suspend fun removeTrack(playlistId: Long, trackId: String)
    suspend fun moveTrack(playlistId: Long, trackId: String, direction: Int)
    suspend fun ensureDefaultLocalPlaylist(): Long
    suspend fun syncDefaultLocalPlaylist(localTracks: List<Track>)
}
