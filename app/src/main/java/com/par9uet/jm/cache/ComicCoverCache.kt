package com.par9uet.jm.cache

import coil.request.CachePolicy
import coil.request.ImageRequest
import com.par9uet.jm.data.models.COVER_CACHE_MAX_DURATION_HOURS

private const val HOUR_MILLIS = 60 * 60 * 1000L

/**
 * A time-windowed key gives Coil a predictable TTL without serving expired covers.
 * Old keys remain subject to Coil's normal LRU eviction.
 */
fun comicCoverCacheKey(
    comicId: Int,
    durationHours: Int,
    nowMillis: Long = System.currentTimeMillis(),
): String? {
    val normalizedDuration = durationHours.coerceIn(0, COVER_CACHE_MAX_DURATION_HOURS)
    if (normalizedDuration == 0) return null
    val bucket = nowMillis / (normalizedDuration * HOUR_MILLIS)
    return "comic-cover:$comicId:$bucket"
}

fun ImageRequest.Builder.applyComicCoverCache(cacheKey: String?): ImageRequest.Builder {
    return if (cacheKey == null) {
        memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
    } else {
        memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
    }
}
