package com.slowly.music

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.*
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object MusicCache {
    const val LIMIT = 5L * 1024 * 1024 * 1024
    @Volatile private var instance: SimpleCache? = null
    @Synchronized fun get(context: Context): SimpleCache = instance ?: SimpleCache(
        File(context.applicationContext.cacheDir,"audio"), LeastRecentlyUsedCacheEvictor(LIMIT),
        StandaloneDatabaseProvider(context.applicationContext)
    ).also { instance=it }
    fun clear(context: Context) { val cache=get(context); cache.keys.toList().forEach { cache.removeResource(it) } }
}
