package com.example.musicplayer.data.drive

import com.example.musicplayer.core.model.DriveNode
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DriveRepository : DriveDataSource {

    override suspend fun listPublicFolder(publicFolderUrl: String): List<DriveNode> = withContext(Dispatchers.IO) {
        val normalizedUrl = publicFolderUrl.trim()
        val folderId = extractFolderId(normalizedUrl)
            ?: throw IllegalArgumentException("Enter a valid public Google Drive folder URL")

        val embeddedUrl = "https://drive.google.com/embeddedfolderview?id=${urlEncode(folderId)}#list"
        val html = runCatching { fetchHtml(embeddedUrl) }
            .getOrElse { throw RuntimeException("Could not load Drive folder. Check your connection and retry.", it) }
        val document = Jsoup.parse(html)

        val tracks = parseTracksFromDocument(document)

        tracks.mapNotNull { track ->
            val fileId = track.driveFileId ?: ""
            if (fileId.isBlank()) return@mapNotNull null
            DriveNode(
                name = track.title,
                uri = track.uri,
                isFolder = false,
                track = track
            )
        }
    }

    override fun isValidPublicFolderUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return extractFolderId(url.trim()) != null
    }

    fun extractPublicFolderId(url: String): String? = extractFolderId(url.trim())

    internal fun parseTracksFromHtml(html: String): List<Track> {
        return parseTracksFromDocument(Jsoup.parse(html))
    }

    private fun parseTracksFromDocument(document: Document): List<Track> {
        return document.select("a[href]")
            .mapNotNull { element ->
                val href = element.attr("href").trim()
                val fileId = extractFileIdFromHref(href) ?: return@mapNotNull null

                val rawName = element.text().trim()
                val candidateName = decodeHtml(rawName)
                    .substringBefore('?')
                    .substringBefore('#')
                    .trim()
                    .ifBlank { "Unknown title" }
                if (!isLikelyAudio(candidateName)) return@mapNotNull null

                val streamUrl = "https://drive.google.com/uc?export=download&id=${urlEncode(fileId)}"
                Track(
                    id = "drive:$fileId",
                    title = candidateName,
                    artist = "Drive",
                    album = "Drive Public Folder",
                    durationMs = 0L,
                    uri = streamUrl,
                    source = TrackSource.DRIVE,
                    driveFileId = fileId,
                    mimeType = guessMimeType(candidateName)
                )
            }
            .distinctBy { it.id }
    }

    private fun extractFolderId(url: String): String? {
        if (url.isBlank()) return null

        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }

        val uri = runCatching { java.net.URI(normalized) }.getOrNull() ?: return null
        val host = (uri.host ?: "").lowercase()
        if (host != "drive.google.com" && host != "www.drive.google.com") return null

        val path = uri.path.orEmpty()
        val folderPathMatch = Regex("/drive/(?:u/\\d+/)?folders/([a-zA-Z0-9_-]+)").find(path)
        if (folderPathMatch != null) {
            return folderPathMatch.groupValues.getOrNull(1)?.takeIf(::isValidDriveId)
        }

        val shortPathMatch = Regex("/folders/([a-zA-Z0-9_-]+)").find(path)
        if (shortPathMatch != null) {
            return shortPathMatch.groupValues.getOrNull(1)?.takeIf(::isValidDriveId)
        }

        return extractIdFromQuery(uri.query)
    }

    private fun extractFileIdFromHref(href: String): String? {
        val normalized = href.substringBefore('#')
        val filePathMatch = Regex("/file/d/([a-zA-Z0-9_-]+)").find(normalized)
        if (filePathMatch != null) {
            return filePathMatch.groupValues.getOrNull(1)
        }

        val queryIdMatch = Regex("[?&]id=([a-zA-Z0-9_-]+)").find(normalized)
        if (queryIdMatch != null) {
            return queryIdMatch.groupValues.getOrNull(1)
        }

        return null
    }

    private fun extractIdFromQuery(query: String?): String? {
        if (query.isNullOrBlank()) return null
        return query.split('&')
            .asSequence()
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == "id" }
            ?.getOrNull(1)
            ?.takeIf(::isValidDriveId)
    }

    private fun fetchHtml(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun isValidDriveId(id: String): Boolean {
        return DRIVE_ID_REGEX.matches(id)
    }

    private fun isLikelyAudio(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return extension in SUPPORTED_AUDIO_EXTENSIONS
    }

    private fun guessMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            else -> "audio/*"
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    private fun decodeHtml(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    companion object {
        private val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg")
        private val DRIVE_ID_REGEX = Regex("^[A-Za-z0-9_-]{10,}$")
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 12_000
    }
}
