package com.example.musicplayer.ui.search

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSearchTest {

    @Test
    fun returnsAllWhenQueryBlank() {
        val tracks = sampleTracks()

        val filtered = filterTracks(tracks, "   ")

        assertEquals(tracks, filtered)
    }

    @Test
    fun matchesByTitleArtistOrAlbumCaseInsensitive() {
        val tracks = sampleTracks()

        val byTitle = filterTracks(tracks, "night")
        val byArtist = filterTracks(tracks, "dj alpha")
        val byAlbum = filterTracks(tracks, "FOCUS")

        assertEquals(listOf("1"), byTitle.map { it.id })
        assertEquals(listOf("2"), byArtist.map { it.id })
        assertEquals(listOf("3"), byAlbum.map { it.id })
    }

    private fun sampleTracks(): List<Track> = listOf(
        Track(
            id = "1",
            title = "Night Drive",
            artist = "Neon",
            album = "City Lights",
            durationMs = 120_000,
            uri = "file://1",
            source = TrackSource.LOCAL
        ),
        Track(
            id = "2",
            title = "Skyline",
            artist = "DJ Alpha",
            album = "Pulse",
            durationMs = 98_000,
            uri = "file://2",
            source = TrackSource.LOCAL
        ),
        Track(
            id = "3",
            title = "Deep Work",
            artist = "Focus Band",
            album = "Focus Sessions",
            durationMs = 180_000,
            uri = "file://3",
            source = TrackSource.LOCAL
        )
    )
}
