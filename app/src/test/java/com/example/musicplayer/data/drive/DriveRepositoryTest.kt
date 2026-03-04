package com.example.musicplayer.data.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveRepositoryTest {

    private val repository = DriveRepository()

    @Test
    fun validatesPublicDriveFolderUrls() {
        assertTrue(repository.isValidPublicFolderUrl("https://drive.google.com/drive/folders/abc_123-XYZ"))
        assertTrue(repository.isValidPublicFolderUrl("drive.google.com/drive/folders/abc_123-XYZ"))
        assertFalse(repository.isValidPublicFolderUrl("https://example.com/drive/folders/abc"))
        assertFalse(repository.isValidPublicFolderUrl("https://drive.google.com/file/d/abc/view"))
    }

    @Test
    fun parserFiltersNonAudioAndDeduplicates() {
        val html = """
            <html>
            <body>
              <a href="https://drive.google.com/file/d/id1/view">song-one.mp3</a>
              <a href="https://drive.google.com/file/d/id1/view">song-one.mp3</a>
              <a href="https://drive.google.com/file/d/id2/view">song-two.m4a</a>
              <a href="https://drive.google.com/file/d/id3/view">cover.jpg</a>
              <a href="https://drive.google.com/file/d/id4/view"></a>
            </body>
            </html>
        """.trimIndent()

        val tracks = repository.parseTracksFromHtml(html)

        assertEquals(2, tracks.size)
        assertEquals("drive:id1", tracks[0].id)
        assertEquals("audio/mpeg", tracks[0].mimeType)
        assertEquals("audio/mp4", tracks[1].mimeType)
    }

    @Test
    fun parserHandlesUnexpectedStructureWithEmptyList() {
        val html = "<html><body><div>No links here</div></body></html>"

        val tracks = repository.parseTracksFromHtml(html)

        assertTrue(tracks.isEmpty())
    }
}
