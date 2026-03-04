package com.example.musicplayer.ui.nowplaying

import com.example.musicplayer.core.model.Track
import java.util.Locale

data class QueueSearchResult(
    val track: Track,
    val index: Int
)

fun searchQueue(queue: List<Track>, query: String): List<QueueSearchResult> {
    val normalized = query.trim().lowercase(Locale.getDefault())
    if (normalized.isBlank()) return emptyList()
    return queue.mapIndexedNotNull { index, track ->
        val haystack = listOf(track.title, track.artist, track.album)
            .joinToString(" ")
            .lowercase(Locale.getDefault())
        if (haystack.contains(normalized)) QueueSearchResult(track, index) else null
    }
}
