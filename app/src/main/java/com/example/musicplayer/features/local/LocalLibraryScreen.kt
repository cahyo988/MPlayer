package com.example.musicplayer.features.local

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.components.FeedbackStateCard
import com.example.musicplayer.ui.components.PlaylistSelectorBar
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.components.TrackListSkeleton

@Composable
fun LocalLibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LocalLibraryViewModel,
    playbackController: PlaybackController,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    activePlaylistName: String,
    onChangePlaylist: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.refresh()
        }
    }

    if (!hasAudioPermission) {
        Column(
            modifier = Modifier
                .then(modifier)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            FeedbackStateCard(
                title = stringResource(R.string.permission_audio_required),
                actionLabel = stringResource(R.string.action_grant_permission),
                onAction = onRequestPermission
            )
        }
        return
    }

    when {
        state.isLoading -> {
            Column(
                modifier = Modifier
                    .then(modifier)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.loading_scanning_local_library),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TrackListSkeleton()
            }
        }

        state.error != null -> {
            Column(
                modifier = Modifier
                    .then(modifier)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                FeedbackStateCard(
                    title = stringResource(R.string.error_prefix, state.error.orEmpty()),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = { viewModel.refresh() },
                    isError = true
                )
            }
        }

        state.tracks.isEmpty() -> {
            Column(
                modifier = Modifier
                    .then(modifier)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                FeedbackStateCard(
                    title = stringResource(R.string.empty_local_library),
                    actionLabel = stringResource(R.string.action_rescan),
                    onAction = { viewModel.refresh() }
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    PlaylistSelectorBar(
                        activePlaylistName = activePlaylistName,
                        onChangePlaylist = onChangePlaylist,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        label = stringResource(R.string.label_active_playlist),
                        buttonLabel = stringResource(R.string.action_change)
                    )
                }
                itemsIndexed(state.tracks) { index, track ->
                    var menuExpanded by remember { mutableStateOf(false) }
                    TrackListItem(
                        title = track.title,
                        subtitle = "${track.artist} • ${track.album}",
                        duration = formatDuration(track.durationMs),
                        artworkUri = track.artworkUri,
                        onClick = {
                            playbackController.setQueue(state.tracks, startIndex = index, playWhenReady = true)
                            onOpenNowPlaying()
                        },
                        onMoreClick = { menuExpanded = true },
                        moreContentDescription = stringResource(R.string.action_track_actions_for, track.title)
                    )
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

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
