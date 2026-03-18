package com.example.musicplayer.features.settings

import com.example.musicplayer.data.settings.AppSettings
import com.example.musicplayer.data.settings.EqualizerBandLevels
import com.example.musicplayer.data.settings.EqualizerPreset
import com.example.musicplayer.data.settings.FavoritesMaintenance
import com.example.musicplayer.data.settings.RecentsMaintenance
import com.example.musicplayer.data.settings.SettingsStore
import com.example.musicplayer.data.settings.StreamingQuality
import com.example.musicplayer.data.settings.ThemeMode
import com.example.musicplayer.features.playlist.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun updateThemeModePersistsToRepository() = runTest {
        val settingsStore = FakeSettingsStore()
        val viewModel = SettingsViewModel(
            repository = settingsStore,
            favoritesRepository = FakeFavoritesMaintenance(),
            playbackHistoryRepository = FakeRecentsMaintenance()
        )

        viewModel.updateThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, settingsStore.current.themeMode)
        assertEquals(ThemeMode.DARK, viewModel.settings.first().themeMode)
    }

    @Test
    fun updateAutoRescanPersistsToRepository() = runTest {
        val settingsStore = FakeSettingsStore()
        val viewModel = SettingsViewModel(
            repository = settingsStore,
            favoritesRepository = FakeFavoritesMaintenance(),
            playbackHistoryRepository = FakeRecentsMaintenance()
        )

        viewModel.updateAutoRescan(false)

        assertEquals(false, settingsStore.current.autoRescanOnOpen)
    }

    @Test
    fun updateAutoPlayPersistsToRepository() = runTest {
        val settingsStore = FakeSettingsStore()
        val viewModel = SettingsViewModel(
            repository = settingsStore,
            favoritesRepository = FakeFavoritesMaintenance(),
            playbackHistoryRepository = FakeRecentsMaintenance()
        )

        viewModel.updateAutoPlayEnabled(false)

        assertEquals(false, settingsStore.current.autoPlayEnabled)
    }

    @Test
    fun updateStreamingQualityPersistsToRepository() = runTest {
        val settingsStore = FakeSettingsStore()
        val viewModel = SettingsViewModel(
            repository = settingsStore,
            favoritesRepository = FakeFavoritesMaintenance(),
            playbackHistoryRepository = FakeRecentsMaintenance()
        )

        viewModel.updateStreamingQuality(StreamingQuality.HIGH)

        assertEquals(StreamingQuality.HIGH, settingsStore.current.streamingQuality)
    }

    @Test
    fun updateEqualizerPersistsPresetAndLevels() = runTest {
        val settingsStore = FakeSettingsStore()
        val viewModel = SettingsViewModel(
            repository = settingsStore,
            favoritesRepository = FakeFavoritesMaintenance(),
            playbackHistoryRepository = FakeRecentsMaintenance()
        )

        viewModel.updateEqualizerPreset(EqualizerPreset.BASS_BOOST)
        viewModel.updateEqualizerLevels(
            EqualizerBandLevels(
                bass = 8,
                mid = 2,
                treble = -3
            )
        )

        assertEquals(EqualizerPreset.CUSTOM, settingsStore.current.equalizerPreset)
        assertEquals(8, settingsStore.current.equalizerLevels.bass)
        assertEquals(2, settingsStore.current.equalizerLevels.mid)
        assertEquals(-3, settingsStore.current.equalizerLevels.treble)
    }

    @Test
    fun customPresetSaveApplyDeleteUpdatesState() = runTest {
        val settingsStore = FakeSettingsStore()
        val viewModel = SettingsViewModel(
            repository = settingsStore,
            favoritesRepository = FakeFavoritesMaintenance(),
            playbackHistoryRepository = FakeRecentsMaintenance()
        )

        val levels = EqualizerBandLevels(4, 1, 3)
        viewModel.saveCustomEqualizerPreset("My Rock", levels)
        viewModel.applyCustomEqualizerPreset("My Rock")

        assertEquals(levels, settingsStore.current.equalizerLevels)
        assertEquals("My Rock", settingsStore.current.activeCustomEqualizerPresetName)
        assertTrue(settingsStore.current.customEqualizerPresets.containsKey("My Rock"))

        viewModel.deleteCustomEqualizerPreset("My Rock")
        assertTrue(!settingsStore.current.customEqualizerPresets.containsKey("My Rock"))
    }

    @Test
    fun settingsInitialStateUsesRepositorySnapshot() = runTest {
        val settingsStore = FakeSettingsStore(
            initial = AppSettings(themeMode = ThemeMode.DARK)
        )
        val viewModel = SettingsViewModel(
            repository = settingsStore,
            favoritesRepository = FakeFavoritesMaintenance(),
            playbackHistoryRepository = FakeRecentsMaintenance()
        )

        assertEquals(ThemeMode.DARK, viewModel.settings.first().themeMode)
    }

    @Test
    fun clearRecentsDelegatesToMaintenance() = runTest {
        val recentsMaintenance = FakeRecentsMaintenance()
        val viewModel = SettingsViewModel(
            repository = FakeSettingsStore(),
            favoritesRepository = FakeFavoritesMaintenance(),
            playbackHistoryRepository = recentsMaintenance
        )

        viewModel.clearRecents()
        advanceUntilIdle()

        assertTrue(recentsMaintenance.cleared)
    }

    @Test
    fun clearFavoritesDelegatesToMaintenance() = runTest {
        val favoritesMaintenance = FakeFavoritesMaintenance()
        val viewModel = SettingsViewModel(
            repository = FakeSettingsStore(),
            favoritesRepository = favoritesMaintenance,
            playbackHistoryRepository = FakeRecentsMaintenance()
        )

        viewModel.clearFavorites()
        advanceUntilIdle()

        assertTrue(favoritesMaintenance.cleared)
    }
}

private class FakeSettingsStore(
    initial: AppSettings = AppSettings()
) : SettingsStore {
    private val flow = MutableStateFlow(initial)
    val current: AppSettings
        get() = flow.value

    override fun observeSettings(): Flow<AppSettings> = flow

    override fun observeSettingsSnapshot(): AppSettings = flow.value

    override fun updateAutoRescanOnOpen(enabled: Boolean) {
        flow.value = flow.value.copy(autoRescanOnOpen = enabled)
    }

    override fun updateThemeMode(mode: ThemeMode) {
        flow.value = flow.value.copy(themeMode = mode)
    }

    override fun updateAutoPlayEnabled(enabled: Boolean) {
        flow.value = flow.value.copy(autoPlayEnabled = enabled)
    }

    override fun updateStreamingQuality(quality: StreamingQuality) {
        flow.value = flow.value.copy(streamingQuality = quality)
    }

    override fun updateEqualizerPreset(preset: EqualizerPreset) {
        flow.value = flow.value.copy(equalizerPreset = preset)
    }

    override fun updateEqualizerLevels(levels: EqualizerBandLevels) {
        flow.value = flow.value.copy(
            equalizerPreset = EqualizerPreset.CUSTOM,
            equalizerLevels = levels
        )
    }

    override fun saveCustomEqualizerPreset(name: String, levels: EqualizerBandLevels) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        flow.value = flow.value.copy(
            equalizerPreset = EqualizerPreset.CUSTOM,
            equalizerLevels = levels,
            customEqualizerPresets = flow.value.customEqualizerPresets + (trimmed to levels),
            activeCustomEqualizerPresetName = trimmed
        )
    }

    override fun applyCustomEqualizerPreset(name: String) {
        val preset = flow.value.customEqualizerPresets[name] ?: return
        flow.value = flow.value.copy(
            equalizerPreset = EqualizerPreset.CUSTOM,
            equalizerLevels = preset,
            activeCustomEqualizerPresetName = name
        )
    }

    override fun deleteCustomEqualizerPreset(name: String) {
        flow.value = flow.value.copy(
            customEqualizerPresets = flow.value.customEqualizerPresets - name,
            activeCustomEqualizerPresetName =
                if (flow.value.activeCustomEqualizerPresetName == name) null
                else flow.value.activeCustomEqualizerPresetName
        )
    }
}

private class FakeRecentsMaintenance : RecentsMaintenance {
    var cleared = false

    override suspend fun clearRecents() {
        cleared = true
    }
}

private class FakeFavoritesMaintenance : FavoritesMaintenance {
    var cleared = false

    override suspend fun clearFavorites() {
        cleared = true
    }
}
