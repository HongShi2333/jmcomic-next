package com.par9uet.jm.worker

import android.content.Context
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.par9uet.jm.cache.getComicChapterDownloadDir
import com.par9uet.jm.cache.getComicCoverDownloadFile
import com.par9uet.jm.cache.writeComicCacheConfig
import com.par9uet.jm.data.models.ComicPicImageState
import com.par9uet.jm.data.models.ImageResultState
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.database.model.UpdateComicCover
import com.par9uet.jm.database.model.UpdateComicProgress
import com.par9uet.jm.database.model.UpdateComicStatus
import com.par9uet.jm.database.model.UpdateComicZipPath
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.ComicPicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.store.DownloadToastAggregator
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.RemoteSettingManager
import com.par9uet.jm.utils.COMIC_CACHE_NOTIFICATION_ID_BASE
import com.par9uet.jm.utils.cancelProgressNotification
import com.par9uet.jm.utils.compressWebpCompat
import com.par9uet.jm.utils.showProgressNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream

private const val DOWNLOAD_PAGE_TIMEOUT_MS = 180_000L
private const val DOWNLOAD_MAX_ATTEMPTS = 6

class DownloadComicWorker(
    private val appContext: Context,
    params: WorkerParameters,
    private val downloadComicDao: DownloadComicDao,
    private val remoteSettingManager: RemoteSettingManager,
    private val localSettingManager: LocalSettingManager,
    private val comicRepository: ComicRepository,
    private val downloadToastAggregator: DownloadToastAggregator,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val comicId = inputData.getInt("comicId", -1)
        val batchId = inputData.getString("batchId").orEmpty()
        val batchTotal = inputData.getInt("batchTotal", 1)
        if (comicId == -1) {
            return Result.failure()
        }

        return try {
            val downloadTask = downloadComicDao.getById(comicId) ?: return Result.failure()
            val coverOwnerId = downloadTask.groupId.takeIf { it != 0 } ?: comicId
            downloadComicDao.updateStatus(UpdateComicStatus(comicId, "downloading"))
            showComicCacheNotification(
                downloadTask,
                resolveGroupProgress(downloadTask, downloadTask.progress)
            )

            val coverPath = downloadCover(downloadTask, coverOwnerId)
            downloadComicDao.updateCover(UpdateComicCover(comicId, coverPath))

            downloadPicList(downloadTask, localSettingManager.localSettingState.value.shunt)
            showComicCacheNotification(downloadTask, updateChapterProgress(downloadTask, 1f))

            val chapterDirPath = getComicChapterDownloadDir(appContext, downloadTask).absolutePath
            downloadComicDao.updateZipPath(UpdateComicZipPath(comicId, chapterDirPath))
            downloadComicDao.updateStatus(UpdateComicStatus(comicId, "complete"))
            writeCacheConfig(comicId)
            cancelComicCacheNotificationIfIdle(downloadTask)
            downloadToastAggregator.report(batchId, batchTotal, comicId, success = true)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < DOWNLOAD_MAX_ATTEMPTS - 1) {
                Result.retry()
            } else {
                downloadComicDao.updateStatus(UpdateComicStatus(comicId, "error"))
                downloadComicDao.getById(comicId)?.let {
                    cancelComicCacheNotificationIfIdle(it)
                }
                downloadToastAggregator.report(batchId, batchTotal, comicId, success = false)
                Result.failure()
            }
        }
    }

    private suspend fun downloadCover(downloadTask: DownloadComic, coverOwnerId: Int): String {
        return withContext(Dispatchers.IO) {
            val coverUrl =
                "${remoteSettingManager.remoteSettingState.value.imgHost}/media/albums/${coverOwnerId}_3x4.jpg"
            val loader = ImageLoader(appContext)
            val request = ImageRequest.Builder(appContext)
                .data(coverUrl)
                .allowHardware(false)
                .build()

            when (val result = loader.execute(request)) {
                is ErrorResult -> ""
                is SuccessResult -> {
                    val bitmap = result.drawable.toBitmap()
                    val file = getComicCoverDownloadFile(appContext, downloadTask)
                    FileOutputStream(file).use { out ->
                        bitmap.compressWebpCompat(50, out)
                    }
                    file.absolutePath
                }
            }
        }
    }

    private suspend fun downloadPicList(downloadTask: DownloadComic, shunt: String): List<String> {
        return withContext(Dispatchers.IO) {
            val comicId = downloadTask.id
            when (val data = comicRepository.getComicPicList(comicId, shunt)) {
                is NetWorkResult.Error -> throw IllegalStateException(data.message)
                is NetWorkResult.Success<ComicPicListResponse> -> {
                    if (data.data.list.isEmpty()) {
                        throw IllegalStateException("图片列表为空")
                    }

                    val dir = getComicChapterDownloadDir(appContext, downloadTask)
                    val loader = ImageLoader(appContext)
                    var maxProgress = downloadComicDao.getById(comicId)?.progress ?: 0f

                    data.data.list.mapIndexed { index, url ->
                        val file = File(dir, "$index.webp")
                        val nextProgress = (index + 1).toFloat() / data.data.list.size
                        if (file.exists()) {
                            val progress = updateChapterProgressIfAdvanced(
                                downloadTask = downloadTask,
                                currentMaxProgress = maxProgress,
                                nextProgress = nextProgress
                            )
                            maxProgress = progress.chapterProgress
                            showComicCacheNotification(downloadTask, progress.groupProgress)
                            return@mapIndexed file.absolutePath
                        }

                        val imageState = ComicPicImageState(
                            index = index,
                            comicId = comicId,
                            originSrc = url,
                            __scrambleId = data.data.__scrambleId,
                            __speed = data.data.__speed,
                            picImageLoader = loader
                        )
                        try {
                            withTimeout(DOWNLOAD_PAGE_TIMEOUT_MS) {
                                imageState.decode(appContext)
                            }
                        } catch (e: Exception) {
                            throw IllegalStateException("第 ${index + 1} 页下载或解码超时", e)
                        }

                        when (val result = imageState.imageResultState) {
                            is ImageResultState.Success -> {
                                FileOutputStream(file).use { out ->
                                    result.decodeImageBitmap.asAndroidBitmap().compressWebpCompat(50, out)
                                }
                                val progress = updateChapterProgressIfAdvanced(
                                    downloadTask = downloadTask,
                                    currentMaxProgress = maxProgress,
                                    nextProgress = nextProgress
                                )
                                maxProgress = progress.chapterProgress
                                showComicCacheNotification(downloadTask, progress.groupProgress)
                                file.absolutePath
                            }

                            is ImageResultState.Failure -> {
                                throw IllegalStateException("第 ${index + 1} 页下载失败：${result.reason}")
                            }

                            ImageResultState.Loading -> {
                                throw IllegalStateException("第 ${index + 1} 页仍在加载中")
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun writeCacheConfig(comicId: Int) {
        val current = downloadComicDao.getById(comicId) ?: return
        val groupId = current.groupId.takeIf { it != 0 } ?: current.id
        val chapters = downloadComicDao.getByGroupId(groupId)
        withContext(Dispatchers.IO) {
            writeComicCacheConfig(appContext, current, chapters)
        }
    }

    private suspend fun updateChapterProgress(downloadTask: DownloadComic, progress: Float): Float {
        val chapterProgress = progress.coerceIn(0f, 1f)
        downloadComicDao.updateProgress(UpdateComicProgress(downloadTask.id, chapterProgress))
        return resolveGroupProgress(downloadTask, chapterProgress)
    }

    private suspend fun updateChapterProgressIfAdvanced(
        downloadTask: DownloadComic,
        currentMaxProgress: Float,
        nextProgress: Float
    ): DownloadProgress {
        val chapterProgress = maxOf(currentMaxProgress, nextProgress.coerceIn(0f, 1f))
        if (chapterProgress > currentMaxProgress) {
            downloadComicDao.updateProgress(UpdateComicProgress(downloadTask.id, chapterProgress))
        }
        return DownloadProgress(
            chapterProgress = chapterProgress,
            groupProgress = resolveGroupProgress(downloadTask, chapterProgress)
        )
    }

    private suspend fun resolveGroupProgress(downloadTask: DownloadComic, currentProgress: Float): Float {
        val groupId = downloadTask.groupId.takeIf { it != 0 } ?: downloadTask.id
        val chapters = downloadComicDao.getByGroupId(groupId)
        if (chapters.isEmpty()) return currentProgress
        return chapters.map { chapter ->
            when {
                chapter.id == downloadTask.id -> currentProgress
                chapter.status == "complete" -> 1f
                else -> chapter.progress.coerceIn(0f, 1f)
            }
        }.average().toFloat().coerceIn(0f, 1f)
    }

    private suspend fun cancelComicCacheNotificationIfIdle(downloadTask: DownloadComic) {
        val groupId = downloadTask.groupId.takeIf { it != 0 } ?: downloadTask.id
        val chapters = downloadComicDao.getByGroupId(groupId)
        val hasActiveTask = chapters.any { it.status == "pending" || it.status == "downloading" }
        if (!hasActiveTask) {
            cancelProgressNotification(appContext, COMIC_CACHE_NOTIFICATION_ID_BASE + groupId)
        }
    }

    private fun showComicCacheNotification(downloadTask: DownloadComic, progress: Float) {
        val groupId = downloadTask.groupId.takeIf { it != 0 } ?: downloadTask.id
        val setting = localSettingManager.localSettingState.value
        if (!setting.showComicCacheNotification) {
            cancelProgressNotification(appContext, COMIC_CACHE_NOTIFICATION_ID_BASE + groupId)
            return
        }
        val comicName = downloadTask.groupName.ifBlank { downloadTask.name }
        val title = if (setting.showComicCacheNotificationName && comicName.isNotBlank()) {
            "正在缓存$comicName"
        } else {
            "正在缓存漫画"
        }
        val progressPercent = (progress.coerceIn(0f, 1f) * 100).toInt()
        showProgressNotification(
            context = appContext,
            notificationId = COMIC_CACHE_NOTIFICATION_ID_BASE + groupId,
            title = title,
            text = "$progressPercent%",
            progressPercent = progressPercent
        )
    }

    private data class DownloadProgress(
        val chapterProgress: Float,
        val groupProgress: Float
    )
}
