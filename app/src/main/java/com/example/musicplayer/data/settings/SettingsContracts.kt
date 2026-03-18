package com.example.musicplayer.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsStore {
    fun observeSettings(): Flow<AppSettings>
    fun observeSettingsSnapshot(): AppSettings
    fun updateAutoRescanOnOpen(enabled: Boolean)
    fun updateThemeMode(mode: ThemeMode)
    fun updateAutoPlayEnabled(enabled: Boolean)
    fun updateStreamingQuality(quality: StreamingQuality)
    fun updateEqualizerPreset(preset: EqualizerPreset)
    fun updateEqualizerLevels(levels: EqualizerBandLevels)
    fun saveCustomEqualizerPreset(name: String, levels: EqualizerBandLevels)
    fun applyCustomEqualizerPreset(name: String)
    fun deleteCustomEqualizerPreset(name: String)
}

interface RecentsMaintenance {
    suspend fun clearRecents()
}

interface FavoritesMaintenance {
    suspend fun clearFavorites()
}
