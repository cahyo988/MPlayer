package com.example.musicplayer.features.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.core.model.DriveNode
import com.example.musicplayer.data.drive.DriveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DriveUiState(
    val folderUrl: String = "",
    val nodes: List<DriveNode> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DriveViewModel(
    private val repository: DriveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveUiState())
    val uiState: StateFlow<DriveUiState> = _uiState.asStateFlow()

    fun updateFolderUrl(url: String) {
        _uiState.update { it.copy(folderUrl = url, error = null) }
    }

    fun loadFolder() {
        val folderUrl = _uiState.value.folderUrl
        if (folderUrl.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a public Drive folder URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.listPublicFolder(folderUrl)
            }.onSuccess { nodes ->
                _uiState.update {
                    it.copy(
                        nodes = nodes,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load Drive folder"
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: DriveRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DriveViewModel(repository) as T
                }
            }
    }
}
