package com.example.musicplayer.ui.search

import com.example.musicplayer.core.model.Track
import java.util.Locale

fun filterTracks(tracks: List<Track>, query: String): List<Track> {
    val normalizedQuery = query.trim().lowercase(Locale.getDefault())
    if (normalizedQuery.isBlank()) return tracks

    return tracks.filter { track ->
        val haystack = listOf(track.title, track.artist, track.album)
            .joinToString(" ")
            .lowercase(Locale.getDefault())
        haystack.contains(normalizedQuery)
    }
}
