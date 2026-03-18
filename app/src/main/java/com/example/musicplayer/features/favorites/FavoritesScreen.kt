package com.example.musicplayer.features.favorites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.components.FeedbackStateCard
import com.example.musicplayer.ui.components.TrackListItem

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel,
    playbackController: PlaybackController,
    autoPlayEnabled: Boolean,
    onOpenNowPlaying: () -> Unit
) {
    val tracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()

    if (tracks.isEmpty()) {
        FeedbackStateCard(
            title = stringResource(R.string.favorites_empty),
            modifier = modifier.padding(16.dp)
        )
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Text(
                text = stringResource(R.string.favorites_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackListItem(
                title = track.title,
                subtitle = stringResource(R.string.track_subtitle_artist_album, track.artist, track.album),
                duration = formatDuration(track.durationMs),
                artworkUri = track.artworkUri,
                onClick = {
                    playbackController.setQueue(tracks, index, playWhenReady = autoPlayEnabled)
                    onOpenNowPlaying()
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
