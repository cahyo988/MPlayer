package com.example.musicplayer.features.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.favorites.FavoritesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: FavoritesRepository
) : ViewModel() {
    val favoriteTracks: StateFlow<List<Track>> = repository.observeFavorites().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun isFavorite(trackId: String): StateFlow<Boolean> {
        return repository.observeFavorites().map { tracks -> tracks.any { it.id == trackId } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )
    }

    suspend fun isFavoriteNow(trackId: String): Boolean {
        return repository.isFavorite(trackId)
    }

    fun toggleFavorite(track: Track, shouldFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggle(track, shouldFavorite)
        }
    }

    companion object {
        fun factory(repository: FavoritesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FavoritesViewModel(repository) as T
                }
            }
    }
}
