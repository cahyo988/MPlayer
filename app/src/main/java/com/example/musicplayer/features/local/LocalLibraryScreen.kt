package com.example.musicplayer.features.local

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.playback.PlaybackController

@Composable
fun LocalLibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LocalLibraryViewModel,
    playbackController: PlaybackController,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onAddToPlaylist: (Track) -> Unit
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
            Text("Audio permission is required to scan local music.")
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Grant Permission")
            }
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
                CircularProgressIndicator()
                Text("Scanning local library...", modifier = Modifier.padding(top = 12.dp))
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
                Text("Error: ${state.error}")
                Button(onClick = { viewModel.refresh() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Retry")
                }
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
                Text("No local music found.")
                Button(onClick = { viewModel.refresh() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Rescan")
                }
            }
        }

        else -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                itemsIndexed(state.tracks) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playbackController.setQueue(state.tracks, startIndex = index, playWhenReady = true)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${track.artist} • ${track.album}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(onClick = { onAddToPlaylist(track) }) {
                            Text("Add")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = formatDuration(track.durationMs))
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
