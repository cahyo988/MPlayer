package com.example.musicplayer.features.recents

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import com.example.musicplayer.data.history.PlaybackHistoryRepository
import com.example.musicplayer.data.playlist.db.PlaybackHistoryDao
import com.example.musicplayer.data.playlist.db.PlaybackHistoryEntity
import com.example.musicplayer.features.playlist.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecentsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val dispatcherRule = MainDispatcherRule(testDispatcher)

    @Test
    fun clearRecentsEmitsEmptyList() = runTest {
        val dao = FakePlaybackHistoryDao()
        val repository = PlaybackHistoryRepository(
            dao = dao,
            writeScope = CoroutineScope(SupervisorJob() + testDispatcher),
            nowProvider = { 1_000L }
        )
        val viewModel = RecentsViewModel(repository)

        repository.recordPlayedTrack(track("1"))
        advanceUntilIdle()

        viewModel.clearRecents()
        advanceUntilIdle()

        assertTrue(viewModel.recentTracks.first().isEmpty())
    }

    @Test
    fun recentTracksEmitsNewestFirst() = runTest {
        val dao = FakePlaybackHistoryDao()
        val repository = PlaybackHistoryRepository(
            dao = dao,
            writeScope = CoroutineScope(SupervisorJob() + testDispatcher),
            nowProvider = sequenceNowProvider(1_000L, 2_000L)
        )
        val viewModel = RecentsViewModel(repository)

        repository.recordPlayedTrack(track("1"))
        repository.recordPlayedTrack(track("2"))
        advanceUntilIdle()

        assertEquals(listOf("2", "1"), viewModel.recentTracks.first().map { it.id })
    }

    @Test
    fun clearRecentsWhenEmptyRemainsEmpty() = runTest {
        val dao = FakePlaybackHistoryDao()
        val repository = PlaybackHistoryRepository(
            dao = dao,
            writeScope = CoroutineScope(SupervisorJob() + testDispatcher),
            nowProvider = { 1_000L }
        )
        val viewModel = RecentsViewModel(repository)

        viewModel.clearRecents()
        advanceUntilIdle()

        assertTrue(viewModel.recentTracks.first().isEmpty())
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        durationMs = 1_000,
        uri = "content://song/$id",
        source = TrackSource.LOCAL
    )
}

private fun sequenceNowProvider(vararg values: Long): () -> Long {
    var index = 0
    val fallback = values.lastOrNull() ?: 0L
    return {
        val value = values.getOrNull(index) ?: fallback
        index++
        value
    }
}

private class FakePlaybackHistoryDao : PlaybackHistoryDao {
    private val map = linkedMapOf<String, PlaybackHistoryEntity>()
    private val state = MutableStateFlow<List<PlaybackHistoryEntity>>(emptyList())

    override fun observeRecent(limit: Int): Flow<List<PlaybackHistoryEntity>> {
        return state.map { rows -> rows.take(limit) }
    }

    override suspend fun upsert(entry: PlaybackHistoryEntity) {
        map[entry.trackId] = entry
        publish()
    }

    override suspend fun trimToLatest(keep: Int) {
        val keepIds = map.values
            .sortedByDescending { it.lastPlayedAt }
            .take(keep)
            .map { it.trackId }
            .toSet()
        map.keys.retainAll(keepIds)
        publish()
    }

    override suspend fun clear() {
        map.clear()
        publish()
    }

    private fun publish() {
        state.value = map.values.sortedByDescending { it.lastPlayedAt }
    }
}
