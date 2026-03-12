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
    private var lastRefreshAtMs: Long = 0L

    fun tracks(): StateFlow<List<Track>> = tracksState.asStateFlow()

    suspend fun refresh(force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        if (!force && tracksState.value.isNotEmpty() && now - lastRefreshAtMs < REFRESH_COOLDOWN_MS) {
            return false
        }

        val scanned = withContext(Dispatchers.IO) { scanner.scanAudio() }
        lastRefreshAtMs = now

        val hasChanged = hasTrackListChanged(tracksState.value, scanned)
        if (hasChanged) {
            tracksState.value = scanned
        }
        return hasChanged
    }

    private fun hasTrackListChanged(current: List<Track>, scanned: List<Track>): Boolean {
        if (current.size != scanned.size) return true
        return current.zip(scanned).any { (old, new) ->
            old.id != new.id ||
                old.title != new.title ||
                old.artist != new.artist ||
                old.album != new.album ||
                old.durationMs != new.durationMs ||
                old.uri != new.uri
        }
    }

    companion object {
        private const val REFRESH_COOLDOWN_MS = 60_000L
    }
}
