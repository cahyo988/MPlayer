package com.example.musicplayer.data.offline

import android.content.Context
import android.net.Uri
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.data.offline.model.OfflineTrackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.io.DEFAULT_BUFFER_SIZE
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class OfflineDownloadManager(
    context: Context,
    private val offlineStatusRepository: OfflineStatusRepository
) : OfflineDownloadStarter {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningBySource = ConcurrentHashMap.newKeySet<Long>()
    private val jobsBySource = ConcurrentHashMap<Long, Job>()
    private val activeConnectionsBySource = ConcurrentHashMap<Long, MutableSet<HttpURLConnection>>()
    private val startStopLock = Any()

    override fun downloadSource(sourceId: Long, tracks: List<Track>): Boolean {
        if (tracks.isEmpty()) return false
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                offlineStatusRepository.seedSourceTracks(sourceId, tracks)
                tracks.forEach { track ->
                    offlineStatusRepository.markTrackStatus(sourceId, track, OfflineTrackStatus.QUEUED)
                }

                val sourceDir = File(appContext.filesDir, "offline_drive/$sourceId").apply { mkdirs() }
                var sourceBytes = directorySizeBytes(sourceDir)
                val maxSourceBytes = maxSourceSizeBytes()
                for (track in tracks) {
                    var target: File? = null
                    var tempTarget: File? = null
                    try {
                        check(sourceBytes < maxSourceBytes) { "Source storage quota reached" }
                        offlineStatusRepository.markTrackStatus(sourceId, track, OfflineTrackStatus.DOWNLOADING)
                        ensureTrustedDownloadUrl(track.uri)
                        target = safeTargetFile(sourceDir, track)
                        val existingSize = requireNotNull(target).takeIf { it.exists() }
                            ?.length()
                            ?.coerceAtLeast(0L)
                            ?: 0L
                        val remainingSourceBudget = maxSourceBytes - sourceBytes + existingSize
                        val maxBytes = minOf(maxTrackSizeBytes(), remainingSourceBudget)
                        check(maxBytes > 0L) { "Source storage quota reached" }

                        tempTarget = tempFileFor(requireNotNull(target))
                        val downloadedBytes = downloadFile(
                            sourceId = sourceId,
                            remoteUrl = track.uri,
                            target = requireNotNull(tempTarget),
                            maxBytes = maxBytes
                        )
                        replaceTargetFile(requireNotNull(tempTarget), requireNotNull(target))
                        sourceBytes += downloadedBytes - existingSize
                        offlineStatusRepository.markTrackStatus(
                            sourceId = sourceId,
                            track = track,
                            status = OfflineTrackStatus.DOWNLOADED,
                            localFilePath = requireNotNull(target).absolutePath,
                            errorMessage = null
                        )
                    } catch (cancellationException: CancellationException) {
                        tempTarget?.takeIf { it.exists() }?.delete()
                        throw cancellationException
                    } catch (throwable: Throwable) {
                        tempTarget?.takeIf { it.exists() }?.delete()
                        offlineStatusRepository.markTrackStatus(
                            sourceId = sourceId,
                            track = track,
                            status = OfflineTrackStatus.FAILED,
                            localFilePath = null,
                            errorMessage = sanitizeErrorMessage(throwable)
                        )
                    }
                }
            } catch (_: CancellationException) {
                offlineStatusRepository.markPendingAsFailed(sourceId, "Cancelled by user")
                throw CancellationException("Cancelled by user")
            } finally {
                jobsBySource.remove(sourceId)
                activeConnectionsBySource.remove(sourceId)?.forEach { connection ->
                    runCatching { connection.disconnect() }
                }
                runningBySource.remove(sourceId)
                offlineStatusRepository.recomputeAndPersistSummary(sourceId)
            }
        }

        synchronized(startStopLock) {
            if (runningBySource.contains(sourceId)) {
                return false
            }
            jobsBySource[sourceId] = job
            runningBySource.add(sourceId)
            job.start()
        }
        return true
    }

    override fun cancelSource(sourceId: Long): Boolean {
        val job = synchronized(startStopLock) {
            jobsBySource[sourceId]
        } ?: return false
        activeConnectionsBySource[sourceId]?.forEach { connection ->
            runCatching { connection.disconnect() }
        }
        job.cancel(CancellationException("Cancelled by user"))
        return true
    }

    private fun tempFileFor(target: File): File {
        return File(target.parentFile, "${target.name}.part")
    }

    private fun replaceTargetFile(tempFile: File, targetFile: File) {
        if (targetFile.exists()) {
            check(targetFile.delete()) { "Failed to replace existing file" }
        }
        check(tempFile.renameTo(targetFile)) { "Failed to finalize download file" }
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

    private fun ensureTrustedDownloadUrl(
        remoteUrl: String,
        allowDocsGoogleusercontent: Boolean = false
    ) {
        val uri = Uri.parse(remoteUrl)
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        check(scheme == "https") { "Only HTTPS downloads are allowed" }
        check(isTrustedDownloadHost(host, allowDocsGoogleusercontent)) { "Untrusted download host" }
    }

    private suspend fun downloadFile(sourceId: Long, remoteUrl: String, target: File, maxBytes: Long): Long {
        val firstConnection = URL(remoteUrl).openConnection() as HttpURLConnection
        registerConnection(sourceId, firstConnection)
        firstConnection.instanceFollowRedirects = false
        firstConnection.connectTimeout = 12_000
        firstConnection.readTimeout = 20_000
        firstConnection.requestMethod = "GET"
        val finalUrl = try {
            firstConnection.connect()
            val location = firstConnection.getHeaderField("Location")
            if (firstConnection.responseCode in 300..399 && !location.isNullOrBlank()) {
                val redirected = URL(URL(remoteUrl), location).toString()
                ensureTrustedDownloadUrl(redirected, allowDocsGoogleusercontent = true)
                redirected
            } else {
                remoteUrl
            }
        } finally {
            firstConnection.disconnect()
            unregisterConnection(sourceId, firstConnection)
        }

        val connection = URL(finalUrl).openConnection() as HttpURLConnection
        registerConnection(sourceId, connection)
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = false
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            throw IllegalStateException("HTTP ${connection.responseCode}")
        }

        val contentLength = connection.contentLengthLong
        if (contentLength > maxBytes) {
            connection.disconnect()
            throw IllegalStateException("File too large")
        }

        try {
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > maxBytes) {
                            throw IllegalStateException("File too large")
                        }
                        output.write(buffer, 0, read)
                    }
                    return total
                }
            }
        } finally {
            connection.disconnect()
            unregisterConnection(sourceId, connection)
        }
    }

    private fun registerConnection(sourceId: Long, connection: HttpURLConnection) {
        val set = activeConnectionsBySource.getOrPut(sourceId) {
            Collections.newSetFromMap(ConcurrentHashMap<HttpURLConnection, Boolean>())
        }
        set.add(connection)
    }

    private fun unregisterConnection(sourceId: Long, connection: HttpURLConnection) {
        activeConnectionsBySource[sourceId]?.remove(connection)
    }

    private fun maxTrackSizeBytes(): Long {
        return MAX_TRACK_SIZE_MB * 1024L * 1024L
    }

    private fun maxSourceSizeBytes(): Long {
        return MAX_SOURCE_SIZE_MB * 1024L * 1024L
    }

    private fun directorySizeBytes(dir: File): Long {
        return runCatching {
            dir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length().coerceAtLeast(0L) }
        }.getOrDefault(0L)
    }

    private fun sanitizeErrorMessage(throwable: Throwable): String {
        val message = throwable.message.orEmpty().lowercase()
        return when {
            "quota" in message -> "Source download storage limit reached"
            "too large" in message -> "Track is too large to download"
            "https" in message || "untrusted" in message || "url" in message -> "Invalid download URL"
            "http" in message || "network" in message || "timeout" in message -> "Network issue while downloading"
            else -> "Download failed"
        }
    }

    companion object {
        private val SAFE_AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg")
        private val TRUSTED_DRIVE_HOSTS = setOf("drive.google.com", "www.drive.google.com")
        private val TRUSTED_GOOGLE_USER_CONTENT_HOSTS = setOf("docs.googleusercontent.com")
        private const val TRUSTED_GOOGLE_USER_CONTENT_SUFFIX = ".docs.googleusercontent.com"
        private const val MAX_TRACK_SIZE_MB = 200L
        private const val MAX_SOURCE_SIZE_MB = 1024L

        internal fun isTrustedDownloadHost(
            host: String,
            allowDocsGoogleusercontent: Boolean = false
        ): Boolean {
            val normalized = host.lowercase()
            if (normalized in TRUSTED_DRIVE_HOSTS) return true
            if (!allowDocsGoogleusercontent) return false
            return normalized in TRUSTED_GOOGLE_USER_CONTENT_HOSTS ||
                normalized.endsWith(TRUSTED_GOOGLE_USER_CONTENT_SUFFIX)
        }
    }
}

interface OfflineDownloadStarter {
    fun downloadSource(sourceId: Long, tracks: List<Track>): Boolean
    fun cancelSource(sourceId: Long): Boolean
}
