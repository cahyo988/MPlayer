package com.example.musicplayer.features.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.playlist.PlaylistDetail
import com.example.musicplayer.data.playlist.PlaylistsDataSource
import com.example.musicplayer.data.playlist.PlaylistSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PlaylistsUiState(
    val playlists: List<PlaylistSummary> = emptyList(),
    val selectedPlaylistId: Long? = null,
    val selectedPlaylist: PlaylistDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class PlaylistsViewModel(
    private val repository: PlaylistsDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()
    private var selectedPlaylistJob: Job? = null
    private var lastObservedPlaylistId: Long? = null
    private val createOrSelectMutex = Mutex()

    init {
        viewModelScope.launch {
            repository.observePlaylists().collect { playlists ->
                val selectedId = _uiState.value.selectedPlaylistId
                    ?.takeIf { selected -> playlists.any { it.id == selected } }
                    ?: playlists.firstOrNull()?.id

                _uiState.update {
                    it.copy(
                        playlists = playlists,
                        selectedPlaylistId = selectedId,
                        isLoading = false,
                        error = null
                    )
                }
                if (selectedId == null) {
                    selectedPlaylistJob?.cancel()
                    selectedPlaylistJob = null
                    lastObservedPlaylistId = null
                    _uiState.update { it.copy(selectedPlaylist = null) }
                } else if (selectedId != lastObservedPlaylistId) {
                    observePlaylistDetail(selectedId)
                }
            }
        }
    }

    private fun observePlaylistDetail(playlistId: Long) {
        selectedPlaylistJob?.cancel()
        lastObservedPlaylistId = playlistId
        selectedPlaylistJob = viewModelScope.launch {
            repository.observePlaylist(playlistId).collect { detail ->
                _uiState.update { it.copy(selectedPlaylist = detail) }
            }
        }
    }

    fun selectPlaylist(playlistId: Long) {
        _uiState.update { it.copy(selectedPlaylistId = playlistId) }
        observePlaylistDetail(playlistId)
    }

    fun createPlaylist(name: String = "My Playlist") {
        viewModelScope.launch {
            runCatching {
                repository.createPlaylist(name)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(error = it.message ?: "Failed to create playlist")
                }
            }
        }
    }

    fun deleteSelectedPlaylist() {
        val selectedId = _uiState.value.selectedPlaylistId ?: return
        viewModelScope.launch {
            runCatching {
                repository.deletePlaylist(selectedId)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(error = throwable.message ?: "Failed to delete playlist")
                }
            }
        }
    }

    fun addTrackToSelectedPlaylist(track: Track) {
        viewModelScope.launch {
            runCatching {
                val selectedId = createOrSelectMutex.withLock {
                    _uiState.value.selectedPlaylistId
                        ?.takeIf { selected -> _uiState.value.playlists.any { it.id == selected } }
                        ?: repository.createPlaylist("My Playlist").also { newId ->
                            _uiState.update { it.copy(selectedPlaylistId = newId) }
                            observePlaylistDetail(newId)
                        }
                }
                repository.addTrack(selectedId, track)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(error = throwable.message ?: "Failed to add track to playlist")
                }
            }
        }
    }

    fun removeTrackFromSelectedPlaylist(trackId: String) {
        val selectedId = _uiState.value.selectedPlaylistId ?: return
        viewModelScope.launch {
            runCatching {
                repository.removeTrack(selectedId, trackId)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(error = throwable.message ?: "Failed to remove track from playlist")
                }
            }
        }
    }

    companion object {
        fun factory(repository: PlaylistsDataSource): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlaylistsViewModel(repository) as T
                }
            }
    }
}
