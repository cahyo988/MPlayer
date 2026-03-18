package com.example.musicplayer.data.settings

data class EqualizerBandLevels(
    val lowMid: Int = 0,
    val bass: Int = 0,
    val mid: Int = 0,
    val highMid: Int = 0,
    val treble: Int = 0
) {
    fun sanitized(
        min: Int = AppSettingsRepository.MIN_EQ_LEVEL,
        max: Int = AppSettingsRepository.MAX_EQ_LEVEL
    ): EqualizerBandLevels {
        return EqualizerBandLevels(
            lowMid = lowMid.coerceIn(min, max),
            bass = bass.coerceIn(min, max),
            mid = mid.coerceIn(min, max),
            highMid = highMid.coerceIn(min, max),
            treble = treble.coerceIn(min, max)
        )
    }
}

object EqualizerReferencePresets {
    fun levelsFor(preset: EqualizerPreset): EqualizerBandLevels {
        return when (preset) {
            EqualizerPreset.NORMAL -> EqualizerBandLevels(0, 0, 0, 0, 0)
            EqualizerPreset.CLASSICAL -> EqualizerBandLevels(-1, 2, -1, 1, 3)
            EqualizerPreset.POP -> EqualizerBandLevels(1, 3, 1, 2, 2)
            EqualizerPreset.ROCK -> EqualizerBandLevels(2, 4, 1, 3, 3)
            EqualizerPreset.JAZZ -> EqualizerBandLevels(1, 2, 2, 2, 3)
            EqualizerPreset.VOCAL -> EqualizerBandLevels(2, -1, 3, 4, 2)
            EqualizerPreset.ACOUSTIC -> EqualizerBandLevels(1, 1, 2, 3, 2)
            EqualizerPreset.ELECTRONIC -> EqualizerBandLevels(3, 5, 0, 2, 4)
            EqualizerPreset.BASS_BOOST -> EqualizerBandLevels(1, 7, 1, 0, -1)
            EqualizerPreset.TREBLE_BOOST -> EqualizerBandLevels(-1, 0, 1, 3, 7)
            EqualizerPreset.CUSTOM -> EqualizerBandLevels(0, 0, 0, 0, 0)
        }
    }
}
