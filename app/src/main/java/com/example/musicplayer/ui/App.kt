package com.example.musicplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.musicplayer.features.favorites.FavoritesScreen
import com.example.musicplayer.features.favorites.FavoritesViewModel
import com.example.musicplayer.features.local.LocalLibraryScreen
import com.example.musicplayer.features.local.LocalLibraryViewModel
import com.example.musicplayer.features.playlist.PlaylistsScreen
import com.example.musicplayer.features.playlist.PlaylistsViewModel
import com.example.musicplayer.features.recents.RecentsScreen
import com.example.musicplayer.features.recents.RecentsViewModel
import com.example.musicplayer.features.settings.SettingsScreen
import com.example.musicplayer.features.settings.SettingsViewModel
import com.example.musicplayer.playback.PlaybackController
import com.example.musicplayer.ui.AppTab.LIBRARY
import com.example.musicplayer.ui.AppTab.NOW_PLAYING
import com.example.musicplayer.ui.AppTab.PLAYLISTS
import com.example.musicplayer.ui.AppTab.SETTINGS
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
    var selectedTab by rememberSaveable { mutableStateOf(LIBRARY) }
    var selectedLibrarySection by rememberSaveable { mutableStateOf(LibrarySection.LOCAL) }
    val playbackState by playbackController.state.collectAsStateWithLifecycle()
    val bottomTabs = remember { listOf(LIBRARY, PLAYLISTS, SETTINGS, NOW_PLAYING) }

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
    val favoritesViewModel: FavoritesViewModel = viewModel(
        factory = FavoritesViewModel.factory(appContainer.favoritesRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            repository = appContainer.settingsRepository,
            favoritesRepository = appContainer.favoritesRepository,
            playbackHistoryRepository = appContainer.playbackHistoryRepository
        )
    )
    val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
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

    MusicPlayerTheme(themeMode = settingsState.themeMode) {
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
                            bottomTabs.forEach { tab ->
                                val nowPlayingIcon = if (playbackState.isPlaying) {
                                    Icons.Filled.FastForward
                                } else {
                                    Icons.Filled.PlayArrow
                                }
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
                                            imageVector = if (tab == NOW_PLAYING) nowPlayingIcon else tab.icon,
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
                        LIBRARY -> Column(modifier = Modifier.padding(innerPadding)) {
                            LibrarySectionBar(
                                selected = selectedLibrarySection,
                                onSelected = { selectedLibrarySection = it }
                            )
                            when (selectedLibrarySection) {
                                LibrarySection.LOCAL -> LocalLibraryScreen(
                                    modifier = Modifier.weight(1f),
                                    viewModel = localLibraryViewModel,
                                    favoritesViewModel = favoritesViewModel,
                                    playbackController = playbackController,
                                    hasAudioPermission = hasAudioPermission,
                                    autoPlayEnabled = settingsState.autoPlayEnabled,
                                    autoRescanOnOpen = settingsState.autoRescanOnOpen,
                                    onRequestPermission = onRequestAudioPermission,
                                    activePlaylistName = activePlaylistName,
                                    onChangePlaylist = { selectedTab = PLAYLISTS },
                                    onAddToPlaylist = { track ->
                                        playlistsViewModel.addTrackToSelectedPlaylist(track)
                                    },
                                    onOpenNowPlaying = { selectedTab = NOW_PLAYING }
                                )

                                LibrarySection.DRIVE -> DriveBrowserScreen(
                                    modifier = Modifier.weight(1f),
                                    viewModel = driveViewModel,
                                    onPlayTracks = { tracks, index ->
                                        playbackController.setQueue(
                                            tracks,
                                            index,
                                            playWhenReady = settingsState.autoPlayEnabled
                                        )
                                        selectedTab = NOW_PLAYING
                                    },
                                    activePlaylistName = activePlaylistName,
                                    onChangePlaylist = { selectedTab = PLAYLISTS },
                                    onAddToPlaylist = { track ->
                                        playlistsViewModel.addTrackToSelectedPlaylist(track)
                                    }
                                )

                                LibrarySection.FAVORITES -> FavoritesScreen(
                                    modifier = Modifier.weight(1f),
                                    viewModel = favoritesViewModel,
                                    playbackController = playbackController,
                                    autoPlayEnabled = settingsState.autoPlayEnabled,
                                    onOpenNowPlaying = { selectedTab = NOW_PLAYING }
                                )

                                LibrarySection.RECENTS -> RecentsScreen(
                                    modifier = Modifier.weight(1f),
                                    viewModel = recentsViewModel,
                                    playbackController = playbackController,
                                    autoPlayEnabled = settingsState.autoPlayEnabled,
                                    onOpenNowPlaying = { selectedTab = NOW_PLAYING }
                                )
                            }
                        }

                        PLAYLISTS -> PlaylistsScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = playlistsViewModel,
                            playbackController = playbackController,
                            autoPlayEnabled = settingsState.autoPlayEnabled,
                            onOpenNowPlaying = { selectedTab = NOW_PLAYING }
                        )

                        SETTINGS -> SettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = settingsViewModel
                        )

                        NOW_PLAYING -> NowPlayingScreen(
                            playbackController = playbackController,
                            autoPlayEnabled = settingsState.autoPlayEnabled,
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
    LIBRARY(R.string.tab_library, Icons.Filled.LibraryMusic),
    PLAYLISTS(R.string.tab_playlists, Icons.AutoMirrored.Filled.List),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
    NOW_PLAYING(R.string.tab_now_playing, Icons.Filled.PlayArrow)
}

private enum class LibrarySection {
    LOCAL,
    DRIVE,
    FAVORITES,
    RECENTS
}

@Composable
private fun LibrarySectionBar(
    selected: LibrarySection,
    onSelected: (LibrarySection) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibrarySection.entries.forEach { section ->
            val isSelected = section == selected
            val labelRes = when (section) {
                LibrarySection.LOCAL -> R.string.tab_local
                LibrarySection.DRIVE -> R.string.tab_drive
                LibrarySection.FAVORITES -> R.string.tab_favorites
                LibrarySection.RECENTS -> R.string.tab_recents
            }
            if (isSelected) {
                FilledTonalButton(onClick = { onSelected(section) }) {
                    Text(text = stringResource(labelRes))
                }
            } else {
                Button(onClick = { onSelected(section) }) {
                    Text(text = stringResource(labelRes))
                }
            }
        }
    }
}
