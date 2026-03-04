package com.example.musicplayer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.R
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
import com.example.musicplayer.ui.theme.MusicPlayerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    playbackController: PlaybackController,
    appContainer: AppContainer,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(LOCAL) }

    val localLibraryViewModel: LocalLibraryViewModel = viewModel(
        factory = LocalLibraryViewModel.factory(
            appContainer.localMusicRepository,
            appContainer.playlistRepository
        )
    )
    val playlistsViewModel: PlaylistsViewModel = viewModel(
        factory = PlaylistsViewModel.factory(appContainer.playlistRepository)
    )
    val playlistsState by playlistsViewModel.uiState.collectAsStateWithLifecycle()
    val activePlaylistName = playlistsState.selectedPlaylist?.name
        ?: playlistsState.playlists.firstOrNull { it.id == playlistsState.selectedPlaylistId }?.name
        ?: stringResource(R.string.playlist_default_name)
    val driveViewModel: DriveViewModel = viewModel(
        factory = DriveViewModel.factory(
            appContainer.driveRepository,
            appContainer.driveSourceRepository,
            appContainer.offlineStatusRepository,
            appContainer.offlineDownloadManager
        )
    )

    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                when (selectedTab) {
                                    LOCAL -> stringResource(R.string.screen_local_title)
                                    DRIVE -> stringResource(R.string.screen_drive_title)
                                    PLAYLISTS -> stringResource(R.string.screen_playlists_title)
                                    NOW_PLAYING -> stringResource(R.string.screen_now_playing_title)
                                }
                            )
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) }
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
                        activePlaylistName = activePlaylistName,
                        onChangePlaylist = { selectedTab = PLAYLISTS },
                        onAddToPlaylist = { track ->
                            playlistsViewModel.addTrackToSelectedPlaylist(track)
                        },
                        onOpenNowPlaying = { selectedTab = NOW_PLAYING }
                    )

                    DRIVE -> DriveBrowserScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = driveViewModel,
                        onPlayTracks = { tracks, index ->
                            playbackController.setQueue(tracks, index, playWhenReady = true)
                            selectedTab = NOW_PLAYING
                        },
                        activePlaylistName = activePlaylistName,
                        onChangePlaylist = { selectedTab = PLAYLISTS },
                        onAddToPlaylist = { track ->
                            playlistsViewModel.addTrackToSelectedPlaylist(track)
                        }
                    )

                    PLAYLISTS -> PlaylistsScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = playlistsViewModel,
                        playbackController = playbackController,
                        onOpenNowPlaying = { selectedTab = NOW_PLAYING }
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

enum class AppTab(
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    LOCAL(R.string.tab_local, Icons.Filled.LibraryMusic),
    DRIVE(R.string.tab_drive, Icons.Filled.Storage),
    PLAYLISTS(R.string.tab_playlists, Icons.AutoMirrored.Filled.List),
    NOW_PLAYING(R.string.tab_now_playing, Icons.Filled.PlayArrow)
}
