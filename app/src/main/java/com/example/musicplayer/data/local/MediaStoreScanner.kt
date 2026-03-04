package com.example.musicplayer.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource

class MediaStoreScanner(private val context: Context) {

    fun scanAudio(): List<Track> {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val tracks = mutableListOf<Track>()
        resolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val albumId = cursor.getLong(albumIdColumn)
                val artworkUri = if (albumId > 0) {
                    ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId).toString()
                } else {
                    null
                }
                tracks += Track(
                    id = "local:$id",
                    title = cursor.getString(titleColumn) ?: "Unknown title",
                    artist = cursor.getString(artistColumn) ?: "Unknown artist",
                    album = cursor.getString(albumColumn) ?: "Unknown album",
                    durationMs = cursor.getLong(durationColumn),
                    uri = contentUri.toString(),
                    source = TrackSource.LOCAL,
                    artworkUri = artworkUri,
                    mimeType = cursor.getString(mimeColumn)
                )
            }
        }

        return tracks
    }
}
