package com.example.musicplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.example.musicplayer.features.recents.RecentsScreen
import com.example.musicplayer.features.recents.RecentsViewModel
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.AppTab.DRIVE
import com.example.musicplayer.ui.AppTab.LOCAL
import com.example.musicplayer.ui.AppTab.NOW_PLAYING
import com.example.musicplayer.ui.AppTab.PLAYLISTS
import com.example.musicplayer.ui.AppTab.RECENTS
import com.example.musicplayer.ui.components.MiniPlayerBar
import com.example.musicplayer.ui.nowplaying.NowPlayingScreen
import com.example.musicplayer.ui.theme.MusicPlayerTheme

@Composable
fun App(
    playbackController: PlaybackController,
    appContainer: AppContainer,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(LOCAL) }

    DisposableEffect(playbackController) {
        playbackController.connect()
        onDispose { playbackController.disconnect() }
    }

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
    val recentsViewModel: RecentsViewModel = viewModel(
        factory = RecentsViewModel.factory(appContainer.playbackHistoryRepository)
    )

    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = selectedTab != NOW_PLAYING,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
                        ) {
                            MiniPlayerHost(
                                playbackController = playbackController,
                                onOpenNowPlaying = { selectedTab = NOW_PLAYING }
                            )
                        }
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            AppTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn() + slideInVertically(initialOffsetY = { it / 8 })) togetherWith
                            (fadeOut() + slideOutVertically(targetOffsetY = { -it / 8 }))
                    },
                    label = "app_tab_transition"
                ) { tab ->
                    when (tab) {
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

                        RECENTS -> RecentsScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = recentsViewModel,
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
}

@Composable
private fun MiniPlayerHost(
    playbackController: PlaybackController,
    onOpenNowPlaying: () -> Unit
) {
    val playbackState by playbackController.state.collectAsStateWithLifecycle()

    val currentTrack = playbackState.currentTrack ?: return
    MiniPlayerBar(
        track = currentTrack,
        isPlaying = playbackState.isPlaying,
        positionMs = playbackState.positionMs,
        durationMs = playbackState.durationMs,
        onTogglePlay = {
            if (playbackState.isPlaying) {
                playbackController.pause()
            } else {
                playbackController.play()
            }
        },
        onOpenNowPlaying = onOpenNowPlaying
    )
}

enum class AppTab(
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    LOCAL(R.string.tab_local, Icons.Filled.LibraryMusic),
    DRIVE(R.string.tab_drive, Icons.Filled.Storage),
    PLAYLISTS(R.string.tab_playlists, Icons.AutoMirrored.Filled.List),
    RECENTS(R.string.tab_recents, Icons.Filled.History),
    NOW_PLAYING(R.string.tab_now_playing, Icons.Filled.PlayArrow)
}
