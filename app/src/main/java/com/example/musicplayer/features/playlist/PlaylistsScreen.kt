package com.example.musicplayer.features.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.components.FeedbackStateCard
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.components.TrackListSkeleton

@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel,
    playbackController: PlaybackController,
    onOpenNowPlaying: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var trackToRemoveId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.createPlaylist() }) {
                Text(stringResource(R.string.playlist_new))
            }
            Button(onClick = { showDeletePlaylistDialog = true }, enabled = state.selectedPlaylistId != null) {
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

        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
            ) {
                items(state.playlists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectPlaylist(playlist.id) }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = playlist.name,
                            fontWeight = if (playlist.id == state.selectedPlaylistId) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

            Column(modifier = Modifier.weight(0.58f)) {
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
                    Text(
                        text = detail.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (detail.tracks.isEmpty()) {
                        FeedbackStateCard(
                            title = stringResource(R.string.playlist_empty),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(detail.tracks) { track ->
                                TrackListItem(
                                    title = track.title,
                                    subtitle = "${track.artist} • ${track.album}",
                                    duration = formatDuration(track.durationMs),
                                    artworkUri = track.artworkUri,
                                    onClick = {
                                        playbackController.setQueue(detail.tracks, detail.tracks.indexOf(track), true)
                                        onOpenNowPlaying()
                                    },
                                    trailingLabel = stringResource(R.string.playlist_remove_track),
                                    onMoreClick = { trackToRemoveId = track.id },
                                    moreContentDescription = stringResource(R.string.playlist_remove_track)
                                )
                            }
                        }
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
