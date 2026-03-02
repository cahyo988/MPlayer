package com.example.musicplayer.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.core.model.RepeatMode
import com.example.musicplayer.playback.PlaybackController
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    playbackController: PlaybackController,
    modifier: Modifier = Modifier
) {
    val state by playbackController.state.collectAsStateWithLifecycle()

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

    val duration = state.durationMs.coerceAtLeast(1L)
    val position = state.positionMs.coerceIn(0L, duration)
    var sliderPosition by remember(position, duration) { mutableFloatStateOf(position.toFloat()) }
    var isUserSeeking by remember { mutableStateOf(false) }

    if (!isUserSeeking) {
        sliderPosition = position.toFloat().coerceAtMost(duration.toFloat())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Now Playing", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = state.currentTrack?.title ?: "No track selected")
        Text(text = state.currentTrack?.artist ?: "")
        Spacer(modifier = Modifier.height(20.dp))

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
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = "${position / 1000}s / ${duration / 1000}s")

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { playbackController.previous() }) {
                Text("Prev")
            }
            Button(onClick = {
                if (state.isPlaying) playbackController.pause() else playbackController.play()
            }) {
                Text(if (state.isPlaying) "Pause" else "Play")
            }
            Button(onClick = { playbackController.next() }) {
                Text("Next")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { playbackController.cycleRepeatMode() }) {
                Text(
                    when (state.repeatMode) {
                        RepeatMode.OFF -> "Repeat: Off"
                        RepeatMode.ALL -> "Repeat: All"
                        RepeatMode.ONE -> "Repeat: One"
                    }
                )
            }
            Button(onClick = { playbackController.toggleShuffle() }) {
                Text(if (state.shuffleEnabled) "Shuffle: On" else "Shuffle: Off")
            }
        }
    }
}
