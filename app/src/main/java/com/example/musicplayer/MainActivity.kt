package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.musicplayer.di.AppContainer
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.App

class MainActivity : ComponentActivity() {

    private lateinit var playbackController: PlaybackController
    private lateinit var appContainer: AppContainer
    private var hasAudioPermission by mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasAudioPermission = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = AppContainer(applicationContext)
        playbackController = PlaybackController(
            context = applicationContext,
            playbackUriResolver = appContainer.offlinePlaybackResolver,
            onTrackPlayed = { track -> appContainer.playbackHistoryRepository.recordPlayedTrack(track) }
        )
        hasAudioPermission = isAudioPermissionGranted()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            App(
                playbackController = playbackController,
                appContainer = appContainer,
                hasAudioPermission = hasAudioPermission,
                onRequestAudioPermission = {
                    audioPermissionLauncher.launch(audioPermissionName())
                }
            )
        }
    }

    override fun onDestroy() {
        playbackController.disconnect()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        hasAudioPermission = isAudioPermissionGranted()
    }

    private fun audioPermissionName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun isAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            audioPermissionName()
        ) == PackageManager.PERMISSION_GRANTED
    }
}
