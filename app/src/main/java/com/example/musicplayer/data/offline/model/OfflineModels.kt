package com.example.musicplayer.data.offline.model

enum class OfflineTrackStatus {
    NOT_DOWNLOADED,
    QUEUED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

enum class OfflineSourceStatus {
    NOT_STARTED,
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    PARTIAL,
    FAILED
}

data class SourceOfflineSummary(
    val sourceId: Long,
    val status: OfflineSourceStatus,
    val totalTracks: Int,
    val downloadedTracks: Int,
    val failedTracks: Int,
    val downloadingTracks: Int,
    val queuedTracks: Int,
    val progressPercent: Int
)

fun computeSourceSummary(
    sourceId: Long,
    totalTracks: Int,
    downloadedTracks: Int,
    failedTracks: Int,
    downloadingTracks: Int,
    queuedTracks: Int
): SourceOfflineSummary {
    val safeTotal = totalTracks.coerceAtLeast(0)
    val status = when {
        safeTotal == 0 -> OfflineSourceStatus.NOT_STARTED
        downloadedTracks >= safeTotal -> OfflineSourceStatus.COMPLETED
        downloadingTracks > 0 -> OfflineSourceStatus.DOWNLOADING
        queuedTracks > 0 -> OfflineSourceStatus.QUEUED
        downloadedTracks > 0 && failedTracks > 0 -> OfflineSourceStatus.PARTIAL
        downloadedTracks > 0 -> OfflineSourceStatus.PARTIAL
        failedTracks > 0 -> OfflineSourceStatus.FAILED
        else -> OfflineSourceStatus.NOT_STARTED
    }
    val progress = if (safeTotal == 0) {
        0
    } else {
        ((downloadedTracks.coerceAtLeast(0) * 100f) / safeTotal)
            .toInt()
            .coerceIn(0, 100)
    }

    return SourceOfflineSummary(
        sourceId = sourceId,
        status = status,
        totalTracks = safeTotal,
        downloadedTracks = downloadedTracks.coerceAtLeast(0),
        failedTracks = failedTracks.coerceAtLeast(0),
        downloadingTracks = downloadingTracks.coerceAtLeast(0),
        queuedTracks = queuedTracks.coerceAtLeast(0),
        progressPercent = progress
    )
}
