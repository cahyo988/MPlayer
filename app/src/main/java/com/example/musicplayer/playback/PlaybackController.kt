package com.example.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.core.model.RepeatMode
import com.example.musicplayer.core.model.Track
import com.example.musicplayer.core.model.TrackSource
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@MainThread
class PlaybackController(
    context: Context,
    private val playbackUriResolver: PlaybackUriResolver = NoOpPlaybackUriResolver
) {

    private val appContext = context.applicationContext
    private val offlineRootPath = runCatching {
        java.io.File(appContext.filesDir, "offline_drive").canonicalPath + java.io.File.separator
    }.getOrNull()
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var connectGeneration: Long = 0
    private var queue: List<Track> = emptyList()
    private var lastErrorMessage: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var queueJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = publishState()
        override fun onPlaybackStateChanged(playbackState: Int) = publishState()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publishState()
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) = publishState()

        override fun onRepeatModeChanged(repeatMode: Int) = publishState()
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = publishState()
        override fun onPlayerError(error: PlaybackException) {
            val activeTrack = queue.getOrNull(controller?.currentMediaItemIndex ?: -1)
            val title = activeTrack?.title?.takeIf { it.isNotBlank() } ?: "track"
            lastErrorMessage = "Playback failed for $title. Skipping."

            val current = controller
            if (current != null && current.hasNextMediaItem()) {
                current.seekToNextMediaItem()
                current.prepare()
                current.play()
            } else {
                current?.pause()
            }
            publishState()
        }
    }

    private fun assertMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "PlaybackController must be called on main thread"
        }
    }

    fun connect(onConnected: () -> Unit = {}) {
        assertMainThread()

        if (controller != null) {
            onConnected()
            return
        }

        val generation = connectGeneration

        controllerFuture?.let { existingFuture ->
            existingFuture.addListener(
                {
                    runCatching {
                        if (generation != connectGeneration) {
                            return@runCatching
                        }
                        if (controller == null) {
                            controller = existingFuture.get()
                            controller?.addListener(playerListener)
                        }
                        publishState()
                        onConnected()
                    }.onFailure {
                        controllerFuture = null
                        Log.e("PlaybackController", "Failed to connect MediaController", it)
                    }
                },
                ContextCompat.getMainExecutor(appContext)
            )
            return
        }

        val newGeneration = ++connectGeneration
        val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val resolvedController = future.get()
                    if (newGeneration != connectGeneration) {
                        resolvedController.release()
                        controllerFuture = null
                        return@runCatching
                    }
                    controller = resolvedController
                    controllerFuture = null
                    controller?.addListener(playerListener)
                    publishState()
                    onConnected()
                }.onFailure {
                    controllerFuture = null
                    Log.e("PlaybackController", "Failed to connect MediaController", it)
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun playSample() {
        assertMainThread()

        val sample = Track(
            id = "sample",
            title = "Sample Stream",
            artist = "SoundHelix",
            album = "Demo",
            durationMs = 0L,
            uri = SAMPLE_URL,
            source = TrackSource.LOCAL
        )
        setQueue(listOf(sample), 0, true)
    }

    fun pause() {
        assertMainThread()
        controller?.pause()
        publishState()
    }

    fun play() {
        assertMainThread()
        controller?.play()
        publishState()
    }

    fun next() {
        assertMainThread()
        controller?.seekToNextMediaItem()
        publishState()
    }

    fun previous() {
        assertMainThread()
        controller?.seekToPreviousMediaItem()
        publishState()
    }

    fun seekTo(positionMs: Long) {
        assertMainThread()
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        publishState()
    }

    fun jumpToQueueIndex(index: Int, playWhenReady: Boolean = true) {
        assertMainThread()
        val current = controller ?: return
        if (queue.isEmpty()) return
        val safeIndex = index.coerceIn(0, queue.lastIndex)
        current.seekToDefaultPosition(safeIndex)
        if (playWhenReady) current.play()
        publishState()
    }

    fun setQueue(tracks: List<Track>, startIndex: Int = 0, playWhenReady: Boolean = true) {
        assertMainThread()
        if (tracks.isEmpty()) return
        queueJob?.cancel()
        queueJob = scope.launch {
            val resolved = tracks.map { playbackUriResolver.resolve(it) }
            val allowedTracks = resolved.filter(::isAllowedPlaybackTrack)
            if (allowedTracks.isEmpty()) {
                lastErrorMessage = "No playable tracks: invalid media URL"
                publishState()
                return@launch
            }
            val safeIndex = startIndex.coerceIn(0, allowedTracks.lastIndex)
            lastErrorMessage = null

            connect {
                val current = controller ?: return@connect
                queue = allowedTracks
                current.setMediaItems(allowedTracks.map(MediaItemMapper::toMediaItem), safeIndex, 0L)
                current.prepare()
                if (playWhenReady) {
                    current.play()
                }
                publishState()
            }
        }
    }

    fun cycleRepeatMode() {
        assertMainThread()
        val current = controller ?: return
        current.repeatMode = when (current.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        publishState()
    }

    fun toggleShuffle() {
        assertMainThread()
        controller?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
        publishState()
    }

    fun syncProgress() {
        assertMainThread()
        publishState()
    }

    fun disconnect() {
        assertMainThread()

        connectGeneration++

        controller?.let {
            it.removeListener(playerListener)
            it.release()
        }
        controller = null
        queue = emptyList()
        lastErrorMessage = null
        queueJob?.cancel()
        queueJob = null
        scope.cancel()

        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
        _state.value = PlaybackState()
    }

    fun isPlaying(): Boolean {
        assertMainThread()
        return controller?.isPlaying == true
    }

    fun positionMs(): Long {
        assertMainThread()
        return controller?.currentPosition ?: 0L
    }

    fun durationMs(): Long {
        assertMainThread()
        val duration = controller?.duration ?: 0L
        return if (duration == C.TIME_UNSET) 0L else duration
    }

    fun clearErrorMessage() {
        assertMainThread()
        if (lastErrorMessage != null) {
            lastErrorMessage = null
            publishState()
        }
    }

    private fun publishState() {
        val current = controller
        if (current == null) {
            _state.value = PlaybackState()
            return
        }

        val repeatMode = when (current.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }

        val index = current.currentMediaItemIndex
        val activeTrack = queue.getOrNull(index)
            ?: current.currentMediaItem?.let(MediaItemMapper::toTrack)

        _state.value = PlaybackState(
            isConnected = true,
            isPlaying = current.isPlaying,
            currentTrack = activeTrack,
            queue = queue,
            currentIndex = index,
            positionMs = current.currentPosition.coerceAtLeast(0L),
            durationMs = if (current.duration == C.TIME_UNSET) 0L else current.duration,
            repeatMode = repeatMode,
            shuffleEnabled = current.shuffleModeEnabled,
            errorMessage = lastErrorMessage
        )
    }

    companion object {
        private val TRUSTED_DRIVE_HOSTS = setOf(
            "drive.google.com",
            "www.drive.google.com"
        )

        private const val SAMPLE_URL =
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    }

    private fun isAllowedPlaybackTrack(track: Track): Boolean {
        val uri = runCatching { Uri.parse(track.uri) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase().orEmpty()

        return when (track.source) {
            TrackSource.DRIVE -> {
                (scheme == "https" && host in TRUSTED_DRIVE_HOSTS) ||
                    (scheme == "file" && isTrustedOfflineFile(uri))
            }
            TrackSource.LOCAL -> scheme in setOf("content", "file", "https")
        }
    }

    private fun isTrustedOfflineFile(uri: Uri): Boolean {
        val path = uri.path ?: return false
        val trustedRoot = offlineRootPath?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            val canonical = java.io.File(path).canonicalPath
            canonical.startsWith(trustedRoot)
        }.getOrDefault(false)
    }
}
