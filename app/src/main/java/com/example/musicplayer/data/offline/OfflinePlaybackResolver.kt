package com.example.musicplayer.data.offline

import android.content.Context
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import com.example.musicplayer.playback.PlaybackUriResolver
import java.io.File

class OfflinePlaybackResolver(
    private val offlineStatusRepository: OfflinePathLookup,
    private val offlineRootPath: String? = null
) : PlaybackUriResolver {
    constructor(
        context: Context,
        offlineStatusRepository: OfflinePathLookup
    ) : this(
        offlineStatusRepository = offlineStatusRepository,
        offlineRootPath = buildOfflineRootPath(context)
    )

    override suspend fun resolve(track: Track): Track {
        if (track.source != TrackSource.DRIVE) return track
        val localPath = offlineStatusRepository.findAnyDownloadedPath(track.id) ?: return track
        val localFile = File(localPath)
        if (!isPathUnderOfflineRoot(localFile)) return track
        return track.copy(uri = localFile.toURI().toString())
    }

    private fun isPathUnderOfflineRoot(file: File): Boolean {
        val root = offlineRootPath ?: return false
        return runCatching {
            val candidate = file.canonicalPath
            candidate.startsWith(root)
        }.getOrDefault(false)
    }

    companion object {
        private const val OFFLINE_ROOT_DIR = "offline_drive"

        private fun buildOfflineRootPath(context: Context): String? {
            return runCatching {
                File(context.applicationContext.filesDir, OFFLINE_ROOT_DIR).canonicalPath + File.separator
            }.getOrNull()
        }
    }
}
