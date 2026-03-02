package com.example.musicplayer.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource

object MediaItemMapper {
    private const val EXTRA_SOURCE = "source"
    private const val EXTRA_DRIVE_FILE_ID = "driveFileId"
    private const val EXTRA_MIME_TYPE = "mimeType"

    fun toMediaItem(track: Track): MediaItem {
        val extras = android.os.Bundle().apply {
            putString(EXTRA_SOURCE, track.source.name)
            putString(EXTRA_DRIVE_FILE_ID, track.driveFileId)
            putString(EXTRA_MIME_TYPE, track.mimeType)
        }

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    fun toTrack(item: MediaItem): Track {
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        val source = runCatching {
            TrackSource.valueOf(extras?.getString(EXTRA_SOURCE) ?: TrackSource.LOCAL.name)
        }.getOrDefault(TrackSource.LOCAL)

        return Track(
            id = item.mediaId,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            album = metadata.albumTitle?.toString().orEmpty(),
            durationMs = 0L,
            uri = item.localConfiguration?.uri?.toString().orEmpty(),
            source = source,
            driveFileId = extras?.getString(EXTRA_DRIVE_FILE_ID),
            mimeType = extras?.getString(EXTRA_MIME_TYPE)
        )
    }
}
