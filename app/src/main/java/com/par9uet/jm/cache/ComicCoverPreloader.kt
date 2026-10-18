package com.par9uet.jm.cache

import android.content.Context
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.network.ComicCoverUrlResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val MAX_PRELOAD_COVERS = 40
private const val COVER_PRELOAD_CONCURRENCY = 2
private const val COVER_PRELOAD_TIMEOUT_MS = 12_000L
// Shared with the startup favorites warm-up: multiple screens cannot multiply concurrency.
private val coverPreloadSlots = Semaphore(COVER_PRELOAD_CONCURRENCY)

internal fun comicCoverPreloadCandidates(comics: List<Comic>, durationHours: Int): List<Comic> {
    if (durationHours <= 0) return emptyList()
    return comics.asSequence()
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .take(MAX_PRELOAD_COVERS)
        .toList()
}

/** Use the normal cover cache, without evicting visible covers from the memory cache. */
suspend fun preloadComicCovers(
    context: Context,
    imageLoader: ImageLoader,
    comics: List<Comic>,
    durationHours: Int,
    configuredImageHost: String = "",
) {
    val candidates = comicCoverPreloadCandidates(comics, durationHours)
    if (candidates.isEmpty()) return
    withContext(Dispatchers.IO) {
        withTimeoutOrNull(COVER_PRELOAD_TIMEOUT_MS) {
            candidates.chunked(COVER_PRELOAD_CONCURRENCY).forEach { batch ->
                coroutineScope {
                    batch.map { comic ->
                        async {
                            coverPreloadSlots.withPermit {
                                currentCoroutineContext().ensureActive()
                                val url = ComicCoverUrlResolver.resolve(
                                    comic.id, comic.coverUrl, configuredImageHost,
                                ).firstOrNull() ?: return@withPermit
                                try {
                                    imageLoader.execute(
                                        ImageRequest.Builder(context.applicationContext)
                                            .data(url)
                                            .size(256, 342)
                                            .applyComicCoverCache(comicCoverCacheKey(comic.id, durationHours))
                                            .memoryCachePolicy(CachePolicy.READ_ONLY)
                                            .build(),
                                    )
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (_: Exception) {
                                    // A single failed cover must not stop the rest or show a dialog.
                                }
                            }
                        }
                    }.awaitAll()
                }
            }
        }
    }
}
