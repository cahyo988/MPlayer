package com.example.musicplayer.features.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.local.LocalMusicRepository
import com.example.musicplayer.data.playlist.PlaylistsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class LocalLibraryUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class LocalLibraryViewModel(
    private val repository: LocalMusicRepository,
    private val playlistsRepository: PlaylistsDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalLibraryUiState(isLoading = true))
    val uiState: StateFlow<LocalLibraryUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { playlistsRepository.ensureDefaultLocalPlaylist() }
        }
        viewModelScope.launch {
            repository.tracks().collect { tracks ->
                _uiState.update { it.copy(tracks = tracks, isLoading = false, error = null) }
            }
        }
    }

    fun refresh(force: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update { state ->
                val hasTracks = state.tracks.isNotEmpty()
                state.copy(
                    isLoading = !hasTracks,
                    isRefreshing = hasTracks,
                    error = null
                )
            }
            runCatching {
                val hasChanged = repository.refresh(force = force)
                if (hasChanged) {
                    playlistsRepository.syncDefaultLocalPlaylist(repository.tracks().value)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = throwable.message ?: "Failed to load local songs"
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    companion object {
        fun factory(
            repository: LocalMusicRepository,
            playlistsRepository: PlaylistsDataSource
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LocalLibraryViewModel(repository, playlistsRepository) as T
                }
            }
    }
}
