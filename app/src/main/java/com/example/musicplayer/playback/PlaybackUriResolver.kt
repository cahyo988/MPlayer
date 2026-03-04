package com.example.musicplayer.playback

import com.example.musicplayer.core.model.Track

interface PlaybackUriResolver {
    suspend fun resolve(track: Track): Track
}

object NoOpPlaybackUriResolver : PlaybackUriResolver {
    override suspend fun resolve(track: Track): Track = track
}
