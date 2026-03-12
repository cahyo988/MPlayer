package com.example.musicplayer.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @OptIn(markerClass = [UnstableApi::class])
    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()

        val player = PlayerFactory.create(this)
        mediaSession = MediaSession.Builder(this, player)
            .setId(SESSION_ID)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val isTrustedController = controllerInfo.packageName == packageName
        return if (isTrustedController) mediaSession else null
    }


    @OptIn(markerClass = [UnstableApi::class])
    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        PlayerFactory.releaseCache()
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "playback"
        const val SESSION_ID = "music-player-session"
    }
}
