package com.example.musicplayer.features.drive

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.offline.model.OfflineSourceStatus
import com.example.musicplayer.data.offline.model.OfflineTrackStatus
import com.example.musicplayer.data.offline.model.SourceOfflineSummary
import com.example.musicplayer.ui.components.FeedbackStateCard
import com.example.musicplayer.ui.components.PlaylistSelectorBar
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.components.TrackListSkeleton

@Composable
fun DriveBrowserScreen(
    modifier: Modifier = Modifier,
    viewModel: DriveViewModel,
    onPlayTracks: (tracks: List<Track>, startIndex: Int) -> Unit,
    activePlaylistName: String,
    onChangePlaylist: () -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearMessage()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.switchPage(DrivePage.LOAD) },
                enabled = state.page != DrivePage.LOAD
            ) { Text(stringResource(R.string.drive_mode_load)) }
            Button(
                onClick = { viewModel.switchPage(DrivePage.SAVED) },
                enabled = state.page != DrivePage.SAVED
            ) { Text(stringResource(R.string.drive_mode_saved)) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (state.page) {
            DrivePage.LOAD -> LoadPage(state = state, viewModel = viewModel)
            DrivePage.SAVED -> SavedPage(
                state = state,
                viewModel = viewModel,
                onPlayTracks = onPlayTracks,
                activePlaylistName = activePlaylistName,
                onChangePlaylist = onChangePlaylist,
                onAddToPlaylist = onAddToPlaylist
            )
        }
    }
}

@Composable
private fun LoadPage(state: DriveUiState, viewModel: DriveViewModel) {
    Text(
        stringResource(R.string.drive_public_url_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp)
    )
    OutlinedTextField(
        value = state.sourceTitleInput,
        onValueChange = viewModel::updateSourceTitle,
        label = { Text(stringResource(R.string.drive_source_title_label)) },
        placeholder = { Text(stringResource(R.string.drive_source_title_placeholder)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
    OutlinedTextField(
        value = state.folderUrl,
        onValueChange = viewModel::updateFolderUrl,
        label = { Text(stringResource(R.string.drive_url_label)) },
        placeholder = { Text(stringResource(R.string.drive_url_placeholder)) },
        supportingText = {
            Text(stringResource(R.string.drive_url_supporting))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
    Row(
        modifier = Modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = viewModel::addSource, enabled = !state.isLoading) {
            Text(stringResource(R.string.drive_save_source))
        }
    }
    state.error?.let {
        FeedbackStateCard(
            title = stringResource(R.string.error_prefix, it),
            isError = true,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun SavedPage(
    state: DriveUiState,
    viewModel: DriveViewModel,
    onPlayTracks: (tracks: List<Track>, startIndex: Int) -> Unit,
    activePlaylistName: String,
    onChangePlaylist: () -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    var sourceToDelete by remember { mutableStateOf<Long?>(null) }

    PlaylistSelectorBar(
        activePlaylistName = activePlaylistName,
        onChangePlaylist = onChangePlaylist,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        label = stringResource(R.string.label_active_playlist),
        buttonLabel = stringResource(R.string.action_change)
    )

    if (state.sources.isEmpty()) {
        FeedbackStateCard(title = stringResource(R.string.drive_no_saved_sources))
        return
    }

    Text(
        text = stringResource(R.string.drive_saved_sources),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .padding(bottom = 8.dp),
        contentPadding = PaddingValues(bottom = 4.dp)
    ) {
        items(state.sources, key = { it.id }) { source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.selectSource(source.id)
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.title,
                        fontWeight = if (source.id == state.selectedSourceId) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = source.folderUrl,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (source.id == state.selectedSourceId) {
                        SourceStatusRow(state)
                    }
                }
                Button(onClick = { sourceToDelete = source.id }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
    HorizontalDivider()

    Row(
        modifier = Modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { viewModel.loadSelectedSource() }, enabled = !state.isLoading) {
            Text(if (state.isLoading) stringResource(R.string.action_loading) else stringResource(R.string.action_load_tracks))
        }
        Button(onClick = viewModel::downloadSelectedSource, enabled = !state.isLoading) {
            Text(stringResource(R.string.action_download))
        }
        if (state.error != null || state.hasLoaded) {
            Button(onClick = { viewModel.loadSelectedSource(force = true) }, enabled = !state.isLoading) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }

    when {
        state.isLoading -> TrackListSkeleton(modifier = Modifier.padding(top = 16.dp))
        state.error != null -> FeedbackStateCard(
            title = stringResource(R.string.error_prefix, state.error.orEmpty()),
            isError = true,
            modifier = Modifier.padding(top = 12.dp)
        )
        state.hasLoaded && state.nodes.isEmpty() -> FeedbackStateCard(
            title = stringResource(R.string.drive_no_playable_audio),
            modifier = Modifier.padding(top = 12.dp)
        )
        !state.hasLoaded -> FeedbackStateCard(
            title = stringResource(R.string.drive_select_source_prompt),
            modifier = Modifier.padding(top = 12.dp)
        )
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                items(state.nodes) { node ->
                    val track = node.track
                    var menuExpanded by remember { mutableStateOf(false) }
                    val status = track?.let(viewModel::trackOfflineStatus) ?: OfflineTrackStatus.NOT_DOWNLOADED
                    TrackListItem(
                        title = node.name,
                        subtitle = trackStatusText(status),
                        artworkUri = track?.artworkUri,
                        onClick = {
                            val tracks = state.nodes.mapNotNull { it.track }
                            val index = tracks.indexOfFirst { it.id == node.track?.id }
                            if (index >= 0) onPlayTracks(tracks, index)
                        },
                        onMoreClick = if (track != null) ({ menuExpanded = true }) else null,
                        moreContentDescription = if (track != null) stringResource(R.string.action_track_actions_for, node.name) else null,
                        trailingLabel = if (status == OfflineTrackStatus.DOWNLOADED) "✓" else null
                    )
                    if (track != null) {
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                onClick = {
                                    menuExpanded = false
                                    onAddToPlaylist(track)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (sourceToDelete != null) {
        AlertDialog(
            onDismissRequest = { sourceToDelete = null },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.drive_remove_source_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSource(sourceToDelete!!)
                        sourceToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { sourceToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SourceStatusRow(state: DriveUiState) {
    val summary = state.sourceOfflineSummary ?: return
    val icon = when (summary.status) {
        OfflineSourceStatus.COMPLETED -> Icons.Default.CheckCircle
        else -> Icons.Default.Sync
    }
    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null)
        Text(
            text = stringResource(
                R.string.offline_summary_format,
                sourceStatusLabel(summary),
                summary.downloadedTracks,
                summary.totalTracks,
                summary.progressPercent
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun sourceStatusLabel(summary: SourceOfflineSummary): String {
    return when (summary.status) {
        OfflineSourceStatus.NOT_STARTED -> stringResource(R.string.drive_status_none)
        OfflineSourceStatus.QUEUED -> stringResource(R.string.drive_status_queued)
        OfflineSourceStatus.DOWNLOADING -> stringResource(R.string.drive_status_downloading)
        OfflineSourceStatus.COMPLETED -> stringResource(R.string.drive_status_completed)
        OfflineSourceStatus.PARTIAL -> stringResource(R.string.drive_status_partial)
        OfflineSourceStatus.FAILED -> stringResource(R.string.drive_status_failed)
    }
}

@Composable
private fun trackStatusText(status: OfflineTrackStatus): String {
    return when (status) {
        OfflineTrackStatus.DOWNLOADED -> stringResource(R.string.offline_status_downloaded)
        OfflineTrackStatus.DOWNLOADING -> stringResource(R.string.offline_status_downloading)
        OfflineTrackStatus.QUEUED -> stringResource(R.string.offline_status_queued)
        OfflineTrackStatus.FAILED -> stringResource(R.string.offline_status_failed)
        OfflineTrackStatus.NOT_DOWNLOADED -> stringResource(R.string.offline_status_remote)
    }
}
