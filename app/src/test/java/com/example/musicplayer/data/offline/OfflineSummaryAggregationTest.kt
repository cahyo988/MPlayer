package com.example.musicplayer.data.offline

import com.example.musicplayer.data.offline.model.OfflineSourceStatus
import com.example.musicplayer.data.offline.model.computeSourceSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineSummaryAggregationTest {

    @Test
    fun completedWhenAllTracksDownloaded() {
        val summary = computeSourceSummary(
            sourceId = 1L,
            totalTracks = 3,
            downloadedTracks = 3,
            failedTracks = 0,
            downloadingTracks = 0,
            queuedTracks = 0
        )

        assertEquals(OfflineSourceStatus.COMPLETED, summary.status)
        assertEquals(100, summary.progressPercent)
    }

    @Test
    fun partialWhenMixedResults() {
        val summary = computeSourceSummary(
            sourceId = 1L,
            totalTracks = 4,
            downloadedTracks = 2,
            failedTracks = 1,
            downloadingTracks = 0,
            queuedTracks = 0
        )

        assertEquals(OfflineSourceStatus.PARTIAL, summary.status)
        assertEquals(50, summary.progressPercent)
    }
}
