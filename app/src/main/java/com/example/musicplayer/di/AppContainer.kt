package com.example.musicplayer.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.musicplayer.data.local.LocalMusicRepository
import com.example.musicplayer.data.local.MediaStoreScanner
import com.example.musicplayer.data.history.PlaybackHistoryRepository
import com.example.musicplayer.data.offline.OfflineDownloadManager
import com.example.musicplayer.data.offline.OfflinePlaybackResolver
import com.example.musicplayer.data.offline.OfflineStatusRepository
import com.example.musicplayer.data.drive.DriveSourceRepository
import com.example.musicplayer.data.drive.DriveRepository
import com.example.musicplayer.data.playlist.PlaylistRepository
import com.example.musicplayer.data.playlist.db.AppDatabase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "music-player.db")
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .build()
    }

    val localMusicRepository: LocalMusicRepository by lazy {
        LocalMusicRepository(MediaStoreScanner(appContext))
    }

    val playbackHistoryRepository: PlaybackHistoryRepository by lazy {
        PlaybackHistoryRepository(database.playbackHistoryDao())
    }

    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(database.playlistDao())
    }

    val driveRepository: DriveRepository by lazy {
        DriveRepository()
    }

    val driveSourceRepository: DriveSourceRepository by lazy {
        DriveSourceRepository(database.driveSourceDao(), driveRepository)
    }

    val offlineStatusRepository: OfflineStatusRepository by lazy {
        OfflineStatusRepository(
            context = appContext,
            dao = database.offlineStatusDao(),
            driveSourceDao = database.driveSourceDao()
        )
    }

    val offlineDownloadManager: OfflineDownloadManager by lazy {
        OfflineDownloadManager(appContext, offlineStatusRepository)
    }

    val offlinePlaybackResolver: OfflinePlaybackResolver by lazy {
        OfflinePlaybackResolver(appContext, offlineStatusRepository)
    }

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN isSystemDefault INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlist_tracks ADD COLUMN artworkUri TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS drive_sources (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        folderUrl TEXT NOT NULL,
                        folderId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS offline_source_status (
                        sourceId INTEGER NOT NULL PRIMARY KEY,
                        status TEXT NOT NULL,
                        totalTracks INTEGER NOT NULL,
                        downloadedTracks INTEGER NOT NULL,
                        failedTracks INTEGER NOT NULL,
                        downloadingTracks INTEGER NOT NULL,
                        queuedTracks INTEGER NOT NULL,
                        progressPercent INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS offline_track_status (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sourceId INTEGER NOT NULL,
                        trackId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        remoteUri TEXT NOT NULL,
                        mimeType TEXT,
                        driveFileId TEXT,
                        status TEXT NOT NULL,
                        localFilePath TEXT,
                        errorMessage TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_offline_track_status_sourceId ON offline_track_status(sourceId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_offline_track_status_sourceId_trackId ON offline_track_status(sourceId, trackId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playback_history (
                        trackId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        uri TEXT NOT NULL,
                        source TEXT NOT NULL,
                        artworkUri TEXT,
                        driveFileId TEXT,
                        mimeType TEXT,
                        lastPlayedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_lastPlayedAt ON playback_history(lastPlayedAt)")
            }
        }
    }
}
