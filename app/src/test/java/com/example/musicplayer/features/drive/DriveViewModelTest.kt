package com.example.musicplayer.features.drive

import com.example.musicplayer.core.model.DriveNode
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import com.example.musicplayer.data.drive.DriveDataSource
import com.example.musicplayer.data.drive.DriveSource
import com.example.musicplayer.data.drive.DriveSourcesDataSource
import com.example.musicplayer.data.offline.DriveOfflineDataSource
import com.example.musicplayer.data.offline.OfflineDownloadStarter
import com.example.musicplayer.data.offline.OfflineTrackState
import com.example.musicplayer.data.offline.model.OfflineSourceStatus
import com.example.musicplayer.data.offline.model.OfflineTrackStatus
import com.example.musicplayer.data.offline.model.SourceOfflineSummary
import com.example.musicplayer.features.playlist.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DriveViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun invalidUrlSetsErrorImmediately() = runTest {
        val dataSource = FakeDriveDataSource()
        val viewModel = DriveViewModel(
            dataSource,
            FakeDriveSourcesDataSource(),
            FakeOfflineStatusRepository(),
            FakeDownloadStarter()
        )

        viewModel.updateFolderUrl("https://example.com/not-drive")
        viewModel.loadFolder()
        advanceUntilIdle()

        assertEquals(0, dataSource.loadCalls)
        assertTrue(viewModel.uiState.value.error?.contains("Invalid URL") == true)
    }

    @Test
    fun loadSuccessTransitionsToLoadedState() = runTest {
        val dataSource = FakeDriveDataSource(
            nextResult = listOf(
                DriveNode(
                    name = "Song",
                    uri = "https://drive.google.com/uc?export=download&id=1",
                    isFolder = false,
                    track = sampleTrack("1")
                )
            )
        )
        val viewModel = DriveViewModel(
            dataSource,
            FakeDriveSourcesDataSource(),
            FakeOfflineStatusRepository(),
            FakeDownloadStarter()
        )

        viewModel.updateFolderUrl("https://drive.google.com/drive/folders/folderId")
        viewModel.loadFolder()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasLoaded)
        assertFalse(state.isLoading)
        assertEquals(1, state.nodes.size)
        assertEquals(null, state.error)
    }

    @Test
    fun addAndDeleteSourceUpdatesState() = runTest {
        val sources = FakeDriveSourcesDataSource()
        val viewModel = DriveViewModel(
            FakeDriveDataSource(),
            sources,
            FakeOfflineStatusRepository(),
            FakeDownloadStarter()
        )

        viewModel.updateFolderUrl("https://drive.google.com/drive/folders/folderId")
        viewModel.updateSourceTitle("My Source")
        viewModel.addSource()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sources.size)
        val id = viewModel.uiState.value.sources.first().id

        viewModel.deleteSource(id)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.sources.isEmpty())
    }

    private fun sampleTrack(id: String) = Track(
        id = "drive:$id",
        title = "Song $id.mp3",
        artist = "Drive",
        album = "Drive",
        durationMs = 0L,
        uri = "https://drive.google.com/uc?export=download&id=$id",
        source = TrackSource.DRIVE,
        driveFileId = id,
        mimeType = "audio/mpeg"
    )
}

private class FakeDriveDataSource(
    private val nextResult: List<DriveNode> = emptyList(),
    private val throwable: Throwable? = null
) : DriveDataSource {
    var loadCalls: Int = 0

    override suspend fun listPublicFolder(publicFolderUrl: String): List<DriveNode> {
        loadCalls += 1
        throwable?.let { throw it }
        return nextResult
    }

    override fun isValidPublicFolderUrl(url: String): Boolean {
        return url.contains("drive.google.com/drive/folders/")
    }
}

private class FakeDriveSourcesDataSource : DriveSourcesDataSource {
    private var nextId = 1L
    private val state = MutableStateFlow<List<DriveSource>>(emptyList())

    override fun observeSources(): Flow<List<DriveSource>> = state

    override suspend fun getSourceById(id: Long): DriveSource? {
        return state.value.firstOrNull { it.id == id }
    }

    override suspend fun addSource(title: String, folderUrl: String): Long {
        val id = nextId++
        state.value = listOf(
            DriveSource(
                id = id,
                title = title.ifBlank { "Drive Source" },
                folderUrl = folderUrl,
                folderId = "folderId",
                createdAt = 0L
            )
        ) + state.value
        return id
    }

    override suspend fun deleteSource(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}

private class FakeOfflineStatusRepository : DriveOfflineDataSource {
    override fun observeSourceSummary(sourceId: Long): Flow<SourceOfflineSummary?> = flowOf(
        SourceOfflineSummary(
            sourceId = sourceId,
            status = OfflineSourceStatus.NOT_STARTED,
            totalTracks = 0,
            downloadedTracks = 0,
            failedTracks = 0,
            downloadingTracks = 0,
            queuedTracks = 0,
            progressPercent = 0
        )
    )

    override fun observeTrackStates(sourceId: Long): Flow<List<OfflineTrackState>> = flowOf(emptyList())

    override suspend fun clearSource(sourceId: Long) = Unit
    override suspend fun seedSourceTracks(sourceId: Long, tracks: List<Track>) = Unit
}

private class FakeDownloadStarter : OfflineDownloadStarter {
    override fun downloadSource(sourceId: Long, tracks: List<Track>) = Unit
}
