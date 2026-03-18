package com.example.musicplayer.data.settings

import android.content.Context
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val autoRescanOnOpen: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoPlayEnabled: Boolean = true,
    val streamingQuality: StreamingQuality = StreamingQuality.BALANCED,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.NORMAL,
    val equalizerLevels: EqualizerBandLevels = EqualizerReferencePresets.levelsFor(EqualizerPreset.NORMAL),
    val customEqualizerPresets: Map<String, EqualizerBandLevels> = emptyMap(),
    val activeCustomEqualizerPresetName: String? = null
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class StreamingQuality {
    DATA_SAVER,
    BALANCED,
    HIGH
}

enum class EqualizerPreset {
    NORMAL,
    CLASSICAL,
    POP,
    ROCK,
    JAZZ,
    BASS_BOOST,
    VOCAL,
    TREBLE_BOOST,
    ACOUSTIC,
    ELECTRONIC,
    CUSTOM
}

class AppSettingsRepository(
    context: Context
) : SettingsStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val settingsFlow = MutableStateFlow(loadSettings())

    override fun observeSettings(): Flow<AppSettings> = settingsFlow.asStateFlow()

    override fun observeSettingsSnapshot(): AppSettings = settingsFlow.value

    override fun updateAutoRescanOnOpen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESCAN, enabled).apply()
        settingsFlow.value = settingsFlow.value.copy(autoRescanOnOpen = enabled)
    }

    override fun updateThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        settingsFlow.value = settingsFlow.value.copy(themeMode = mode)
    }

    override fun updateAutoPlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY_ENABLED, enabled).apply()
        settingsFlow.value = settingsFlow.value.copy(autoPlayEnabled = enabled)
    }

    override fun updateStreamingQuality(quality: StreamingQuality) {
        prefs.edit().putString(KEY_STREAMING_QUALITY, quality.name).apply()
        settingsFlow.value = settingsFlow.value.copy(streamingQuality = quality)
    }

    override fun updateEqualizerPreset(preset: EqualizerPreset) {
        val nextLevels = if (preset == EqualizerPreset.CUSTOM) {
            settingsFlow.value.equalizerLevels
        } else {
            EqualizerReferencePresets.levelsFor(preset)
        }
        prefs.edit()
            .putString(KEY_EQUALIZER_PRESET, preset.name)
            .putInt(KEY_EQUALIZER_LOW_MID_LEVEL, nextLevels.lowMid)
            .putInt(KEY_EQUALIZER_BASS_LEVEL, nextLevels.bass)
            .putInt(KEY_EQUALIZER_MID_LEVEL, nextLevels.mid)
            .putInt(KEY_EQUALIZER_HIGH_MID_LEVEL, nextLevels.highMid)
            .putInt(KEY_EQUALIZER_TREBLE_LEVEL, nextLevels.treble)
            .remove(KEY_EQUALIZER_ACTIVE_CUSTOM_PRESET_NAME)
            .apply()
        settingsFlow.value = settingsFlow.value.copy(
            equalizerPreset = preset,
            equalizerLevels = nextLevels,
            activeCustomEqualizerPresetName = null
        )
    }

    override fun updateEqualizerLevels(levels: EqualizerBandLevels) {
        val safeLevels = levels.sanitized(MIN_EQ_LEVEL, MAX_EQ_LEVEL)
        prefs.edit()
            .putString(KEY_EQUALIZER_PRESET, EqualizerPreset.CUSTOM.name)
            .putInt(KEY_EQUALIZER_LOW_MID_LEVEL, safeLevels.lowMid)
            .putInt(KEY_EQUALIZER_BASS_LEVEL, safeLevels.bass)
            .putInt(KEY_EQUALIZER_MID_LEVEL, safeLevels.mid)
            .putInt(KEY_EQUALIZER_HIGH_MID_LEVEL, safeLevels.highMid)
            .putInt(KEY_EQUALIZER_TREBLE_LEVEL, safeLevels.treble)
            .remove(KEY_EQUALIZER_ACTIVE_CUSTOM_PRESET_NAME)
            .apply()
        settingsFlow.value = settingsFlow.value.copy(
            equalizerPreset = EqualizerPreset.CUSTOM,
            equalizerLevels = safeLevels,
            activeCustomEqualizerPresetName = null
        )
    }

    override fun saveCustomEqualizerPreset(name: String, levels: EqualizerBandLevels) {
        val trimmedName = name.trim().take(MAX_CUSTOM_PRESET_NAME_LENGTH)
        if (trimmedName.isEmpty()) return

        val safeLevels = levels.sanitized(MIN_EQ_LEVEL, MAX_EQ_LEVEL)
        val updatedPresets = settingsFlow.value.customEqualizerPresets.toMutableMap().apply {
            put(trimmedName, safeLevels)
            if (size > MAX_CUSTOM_PRESET_COUNT) {
                val overflow = size - MAX_CUSTOM_PRESET_COUNT
                keys.sorted().take(overflow).forEach { remove(it) }
            }
        }.toMap()

        if (!persistCustomPresets(updatedPresets)) return
        prefs.edit()
            .putString(KEY_EQUALIZER_PRESET, EqualizerPreset.CUSTOM.name)
            .putString(KEY_EQUALIZER_ACTIVE_CUSTOM_PRESET_NAME, trimmedName)
            .putInt(KEY_EQUALIZER_LOW_MID_LEVEL, safeLevels.lowMid)
            .putInt(KEY_EQUALIZER_BASS_LEVEL, safeLevels.bass)
            .putInt(KEY_EQUALIZER_MID_LEVEL, safeLevels.mid)
            .putInt(KEY_EQUALIZER_HIGH_MID_LEVEL, safeLevels.highMid)
            .putInt(KEY_EQUALIZER_TREBLE_LEVEL, safeLevels.treble)
            .apply()

        settingsFlow.value = settingsFlow.value.copy(
            equalizerPreset = EqualizerPreset.CUSTOM,
            equalizerLevels = safeLevels,
            customEqualizerPresets = updatedPresets,
            activeCustomEqualizerPresetName = trimmedName
        )
    }

    override fun applyCustomEqualizerPreset(name: String) {
        val customPreset = settingsFlow.value.customEqualizerPresets[name] ?: return
        val safeLevels = customPreset.sanitized(MIN_EQ_LEVEL, MAX_EQ_LEVEL)

        prefs.edit()
            .putString(KEY_EQUALIZER_PRESET, EqualizerPreset.CUSTOM.name)
            .putString(KEY_EQUALIZER_ACTIVE_CUSTOM_PRESET_NAME, name)
            .putInt(KEY_EQUALIZER_LOW_MID_LEVEL, safeLevels.lowMid)
            .putInt(KEY_EQUALIZER_BASS_LEVEL, safeLevels.bass)
            .putInt(KEY_EQUALIZER_MID_LEVEL, safeLevels.mid)
            .putInt(KEY_EQUALIZER_HIGH_MID_LEVEL, safeLevels.highMid)
            .putInt(KEY_EQUALIZER_TREBLE_LEVEL, safeLevels.treble)
            .apply()

        settingsFlow.value = settingsFlow.value.copy(
            equalizerPreset = EqualizerPreset.CUSTOM,
            equalizerLevels = safeLevels,
            activeCustomEqualizerPresetName = name
        )
    }

    override fun deleteCustomEqualizerPreset(name: String) {
        val updatedPresets = settingsFlow.value.customEqualizerPresets.toMutableMap().apply {
            remove(name)
        }.toMap()

        if (!persistCustomPresets(updatedPresets)) return

        val shouldClearActive = settingsFlow.value.activeCustomEqualizerPresetName == name
        if (shouldClearActive) {
            prefs.edit().remove(KEY_EQUALIZER_ACTIVE_CUSTOM_PRESET_NAME).apply()
        }

        settingsFlow.value = settingsFlow.value.copy(
            customEqualizerPresets = updatedPresets,
            activeCustomEqualizerPresetName = if (shouldClearActive) null else settingsFlow.value.activeCustomEqualizerPresetName
        )
    }

    private fun loadSettings(): AppSettings {
        val loadedPresets = loadCustomPresets().toMutableMap().apply {
            keys.filter { it.length > MAX_CUSTOM_PRESET_NAME_LENGTH }.forEach { remove(it) }
            if (size > MAX_CUSTOM_PRESET_COUNT) {
                val overflow = size - MAX_CUSTOM_PRESET_COUNT
                keys.sorted().take(overflow).forEach { remove(it) }
            }
        }.toMap()
        val loadedPreset = loadEqualizerPreset()
        val presetDefaultLevels = EqualizerReferencePresets.levelsFor(loadedPreset)
        val loadedLevels = EqualizerBandLevels(
            lowMid = prefs.getInt(KEY_EQUALIZER_LOW_MID_LEVEL, presetDefaultLevels.lowMid),
            bass = prefs.getInt(KEY_EQUALIZER_BASS_LEVEL, presetDefaultLevels.bass),
            mid = prefs.getInt(KEY_EQUALIZER_MID_LEVEL, presetDefaultLevels.mid),
            highMid = prefs.getInt(KEY_EQUALIZER_HIGH_MID_LEVEL, presetDefaultLevels.highMid),
            treble = prefs.getInt(KEY_EQUALIZER_TREBLE_LEVEL, presetDefaultLevels.treble)
        ).sanitized(MIN_EQ_LEVEL, MAX_EQ_LEVEL)
        val activeCustomPresetName = prefs.getString(KEY_EQUALIZER_ACTIVE_CUSTOM_PRESET_NAME, null)
            ?.takeIf { loadedPresets.containsKey(it) }

        return AppSettings(
            autoRescanOnOpen = prefs.getBoolean(KEY_AUTO_RESCAN, true),
            themeMode = loadThemeMode(),
            autoPlayEnabled = prefs.getBoolean(KEY_AUTO_PLAY_ENABLED, true),
            streamingQuality = loadStreamingQuality(),
            equalizerPreset = loadedPreset,
            equalizerLevels = loadedLevels,
            customEqualizerPresets = loadedPresets,
            activeCustomEqualizerPresetName = activeCustomPresetName
        )
    }

    private fun loadThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(raw.orEmpty()) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private fun loadStreamingQuality(): StreamingQuality {
        val raw = prefs.getString(KEY_STREAMING_QUALITY, StreamingQuality.BALANCED.name)
        return runCatching {
            StreamingQuality.valueOf(raw.orEmpty())
        }.getOrDefault(StreamingQuality.BALANCED)
    }

    private fun loadEqualizerPreset(): EqualizerPreset {
        val raw = prefs.getString(KEY_EQUALIZER_PRESET, EqualizerPreset.NORMAL.name)
        return runCatching {
            EqualizerPreset.valueOf(raw.orEmpty())
        }.getOrDefault(EqualizerPreset.NORMAL)
    }

    private fun persistCustomPresets(presets: Map<String, EqualizerBandLevels>): Boolean {
        val serialized = presets.entries.joinToString(separator = ";") { (name, levels) ->
            val safeLevels = levels.sanitized(MIN_EQ_LEVEL, MAX_EQ_LEVEL)
            val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
            "$encodedName,${safeLevels.lowMid},${safeLevels.bass},${safeLevels.mid},${safeLevels.highMid},${safeLevels.treble}"
        }
        if (serialized.toByteArray(StandardCharsets.UTF_8).size > MAX_CUSTOM_PRESETS_BYTES) {
            return false
        }
        prefs.edit().putString(KEY_EQUALIZER_CUSTOM_PRESETS, serialized).apply()
        return true
    }

    private fun loadCustomPresets(): Map<String, EqualizerBandLevels> {
        val raw = prefs.getString(KEY_EQUALIZER_CUSTOM_PRESETS, null).orEmpty()
        if (raw.isBlank()) return emptyMap()
        if (raw.toByteArray(StandardCharsets.UTF_8).size > MAX_CUSTOM_PRESETS_BYTES) {
            prefs.edit().remove(KEY_EQUALIZER_CUSTOM_PRESETS).apply()
            return emptyMap()
        }

        return raw.split(";")
            .asSequence()
            .mapNotNull { entry ->
                val tokens = entry.split(",")
                if (tokens.size != 6 && tokens.size != 4) return@mapNotNull null
                val name = runCatching {
                    URLDecoder.decode(tokens[0], StandardCharsets.UTF_8.name())
                }.getOrNull() ?: return@mapNotNull null
                val levels = if (tokens.size == 6) {
                    val lowMid = tokens[1].toIntOrNull() ?: return@mapNotNull null
                    val bass = tokens[2].toIntOrNull() ?: return@mapNotNull null
                    val mid = tokens[3].toIntOrNull() ?: return@mapNotNull null
                    val highMid = tokens[4].toIntOrNull() ?: return@mapNotNull null
                    val treble = tokens[5].toIntOrNull() ?: return@mapNotNull null
                    EqualizerBandLevels(
                        lowMid = lowMid,
                        bass = bass,
                        mid = mid,
                        highMid = highMid,
                        treble = treble
                    )
                } else {
                    val bass = tokens[1].toIntOrNull() ?: return@mapNotNull null
                    val mid = tokens[2].toIntOrNull() ?: return@mapNotNull null
                    val treble = tokens[3].toIntOrNull() ?: return@mapNotNull null
                    EqualizerBandLevels(
                        lowMid = 0,
                        bass = bass,
                        mid = mid,
                        highMid = 0,
                        treble = treble
                    )
                }

                name to levels.sanitized(MIN_EQ_LEVEL, MAX_EQ_LEVEL)
            }
            .toMap()
    }

    companion object {
        const val MIN_EQ_LEVEL = -10
        const val MAX_EQ_LEVEL = 10
        private const val MAX_CUSTOM_PRESET_NAME_LENGTH = 40
        private const val MAX_CUSTOM_PRESET_COUNT = 20
        private const val MAX_CUSTOM_PRESETS_BYTES = 8 * 1024
        private const val PREF_NAME = "app_settings"
        private const val KEY_AUTO_RESCAN = "auto_rescan_on_open"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AUTO_PLAY_ENABLED = "auto_play_enabled"
        private const val KEY_STREAMING_QUALITY = "streaming_quality"
        private const val KEY_EQUALIZER_PRESET = "equalizer_preset"
        private const val KEY_EQUALIZER_CUSTOM_PRESETS = "equalizer_custom_presets"
        private const val KEY_EQUALIZER_ACTIVE_CUSTOM_PRESET_NAME = "equalizer_active_custom_preset_name"
        private const val KEY_EQUALIZER_LOW_MID_LEVEL = "equalizer_low_mid_level"
        private const val KEY_EQUALIZER_BASS_LEVEL = "equalizer_bass_level"
        private const val KEY_EQUALIZER_MID_LEVEL = "equalizer_mid_level"
        private const val KEY_EQUALIZER_HIGH_MID_LEVEL = "equalizer_high_mid_level"
        private const val KEY_EQUALIZER_TREBLE_LEVEL = "equalizer_treble_level"
    }
}
