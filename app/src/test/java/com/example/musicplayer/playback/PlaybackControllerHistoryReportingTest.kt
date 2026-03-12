package com.example.musicplayer.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerHistoryReportingTest {

    @Test
    fun shouldReportPlayedTrackWhenPlayingPastThresholdAndNewTrack() {
        val shouldReport = PlaybackController.shouldReportPlayedTrack(
            activeTrackId = "track-1",
            isPlaying = true,
            positionMs = 1_500L,
            lastReportedTrackId = null
        )

        assertTrue(shouldReport)
    }

    @Test
    fun shouldNotReportWhenNotPlayingOrBeforeThresholdOrSameTrack() {
        assertFalse(
            PlaybackController.shouldReportPlayedTrack(
                activeTrackId = "track-1",
                isPlaying = false,
                positionMs = 5_000L,
                lastReportedTrackId = null
            )
        )
        assertFalse(
            PlaybackController.shouldReportPlayedTrack(
                activeTrackId = "track-1",
                isPlaying = true,
                positionMs = 500L,
                lastReportedTrackId = null
            )
        )
        assertFalse(
            PlaybackController.shouldReportPlayedTrack(
                activeTrackId = "track-1",
                isPlaying = true,
                positionMs = 5_000L,
                lastReportedTrackId = "track-1"
            )
        )
    }
}
