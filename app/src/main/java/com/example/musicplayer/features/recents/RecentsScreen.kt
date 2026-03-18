package com.example.musicplayer.features.recents

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.components.FeedbackStateCard
import com.example.musicplayer.ui.components.TrackListItem

@Composable
fun RecentsScreen(
    modifier: Modifier = Modifier,
    viewModel: RecentsViewModel,
    playbackController: PlaybackController,
    autoPlayEnabled: Boolean,
    onOpenNowPlaying: () -> Unit
) {
    val tracks by viewModel.recentTracks.collectAsStateWithLifecycle(initialValue = emptyList())
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Button(
                onClick = { showClearDialog = true },
                enabled = tracks.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = stringResource(R.string.recents_clear))
            }
        }

        if (tracks.isEmpty()) {
            item {
                FeedbackStateCard(
                    title = stringResource(R.string.recents_empty_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            return@LazyColumn
        }

        itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
            TrackListItem(
                title = track.title,
                subtitle = stringResource(
                    R.string.track_subtitle_artist_album,
                    track.artist,
                    track.album
                ),
                duration = formatDuration(track.durationMs),
                artworkUri = track.artworkUri,
                onClick = {
                    playbackController.setQueue(tracks, index, playWhenReady = autoPlayEnabled)
                    onOpenNowPlaying()
                }
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            text = { Text(stringResource(R.string.recents_clear_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearRecents()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.recents_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
