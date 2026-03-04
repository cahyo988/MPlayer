package com.example.musicplayer.features.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.core.model.DriveNode
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.drive.DriveDataSource
import com.example.musicplayer.data.drive.DriveSource
import com.example.musicplayer.data.drive.DriveSourcesDataSource
import com.example.musicplayer.data.offline.DriveOfflineDataSource
import com.example.musicplayer.data.offline.OfflineDownloadStarter
import com.example.musicplayer.data.offline.OfflineTrackState
import com.example.musicplayer.data.offline.model.OfflineTrackStatus
import com.example.musicplayer.data.offline.model.SourceOfflineSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

enum class DrivePage {
    LOAD,
    SAVED
}

data class DriveUiState(
    val page: DrivePage = DrivePage.LOAD,
    val folderUrl: String = "",
    val sourceTitleInput: String = "",
    val sources: List<DriveSource> = emptyList(),
    val selectedSourceId: Long? = null,
    val nodes: List<DriveNode> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val hasLoaded: Boolean = false,
    val sourceOfflineSummary: SourceOfflineSummary? = null,
    val trackOfflineStates: Map<String, OfflineTrackState> = emptyMap()
)

class DriveViewModel(
    private val repository: DriveDataSource,
    private val sourceRepository: DriveSourcesDataSource,
    private val offlineStatusRepository: DriveOfflineDataSource,
    private val downloadStarter: OfflineDownloadStarter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveUiState())
    val uiState: StateFlow<DriveUiState> = _uiState.asStateFlow()
    private val loadMutex = Mutex()
    private var lastSubmittedUrl: String = ""
    private var latestLoadRequestId: Long = 0L
    private var activeLoadRequestId: Long? = null
    private var summaryJob: Job? = null
    private var tracksJob: Job? = null

    init {
        viewModelScope.launch {
            sourceRepository.observeSources().collect { sources ->
                val selected = _uiState.value.selectedSourceId
                    ?.takeIf { id -> sources.any { it.id == id } }
                    ?: sources.firstOrNull()?.id
                _uiState.update { it.copy(sources = sources, selectedSourceId = selected) }
                observeOffline(selected)
            }
        }
    }

    fun switchPage(page: DrivePage) {
        _uiState.update { it.copy(page = page) }
    }

    fun updateFolderUrl(url: String) {
        latestLoadRequestId++
        activeLoadRequestId = null
        _uiState.update { it.copy(folderUrl = url, error = null, message = null, isLoading = false) }
    }

    fun updateSourceTitle(title: String) {
        _uiState.update { it.copy(sourceTitleInput = title, error = null, message = null) }
    }

    fun addSource() {
        val state = _uiState.value
        val folderUrl = state.folderUrl.trim()
        val title = state.sourceTitleInput.trim()
        if (folderUrl.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a Public Drive URL") }
            return
        }
        if (!repository.isValidPublicFolderUrl(folderUrl)) {
            _uiState.update {
                it.copy(error = "Invalid URL. Example: https://drive.google.com/drive/folders/yourFolderId")
            }
            return
        }
        viewModelScope.launch {
            runCatching {
                sourceRepository.addSource(title = title, folderUrl = folderUrl)
            }.onSuccess { id ->
                _uiState.update {
                    it.copy(
                        selectedSourceId = id,
                        sourceTitleInput = "",
                        page = DrivePage.SAVED,
                        message = "Source saved",
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(error = throwable.message ?: "Failed to save source", message = null)
                }
            }
        }
    }

    fun deleteSource(sourceId: Long) {
        viewModelScope.launch {
            runCatching {
                offlineStatusRepository.clearSource(sourceId)
                sourceRepository.deleteSource(sourceId)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(error = throwable.message ?: "Failed to delete source", message = null)
                }
            }
        }
    }

    fun selectSource(sourceId: Long) {
        val source = _uiState.value.sources.firstOrNull { it.id == sourceId } ?: return
        latestLoadRequestId++
        activeLoadRequestId = null
        _uiState.update {
            it.copy(
                selectedSourceId = sourceId,
                folderUrl = source.folderUrl,
                nodes = emptyList(),
                hasLoaded = false,
                isLoading = false,
                error = null,
                message = null
            )
        }
        observeOffline(sourceId)
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun loadSelectedSource(force: Boolean = false) {
        val sourceId = _uiState.value.selectedSourceId
        if (sourceId == null) {
            _uiState.update { it.copy(error = "Select a saved source first") }
            return
        }
        viewModelScope.launch {
            val source = sourceRepository.getSourceById(sourceId)
            if (source == null) {
                _uiState.update { it.copy(error = "Source not found") }
                return@launch
            }
            updateFolderUrl(source.folderUrl)
            loadFolder(force)
        }
    }

    fun loadFolder(force: Boolean = false) {
        val folderUrl = _uiState.value.folderUrl.trim()
        val expectedSourceId = _uiState.value.selectedSourceId
        if (folderUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    error = "Please enter a Public Drive URL",
                    message = null
                )
            }
            return
        }
        if (!repository.isValidPublicFolderUrl(folderUrl)) {
            _uiState.update {
                it.copy(
                    error = "Invalid URL. Example: https://drive.google.com/drive/folders/yourFolderId",
                    message = null,
                    hasLoaded = false
                )
            }
            return
        }

        viewModelScope.launch {
            val requestId = ++latestLoadRequestId
            loadMutex.withLock {
                if (_uiState.value.isLoading) return@withLock
                if (!force && folderUrl == lastSubmittedUrl && _uiState.value.hasLoaded) {
                    _uiState.update { it.copy(message = "Already loaded. Tap Retry to refresh.") }
                    return@withLock
                }

                _uiState.update { it.copy(isLoading = true, error = null, message = null) }
                activeLoadRequestId = requestId

                val result = runCatching {
                    fetchWithRetry(folderUrl)
                }.onFailure {
                    if (it is CancellationException && it !is TimeoutCancellationException) throw it
                }

                result.onSuccess { nodes ->
                    if (requestId != activeLoadRequestId) return@onSuccess
                    if (
                        _uiState.value.selectedSourceId != expectedSourceId ||
                        _uiState.value.folderUrl.trim() != folderUrl
                    ) {
                        activeLoadRequestId = null
                        _uiState.update { it.copy(isLoading = false) }
                        return@onSuccess
                    }

                    lastSubmittedUrl = folderUrl
                    activeLoadRequestId = null
                    _uiState.update {
                        it.copy(
                            nodes = nodes,
                            isLoading = false,
                            error = null,
                            hasLoaded = true,
                            message = if (nodes.isEmpty()) "No audio files found in this public folder." else null
                        )
                    }
                    seedOfflineForLoadedNodes()
                }.onFailure { throwable ->
                    if (requestId != activeLoadRequestId) return@onFailure
                    if (_uiState.value.selectedSourceId != expectedSourceId) {
                        activeLoadRequestId = null
                        _uiState.update { it.copy(isLoading = false) }
                        return@onFailure
                    }

                    activeLoadRequestId = null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load Drive folder",
                            hasLoaded = true
                        )
                    }
                }
            }
        }
    }

    fun downloadSelectedSource() {
        val sourceId = _uiState.value.selectedSourceId ?: return
        if (!_uiState.value.hasLoaded) {
            _uiState.update { it.copy(message = "Load tracks first") }
            return
        }
        val tracks = _uiState.value.nodes.mapNotNull { it.track }
        if (tracks.isEmpty()) {
            _uiState.update { it.copy(message = "Load tracks first") }
            return
        }
        downloadStarter.downloadSource(sourceId, tracks)
        _uiState.update { it.copy(message = "Download started") }
    }

    private fun observeOffline(sourceId: Long?) {
        summaryJob?.cancel()
        tracksJob?.cancel()
        if (sourceId == null) {
            _uiState.update { it.copy(sourceOfflineSummary = null, trackOfflineStates = emptyMap()) }
            return
        }
        summaryJob = viewModelScope.launch {
            offlineStatusRepository.observeSourceSummary(sourceId).collect { summary ->
                _uiState.update { it.copy(sourceOfflineSummary = summary) }
            }
        }
        tracksJob = viewModelScope.launch {
            offlineStatusRepository.observeTrackStates(sourceId).collect { tracks ->
                _uiState.update { it.copy(trackOfflineStates = tracks.associateBy { row -> row.trackId }) }
            }
        }
    }

    private fun seedOfflineForLoadedNodes() {
        val sourceId = _uiState.value.selectedSourceId ?: return
        val tracks = _uiState.value.nodes.mapNotNull { it.track }
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            offlineStatusRepository.seedSourceTracks(sourceId, tracks)
        }
    }

    private suspend fun fetchWithRetry(folderUrl: String): List<DriveNode> {
        var failure: Throwable? = null
        repeat(MAX_RETRIES + 1) { attempt ->
            val result = runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    repository.listPublicFolder(folderUrl)
                }
            }.onFailure {
                if (it is CancellationException && it !is TimeoutCancellationException) throw it
            }
            if (result.isSuccess) return result.getOrThrow()
            failure = result.exceptionOrNull()
            if (attempt < MAX_RETRIES) {
                kotlinx.coroutines.delay(RETRY_DELAY_MS)
            }
        }
        throw (failure ?: IllegalStateException("Failed to load Drive folder"))
    }

    fun trackOfflineStatus(track: Track): OfflineTrackStatus {
        return _uiState.value.trackOfflineStates[track.id]?.status ?: OfflineTrackStatus.NOT_DOWNLOADED
    }

    companion object {
        private const val MAX_RETRIES = 1
        private const val RETRY_DELAY_MS = 600L
        private const val LOAD_TIMEOUT_MS = 12_000L

        fun factory(
            repository: DriveDataSource,
            sourceRepository: DriveSourcesDataSource,
            offlineStatusRepository: DriveOfflineDataSource,
            downloadStarter: OfflineDownloadStarter
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DriveViewModel(
                        repository,
                        sourceRepository,
                        offlineStatusRepository,
                        downloadStarter
                    ) as T
                }
            }
    }
}
