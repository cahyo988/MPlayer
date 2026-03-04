package com.example.musicplayer.features.playlist

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import com.example.musicplayer.data.playlist.PlaylistDetail
import com.example.musicplayer.data.playlist.PlaylistSummary
import com.example.musicplayer.data.playlist.PlaylistsDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun concurrentAddTrackCreatesSinglePlaylist() = runTest {
        val dataSource = FakePlaylistsDataSource()
        val viewModel = PlaylistsViewModel(dataSource)
        val track = sampleTrack("1")

        val first = async { viewModel.addTrackToSelectedPlaylist(track) }
        val second = async { viewModel.addTrackToSelectedPlaylist(track.copy(id = "2", title = "Song 2")) }

        first.await()
        second.await()
        advanceUntilIdle()

        assertEquals(1, dataSource.playlists.value.size)
        val selected = dataSource.playlists.value.first().id
        assertEquals(2, dataSource.tracksByPlaylist[selected]?.size)
    }

    @Test
    fun concurrentAddsKeepUniquePositions() = runTest {
        val dataSource = FakePlaylistsDataSource()
        val playlistId = dataSource.createPlaylist("My Playlist")
        val viewModel = PlaylistsViewModel(dataSource)
        viewModel.selectPlaylist(playlistId)

        val jobs = (1..5).map { index ->
            async {
                viewModel.addTrackToSelectedPlaylist(sampleTrack(index.toString()))
            }
        }

        jobs.forEach { it.await() }
        advanceUntilIdle()

        val positions = dataSource.tracksByPlaylist[playlistId].orEmpty().mapIndexed { i, _ -> i }
        assertEquals(5, positions.distinct().size)
        assertTrue(dataSource.tracksByPlaylist[playlistId].orEmpty().size == 5)
    }

    private fun sampleTrack(id: String): Track = Track(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        durationMs = 1000,
        uri = "content://song/$id",
        source = TrackSource.LOCAL
    )
}

private class FakePlaylistsDataSource : PlaylistsDataSource {
    val playlists = MutableStateFlow<List<PlaylistSummary>>(emptyList())
    val tracksByPlaylist = linkedMapOf<Long, MutableList<Track>>()
    private var nextId = 1L

    override fun observePlaylists(): Flow<List<PlaylistSummary>> = playlists

    override fun observePlaylist(playlistId: Long): Flow<PlaylistDetail?> = playlists.map {
        val summary = it.firstOrNull { item -> item.id == playlistId } ?: return@map null
        PlaylistDetail(summary.id, summary.name, tracksByPlaylist[playlistId].orEmpty())
    }

    override suspend fun createPlaylist(name: String): Long {
        val id = nextId++
        playlists.value = playlists.value + PlaylistSummary(id, name)
        tracksByPlaylist[id] = mutableListOf()
        return id
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlists.value = playlists.value.filterNot { it.id == playlistId }
        tracksByPlaylist.remove(playlistId)
    }

    override suspend fun addTrack(playlistId: Long, track: Track) {
        val list = tracksByPlaylist.getOrPut(playlistId) { mutableListOf() }
        list.removeAll { it.id == track.id }
        list += track
    }

    override suspend fun removeTrack(playlistId: Long, trackId: String) {
        tracksByPlaylist[playlistId]?.removeAll { it.id == trackId }
    }

    override suspend fun ensureDefaultLocalPlaylist(): Long {
        return playlists.value.firstOrNull { it.name == "All Local Songs" }?.id
            ?: createPlaylist("All Local Songs")
    }

    override suspend fun syncDefaultLocalPlaylist(localTracks: List<Track>) {
        val id = ensureDefaultLocalPlaylist()
        tracksByPlaylist[id] = localTracks.distinctBy { it.id }.toMutableList()
    }
}
