package com.example.musicplayer.data.offline

import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class OfflinePlaybackResolverTest {

    @Test
    fun usesLocalFileWhenDownloadedExists() = runTest {
        val localFile = File(System.getProperty("java.io.tmpdir"), "a.mp3")
        val offlineRootPath = requireNotNull(localFile.parentFile).canonicalPath + File.separator
        val resolver = OfflinePlaybackResolver(
            offlineStatusRepository = object : OfflinePathLookup {
                override suspend fun findAnyDownloadedPath(trackId: String): String? =
                    if (trackId == "drive:abc") localFile.path else null
            },
            offlineRootPath = offlineRootPath
        )

        val resolved = resolver.resolve(sampleDriveTrack())

        assertEquals(localFile.toURI().toString(), resolved.uri)
    }

    @Test
    fun keepsRemoteUriWhenNoDownloadedFile() = runTest {
        val resolver = OfflinePlaybackResolver(
            offlineStatusRepository = object : OfflinePathLookup {
                override suspend fun findAnyDownloadedPath(trackId: String): String? = null
            },
            offlineRootPath = "/tmp/"
        )

        val track = sampleDriveTrack()
        val resolved = resolver.resolve(track)

        assertEquals(track.uri, resolved.uri)
    }

    private fun sampleDriveTrack() = Track(
        id = "drive:abc",
        title = "Song",
        artist = "Drive",
        album = "Drive",
        durationMs = 0L,
        uri = "https://drive.google.com/uc?export=download&id=abc",
        source = TrackSource.DRIVE,
        driveFileId = "abc"
    )
}
