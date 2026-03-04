package com.example.musicplayer.data.offline

import android.content.Context
import android.net.Uri
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.offline.model.OfflineTrackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class OfflineDownloadManager(
    context: Context,
    private val offlineStatusRepository: OfflineStatusRepository
) : OfflineDownloadStarter {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningBySource = ConcurrentHashMap.newKeySet<Long>()

    override fun downloadSource(sourceId: Long, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        if (!runningBySource.add(sourceId)) return

        scope.launch {
            try {
                offlineStatusRepository.seedSourceTracks(sourceId, tracks)
                tracks.forEach { track ->
                    offlineStatusRepository.markTrackStatus(sourceId, track, OfflineTrackStatus.QUEUED)
                }

                val sourceDir = File(appContext.filesDir, "offline_drive/$sourceId").apply { mkdirs() }
                tracks.forEach { track ->
                    runCatching {
                        offlineStatusRepository.markTrackStatus(sourceId, track, OfflineTrackStatus.DOWNLOADING)
                        ensureTrustedDownloadUrl(track.uri)
                        val target = safeTargetFile(sourceDir, track)
                        downloadFile(track.uri, target)
                        offlineStatusRepository.markTrackStatus(
                            sourceId = sourceId,
                            track = track,
                            status = OfflineTrackStatus.DOWNLOADED,
                            localFilePath = target.absolutePath,
                            errorMessage = null
                        )
                    }.onFailure { throwable ->
                        offlineStatusRepository.markTrackStatus(
                            sourceId = sourceId,
                            track = track,
                            status = OfflineTrackStatus.FAILED,
                            localFilePath = null,
                            errorMessage = sanitizeErrorMessage(throwable)
                        )
                    }
                }
            } finally {
                runningBySource.remove(sourceId)
                offlineStatusRepository.recomputeAndPersistSummary(sourceId)
            }
        }
    }

    private fun fileNameFor(track: Track): String {
        val rawId = track.driveFileId ?: track.id
        val idSafe = rawId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val extension = safeAudioExtension(track)
        return "$idSafe.$extension"
    }

    private fun safeAudioExtension(track: Track): String {
        val fromMime = when (track.mimeType?.lowercase()) {
            "audio/mpeg" -> "mp3"
            "audio/mp4" -> "m4a"
            "audio/aac" -> "aac"
            "audio/wav" -> "wav"
            "audio/flac" -> "flac"
            "audio/ogg" -> "ogg"
            else -> null
        }
        if (fromMime != null) return fromMime

        val fromTitle = track.title.substringAfterLast('.', "").lowercase()
        return if (fromTitle in SAFE_AUDIO_EXTENSIONS) fromTitle else "mp3"
    }

    private fun safeTargetFile(sourceDir: File, track: Track): File {
        val target = File(sourceDir, fileNameFor(track))
        val sourceCanonical = sourceDir.canonicalPath + File.separator
        val targetCanonical = target.canonicalPath
        check(targetCanonical.startsWith(sourceCanonical)) { "Invalid target path" }
        return target
    }

    private fun ensureTrustedDownloadUrl(remoteUrl: String) {
        val uri = Uri.parse(remoteUrl)
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        check(scheme == "https") { "Only HTTPS downloads are allowed" }
        check(host in TRUSTED_DRIVE_HOSTS) { "Untrusted download host" }
    }

    private fun downloadFile(remoteUrl: String, target: File) {
        val firstConnection = URL(remoteUrl).openConnection() as HttpURLConnection
        firstConnection.instanceFollowRedirects = false
        firstConnection.connectTimeout = 12_000
        firstConnection.readTimeout = 20_000
        firstConnection.requestMethod = "GET"
        firstConnection.connect()

        val finalUrl = try {
            val location = firstConnection.getHeaderField("Location")
            if (firstConnection.responseCode in 300..399 && !location.isNullOrBlank()) {
                val redirected = URL(URL(remoteUrl), location).toString()
                ensureTrustedDownloadUrl(redirected)
                redirected
            } else {
                remoteUrl
            }
        } finally {
            firstConnection.disconnect()
        }

        val connection = URL(finalUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = false
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            throw IllegalStateException("HTTP ${connection.responseCode}")
        }

        try {
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sanitizeErrorMessage(throwable: Throwable): String {
        val message = throwable.message.orEmpty().lowercase()
        return when {
            "https" in message || "untrusted" in message || "url" in message -> "Invalid download URL"
            "http" in message || "network" in message || "timeout" in message -> "Network issue while downloading"
            else -> "Download failed"
        }
    }

    companion object {
        private val SAFE_AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg")
        private val TRUSTED_DRIVE_HOSTS = setOf("drive.google.com", "www.drive.google.com")
    }
}

interface OfflineDownloadStarter {
    fun downloadSource(sourceId: Long, tracks: List<Track>)
}
