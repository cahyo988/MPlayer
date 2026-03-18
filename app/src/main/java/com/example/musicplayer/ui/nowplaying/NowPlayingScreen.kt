package com.example.musicplayer.ui.nowplaying

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.core.model.RepeatMode
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.components.FeedbackStateCard
import com.example.musicplayer.ui.components.TrackArtwork
import com.example.musicplayer.ui.components.TrackListItem

@Composable
fun NowPlayingScreen(
    playbackController: PlaybackController,
    autoPlayEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val state by playbackController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = state.currentTrack?.artworkUri,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "now_playing_artwork_transition"
                    ) { artworkUri ->
                        TrackArtwork(
                            artworkUri = artworkUri,
                            sizeDp = 250
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                AnimatedContent(
                    targetState = state.currentTrack?.id,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "now_playing_track_text_transition"
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.currentTrack?.title ?: stringResource(R.string.now_playing_no_track),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.currentTrack?.artist.orEmpty().ifBlank { "-" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

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
            modifier = Modifier.fillMaxWidth(),
            enabled = hasTrack
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlaybackTime(position),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatPlaybackTime(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val repeatActive = state.repeatMode != RepeatMode.OFF
            CircularControl(
                active = repeatActive,
                onClick = { playbackController.cycleRepeatMode() },
                enabled = hasTrack
            ) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = when (state.repeatMode) {
                        RepeatMode.OFF -> stringResource(R.string.control_repeat_off)
                        RepeatMode.ALL -> stringResource(R.string.control_repeat_all)
                        RepeatMode.ONE -> stringResource(R.string.control_repeat_one)
                    },
                    tint = if (repeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playbackController.previous() }, enabled = hasTrack) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.control_previous))
                }

                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val playInteraction = remember { MutableInteractionSource() }
                    val playPressed by playInteraction.collectIsPressedAsState()
                    val playScale by animateFloatAsState(
                        targetValue = if (playPressed) 0.92f else 1f,
                        animationSpec = tween(durationMillis = 120),
                        label = "now_playing_play_button_scale"
                    )
                    IconButton(
                        onClick = {
                            if (state.isPlaying) playbackController.pause() else playbackController.play()
                        },
                        enabled = hasTrack,
                        interactionSource = playInteraction,
                        modifier = Modifier.scale(playScale)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) {
                                stringResource(R.string.control_pause)
                            } else {
                                stringResource(R.string.control_play)
                            },
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                IconButton(onClick = { playbackController.next() }, enabled = hasTrack) {
                    Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.control_next))
                }
            }

            CircularControl(
                active = state.shuffleEnabled,
                onClick = { playbackController.toggleShuffle() },
                enabled = hasTrack
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (state.shuffleEnabled) {
                        stringResource(R.string.control_shuffle_on)
                    } else {
                        stringResource(R.string.control_shuffle_off)
                    },
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                                    subtitle = stringResource(
                                        R.string.track_subtitle_artist_album,
                                        result.track.artist,
                                        result.track.album
                                    ),
                                    artworkUri = result.track.artworkUri,
                                    onClick = {
                                        playbackController.jumpToQueueIndex(
                                            result.index,
                                            playWhenReady = autoPlayEnabled
                                        )
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

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun CircularControl(
    active: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            content()
        }
    }
}

private fun formatPlaybackTime(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
