package com.example.musicplayer.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

object PlayerFactory {
    @Volatile
    private var cache: SimpleCache? = null

    fun create(context: Context): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(false)

        val upstreamFactory = DefaultDataSource.Factory(context, httpFactory)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(getOrCreateCache(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                playWhenReady = false
            }
    }

    @Synchronized
    private fun getOrCreateCache(context: Context): SimpleCache {
        return cache ?: SimpleCache(
            File(context.cacheDir, "media3-drive-cache"),
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES),
            StandaloneDatabaseProvider(context)
        ).also { cache = it }
    }

    @Synchronized
    fun releaseCache() {
        cache?.release()
        cache = null
    }

    private const val MAX_CACHE_SIZE_BYTES = 100L * 1024L * 1024L
}
