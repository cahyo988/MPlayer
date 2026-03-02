package com.example.musicplayer.features.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.local.LocalMusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalLibraryUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LocalLibraryViewModel(
    private val repository: LocalMusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalLibraryUiState(isLoading = true))
    val uiState: StateFlow<LocalLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.tracks().collect { tracks ->
                _uiState.update { it.copy(tracks = tracks, isLoading = false, error = null) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.refresh()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load local songs"
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: LocalMusicRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LocalLibraryViewModel(repository) as T
                }
            }
    }
}
