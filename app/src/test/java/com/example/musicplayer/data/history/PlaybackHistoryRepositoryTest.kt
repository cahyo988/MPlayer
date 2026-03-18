package com.example.musicplayer.data.history

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackHistoryRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val dispatcherRule = MainDispatcherRule(testDispatcher)

    @Test
    fun recordPlayedTrackAddsMostRecentOnTop() = runTest {
        val dao = FakePlaybackHistoryDao()
        val repository = PlaybackHistoryRepository(
            dao = dao,
            writeScope = CoroutineScope(SupervisorJob() + testDispatcher),
            nowProvider = sequenceNowProvider(1_000L, 2_000L)
        )
        repository.recordPlayedTrack(track("1"))
        repository.recordPlayedTrack(track("2"))
        advanceUntilIdle()

        val ids = dao.observeRecent(50).first().map { it.trackId }
        assertEquals(listOf("2", "1"), ids)
    }

    @Test
    fun recordPlayedTrackDedupesByTrackId() = runTest {
        val dao = FakePlaybackHistoryDao()
        val repository = PlaybackHistoryRepository(
            dao = dao,
            writeScope = CoroutineScope(SupervisorJob() + testDispatcher),
            nowProvider = sequenceNowProvider(1_000L, 2_000L, 3_000L)
        )
        repository.recordPlayedTrack(track("1", title = "Old"))
        repository.recordPlayedTrack(track("2"))
        repository.recordPlayedTrack(track("1", title = "New"))
        advanceUntilIdle()

        val rows = dao.observeRecent(50).first()
        assertEquals(listOf("1", "2"), rows.map { it.trackId })
        assertEquals("New", rows.first().title)
    }

    @Test
    fun clearRemovesAllHistory() = runTest {
        val dao = FakePlaybackHistoryDao()
        val repository = PlaybackHistoryRepository(
            dao = dao,
            writeScope = CoroutineScope(SupervisorJob() + testDispatcher),
            nowProvider = sequenceNowProvider(1_000L)
        )
        repository.recordPlayedTrack(track("1"))
        advanceUntilIdle()

        repository.clearRecents()
        advanceUntilIdle()

        assertEquals(emptyList<PlaybackHistoryEntity>(), dao.observeRecent(50).first())
    }

    private fun track(id: String, title: String = "Song $id") = Track(
        id = id,
        title = title,
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
