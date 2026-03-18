package com.example.musicplayer.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.data.settings.AppSettings
import com.example.musicplayer.data.settings.StreamingQuality
import com.example.musicplayer.data.settings.ThemeMode

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showClearRecentsDialog by remember { mutableStateOf(false) }
    var showClearFavoritesDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ThemeCard(settings = settings, onThemeSelected = viewModel::updateThemeMode)
        AutoPlayCard(
            settings = settings,
            onAutoPlayChanged = viewModel::updateAutoPlayEnabled
        )
        StreamingQualityCard(
            settings = settings,
            onStreamingQualitySelected = viewModel::updateStreamingQuality
        )
        AutoRescanCard(settings = settings, onAutoRescanChanged = viewModel::updateAutoRescan)
        EqualizerCard(
            settings = settings,
            onOpenDialog = { showEqualizerDialog = true }
        )
        DataManagementCard(
            onClearRecents = { showClearRecentsDialog = true },
            onClearFavorites = { showClearFavoritesDialog = true }
        )
        AboutCard()
    }

    if (showClearRecentsDialog) {
        ClearRecentsDialog(
            onDismiss = { showClearRecentsDialog = false },
            onConfirm = {
                viewModel.clearRecents()
                showClearRecentsDialog = false
            }
        )
    }

    if (showClearFavoritesDialog) {
        ClearFavoritesDialog(
            onDismiss = { showClearFavoritesDialog = false },
            onConfirm = {
                viewModel.clearFavorites()
                showClearFavoritesDialog = false
            }
        )
    }

    if (showEqualizerDialog) {
        EqualizerDialog(
            settings = settings,
            onDismiss = { showEqualizerDialog = false },
            onApplyPreset = { preset -> viewModel.updateEqualizerPreset(preset) },
            onSaveLevels = { levels -> viewModel.updateEqualizerLevels(levels) },
            onSaveCustomPreset = { name, levels -> viewModel.saveCustomEqualizerPreset(name, levels) },
            onApplyCustomPreset = { name -> viewModel.applyCustomEqualizerPreset(name) },
            onDeleteCustomPreset = { name -> viewModel.deleteCustomEqualizerPreset(name) }
        )
    }
}

@Composable
private fun AutoPlayCard(
    settings: AppSettings,
    onAutoPlayChanged: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ToggleSettingRow(
            title = stringResource(R.string.settings_auto_play_title),
            description = stringResource(R.string.settings_auto_play_description),
            checked = settings.autoPlayEnabled,
            onCheckedChange = onAutoPlayChanged
        )
    }
}

@Composable
private fun StreamingQualityCard(
    settings: AppSettings,
    onStreamingQualitySelected: (StreamingQuality) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_streaming_quality_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.settings_streaming_quality_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            QualityOptionRow(
                label = stringResource(R.string.settings_streaming_quality_data_saver),
                selected = settings.streamingQuality == StreamingQuality.DATA_SAVER,
                onClick = { onStreamingQualitySelected(StreamingQuality.DATA_SAVER) }
            )
            QualityOptionRow(
                label = stringResource(R.string.settings_streaming_quality_balanced),
                selected = settings.streamingQuality == StreamingQuality.BALANCED,
                onClick = { onStreamingQualitySelected(StreamingQuality.BALANCED) }
            )
            QualityOptionRow(
                label = stringResource(R.string.settings_streaming_quality_high),
                selected = settings.streamingQuality == StreamingQuality.HIGH,
                onClick = { onStreamingQualitySelected(StreamingQuality.HIGH) }
            )
        }
    }
}

@Composable
private fun ThemeCard(
    settings: AppSettings,
    onThemeSelected: (ThemeMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.settings_theme_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ThemeOptionRow(
                label = stringResource(R.string.settings_theme_system),
                selected = settings.themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeSelected(ThemeMode.SYSTEM) }
            )
            ThemeOptionRow(
                label = stringResource(R.string.settings_theme_light),
                selected = settings.themeMode == ThemeMode.LIGHT,
                onClick = { onThemeSelected(ThemeMode.LIGHT) }
            )
            ThemeOptionRow(
                label = stringResource(R.string.settings_theme_dark),
                selected = settings.themeMode == ThemeMode.DARK,
                onClick = { onThemeSelected(ThemeMode.DARK) }
            )
        }
    }
}

@Composable
private fun AutoRescanCard(
    settings: AppSettings,
    onAutoRescanChanged: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ToggleSettingRow(
            title = stringResource(R.string.settings_auto_rescan_title),
            description = stringResource(R.string.settings_auto_rescan_description),
            checked = settings.autoRescanOnOpen,
            onCheckedChange = onAutoRescanChanged
        )
    }
}

@Composable
private fun EqualizerCard(
    settings: AppSettings,
    onOpenDialog: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_equalizer_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.settings_equalizer_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.settings_equalizer_summary,
                    equalizerPresetLabel(settings.equalizerPreset),
                    settings.equalizerLevels.lowMid,
                    settings.equalizerLevels.bass,
                    settings.equalizerLevels.mid,
                    settings.equalizerLevels.highMid,
                    settings.equalizerLevels.treble
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onOpenDialog, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_equalizer_open))
            }
        }
    }
}

@Composable
private fun DataManagementCard(
    onClearRecents: () -> Unit,
    onClearFavorites: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_data_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.settings_data_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onClearRecents, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_clear_recents))
            }
            OutlinedButton(onClick = onClearFavorites, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_clear_favorites))
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClearRecentsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_clear_recents)) },
        text = { Text(stringResource(R.string.recents_clear_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
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
private fun ClearFavoritesDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_clear_favorites)) },
        text = { Text(stringResource(R.string.settings_clear_favorites_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
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
private fun ToggleSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
private fun QualityOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}
