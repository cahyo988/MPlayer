package com.example.musicplayer.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.musicplayer.data.settings.EqualizerPreset
import com.example.musicplayer.data.settings.EqualizerBandLevels
import com.example.musicplayer.data.settings.StreamingQuality
import com.example.musicplayer.data.settings.ThemeMode
import com.example.musicplayer.data.settings.FavoritesMaintenance
import com.example.musicplayer.data.settings.RecentsMaintenance
import com.example.musicplayer.data.settings.AppSettings
import com.example.musicplayer.data.settings.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsStore,
    private val favoritesRepository: FavoritesMaintenance,
    private val playbackHistoryRepository: RecentsMaintenance
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.observeSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.observeSettingsSnapshot()
    )

    fun updateAutoRescan(enabled: Boolean) {
        repository.updateAutoRescanOnOpen(enabled)
    }

    fun updateThemeMode(mode: ThemeMode) {
        repository.updateThemeMode(mode)
    }

    fun updateAutoPlayEnabled(enabled: Boolean) {
        repository.updateAutoPlayEnabled(enabled)
    }

    fun updateStreamingQuality(quality: StreamingQuality) {
        repository.updateStreamingQuality(quality)
    }

    fun updateEqualizerPreset(preset: EqualizerPreset) {
        repository.updateEqualizerPreset(preset)
    }

    fun updateEqualizerLevels(levels: EqualizerBandLevels) {
        repository.updateEqualizerLevels(levels)
    }

    fun saveCustomEqualizerPreset(name: String, levels: EqualizerBandLevels) {
        repository.saveCustomEqualizerPreset(name, levels)
    }

    fun applyCustomEqualizerPreset(name: String) {
        repository.applyCustomEqualizerPreset(name)
    }

    fun deleteCustomEqualizerPreset(name: String) {
        repository.deleteCustomEqualizerPreset(name)
    }

    fun clearRecents() {
        viewModelScope.launch {
            try {
                playbackHistoryRepository.clearRecents()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to clear recents", error)
            }
        }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            try {
                favoritesRepository.clearFavorites()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to clear favorites", error)
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"

        fun factory(
            repository: SettingsStore,
            favoritesRepository: FavoritesMaintenance,
            playbackHistoryRepository: RecentsMaintenance
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        repository = repository,
                        favoritesRepository = favoritesRepository,
                        playbackHistoryRepository = playbackHistoryRepository
                    ) as T
                }
            }
    }
}
