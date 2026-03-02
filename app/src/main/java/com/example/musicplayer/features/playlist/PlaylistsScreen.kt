package com.example.musicplayer.features.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.playback.PlaybackController

@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel,
    playbackController: PlaybackController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.createPlaylist() }) {
                Text("New Playlist")
            }
            Button(onClick = { viewModel.deleteSelectedPlaylist() }, enabled = state.selectedPlaylistId != null) {
                Text("Delete")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
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

        HorizontalDivider()

        val detail = state.selectedPlaylist
        if (detail == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select a playlist to view tracks")
            }
            return
        }

        Text(
            text = detail.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            items(detail.tracks) { track ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            playbackController.setQueue(detail.tracks, detail.tracks.indexOf(track), true)
                        }
                        .padding(12.dp)
                ) {
                    Text(track.title, fontWeight = FontWeight.SemiBold)
                    Text("${track.artist} • ${track.album}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = { viewModel.removeTrackFromSelectedPlaylist(track.id) }) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}
