package com.par9uet.jm.store

import android.content.Context
import com.par9uet.jm.cache.getCommonCacheDir
import com.par9uet.jm.utils.APP_UPDATE_NOTIFICATION_ID
import com.par9uet.jm.utils.cancelProgressNotification
import com.par9uet.jm.utils.showProgressNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class AppUpdateDownloadStatus {
    Idle,
    Downloading,
    Paused,
    Completed,
    Canceled,
    Error
}

data class AppUpdateDownloadState(
    val status: AppUpdateDownloadStatus = AppUpdateDownloadStatus.Idle,
    val version: String = "",
    val fileName: String = "",
    val downloadUrl: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSecond: Long = 0L,
    val background: Boolean = false,
    val errorMessage: String = "",
    val savedPath: String = ""
) {
    val progress: Float
        get() = if (totalBytes > 0L) downloadedBytes.toFloat() / totalBytes else 0f
}

class AppUpdateDownloadManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val toastManager: ToastManager
) {
    private val client = OkHttpClient()
    private var job: Job? = null
    private var paused = false
    private var canceled = false
    private var activeRequest: AppUpdateDownloadRequest? = null

    private val _state = MutableStateFlow(AppUpdateDownloadState())
    val state = _state.asStateFlow()

    fun start(request: AppUpdateDownloadRequest) {
        if (request.downloadUrl.isBlank()) {
            toastManager.showAsync("未找到 APK 下载链接")
            return
        }
        cancelInternal(resetState = false)
        activeRequest = request
        paused = false
        canceled = false
        _state.value = AppUpdateDownloadState(
            status = AppUpdateDownloadStatus.Downloading,
            version = request.version,
            fileName = request.fileName,
            downloadUrl = request.downloadUrl
        )
        job = scope.launch {
            download(request)
        }
    }

    fun pause() {
        paused = true
        _state.update {
            if (it.status == AppUpdateDownloadStatus.Downloading) {
                it.copy(status = AppUpdateDownloadStatus.Paused, speedBytesPerSecond = 0L)
            } else {
                it
            }
        }
    }

    fun resume() {
        paused = false
        _state.update {
            if (it.status == AppUpdateDownloadStatus.Paused) {
                it.copy(status = AppUpdateDownloadStatus.Downloading)
            } else {
                it
            }
        }
    }

    fun cancel() {
        cancelInternal(resetState = true)
        cancelProgressNotification(context, APP_UPDATE_NOTIFICATION_ID)
    }

    fun sendToBackground() {
        _state.update { it.copy(background = true) }
        notifyProgress()
    }

    private fun cancelInternal(resetState: Boolean) {
        canceled = true
        paused = false
        job?.cancel()
        job = null
        if (resetState) {
            _state.update { it.copy(status = AppUpdateDownloadStatus.Canceled, speedBytesPerSecond = 0L) }
        }
    }

    private suspend fun download(request: AppUpdateDownloadRequest) = withContext(Dispatchers.IO) {
        runCatching {
            val httpRequest = Request.Builder()
                .url(request.downloadUrl)
                .header("User-Agent", "jmcomic-next-android")
                .build()
            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    error("下载失败：HTTP ${response.code}")
                }
                val body = response.body
                val totalBytes = body.contentLength().takeIf { it > 0L } ?: 0L
                val file = File(getCommonCacheDir(context), "updates/${request.fileName}")
                file.parentFile?.mkdirs()
                var downloaded = 0L
                var windowBytes = 0L
                var lastTick = System.currentTimeMillis()
                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            while (paused && !canceled) {
                                delay(250)
                            }
                            if (canceled) {
                                file.delete()
                                return@withContext
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            windowBytes += read
                            val now = System.currentTimeMillis()
                            if (now - lastTick >= 500L) {
                                val speed = (windowBytes * 1000f / (now - lastTick)).roundToInt().toLong()
                                _state.update {
                                    it.copy(
                                        downloadedBytes = downloaded,
                                        totalBytes = totalBytes,
                                        speedBytesPerSecond = speed,
                                        status = AppUpdateDownloadStatus.Downloading
                                    )
                                }
                                notifyProgress()
                                windowBytes = 0L
                                lastTick = now
                            }
                        }
                    }
                }
                _state.update {
                    it.copy(
                        status = AppUpdateDownloadStatus.Completed,
                        downloadedBytes = downloaded,
                        totalBytes = totalBytes,
                        speedBytesPerSecond = 0L,
                        savedPath = file.absolutePath
                    )
                }
                cancelProgressNotification(context, APP_UPDATE_NOTIFICATION_ID)
                toastManager.showAsync("更新包下载完成")
            }
        }.onFailure { throwable ->
            if (!canceled) {
                _state.update {
                    it.copy(
                        status = AppUpdateDownloadStatus.Error,
                        speedBytesPerSecond = 0L,
                        errorMessage = throwable.message ?: "下载失败"
                    )
                }
                cancelProgressNotification(context, APP_UPDATE_NOTIFICATION_ID)
            }
        }
    }

    private fun notifyProgress() {
        val state = _state.value
        if (!state.background) return
        showProgressNotification(
            context = context,
            notificationId = APP_UPDATE_NOTIFICATION_ID,
            title = "正在下载更新 ${state.version}",
            text = "${(state.progress * 100).roundToInt()}% · ${formatBytes(state.speedBytesPerSecond)}/s",
            progressPercent = (state.progress * 100).roundToInt()
        )
    }
}

data class AppUpdateDownloadRequest(
    val version: String,
    val fileName: String,
    val downloadUrl: String
)

fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024f
    if (kb < 1024f) return "%.1f KB".format(kb)
    val mb = kb / 1024f
    if (mb < 1024f) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024f)
}
