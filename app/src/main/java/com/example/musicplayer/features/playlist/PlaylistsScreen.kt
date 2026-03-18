package com.example.musicplayer.features.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.components.FeedbackStateCard
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.components.TrackListSkeleton
import com.example.musicplayer.ui.search.filterTracks

@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel,
    playbackController: PlaybackController,
    autoPlayEnabled: Boolean,
    onOpenNowPlaying: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var trackToRemoveId by remember { mutableStateOf<String?>(null) }
    var trackActionMenuForId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember(state.selectedPlaylistId) { mutableStateOf("") }
    val selectedSummary = state.playlists.firstOrNull { it.id == state.selectedPlaylistId }
    val canDeleteSelected = selectedSummary?.isSystemDefault == false
    val canRenameSelected = selectedSummary?.isSystemDefault == false
    val normalizedRenameInput = renameInput.trim()
    val canSaveRename = canRenameSelected && normalizedRenameInput.isNotEmpty() && normalizedRenameInput != selectedSummary?.name

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(onClick = { viewModel.createPlaylist() }) {
                Text(stringResource(R.string.playlist_new))
            }
            Button(
                onClick = {
                    renameInput = selectedSummary?.name.orEmpty()
                    showRenameDialog = true
                },
                enabled = canRenameSelected
            ) {
                Text(stringResource(R.string.playlist_rename))
            }
            Button(onClick = { showDeletePlaylistDialog = true }, enabled = canDeleteSelected) {
                Text(stringResource(R.string.playlist_delete))
            }
        }

        when {
            state.isLoading -> {
                TrackListSkeleton(modifier = Modifier.padding(top = 8.dp))
                return
            }

            state.error != null -> {
                FeedbackStateCard(
                    title = stringResource(R.string.error_prefix, state.error.orEmpty()),
                    isError = true,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                return
            }
        }

        if (state.playlists.isEmpty()) {
            FeedbackStateCard(
                title = stringResource(R.string.playlist_select_prompt),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            return
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.playlists, key = { it.id }) { playlist ->
                FilterChip(
                    selected = playlist.id == state.selectedPlaylistId,
                    onClick = { viewModel.selectPlaylist(playlist.id) },
                    label = { Text(playlist.name) }
                )
            }
        }

        val detail = state.selectedPlaylist
        if (detail == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.playlist_select_prompt))
            }
        } else {
            val filteredTracks = remember(detail.tracks, searchQuery) {
                filterTracks(detail.tracks, searchQuery)
            }
            val indexById = remember(detail.tracks) {
                detail.tracks.mapIndexed { originalIndex, item ->
                    item.id to originalIndex
                }.toMap()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = detail.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.playlist_track_count,
                            detail.tracks.size,
                            detail.tracks.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.playlist_search_label)) },
                        placeholder = { Text(stringResource(R.string.playlist_search_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            if (detail.tracks.isEmpty()) {
                FeedbackStateCard(
                    title = stringResource(R.string.playlist_empty),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            } else if (filteredTracks.isEmpty()) {
                FeedbackStateCard(
                    title = stringResource(R.string.search_no_results),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(filteredTracks, key = { _, track -> track.id }) { index, track ->
                        val isMenuExpanded = trackActionMenuForId == track.id
                        val originalIndex = indexById[track.id] ?: -1
                        TrackListItem(
                            title = track.title,
                            subtitle = "${track.artist} • ${track.album}",
                            duration = formatDuration(track.durationMs),
                            artworkUri = track.artworkUri,
                            onClick = {
                                if (originalIndex >= 0) {
                                    playbackController.setQueue(
                                        detail.tracks,
                                        originalIndex,
                                        autoPlayEnabled
                                    )
                                } else {
                                    playbackController.setQueue(filteredTracks, index, autoPlayEnabled)
                                }
                                onOpenNowPlaying()
                            },
                            trailingLabel = null,
                            moreMenuExpanded = isMenuExpanded,
                            onMoreMenuExpandedChange = { expanded ->
                                trackActionMenuForId = if (expanded) track.id else null
                            },
                            moreMenuContent = {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.playlist_move_up)) },
                                    onClick = {
                                        trackActionMenuForId = null
                                        viewModel.moveTrackUp(track.id)
                                    },
                                    enabled = originalIndex > 0
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.playlist_move_down)) },
                                    onClick = {
                                        trackActionMenuForId = null
                                        viewModel.moveTrackDown(track.id)
                                    },
                                    enabled = originalIndex in 0 until detail.tracks.lastIndex
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.playlist_remove_track)) },
                                    onClick = {
                                        trackActionMenuForId = null
                                        trackToRemoveId = track.id
                                    }
                                )
                            },
                            moreContentDescription = stringResource(R.string.playlist_track_actions)
                        )
                    }
                }
            }
        }

        if (showDeletePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showDeletePlaylistDialog = false },
                title = { Text(stringResource(R.string.playlist_delete)) },
                text = { Text(stringResource(R.string.playlist_delete_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSelectedPlaylist()
                        showDeletePlaylistDialog = false
                    }) {
                        Text(stringResource(R.string.playlist_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePlaylistDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text(stringResource(R.string.playlist_rename)) },
                text = {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text(stringResource(R.string.playlist_rename_label)) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = canSaveRename,
                        onClick = {
                            viewModel.renameSelectedPlaylist(normalizedRenameInput)
                            showRenameDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (trackToRemoveId != null) {
            AlertDialog(
                onDismissRequest = { trackToRemoveId = null },
                title = { Text(stringResource(R.string.playlist_remove_track)) },
                text = { Text(stringResource(R.string.playlist_remove_track_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeTrackFromSelectedPlaylist(trackToRemoveId!!)
                        trackToRemoveId = null
                    }) {
                        Text(stringResource(R.string.playlist_remove_track))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { trackToRemoveId = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
