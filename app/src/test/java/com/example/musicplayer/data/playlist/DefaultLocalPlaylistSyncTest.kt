package com.example.musicplayer.data.playlist

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLocalPlaylistSyncTest {

    @Test
    fun mappingDeduplicatesAndPreservesOrder() {
        val tracks = listOf(
            track("1", "Song 1"),
            track("1", "Song 1 duplicate"),
            track("2", "Song 2")
        )

        val mapped = mapLocalTracksForDefaultPlaylist(playlistId = 99L, tracks = tracks)

        assertEquals(2, mapped.size)
        assertEquals("1", mapped[0].trackId)
        assertEquals(0, mapped[0].position)
        assertEquals("2", mapped[1].trackId)
        assertEquals(1, mapped[1].position)
        assertTrue(mapped.all { it.playlistId == 99L })
    }

    private fun track(id: String, title: String) = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 1000,
        uri = "content://local/$id",
        source = TrackSource.LOCAL,
        artworkUri = "content://art/$id"
    )
}
