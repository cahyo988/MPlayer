package com.example.musicplayer.features.settings

import com.example.musicplayer.data.settings.EqualizerBandLevels
import com.example.musicplayer.data.settings.EqualizerPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerDialogCommitPlanTest {

    @Test
    fun builtInSelectionProducesBuiltInApplyAction() {
        val plan = buildEqualizerCommitPlan(
            originalCustomPresets = emptyMap(),
            draftCustomPresets = emptyMap(),
            selectedPreset = EqualizerPreset.ROCK,
            selectedCustomPresetName = null,
            draftLevels = EqualizerBandLevels(2, 4, 1, 3, 3)
        )

        assertTrue(plan.deletedPresetNames.isEmpty())
        assertTrue(plan.upsertPresets.isEmpty())
        assertEquals(
            EqualizerApplyAction.BuiltInPreset(EqualizerPreset.ROCK),
            plan.applyAction
        )
    }

    @Test
    fun selectedCustomPresetProducesCustomApplyAction() {
        val customLevels = EqualizerBandLevels(lowMid = 1, bass = 2, mid = 3, highMid = 4, treble = 5)
        val plan = buildEqualizerCommitPlan(
            originalCustomPresets = mapOf("MyPreset" to customLevels),
            draftCustomPresets = mapOf("MyPreset" to customLevels),
            selectedPreset = EqualizerPreset.CUSTOM,
            selectedCustomPresetName = "MyPreset",
            draftLevels = customLevels
        )

        assertTrue(plan.deletedPresetNames.isEmpty())
        assertTrue(plan.upsertPresets.isEmpty())
        assertEquals(EqualizerApplyAction.CustomPreset("MyPreset"), plan.applyAction)
    }

    @Test
    fun editedLevelsProduceCustomLevelsApplyAction() {
        val original = EqualizerBandLevels()
        val edited = EqualizerBandLevels(lowMid = 2, bass = 1, mid = 0, highMid = -1, treble = -2)

        val plan = buildEqualizerCommitPlan(
            originalCustomPresets = mapOf("A" to original),
            draftCustomPresets = mapOf("A" to original),
            selectedPreset = EqualizerPreset.CUSTOM,
            selectedCustomPresetName = "A",
            draftLevels = edited
        )

        assertEquals(EqualizerApplyAction.CustomLevels(edited), plan.applyAction)
    }

    @Test
    fun commitPlanTracksDeletedAndUpdatedCustomPresets() {
        val original = mapOf(
            "Keep" to EqualizerBandLevels(),
            "DeleteMe" to EqualizerBandLevels(bass = 1)
        )
        val updated = mapOf(
            "Keep" to EqualizerBandLevels(mid = 2),
            "NewOne" to EqualizerBandLevels(treble = 3)
        )

        val plan = buildEqualizerCommitPlan(
            originalCustomPresets = original,
            draftCustomPresets = updated,
            selectedPreset = EqualizerPreset.CUSTOM,
            selectedCustomPresetName = null,
            draftLevels = EqualizerBandLevels(mid = 2)
        )

        assertEquals(setOf("DeleteMe"), plan.deletedPresetNames)
        assertEquals(updated, plan.upsertPresets)
    }
}
