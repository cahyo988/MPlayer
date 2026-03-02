package com.example.musicplayer.playback

import com.example.musicplayer.core.model.RepeatMode
import com.example.musicplayer.core.model.Track

data class PlaybackState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false
)
