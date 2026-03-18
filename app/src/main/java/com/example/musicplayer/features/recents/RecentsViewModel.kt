package com.example.musicplayer.features.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.history.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class RecentsViewModel(
    private val historyRepository: PlaybackHistoryRepository
) : ViewModel() {
    val recentTracks: Flow<List<Track>> = historyRepository.observeRecentTracks()

    fun clearRecents() {
        viewModelScope.launch {
            historyRepository.clearRecents()
        }
    }

    companion object {
        fun factory(historyRepository: PlaybackHistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecentsViewModel(historyRepository) as T
                }
            }
    }
}
