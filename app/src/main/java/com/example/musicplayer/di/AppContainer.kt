package com.example.musicplayer.di

import android.content.Context
import androidx.room.Room
import com.example.musicplayer.data.local.LocalMusicRepository
import com.example.musicplayer.data.local.MediaStoreScanner
import com.example.musicplayer.data.drive.DriveRepository
import com.example.musicplayer.data.playlist.PlaylistRepository
import com.example.musicplayer.data.playlist.db.AppDatabase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "music-player.db").build()
    }

    val localMusicRepository: LocalMusicRepository by lazy {
        LocalMusicRepository(MediaStoreScanner(appContext))
    }

    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(database.playlistDao())
    }

    val driveRepository: DriveRepository by lazy {
        DriveRepository()
    }
}
