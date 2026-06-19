package com.par9uet.jm.worker

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.ui.graphics.asAndroidBitmap
import com.par9uet.jm.cache.getDownloadDir
import com.par9uet.jm.data.models.ComicPicImageState
import com.par9uet.jm.data.models.ImageResultState
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.database.model.UpdateComicCover
import com.par9uet.jm.database.model.UpdateComicProgress
import com.par9uet.jm.database.model.UpdateComicStatus
import com.par9uet.jm.database.model.UpdateComicZipPath
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.ComicPicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.RemoteSettingManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.utils.compressWebpCompat
import com.par9uet.jm.utils.tryCreateDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DownloadComicWorker(
    private val appContext: Context,
    params: WorkerParameters,
    private val downloadComicDao: DownloadComicDao,
    private val remoteSettingManager: RemoteSettingManager,
    private val localSettingManager: LocalSettingManager,
    private val comicRepository: ComicRepository,
    private val toastManager: ToastManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val comicId = inputData.getInt("comicId", -1)
        if (comicId == -1) {
            return Result.failure()
        }
        return try {
            downloadComicDao.updateStatus(
                UpdateComicStatus(
                    comicId,
                    "downloading"
                )
            )
            val coverPath = downloadCover(comicId)
            downloadComicDao.updateCover(
                UpdateComicCover(
                    comicId,
                    coverPath
                )
            )
            val picPathList =
                downloadPicList(comicId, localSettingManager.localSettingState.value.shunt)
            val zipPath = zipPicPathList(comicId, picPathList)
            downloadComicDao.updateZipPath(
                UpdateComicZipPath(
                    comicId,
                    zipPath
                )
            )
            downloadComicDao.updateStatus(
                UpdateComicStatus(
                    comicId,
                    "complete"
                )
            )
            toastManager.showAsync("下载成功")
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry() // 如果失败了，系统会自动尝试重试
            } else {
                downloadComicDao.updateStatus(
                    UpdateComicStatus(
                        comicId,
                        "error"
                    )
                )
                Result.failure()
            }
        }
    }

    private suspend fun downloadCover(comicId: Int): String {
        return withContext(Dispatchers.IO) {
            val coverUrl =
                "${remoteSettingManager.remoteSettingState.value.imgHost}/media/albums/${comicId}_3x4.jpg"
            val loader = ImageLoader(appContext)
            val request = ImageRequest.Builder(appContext)
                .data(coverUrl)
                .allowHardware(false)
                .build()

            when (val result = loader.execute(request)) {
                is ErrorResult -> {
                    // TODO 处理错误
                    ""
                }

                is SuccessResult -> {
                    val bitmap = result.drawable.toBitmap()
                    val dir = getComicCoverDownloadDir()
                    val file = File(dir, "${comicId}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compressWebpCompat(50, out)
                    }
                    file.absolutePath
                }
            }
        }
    }

    private suspend fun downloadPicList(comicId: Int, shunt: String): List<String> {
        return withContext(Dispatchers.IO) {
            when (val data = comicRepository.getComicPicList(comicId, shunt)) {
                is NetWorkResult.Error -> {
                    // TODO
                    listOf()
                }

                is NetWorkResult.Success<ComicPicListResponse> -> {
                    val dir = getComicPicListDownloadDir(comicId)
                    val loader = ImageLoader(appContext)
                    data.data.list.mapIndexed { index, url ->
                        val imageState = ComicPicImageState(
                            index = index,
                            comicId = comicId,
                            originSrc = url,
                            __scrambleId = data.data.__scrambleId,
                            __speed = data.data.__speed,
                            picImageLoader = loader
                        )
                        imageState.decode(appContext)
                        when (val result = imageState.imageResultState) {
                            is ImageResultState.Success -> {
                                val file = File(dir, "$index.webp")
                                FileOutputStream(file).use { out ->
                                    result.decodeImageBitmap.asAndroidBitmap().compressWebpCompat(50, out)
                                }
                                downloadComicDao.updateProgress(
                                    UpdateComicProgress(
                                        comicId,
                                        (index + 1).toFloat() / data.data.list.size
                                    )
                                )
                                file.absolutePath
                            }

                            is ImageResultState.Failure -> {
                                ""
                            }

                            ImageResultState.Loading -> {
                                ""
                            }
                        }
                    }
                }
            }
        }
    }

    private fun zipPicPathList(comicId: Int, picPathList: List<String>): String {
        val zipFile = File(getDownloadDir(appContext), "$comicId.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            picPathList.forEach { source ->
                val file = File(source)
                if (file.exists()) {
                    val entryName = "$comicId/${file.name}"
                    val zipEntry = ZipEntry(entryName)
                    zipOut.putNextEntry(zipEntry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
        }
        return zipFile.absolutePath
    }

    private fun getComicPicListDownloadDir(comicId: Int): File {
        val dir = getDownloadDir(appContext)
        return tryCreateDir(File(dir, "$comicId"))
    }

    private fun getComicCoverDownloadDir(): File {
        val dir = getDownloadDir(appContext)
        return tryCreateDir(File(dir, "cover"))
    }
}
