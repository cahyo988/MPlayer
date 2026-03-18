package com.example.musicplayer.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.musicplayer.R
import com.example.musicplayer.data.settings.AppSettings
import com.example.musicplayer.data.settings.EqualizerBandLevels
import com.example.musicplayer.data.settings.EqualizerPreset
import com.example.musicplayer.data.settings.EqualizerReferencePresets
import kotlin.math.roundToInt

private const val CUSTOM_PRESET_MAX_LENGTH = 40

internal sealed interface EqualizerApplyAction {
    data class BuiltInPreset(val preset: EqualizerPreset) : EqualizerApplyAction
    data class CustomPreset(val name: String) : EqualizerApplyAction
    data class CustomLevels(val levels: EqualizerBandLevels) : EqualizerApplyAction
}

internal data class EqualizerCommitPlan(
    val deletedPresetNames: Set<String>,
    val upsertPresets: Map<String, EqualizerBandLevels>,
    val applyAction: EqualizerApplyAction
)

internal fun buildEqualizerCommitPlan(
    originalCustomPresets: Map<String, EqualizerBandLevels>,
    draftCustomPresets: Map<String, EqualizerBandLevels>,
    selectedPreset: EqualizerPreset,
    selectedCustomPresetName: String?,
    draftLevels: EqualizerBandLevels
): EqualizerCommitPlan {
    val selectedCustomLevels = selectedCustomPresetName?.let { draftCustomPresets[it] }
    val deletedPresetNames = originalCustomPresets.keys - draftCustomPresets.keys
    val upsertPresets = draftCustomPresets.filter { (name, levels) ->
        originalCustomPresets[name] != levels
    }

    val applyAction = when {
        selectedPreset != EqualizerPreset.CUSTOM &&
            draftLevels == EqualizerReferencePresets.levelsFor(selectedPreset) -> {
            EqualizerApplyAction.BuiltInPreset(selectedPreset)
        }

        selectedCustomPresetName != null && draftLevels == selectedCustomLevels -> {
            EqualizerApplyAction.CustomPreset(selectedCustomPresetName)
        }

        else -> EqualizerApplyAction.CustomLevels(draftLevels)
    }

    return EqualizerCommitPlan(
        deletedPresetNames = deletedPresetNames,
        upsertPresets = upsertPresets,
        applyAction = applyAction
    )
}

@Composable
fun EqualizerDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onApplyPreset: (EqualizerPreset) -> Unit,
    onSaveLevels: (EqualizerBandLevels) -> Unit,
    onSaveCustomPreset: (String, EqualizerBandLevels) -> Unit,
    onApplyCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit
) {
    val builtInPresets = remember {
        listOf(
            EqualizerPreset.NORMAL,
            EqualizerPreset.CLASSICAL,
            EqualizerPreset.POP,
            EqualizerPreset.ROCK,
            EqualizerPreset.JAZZ,
            EqualizerPreset.VOCAL,
            EqualizerPreset.ACOUSTIC,
            EqualizerPreset.ELECTRONIC,
            EqualizerPreset.BASS_BOOST,
            EqualizerPreset.TREBLE_BOOST
        )
    }

    var selectedPreset by remember(settings.equalizerPreset) {
        mutableStateOf(settings.equalizerPreset)
    }
    var draftLevels by remember(settings.equalizerLevels) {
        mutableStateOf(settings.equalizerLevels)
    }
    var draftCustomPresets by remember(settings.customEqualizerPresets) {
        mutableStateOf(settings.customEqualizerPresets)
    }
    var customPresetName by remember(settings.activeCustomEqualizerPresetName) {
        mutableStateOf(settings.activeCustomEqualizerPresetName.orEmpty())
    }
    var selectedCustomPresetName by remember(
        settings.activeCustomEqualizerPresetName,
        settings.customEqualizerPresets
    ) {
        mutableStateOf(
            settings.activeCustomEqualizerPresetName
                ?.takeIf { settings.customEqualizerPresets.containsKey(it) }
        )
    }
    val customPresetNames = remember(draftCustomPresets) { draftCustomPresets.keys.sorted() }
    val canSaveCustomPreset = customPresetName.trim().isNotEmpty()
    val canDeleteCustomPreset = selectedCustomPresetName != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_equalizer_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_equalizer_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PresetDropdown(
                    label = stringResource(R.string.settings_equalizer_preset_selector_label),
                    selectedLabel = equalizerPresetLabel(selectedPreset),
                    options = builtInPresets.map { equalizerPresetLabel(it) },
                    onOptionSelected = { index ->
                        val preset = builtInPresets[index]
                        selectedPreset = preset
                        selectedCustomPresetName = null
                        draftLevels = EqualizerReferencePresets.levelsFor(preset)
                    }
                )

                PresetDropdown(
                    label = stringResource(R.string.settings_equalizer_custom_presets_title),
                    selectedLabel = selectedCustomPresetName
                        ?: stringResource(R.string.settings_equalizer_no_custom_presets),
                    options = customPresetNames,
                    onOptionSelected = { index ->
                        val name = customPresetNames[index]
                        val levels = draftCustomPresets[name] ?: return@PresetDropdown
                        selectedPreset = EqualizerPreset.CUSTOM
                        selectedCustomPresetName = name
                        draftLevels = levels
                        customPresetName = name
                    }
                )

                EqualizerSliders(
                    levels = draftLevels,
                    onLevelsChanged = {
                        selectedPreset = EqualizerPreset.CUSTOM
                        selectedCustomPresetName = null
                        draftLevels = it
                    }
                )

                OutlinedTextField(
                    value = customPresetName,
                    onValueChange = { input ->
                        customPresetName = input.take(CUSTOM_PRESET_MAX_LENGTH)
                    },
                    label = { Text(stringResource(R.string.settings_equalizer_custom_preset_name)) },
                    supportingText = {
                        Text(stringResource(R.string.settings_equalizer_custom_preset_name_hint))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = canSaveCustomPreset,
                        onClick = {
                            val name = customPresetName.trim()
                            if (name.isEmpty()) return@OutlinedButton
                            draftCustomPresets = draftCustomPresets + (name to draftLevels)
                            selectedPreset = EqualizerPreset.CUSTOM
                            selectedCustomPresetName = name
                            customPresetName = name
                        }
                    ) {
                        Text(stringResource(R.string.settings_equalizer_save_custom_preset))
                    }
                    OutlinedButton(
                        enabled = canDeleteCustomPreset,
                        onClick = {
                            val name = selectedCustomPresetName ?: return@OutlinedButton
                            draftCustomPresets = draftCustomPresets - name
                            selectedCustomPresetName = null
                            if (customPresetName == name) customPresetName = ""
                        }
                    ) {
                        Text(stringResource(R.string.settings_equalizer_delete_selected_custom_preset))
                    }
                }

                TextButton(
                    onClick = {
                        selectedPreset = EqualizerPreset.NORMAL
                        selectedCustomPresetName = null
                        draftLevels = EqualizerReferencePresets.levelsFor(EqualizerPreset.NORMAL)
                    }
                ) {
                    Text(stringResource(R.string.settings_equalizer_reset))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val commitPlan = buildEqualizerCommitPlan(
                        originalCustomPresets = settings.customEqualizerPresets,
                        draftCustomPresets = draftCustomPresets,
                        selectedPreset = selectedPreset,
                        selectedCustomPresetName = selectedCustomPresetName,
                        draftLevels = draftLevels
                    )

                    commitPlan.deletedPresetNames.forEach { name ->
                        onDeleteCustomPreset(name)
                    }
                    commitPlan.upsertPresets.forEach { (name, levels) ->
                        onSaveCustomPreset(name, levels)
                    }

                    when (val action = commitPlan.applyAction) {
                        is EqualizerApplyAction.BuiltInPreset -> onApplyPreset(action.preset)
                        is EqualizerApplyAction.CustomPreset -> onApplyCustomPreset(action.name)
                        is EqualizerApplyAction.CustomLevels -> onSaveLevels(action.levels)
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun EqualizerSliders(
    levels: EqualizerBandLevels,
    onLevelsChanged: (EqualizerBandLevels) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EqualizerBandSlider(
            label = stringResource(R.string.settings_equalizer_low_mid_label),
            value = levels.lowMid,
            onValueChange = { onLevelsChanged(levels.copy(lowMid = it)) }
        )
        EqualizerBandSlider(
            label = stringResource(R.string.settings_equalizer_bass_label),
            value = levels.bass,
            onValueChange = { onLevelsChanged(levels.copy(bass = it)) }
        )
        EqualizerBandSlider(
            label = stringResource(R.string.settings_equalizer_mid_label),
            value = levels.mid,
            onValueChange = { onLevelsChanged(levels.copy(mid = it)) }
        )
        EqualizerBandSlider(
            label = stringResource(R.string.settings_equalizer_high_mid_label),
            value = levels.highMid,
            onValueChange = { onLevelsChanged(levels.copy(highMid = it)) }
        )
        EqualizerBandSlider(
            label = stringResource(R.string.settings_equalizer_treble_label),
            value = levels.treble,
            onValueChange = { onLevelsChanged(levels.copy(treble = it)) }
        )
    }
}

@Composable
private fun EqualizerBandSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.settings_equalizer_band_value, label, value),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(-10, 10)) },
            valueRange = -10f..10f,
            steps = 19
        )
    }
}

@Composable
private fun PresetDropdown(
    label: String,
    selectedLabel: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedLabel)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_equalizer_no_custom_presets)) },
                        onClick = { expanded = false }
                    )
                } else {
                    options.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onOptionSelected(index)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun equalizerPresetLabel(preset: EqualizerPreset): String {
    val labelRes = when (preset) {
        EqualizerPreset.NORMAL -> R.string.settings_equalizer_preset_normal
        EqualizerPreset.CLASSICAL -> R.string.settings_equalizer_preset_classical
        EqualizerPreset.POP -> R.string.settings_equalizer_preset_pop
        EqualizerPreset.ROCK -> R.string.settings_equalizer_preset_rock
        EqualizerPreset.JAZZ -> R.string.settings_equalizer_preset_jazz
        EqualizerPreset.BASS_BOOST -> R.string.settings_equalizer_preset_bass_boost
        EqualizerPreset.VOCAL -> R.string.settings_equalizer_preset_vocal
        EqualizerPreset.TREBLE_BOOST -> R.string.settings_equalizer_preset_treble_boost
        EqualizerPreset.ACOUSTIC -> R.string.settings_equalizer_preset_acoustic
        EqualizerPreset.ELECTRONIC -> R.string.settings_equalizer_preset_electronic
        EqualizerPreset.CUSTOM -> R.string.settings_equalizer_preset_custom
    }
    return stringResource(labelRes)
}
