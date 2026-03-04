package com.example.musicplayer.ui.nowplaying

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.core.model.RepeatMode
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.components.FeedbackStateCard
import com.example.musicplayer.ui.components.TrackListItem
import com.example.musicplayer.ui.components.TrackArtwork
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    playbackController: PlaybackController,
    modifier: Modifier = Modifier
) {
    val state by playbackController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(playbackController) {
        playbackController.connect()
        onDispose { }
    }

    LaunchedEffect(Unit) {
        while (true) {
            playbackController.syncProgress()
            delay(500)
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        playbackController.clearErrorMessage()
    }

    val duration = state.durationMs.coerceAtLeast(1L)
    val position = state.positionMs.coerceIn(0L, duration)
    var sliderPosition by remember(position, duration) { mutableFloatStateOf(position.toFloat()) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(state.queue, searchQuery) {
        searchQueue(state.queue, searchQuery)
    }

    if (!isUserSeeking) {
        sliderPosition = position.toFloat().coerceAtMost(duration.toFloat())
    }

    val hasTrack = state.currentTrack != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrackArtwork(
            artworkUri = state.currentTrack?.artworkUri,
            sizeDp = 200
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = state.currentTrack?.title ?: stringResource(R.string.now_playing_no_track),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = state.currentTrack?.artist.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = sliderPosition,
            onValueChange = {
                isUserSeeking = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                playbackController.seekTo(sliderPosition.toLong())
                isUserSeeking = false
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            enabled = hasTrack
        )
        Text(
            text = stringResource(
                R.string.playback_time_format,
                formatPlaybackTime(position),
                formatPlaybackTime(duration)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { playbackController.previous() }, enabled = hasTrack) {
                Text(stringResource(R.string.control_previous))
            }
            Button(onClick = {
                if (state.isPlaying) playbackController.pause() else playbackController.play()
            }, enabled = hasTrack) {
                Text(if (state.isPlaying) stringResource(R.string.control_pause) else stringResource(R.string.control_play))
            }
            Button(onClick = { playbackController.next() }, enabled = hasTrack) {
                Text(stringResource(R.string.control_next))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { playbackController.cycleRepeatMode() }, enabled = hasTrack) {
                Text(
                    when (state.repeatMode) {
                        RepeatMode.OFF -> stringResource(R.string.control_repeat_off)
                        RepeatMode.ALL -> stringResource(R.string.control_repeat_all)
                        RepeatMode.ONE -> stringResource(R.string.control_repeat_one)
                    }
                )
            }
            Button(onClick = { playbackController.toggleShuffle() }, enabled = hasTrack) {
                Text(if (state.shuffleEnabled) stringResource(R.string.control_shuffle_on) else stringResource(R.string.control_shuffle_off))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.now_playing_search_label)) },
                    placeholder = { Text(stringResource(R.string.now_playing_search_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (searchQuery.isNotBlank()) {
                    if (searchResults.isEmpty()) {
                        FeedbackStateCard(
                            title = stringResource(R.string.now_playing_no_results),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(180.dp)
                        ) {
                            items(searchResults, key = { it.track.id + ":" + it.index }) { result ->
                                TrackListItem(
                                    title = result.track.title,
                                    subtitle = "${result.track.artist} • ${result.track.album}",
                                    artworkUri = result.track.artworkUri,
                                    onClick = {
                                        playbackController.jumpToQueueIndex(result.index, playWhenReady = true)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!hasTrack) {
            Spacer(modifier = Modifier.height(12.dp))
            FeedbackStateCard(title = stringResource(R.string.now_playing_no_track))
        }
    }
}

private fun formatPlaybackTime(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
