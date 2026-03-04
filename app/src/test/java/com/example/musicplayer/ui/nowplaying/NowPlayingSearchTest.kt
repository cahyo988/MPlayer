package com.example.musicplayer.ui.nowplaying

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingSearchTest {

    @Test
    fun searchMatchesTitleArtistAlbumCaseInsensitive() {
        val queue = listOf(
            track("1", "Alpha Song", "First Artist", "Album One"),
            track("2", "Beta", "Second", "Evening Album")
        )

        val byTitle = searchQueue(queue, "alpha")
        val byArtist = searchQueue(queue, "second")
        val byAlbum = searchQueue(queue, "evening")

        assertEquals(1, byTitle.size)
        assertEquals(0, byTitle.first().index)
        assertEquals(1, byArtist.first().index)
        assertEquals(1, byAlbum.first().index)
    }

    @Test
    fun blankQueryReturnsEmpty() {
        assertTrue(searchQueue(listOf(track("1", "A", "B", "C")), "  ").isEmpty())
    }

    private fun track(id: String, title: String, artist: String, album: String) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 0L,
        uri = "content://$id",
        source = TrackSource.LOCAL
    )
}
