package com.par9uet.jm.store

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.workDataOf
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.worker.DownloadComicWorker
import com.par9uet.jm.cache.cachePathExists
import com.par9uet.jm.cache.deleteCachePath
import com.par9uet.jm.cache.listComicImagePaths
import com.par9uet.jm.cache.CacheIntegrityResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import com.par9uet.jm.cache.WatchCacheJobs
import com.par9uet.jm.cache.watchCacheWorkName
import com.par9uet.jm.cache.checkComicCacheIntegrity
import com.par9uet.jm.data.models.CACHE_INTEGRITY_CHECK_FULL
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DOWNLOAD_RETRY_BACKOFF_SECONDS = 30L

class DownloadManager(
    private val context: Context,
    private val downloadComicDao: DownloadComicDao,
    private val scope: CoroutineScope,
    private val toastManager: ToastManager,
) {
    fun downloadComic(comic: Comic) {
        scope.launch(Dispatchers.IO) { WatchCacheJobs.mutations.withLock {
            if (downloadComicDao.getExistingIds(listOf(comic.id)).isNotEmpty()) {
                toastManager.showAsync("该漫画已在缓存列表中")
                return@withLock
            }
            insertComicTask(comic)
            toastManager.showAsync("创建缓存任务成功")
            enqueueDownload(comic.id)
        } }
    }

    fun downloadComics(comics: List<Comic>) {
        if (comics.isEmpty()) return
        scope.launch(Dispatchers.IO) { WatchCacheJobs.mutations.withLock {
            val existingIds = downloadComicDao.getExistingIds(comics.map { it.id }).toSet()
            val newComics = comics.filterNot { it.id in existingIds }
            if (newComics.isEmpty()) {
                toastManager.showAsync("所选漫画已在缓存列表中")
                return@withLock
            }

            newComics.forEach { insertComicTask(it) }
            enqueueDownloads(newComics.map { it.id })

            val skippedCount = comics.size - newComics.size
            toastManager.showAsync(
                if (skippedCount > 0) {
                    "已创建 ${newComics.size} 个缓存任务，跳过 $skippedCount 个已存在漫画"
                } else {
                    "已创建 ${newComics.size} 个缓存任务"
                }
            )
        } }
    }

    fun downloadChapters(parentComic: Comic, chapters: List<ComicChapter>) {
        if (chapters.isEmpty()) return
        scope.launch(Dispatchers.IO) { WatchCacheJobs.mutations.withLock {
            val existingIds = downloadComicDao.getExistingIds(chapters.map { it.id }).toSet()
            val newChapters = chapters.filterNot { it.id in existingIds }
            if (newChapters.isEmpty()) {
                toastManager.showAsync("所选章节已在缓存列表中")
                return@withLock
            }

            val now = System.currentTimeMillis()
            newChapters.forEachIndexed { index, chapter ->
                downloadComicDao.insert(
                    DownloadComic(
                        id = chapter.id,
                        name = "${parentComic.name} ${chapter.name}".trim(),
                        authorList = parentComic.authorList,
                        tagList = parentComic.tagList,
                        coverPath = "",
                        zipPath = "",
                        progress = 0f,
                        status = "pending",
                        createTime = now + index,
                        groupId = parentComic.id,
                        groupName = parentComic.name,
                        chapterName = chapter.name
                    )
                )
            }
            enqueueDownloads(newChapters.map { it.id })

            val skippedCount = chapters.size - newChapters.size
            toastManager.showAsync(
                if (skippedCount > 0) {
                    "已创建 ${newChapters.size} 个缓存任务，跳过 $skippedCount 个已存在章节"
                } else {
                    "已创建 ${newChapters.size} 个缓存任务"
                }
            )
        } }
    }

    private suspend fun insertComicTask(comic: Comic) {
        downloadComicDao.insert(
            DownloadComic(
                id = comic.id,
                name = comic.name,
                authorList = comic.authorList,
                tagList = comic.tagList,
                coverPath = "",
                zipPath = "",
                progress = 0f,
                status = "pending",
                createTime = System.currentTimeMillis(),
                groupId = comic.id,
                groupName = comic.name
            )
        )
    }

    private fun enqueueDownload(comicId: Int) {
        enqueueDownloads(listOf(comicId))
    }

    private fun enqueueDownloads(
        comicIds: List<Int>,
        downloadCover: Boolean = true,
        downloadPages: Boolean = true,
        writeConfig: Boolean = true,
    ) {
        if (comicIds.isEmpty()) return
        val distinctComicIds = comicIds.distinct()
        val batchId = if (distinctComicIds.size > 1) UUID.randomUUID().toString() else ""
        val workManager = WorkManager.getInstance(context)
        distinctComicIds.forEach { comicId ->
            val downloadRequest = OneTimeWorkRequestBuilder<DownloadComicWorker>()
                .setInputData(
                    workDataOf(
                        "comicId" to comicId,
                        "batchId" to batchId,
                        "batchTotal" to distinctComicIds.size
                        ,"downloadCover" to downloadCover
                        ,"downloadPages" to downloadPages
                        ,"writeConfig" to writeConfig
                    )
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    DOWNLOAD_RETRY_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()
            workManager.enqueueUniqueWork(watchCacheWorkName(comicId), ExistingWorkPolicy.KEEP, downloadRequest)
        }
    }

    /**
     * Re-enqueue pending cache jobs without a WorkManager network constraint.
     *
     * Wear OS emulators and some watches report CONNECTED=false while an
     * ordinary HTTPS request is already usable. The old constrained work then
     * stays ENQUEUED forever and never reaches DownloadComicWorker. The worker
     * itself already has request timeouts/retry handling, so letting it start
     * is more reliable than delegating network detection to WorkManager.
     */
    fun requeuePendingDownloads() {
        scope.launch(Dispatchers.IO) { WatchCacheJobs.mutations.withLock {
            val pendingIds = downloadComicDao.getAll()
                .filter { it.status == "pending" }
                .map { it.id }
                .distinct()
            if (pendingIds.isEmpty()) return@launch

            val workManager = WorkManager.getInstance(context)
            pendingIds.forEach { id ->
                runCatching {
                    workManager.cancelUniqueWork(watchCacheWorkName(id)).result.get()
                }
            }
            enqueueDownloads(pendingIds)
        } }
    }

    fun retryDownload(comicId: Int) {
        scope.launch(Dispatchers.IO) {
            val task = downloadComicDao.getById(comicId) ?: return@launch
            downloadComicDao.updateProgress(
                com.par9uet.jm.database.model.UpdateComicProgress(comicId, 0f)
            )
            downloadComicDao.updateStatus(
                com.par9uet.jm.database.model.UpdateComicStatus(comicId, "pending")
            )
            enqueueDownload(comicId)
            toastManager.showAsync("已重新加入下载队列")
        }
    }

    fun pauseDownloads(ids: Collection<Int>) {
        scope.launch(Dispatchers.IO) {
            val active = ids.distinct().filter { downloadComicDao.getById(it)?.status in setOf("pending", "downloading") }
            if (active.isEmpty()) return@launch
            downloadComicDao.updateStatusByIds(active, "paused")
            val workManager = WorkManager.getInstance(context)
            active.forEach { workManager.cancelUniqueWork(watchCacheWorkName(it)).result.get() }
            toastManager.showAsync("已暂停 ${active.size} 个缓存任务")
        }
    }

    /**
     * 恢复已暂停的下载任务：更新状态为 pending 并重新入队 WorkManager。
     * 与 retryDownload 不同，不会重置已下载进度，而是从断点继续。
     */
    fun resumeDownloads(comicIds: List<Int>) {
        if (comicIds.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val validIds = comicIds.filter { id ->
                val task = downloadComicDao.getById(id)
                task != null && task.status != "complete"
            }.distinct()
            if (validIds.isEmpty()) {
                toastManager.showAsync("没有可恢复的下载任务")
                return@launch
            }
            downloadComicDao.updateStatusByIds(validIds, "pending")
            enqueueDownloads(validIds)
            toastManager.showAsync("已恢复 ${validIds.size} 个下载任务")
        }
    }

    fun retryGroup(groupId: Int) {
        scope.launch(Dispatchers.IO) {
            val chapters = downloadComicDao.getByGroupId(groupId)
            val errorIds = chapters.filter { it.status == "error" }.map { it.id }
            if (errorIds.isEmpty()) return@launch
            downloadComicDao.updateStatusByIds(errorIds, "pending")
            errorIds.forEach { id ->
                downloadComicDao.updateProgress(
                    com.par9uet.jm.database.model.UpdateComicProgress(id, 0f)
                )
            }
            enqueueDownloads(errorIds)
            toastManager.showAsync("已重新加入 ${errorIds.size} 个下载任务")
        }
    }

    suspend fun deleteCachedItems(ids: Collection<Int>): Int = withContext(Dispatchers.IO) {
        val selectedIds = ids.distinct()
        if (selectedIds.isEmpty()) return@withContext 0
        // Mark paused before cancelling so queued jobs also exit without writing anything.
        val running = selectedIds.filter { downloadComicDao.getById(it)?.status in setOf("pending", "downloading") }
        if (running.isNotEmpty()) downloadComicDao.updateStatusByIds(running, "paused")
        selectedIds.forEach { WorkManager.getInstance(context).cancelUniqueWork(watchCacheWorkName(it)) }
        val deletedCount = withTimeoutOrNull(15_000L) {
            WatchCacheJobs.writer.withPermit {
        val selected = mutableListOf<DownloadComic>()
        for (id in selectedIds) {
            downloadComicDao.getById(id)?.let(selected::add)
        }
        var deletedCount = 0
        selected.groupBy { it.groupId.takeIf { id -> id != 0 } ?: it.id }.forEach { (groupId, groupItems) ->
            val allGroupItems = downloadComicDao.getByGroupId(groupId)
            val deletingWholeGroup = allGroupItems.all { item -> item.id in selectedIds }
            val groupCoverPaths = groupItems.map { it.coverPath }.filter(String::isNotBlank).distinct()
            groupItems.forEach { item ->
                val zipDeleted = item.zipPath.isBlank() || !cachePathExists(context, item.zipPath) || deleteCachePath(context, item.zipPath)
                if (zipDeleted) {
                    downloadComicDao.delete(item)
                    deletedCount++
                }
            }
            if (deletingWholeGroup) {
                groupCoverPaths.forEach { path ->
                    if (cachePathExists(context, path)) deleteCachePath(context, path)
                }
            }
        }
        deletedCount
        }
        } ?: -1
        deletedCount
    }

    fun redownloadMissing(groupId: Int) {
        scope.launch(Dispatchers.IO) {
            val items = downloadComicDao.getByGroupId(groupId)
            val missing = items.filter { item ->
                item.status == "complete" && (
                    item.zipPath.isBlank() ||
                        !cachePathExists(context, item.zipPath) ||
                        runCatching { listComicImagePaths(context, item.zipPath).isEmpty() }.getOrDefault(true)
                    )
            }
            if (missing.isEmpty()) {
                toastManager.showAsync("本地缓存文件完整")
                return@launch
            }
            missing.forEach { item ->
                if (item.zipPath.isNotBlank() && cachePathExists(context, item.zipPath)) deleteCachePath(context, item.zipPath)
                downloadComicDao.updateStatus(com.par9uet.jm.database.model.UpdateComicStatus(item.id, "pending"))
                downloadComicDao.updateProgress(com.par9uet.jm.database.model.UpdateComicProgress(item.id, 0f))
            }
            enqueueDownloads(missing.map { it.id })
            toastManager.showAsync("已重新下载 ${missing.size} 个缺失章节")
        }
    }

    fun checkAndRepairGroup(groupId: Int) {
        scope.launch(Dispatchers.IO) {
            val result = WatchCacheJobs.writer.withPermit {
                checkComicCacheIntegrity(context, downloadComicDao.getCompleteByGroupId(groupId), CACHE_INTEGRITY_CHECK_FULL)
            }
            if (result.isHealthy) toastManager.showAsync("本地缓存文件完整")
            else repairCachedItems(result)
        }
    }

    fun repairCachedItems(result: CacheIntegrityResult) {
        val taskIds = (result.brokenChapterIds + result.chapterIds).distinct()
        if (taskIds.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val tasks = mutableListOf<DownloadComic>()
            for (id in taskIds) {
                downloadComicDao.getById(id)?.let(tasks::add)
            }
            if (tasks.isEmpty()) return@launch
            val needsPages = tasks.associate { it.id to (it.id in result.brokenChapterIds) }
            val owner = tasks.minByOrNull { it.createTime }?.id
            // A missing cover/JSON belongs to the comic, while broken pages belong
            // to individual chapters. Do not enqueue every chapter for metadata-only
            // repairs; that used to create needless worker runs and could look like a
            // full comic re-download.
            val repairIds = buildList {
                if (owner != null && (result.missingCover || result.missingConfig)) add(owner)
                addAll(result.brokenChapterIds)
            }.distinct()
            if (repairIds.isEmpty()) return@launch
            repairIds.forEach { id ->
                val item = tasks.firstOrNull { it.id == id } ?: return@forEach
                downloadComicDao.updateStatus(com.par9uet.jm.database.model.UpdateComicStatus(item.id, "pending"))
                // A repair must start from a visible, indeterminate-looking
                // zero state. Keeping 100% from the previous complete row
                // makes a missing-page repair appear frozen at 0 KB/100%.
                if (id in result.brokenChapterIds) {
                    downloadComicDao.updateProgress(
                        com.par9uet.jm.database.model.UpdateComicProgress(item.id, 0f)
                    )
                }
            }
            val requests = repairIds.distinct().map { id ->
                val isOwner = id == owner
                DownloadRepairRequest(
                    comicId = id,
                    downloadCover = result.missingCover && isOwner,
                    downloadPages = needsPages[id] == true,
                    writeConfig = result.missingConfig && isOwner,
                    missingPageIndices = result.missingPagesByChapter[id]
                        .orEmpty()
                        .map { it - 1 }
                        .filter { it >= 0 }
                        .toIntArray(),
                )
            }
            enqueueRepairRequests(requests)
            val parts = buildList {
                if (result.missingCover) add("封面")
                if (result.missingConfig) add("JSON")
                if (result.brokenChapterIds.isNotEmpty()) add("缺失页")
            }
            toastManager.showAsync("仅补齐：${parts.joinToString("、")}")
        }
    }

    @Deprecated("Use repairCachedItems(CacheIntegrityResult) to avoid redownloading complete comics")
    fun repairCachedItems(ids: Collection<Int>) =
        repairCachedItems(CacheIntegrityResult(brokenChapterIds = ids.toSet()))

    private data class DownloadRepairRequest(
        val comicId: Int,
        val downloadCover: Boolean,
        val downloadPages: Boolean,
        val writeConfig: Boolean,
        val missingPageIndices: IntArray = intArrayOf(),
    )

    private fun enqueueRepairRequests(requests: List<DownloadRepairRequest>) {
        val workManager = WorkManager.getInstance(context)
        requests.forEach { request ->
            val work = OneTimeWorkRequestBuilder<DownloadComicWorker>()
                .setInputData(
                    workDataOf(
                        "comicId" to request.comicId,
                        "batchId" to "",
                        "batchTotal" to requests.size,
                        "downloadCover" to request.downloadCover,
                        "downloadPages" to request.downloadPages,
                        "writeConfig" to request.writeConfig,
                        "missingPageIndices" to request.missingPageIndices,
                    )
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, DOWNLOAD_RETRY_BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(watchCacheWorkName(request.comicId), ExistingWorkPolicy.KEEP, work)
        }
    }

    fun redownloadGroup(groupId: Int) {
        scope.launch(Dispatchers.IO) {
            val items = downloadComicDao.getByGroupId(groupId)
            if (items.isEmpty()) return@launch
            items.forEach { item ->
                if (item.zipPath.isNotBlank() && cachePathExists(context, item.zipPath)) deleteCachePath(context, item.zipPath)
                if (item.coverPath.isNotBlank() && cachePathExists(context, item.coverPath)) deleteCachePath(context, item.coverPath)
                downloadComicDao.updateStatus(
                    com.par9uet.jm.database.model.UpdateComicStatus(item.id, "pending")
                )
                downloadComicDao.updateProgress(
                    com.par9uet.jm.database.model.UpdateComicProgress(item.id, 0f)
                )
            }
            enqueueDownloads(items.map { it.id })
            toastManager.showAsync("已重新下载 ${items.size} 个任务")
        }
    }
}
