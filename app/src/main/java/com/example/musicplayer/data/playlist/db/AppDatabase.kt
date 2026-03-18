package com.example.musicplayer.data.playlist.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        DriveSourceEntity::class,
        OfflineSourceStatusEntity::class,
        OfflineTrackStatusEntity::class,
        PlaybackHistoryEntity::class,
        FavoriteTrackEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun driveSourceDao(): DriveSourceDao
    abstract fun offlineStatusDao(): OfflineStatusDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun favoriteTrackDao(): FavoriteTrackDao
}
