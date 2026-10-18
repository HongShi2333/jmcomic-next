package com.par9uet.jm.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jukomu.jmcomic.api.enums.SearchMainTag
import io.github.jukomu.jmcomic.api.model.FavoriteQuery
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta
import io.github.jukomu.jmcomic.api.model.SearchQuery
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.random.Random

/**
 * 基于 JM 漫画标签偏好的简易推荐器。
 *
 * 从 Java 版 SimpleRecommender 移植而来，适配 Android 平台：
 * - CompletableFuture → Kotlin 协程 (async/awaitAll)
 * - java.nio.file → java.io.File
 * - SLF4J Logger → android.util.Log
 * - JSON 序列化使用 Gson
 */
object SimpleRecommender {

    private const val TAG = "SimpleRecommender"
    private val GSON = Gson()
    private const val CACHE_FILE_NAME = ".jmcomic_preferences.json"
    private const val MAX_SAMPLE = 30
    private const val MAX_CANDIDATE_ENRICH = 80
    private const val GET_ALBUM_TIMEOUT_MS = 30_000L

    /** 用户偏好数据，包含标签频率与已收藏 ID 集合。 */
    data class PreferenceData(
        val tagFreq: MutableMap<String, Int> = mutableMapOf(),
        val favoritedIds: MutableSet<String> = mutableSetOf()
    )

    /** 带评分的推荐结果。 */
    data class ScoredAlbum(
        val album: JmAlbumMeta,
        val score: Double,
        val tags: List<String>
    )

    private data class AlbumTags(
        val id: String,
        val tags: List<String>
    )

    /**
     * 生成推荐列表。
     *
     * @param client     JmApiClient 实例
     * @param maxResult  最多返回的推荐数量
     * @param cacheDir   偏好缓存文件存放目录（通常为应用缓存目录）
     * @return 按评分降序排列的推荐列表
     */
    suspend fun recommend(
        client: JmApiClient,
        maxResult: Int,
        cacheDir: File
    ): List<ScoredAlbum> {
        val pref = loadOrExtractPreferences(client, cacheDir)
        val candidates = fetchCandidates(client, pref)
        val deduped = deduplicateAndExclude(candidates, pref.favoritedIds)
        val enrichLimit = minOf(deduped.size, MAX_CANDIDATE_ENRICH)
        val toEnrich = deduped.subList(0, enrichLimit)
        val candidateIds = toEnrich.map { it.id }
        val tagMap = parallelGetAlbumTags(client, candidateIds)

        val scored = mutableListOf<ScoredAlbum>()
        for (meta in toEnrich) {
            val tags = tagMap[meta.id]
            if (tags != null && tags.isNotEmpty()) {
                val score = computeTagScore(tags, pref.tagFreq)
                if (score > 0) {
                    scored.add(ScoredAlbum(meta, score, tags))
                }
            }
        }
        scored.sortByDescending { it.score }
        return scored.take(maxResult)
    }

    /** 抓取候选本子：基于偏好标签搜索 + 随机推荐 + 最新上架。 */
    private suspend fun fetchCandidates(
        client: JmApiClient,
        pref: PreferenceData
    ): List<JmAlbumMeta> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<JmAlbumMeta>()

        val topTags = topTagEntries(pref.tagFreq, 20)
        if (topTags.isNotEmpty()) {
            val pickedTags = weightedRandomPick(topTags, 4, 6)
            for (tag in pickedTags) {
                val page = 1 + Random.nextInt(5)
                try {
                    val result = client.search(
                        SearchQuery.Builder()
                            .text(tag)
                            .mainTag(SearchMainTag.TAG)
                            .page(page)
                            .build()
                    )
                    candidates.addAll(result.content)
                } catch (e: Exception) {
                    Log.w(TAG, "tag search failed: ${e.message}")
                }
            }
        }

        try {
            val randoms = client.getRandomRecommend()
            if (randoms != null) candidates.addAll(randoms)
        } catch (e: Exception) {
            Log.w(TAG, "random recommend failed: ${e.message}")
        }

        try {
            val latestPage = 1 + Random.nextInt(10)
            val latest = client.getLatest(latestPage)
            candidates.addAll(latest.content)
        } catch (e: Exception) {
            Log.w(TAG, "latest failed: ${e.message}")
        }

        candidates
    }

    /** 去重并排除已收藏的本子，保留首次出现的顺序。 */
    private fun deduplicateAndExclude(
        candidates: List<JmAlbumMeta>,
        favoritedIds: Set<String>
    ): List<JmAlbumMeta> {
        val map = LinkedHashMap<String, JmAlbumMeta>()
        for (a in candidates) {
            val id = a.id
            if (id in favoritedIds) continue
            if (!map.containsKey(id)) {
                map[id] = a
            }
        }
        return map.values.toList()
    }

    /**
     * 计算候选标签与偏好标签的匹配得分。
     *
     * @param candidateTags 候选本子的标签列表
     * @param tagFreq       用户偏好标签频率表
     * @return 匹配权重占比 [0, 1]，偏好为空时返回 1.0
     */
    fun computeTagScore(candidateTags: List<String>, tagFreq: Map<String, Int>): Double {
        if (tagFreq.isEmpty()) return 1.0
        val totalWeight = tagFreq.values.sum()
        val matched = candidateTags.sumOf { tagFreq.getOrDefault(it, 0) }
        return matched.toDouble() / totalWeight
    }

    /** 并发获取多个本子的标签信息，使用协程实现并行。 */
    private suspend fun parallelGetAlbumTags(
        client: JmApiClient,
        ids: List<String>
    ): Map<String, List<String>> = coroutineScope {
        val results = ids.map { id ->
            async(Dispatchers.IO) {
                try {
                    val album = withTimeoutOrNull(GET_ALBUM_TIMEOUT_MS) {
                        client.getAlbum(id)
                    }
                    val tags = album?.tags
                    AlbumTags(id, tags ?: emptyList())
                } catch (e: Exception) {
                    Log.d(TAG, "getAlbum $id failed: ${e.message}")
                    AlbumTags(id, emptyList())
                }
            }
        }.awaitAll()

        val result = LinkedHashMap<String, List<String>>()
        for (at in results) {
            if (at.tags.isNotEmpty()) {
                result[at.id] = at.tags
            }
        }
        result
    }

    /** 加载缓存的偏好数据，缓存不存在时重新提取并保存。 */
    private suspend fun loadOrExtractPreferences(
        client: JmApiClient,
        cacheDir: File
    ): PreferenceData {
        val cached = loadFromFile(cacheDir)
        if (cached != null) return cached
        val fresh = extractPreferences(client)
        saveToFile(fresh, cacheDir)
        return fresh
    }

    /** 从收藏夹和收藏标签中提取用户偏好。 */
    private suspend fun extractPreferences(client: JmApiClient): PreferenceData =
        withContext(Dispatchers.IO) {
            val tagFreq = mutableMapOf<String, Int>()
            val favoritedIds = linkedSetOf<String>()
            val sampleIds = mutableListOf<String>()

            try {
                for (page in 1..5) {
                    val fav = client.getFavorites(
                        FavoriteQuery.Builder().folderId(0).page(page).build()
                    )
                    for (album in fav.content) {
                        favoritedIds.add(album.id)
                        if (sampleIds.size < MAX_SAMPLE) sampleIds.add(album.id)
                    }
                    if (page >= fav.totalPages) break
                }

                for (tf in client.getTagsFavorite()) {
                    tagFreq[tf.tag] = (tagFreq[tf.tag] ?: 0) + 3
                }

                val tagMap = parallelGetAlbumTags(client, sampleIds)
                for (tags in tagMap.values) {
                    for (tag in tags) {
                        tagFreq[tag] = (tagFreq[tag] ?: 0) + 1
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "preference extraction failed: ${e.message}")
            }

            PreferenceData(tagFreq, favoritedIds)
        }

    /** 从缓存文件加载偏好数据。 */
    private fun loadFromFile(cacheDir: File): PreferenceData? {
        return try {
            val file = File(cacheDir, CACHE_FILE_NAME)
            if (!file.exists()) return null
            val json = file.readText()
            val type = object : TypeToken<PreferenceData>() {}.type
            GSON.fromJson(json, type)
        } catch (e: Exception) {
            Log.w(TAG, "load cache failed: ${e.message}")
            null
        }
    }

    /** 保存偏好数据到缓存文件。 */
    private fun saveToFile(pref: PreferenceData, cacheDir: File) {
        try {
            val file = File(cacheDir, CACHE_FILE_NAME)
            file.writeText(GSON.toJson(pref))
        } catch (e: Exception) {
            Log.w(TAG, "save cache failed: ${e.message}")
        }
    }

    /** 取频率最高的前 [limit] 个标签。 */
    private fun topTagEntries(tagFreq: Map<String, Int>, limit: Int): List<Pair<String, Int>> {
        return tagFreq.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    /**
     * 按权重随机选取标签。
     *
     * @param entries 候选标签及其权重
     * @param min     最少选取数量
     * @param max     最多选取数量
     * @return 选中的标签列表
     */
    private fun weightedRandomPick(
        entries: List<Pair<String, Int>>,
        min: Int,
        max: Int
    ): List<String> {
        if (entries.isEmpty()) return emptyList()
        val count = min + Random.nextInt(max - min + 1)
        val actualCount = minOf(count, entries.size)
        val result = mutableListOf<String>()
        val pool = entries.toMutableList()

        repeat(actualCount) {
            if (pool.isEmpty()) return@repeat
            val totalWeight = pool.sumOf { it.second }
            if (totalWeight <= 0) return@repeat
            var r = Random.nextInt(totalWeight)
            var pickedIndex = 0
            for (j in pool.indices) {
                r -= pool[j].second
                if (r < 0) {
                    pickedIndex = j
                    break
                }
            }
            result.add(pool[pickedIndex].first)
            pool.removeAt(pickedIndex)
        }
        return result
    }
}
