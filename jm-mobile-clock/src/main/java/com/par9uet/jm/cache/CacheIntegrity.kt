package com.par9uet.jm.cache

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.google.gson.Gson
import com.par9uet.jm.data.models.CACHE_INTEGRITY_CHECK_FULL
import com.par9uet.jm.data.models.CACHE_INTEGRITY_CHECK_OFF
import com.par9uet.jm.data.models.CACHE_INTEGRITY_CHECK_PARTIAL
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.utils.log

data class CacheIntegrityResult(
    val brokenChapterIds: Set<Int> = emptySet(),
    val reason: String = "",
    val missingCover: Boolean = false,
    val missingConfig: Boolean = false,
    val chapterIds: Set<Int> = emptySet(),
    /** Human-readable page numbers (1-based) missing from each broken chapter. */
    val missingPagesByChapter: Map<Int, List<Int>> = emptyMap(),
) {
    val isHealthy: Boolean get() = brokenChapterIds.isEmpty() && !missingCover && !missingConfig
}

fun checkComicCacheIntegrity(
    context: Context,
    chapters: List<DownloadComic>,
    mode: String,
    gson: Gson = Gson(),
): CacheIntegrityResult {
    if (mode == CACHE_INTEGRITY_CHECK_OFF || chapters.isEmpty()) return CacheIntegrityResult()
    val completed = chapters.filter { it.status == "complete" }
    if (completed.isEmpty()) return CacheIntegrityResult()
    if (getDownloadTreeUri(context) == null) {
        completed.asSequence()
            .flatMap { sequenceOf(it.zipPath, it.coverPath) }
            .mapNotNull(::getTreeUriForCachePath)
            .firstOrNull()
            ?.let { setDownloadTreeUri(context, it.toString()) }
    }
    // The cover belongs to the comic root while pages live in chapter folders.
    // Prefer the persisted paths first: old versions used several directory
    // naming schemes, so reconstructing a root from the current title can point
    // at a newly-created empty folder and falsely report missing metadata.
    val persistedRoots = completed.asSequence()
        .flatMap { sequenceOf(it.coverPath, it.zipPath) }
        .filter(String::isNotBlank)
        .mapNotNull(::getCacheParentPath)
        .distinct()
        .toList()
    val fallbackRoot = getComicDownloadRootPath(context, completed.first())
    val rootCandidates = (persistedRoots + fallbackRoot).distinct()
    val rootPath = rootCandidates.firstOrNull { candidate ->
        val config = findCacheChildPath(context, candidate, "config.json")
        val cover = findCacheChildPath(context, candidate, "cover.webp")
        config != null && cover != null &&
            cachePathHasContent(context, config) && cachePathHasContent(context, cover)
    } ?: rootCandidates.firstOrNull { candidate ->
        findCacheChildPath(context, candidate, "config.json") != null ||
            findCacheChildPath(context, candidate, "cover.webp") != null
    } ?: fallbackRoot

    val configPath = findCacheChildPath(context, rootPath, "config.json")
    val coverPath = findCacheChildPath(context, rootPath, "cover.webp")
    val configMissing = configPath == null || !cachePathHasContent(context, configPath)
    val coverMissing = coverPath == null || !cachePathHasContent(context, coverPath)
    if (configMissing || coverMissing) {
        return CacheIntegrityResult(
            reason = listOfNotNull(
                "config.json 缺失或为空".takeIf { configMissing },
                "封面文件缺失或为空".takeIf { coverMissing },
            ).joinToString("；"),
            missingCover = coverMissing,
            missingConfig = configMissing,
            chapterIds = completed.mapTo(mutableSetOf()) { it.id },
        )
    }

    val config = runCatching {
        openCacheInputStream(context, configPath)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            gson.fromJson(reader, DownloadComicCacheConfig::class.java)
        }
    }.getOrNull()
        ?: return CacheIntegrityResult(
            reason = "config.json 无法读取",
            missingConfig = true,
            chapterIds = completed.mapTo(mutableSetOf()) { it.id },
        )
    // Gson can deserialize an old or hand-edited config with a null chapter list.
    // Treat it as a missing manifest so it is repaired instead of crashing or passing.
    val configChapters = runCatching { config.chapters }.getOrNull()
        ?: return CacheIntegrityResult(
            reason = "config.json 缺少章节记录",
            missingConfig = true,
            chapterIds = completed.mapTo(mutableSetOf()) { it.id },
        )
    val expectedGroupId = completed.first().groupId.takeIf { it != 0 } ?: completed.first().id
    val manifestIds = configChapters.map { it.id }.toSet()
    // A config can be written while another chapter is still downloading, so
    // its chapterCount may be larger than the completed rows visible here. The
    // reliable invariant is that every completed chapter has a manifest entry.
    if (configChapters.isEmpty() ||
        config.id != expectedGroupId ||
        completed.any { it.id !in manifestIds }
    ) {
        return CacheIntegrityResult(
            reason = "config.json 与当前漫画缓存不匹配",
            missingConfig = true,
            chapterIds = completed.mapTo(mutableSetOf()) { it.id },
        )
    }
    if (mode == CACHE_INTEGRITY_CHECK_PARTIAL) return CacheIntegrityResult()
    if (mode != CACHE_INTEGRITY_CHECK_FULL) return CacheIntegrityResult()

    val expectedByChapterId = configChapters.associateBy { it.id }
    val missingPagesByChapter = linkedMapOf<Int, List<Int>>()
    val broken = completed.filterTo(mutableSetOf()) { chapter ->
        val expectedChapter = expectedByChapterId[chapter.id] ?: return@filterTo true
        // The manifest stores the exact path written during download/migration.
        // Prefer it over reconstructing a folder name, especially for a first
        // chapter whose server title may be the album title rather than "第1章".
        val resolvedPath = resolveIntegrityChapterPath(context, chapter, expectedChapter.path)
        val images = listComicImageEntries(context, resolvedPath)
        val expected = expectedChapter.imageCount.takeIf { it > 0 }
            ?: expectedChapter.imageFiles.size.takeIf { it > 0 }
            ?: images.maxOfOrNull { it.name.substringBeforeLast('.').toIntOrNull() ?: -1 }
                ?.plus(1)
                ?: 0
        if (expected <= 0 || images.size != expected) {
            val actualPages = images.mapNotNull {
                it.name.substringBeforeLast('.').toIntOrNull()
            }.toSet()
            val missingPages = if (expected > 0) {
                ((0 until expected).toSet() - actualPages)
                    .sorted()
                    .map { it + 1 }
            } else {
                emptyList()
            }
            if (missingPages.isNotEmpty()) missingPagesByChapter[chapter.id] = missingPages
            return@filterTo true
        }
        val actualNames = images.map(CacheImageEntry::name)
        val actualPages = actualNames.mapNotNull { it.substringBeforeLast('.').toIntOrNull() }
        // Cached pages are zero-based (0.webp..33.webp for 34 pages). Compare
        // the numeric set, not the raw names or list positions, so extension
        // changes and SAF cursor ordering do not create false corruption alerts.
        val missingPages = ((0 until expected).toSet() - actualPages.toSet())
            .sorted()
            .map { it + 1 }
        val brokenPages = actualPages.size != actualNames.size || missingPages.isNotEmpty()
        if (missingPages.isNotEmpty()) missingPagesByChapter[chapter.id] = missingPages
        if (brokenPages) {
            log("CacheIntegrity", "章节 ${chapter.id} 检查失败：path=$resolvedPath expected=$expected actual=${actualNames.joinToString()}")
        }
        brokenPages
    }.mapTo(mutableSetOf()) { it.id }
    return if (broken.isEmpty()) CacheIntegrityResult()
    else {
        val detail = missingPagesByChapter.entries.joinToString("；") { (chapterId, pages) ->
            "章节 $chapterId 缺少第 ${pages.joinToString("、")} 页"
        }
        CacheIntegrityResult(
            brokenChapterIds = broken,
            reason = detail.ifBlank { "漫画图片存在缺页、少页或页号异常" },
            missingPagesByChapter = missingPagesByChapter,
        )
    }
}

/**
 * Resolve chapter pages against the active storage tree. A DAO row can still
 * contain the old default path after a migration, so using zipPath blindly
 * makes a healthy custom cache look incomplete (and makes readers open the
 * wrong copy). Legacy directory names are resolved inside the active tree.
 */
private fun resolveIntegrityChapterPath(
    context: Context,
    chapter: DownloadComic,
    manifestPath: String,
): String {
    val activeTree = getDownloadTreeUri(context)
    val candidatePaths = listOf(chapter.zipPath, manifestPath)
        .filter { it.isNotBlank() }
        .distinct()
    val currentCandidate = runCatching {
        findExistingComicChapterDownloadPath(context, chapter)
            ?: getComicChapterDownloadPath(context, chapter)
    }.getOrNull()
    val allCandidates = (candidatePaths + listOfNotNull(currentCandidate)).distinct()
    if (activeTree == null) {
        return allCandidates.firstOrNull { listComicImageEntries(context, it).isNotEmpty() }
            ?: allCandidates.firstOrNull().orEmpty()
    }
    val activeCandidate = allCandidates.firstOrNull { path ->
        val tree = getTreeUriForCachePath(path)
        tree != null && sameCacheTree(tree, activeTree) &&
            listComicImageEntries(context, path).isNotEmpty()
    }
    return activeCandidate ?: currentCandidate ?: manifestPath
}

private fun sameCacheTree(first: Uri?, second: Uri?): Boolean = runCatching {
    first != null && second != null &&
        first.authority == second.authority &&
        DocumentsContract.getTreeDocumentId(first) == DocumentsContract.getTreeDocumentId(second)
}.getOrDefault(false)
