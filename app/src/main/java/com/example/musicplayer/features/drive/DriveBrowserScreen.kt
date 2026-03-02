package com.example.musicplayer.features.drive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.core.model.Track

@Composable
fun DriveBrowserScreen(
    modifier: Modifier = Modifier,
    viewModel: DriveViewModel,
    onPlayTracks: (tracks: List<Track>, startIndex: Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Public Google Drive Folder (MP3)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.folderUrl,
            onValueChange = viewModel::updateFolderUrl,
            label = { Text("Folder URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.loadFolder() }) {
                Text("Load MP3 List")
            }
        }

        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
            }

            state.error != null -> {
                Text(
                    text = "Error: ${state.error}",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            state.nodes.isEmpty() -> {
                Text(
                    text = "No MP3 found. Ensure folder is public and contains .mp3 files.",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.nodes) { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val tracks = state.nodes.mapNotNull { it.track }
                                    val index = tracks.indexOfFirst { it.id == node.track?.id }
                                    if (index >= 0) onPlayTracks(tracks, index)
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🎵 ${node.name}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Tap to play", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
