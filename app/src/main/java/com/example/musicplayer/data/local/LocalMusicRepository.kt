package com.example.musicplayer.data.local

import com.example.musicplayer.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LocalMusicRepository(
    private val scanner: MediaStoreScanner
) {
    private val tracksState = MutableStateFlow<List<Track>>(emptyList())

    fun tracks(): StateFlow<List<Track>> = tracksState.asStateFlow()

    suspend fun refresh() {
        val scanned = withContext(Dispatchers.IO) { scanner.scanAudio() }
        tracksState.value = scanned
    }
}
