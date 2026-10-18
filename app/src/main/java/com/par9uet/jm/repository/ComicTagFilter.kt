package com.par9uet.jm.repository

import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.utils.isBlockedByTags
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class CachedComicTags(
    val tagList: List<String>,
    val roleList: List<String>,
    val workList: List<String>,
    val updatedAt: Long,
) {
    fun applyTo(comic: Comic): Comic = comic.copy(
        tagList = (comic.tagList + tagList).distinct(),
        roleList = (comic.roleList + roleList).distinct(),
        workList = (comic.workList + workList).distinct(),
    )
}

interface ComicTagCache {
    suspend fun read(): Map<String, CachedComicTags>
    suspend fun write(entries: Map<String, CachedComicTags>)
}

data class ComicTagFilterResult(
    val comics: List<Comic>,
    val unresolvedCount: Int = 0,
)

data class ComicTagFilterProgress(
    val result: ComicTagFilterResult,
    val checkedCount: Int,
)

/** 列表标签不完整，需通过详情确认。 */
class ComicTagFilter(
    private val fetchDetail: suspend (Int) -> NetWorkResult<ComicDetailResponse>,
    private val storage: ComicTagCache? = null,
) {
    private val semaphore = Semaphore(6)
    private val cacheMutex = Mutex()
    private val cache = LinkedHashMap<String, CachedComicTags>()
    private var cacheLoaded = false
    private var cacheVersion = 0L
    private var persistedVersion = 0L

    suspend fun filter(
        candidates: List<Comic>,
        excludedTags: List<String>,
        source: String,
        onProgress: suspend (ComicTagFilterProgress) -> Unit = {},
    ): ComicTagFilterResult = withContext(Dispatchers.IO) {
        if (excludedTags.all { it.isBlank() } || candidates.isEmpty()) {
            return@withContext ComicTagFilterResult(candidates)
        }
        cacheMutex.withLock {
            if (!cacheLoaded) {
                cache.putAll(storage?.read().orEmpty().filterValues(::isFresh))
                cacheLoaded = true
            }
        }
        val allowed = mutableMapOf<Int, Comic>()
        var unresolvedCount = 0
        var checkedCount = 0
        val uniqueCandidates = candidates.distinctBy { it.id }
        val cachedTags = cacheMutex.withLock {
            uniqueCandidates.associate { comic ->
                comic.id to cache["$source:${comic.id}"]?.takeIf(::isFresh)
            }
        }
        val pending = mutableListOf<Comic>()
        // 优先显示缓存结果。
        uniqueCandidates.forEach { comic ->
            val tags = cachedTags[comic.id]
            when {
                comic.isBlockedByTags(excludedTags) -> checkedCount++
                tags != null -> {
                    val complete = tags.applyTo(comic)
                    if (!complete.isBlockedByTags(excludedTags)) allowed[comic.id] = complete
                    checkedCount++
                }
                else -> pending += comic
            }
        }

        fun result() = ComicTagFilterResult(candidates.mapNotNull { allowed[it.id] }, unresolvedCount)

        suspend fun publish() {
            onProgress(ComicTagFilterProgress(result(), checkedCount))
        }

        try {
            publish()
            coroutineScope {
                val tasks = Channel<Comic>(capacity = 6)
                val results = Channel<Pair<Int, Resolution>>(capacity = 6)
                launch {
                    try {
                        pending.forEach { tasks.send(it) }
                    } finally {
                        tasks.close()
                    }
                }
                repeat(minOf(6, pending.size)) {
                    launch {
                        try {
                            for (comic in tasks) {
                                val tags = resolveTags(comic.id, source)
                                val resolution = if (tags == null) {
                                    Resolution(null, unresolved = true)
                                } else {
                                    val complete = tags.applyTo(comic)
                                    Resolution(complete.takeUnless { it.isBlockedByTags(excludedTags) })
                                }
                                results.send(comic.id to resolution)
                            }
                        } catch (error: CancellationException) {
                            // 子任务自行取消时一并终止收集，避免等待未返回的结果。
                            this@coroutineScope.cancel(error)
                            throw error
                        }
                    }
                }
                repeat(pending.size) { index ->
                    val (id, resolution) = results.receive()
                    if (resolution.unresolved) unresolvedCount++
                    resolution.comic?.let { allowed[id] = it }
                    checkedCount++
                    publish()
                    if ((index + 1) % 6 == 0) persistCache()
                }
            }
            result()
        } finally {
            // 切换分类时保存已完成的检查。
            withContext(NonCancellable) { persistCache() }
        }
    }

    private suspend fun persistCache() {
        cacheMutex.withLock {
            if (cacheVersion == persistedVersion) return@withLock
            val retained = cache.entries.asSequence()
                .filter { isFresh(it.value) }
                .sortedByDescending { it.value.updatedAt }
                .take(1500)
                .associate { it.key to it.value }
            cache.keys.retainAll(retained.keys)
            storage?.write(retained)
            persistedVersion = cacheVersion
        }
    }

    private suspend fun resolveTags(id: Int, source: String): CachedComicTags? = semaphore.withPermit {
        val key = "$source:$id"
        cacheMutex.withLock { cache[key]?.takeIf(::isFresh) }?.let { return@withPermit it }
        val detail = try {
            fetchDetail(id)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return@withPermit null
        }
        currentCoroutineContext().ensureActive()
        if (detail !is NetWorkResult.Success || detail.data.id != id || detail.data.name.isBlank()) {
            return@withPermit null
        }
        val tags = CachedComicTags(
            tagList = detail.data.tags + detail.data.hidden_tags.orEmpty(),
            roleList = detail.data.actors,
            workList = detail.data.works,
            updatedAt = System.currentTimeMillis(),
        )
        cacheMutex.withLock {
            cache[key] = tags
            cacheVersion++
        }
        tags
    }

    private fun isFresh(entry: CachedComicTags): Boolean =
        System.currentTimeMillis() - entry.updatedAt in 0..(30L * 24 * 60 * 60 * 1000)

    private data class Resolution(val comic: Comic?, val unresolved: Boolean = false)
}
