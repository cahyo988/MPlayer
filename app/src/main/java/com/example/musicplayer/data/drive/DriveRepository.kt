package com.example.musicplayer.data.drive

import com.example.musicplayer.core.model.DriveNode
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL

class DriveRepository {

    suspend fun listPublicFolder(publicFolderUrl: String): List<DriveNode> = withContext(Dispatchers.IO) {
        val folderId = extractFolderId(publicFolderUrl)
            ?: throw IllegalArgumentException("Invalid public Drive folder URL")

        val embeddedUrl = "https://drive.google.com/embeddedfolderview?id=$folderId#list"
        val html = URL(embeddedUrl).readText()
        val document = Jsoup.parse(html)

        val tracks = document.select("a[href]")
            .mapNotNull { element ->
                val href = element.attr("href")
                val fileId = extractFileIdFromHref(href) ?: return@mapNotNull null
                val rawName = element.text().trim()
                if (rawName.isBlank()) return@mapNotNull null

                val title = decodeHtml(rawName).ifBlank { "Unknown title" }
                if (!title.lowercase().endsWith(".mp3")) return@mapNotNull null

                val streamUrl = "https://drive.google.com/uc?export=download&id=$fileId"
                Track(
                    id = "drive:$fileId",
                    title = title,
                    artist = "Drive",
                    album = "Drive Public Folder",
                    durationMs = 0L,
                    uri = streamUrl,
                    source = TrackSource.DRIVE,
                    driveFileId = fileId,
                    mimeType = "audio/mpeg"
                )
            }
            .distinctBy { it.id }

        if (tracks.isEmpty()) {
            throw IllegalStateException("Could not parse MP3 files from this public Drive folder")
        }

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

    private fun extractFolderId(url: String): String? {
        val folderRegex = Regex("/folders/([a-zA-Z0-9_-]+)")
        return folderRegex.find(url)?.groupValues?.getOrNull(1)
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

    private fun decodeHtml(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
