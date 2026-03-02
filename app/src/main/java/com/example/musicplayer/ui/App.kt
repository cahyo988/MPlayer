package com.example.musicplayer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.di.AppContainer
import com.example.musicplayer.features.drive.DriveBrowserScreen
import com.example.musicplayer.features.drive.DriveViewModel
import com.example.musicplayer.features.local.LocalLibraryScreen
import com.example.musicplayer.features.local.LocalLibraryViewModel
import com.example.musicplayer.features.playlist.PlaylistsScreen
import com.example.musicplayer.features.playlist.PlaylistsViewModel
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.AppTab.DRIVE
import com.example.musicplayer.ui.AppTab.LOCAL
import com.example.musicplayer.ui.AppTab.NOW_PLAYING
import com.example.musicplayer.ui.AppTab.PLAYLISTS
import com.example.musicplayer.ui.nowplaying.NowPlayingScreen

@Composable
fun App(
    playbackController: PlaybackController,
    appContainer: AppContainer,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(LOCAL) }

    val localLibraryViewModel: LocalLibraryViewModel = viewModel(
        factory = LocalLibraryViewModel.factory(appContainer.localMusicRepository)
    )
    val playlistsViewModel: PlaylistsViewModel = viewModel(
        factory = PlaylistsViewModel.factory(appContainer.playlistRepository)
    )
    val driveViewModel: DriveViewModel = viewModel(
        factory = DriveViewModel.factory(appContainer.driveRepository)
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                when (selectedTab) {
                    LOCAL -> LocalLibraryScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = localLibraryViewModel,
                        playbackController = playbackController,
                        hasAudioPermission = hasAudioPermission,
                        onRequestPermission = onRequestAudioPermission,
                        onAddToPlaylist = { track ->
                            playlistsViewModel.addTrackToSelectedPlaylist(track)
                        }
                    )

                    DRIVE -> DriveBrowserScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = driveViewModel,
                        onPlayTracks = { tracks, index ->
                            playbackController.setQueue(tracks, index, playWhenReady = true)
                            selectedTab = NOW_PLAYING
                        }
                    )

                    PLAYLISTS -> PlaylistsScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = playlistsViewModel,
                        playbackController = playbackController
                    )

                    NOW_PLAYING -> NowPlayingScreen(
                        playbackController = playbackController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class AppTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LOCAL("Local", Icons.Filled.LibraryMusic),
    DRIVE("Drive", Icons.Filled.Storage),
    PLAYLISTS("Playlists", Icons.Filled.List),
    NOW_PLAYING("Now Playing", Icons.Filled.PlayArrow)
}
