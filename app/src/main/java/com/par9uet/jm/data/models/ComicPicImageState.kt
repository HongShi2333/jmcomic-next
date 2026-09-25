package com.par9uet.jm.data.models

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.par9uet.jm.cache.getCommonPicDecodeCacheDir
import com.par9uet.jm.utils.compressWebpCompat
import com.par9uet.jm.utils.logError
import com.par9uet.jm.utils.md5
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class ImageResultState {
    object Loading : ImageResultState()
    data class Success(
        val decodeImageBitmap: ImageBitmap,
        val decodeImageAspectRatio: Float
    ) :
        ImageResultState()

    data class Failure(val reason: String) : ImageResultState()
}

class ComicPicImageState(
    val index: Int,
    val comicId: Int,
    val originSrc: String,
    val __scrambleId: Int,
    val __speed: String,
    private val picImageLoader: ImageLoader,
    private val imageFetcher: (suspend () -> ByteArray?)? = null,
) {

    companion object {
        private val seedMap = listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
        private const val AUTO_RETRY_COUNT = 3
        private const val AUTO_RETRY_ATTEMPTS = AUTO_RETRY_COUNT + 1
        private val AUTO_RETRY_DELAYS_MS = longArrayOf(300L, 600L, 1_000L)
    }

    var imageResultState by mutableStateOf<ImageResultState>(ImageResultState.Loading)

    suspend fun decode(
        context: Context,
        downscale: Boolean = false,
        forceReload: Boolean = false,
    ) {
        withContext(Dispatchers.Default) {
            imageResultState = ImageResultState.Loading
            try {
                decodeImage(context, downscale, forceReload)
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                logError("ComicPicImage", "解码图片 OOM: ${e.message}")
                System.gc()
                imageResultState = ImageResultState.Failure("内存不足，无法解码图片")
            } catch (e: Exception) {
                logError("ComicPicImage", "解码图片异常: ${e.stackTraceToString()}")
                imageResultState = ImageResultState.Failure("图片解码失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun decodeImage(
        context: Context,
        downscale: Boolean = false,
        forceReload: Boolean = false,
    ) {
        val cacheDir = getCommonPicDecodeCacheDir(context, comicId)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val page = extractPageFromUrl()
        val cacheFile = File(cacheDir, "$page.webp")

        // 检查缓存文件是否存在
        if (cacheFile.exists()) {
            try {
                val options = if (downscale) {
                    BitmapFactory.Options().apply { inSampleSize = 2 }
                } else null
                val decodeImageBitmap =
                    BitmapFactory.decodeFile(cacheFile.absolutePath, options)?.asImageBitmap()
                        ?: run {
                            cacheFile.delete()
                            throw IllegalStateException("缓存图片解码为空")
                        }
                val decodeImageAspectRatio =
                    decodeImageBitmap.width * 1.0f / decodeImageBitmap.height
                imageResultState = ImageResultState.Success(decodeImageBitmap, decodeImageAspectRatio)
                return
            } catch (e: Exception) {
                logError("ComicPicImage", "缓存图片解码失败，删除并重新解码: ${e.message}")
                cacheFile.delete()
            }
        }

        // 加载原始图片
        val imageData = File(originSrc).takeIf { it.exists() } ?: originSrc
        val originalBitmap = loadBitmapWithRetry(context, imageData, downscale, forceReload)
            ?: run {
                imageResultState = ImageResultState.Failure("网络错误，已自动重试 3 次，请手动重试")
                return
            }

        try {
            val originalImageBitmap = originalBitmap.asImageBitmap()
            val decodeImageAspectRatio =
                originalImageBitmap.width * 1.0f / originalImageBitmap.height
            var decodedImageBitmap = originalImageBitmap
            if (isGif() || comicId <= __scrambleId || __speed == "1") {
                saveBitmapAsWebp(originalBitmap, cacheFile)
            } else {
                val decodedBitmap = decodeBitmap(originalBitmap, page)
                saveBitmapAsWebp(decodedBitmap, cacheFile)
                decodedImageBitmap = decodedBitmap.asImageBitmap()
            }
            imageResultState =
                ImageResultState.Success(decodedImageBitmap, decodeImageAspectRatio)
        } catch (e: OutOfMemoryError) {
            logError("ComicPicImage", "图片处理 OOM: ${e.message}")
            System.gc()
            imageResultState = ImageResultState.Failure("内存不足")
        } catch (e: Exception) {
            logError("ComicPicImage", "图片处理失败: ${e.stackTraceToString()}")
            imageResultState = ImageResultState.Failure("图片处理失败：${e.message ?: "未知错误"}")
        }
    }

    private suspend fun loadBitmapWithRetry(
        context: Context,
        imageData: Any,
        downscale: Boolean,
        forceReload: Boolean,
    ): Bitmap? {
        var lastFailure: Throwable? = null
        repeat(AUTO_RETRY_ATTEMPTS) { attempt ->
            val bitmap = try {
                // 首次请求保留 Coil 的正常缓存命中；失败后的重试和手动重试
                // 才绕过原始图片缓存，避免坏缓存把所有重试都锁死。
                val requestBuilder = ImageRequest.Builder(context)
                    .data(imageData)
                    .size { Size.ORIGINAL }
                    .allowHardware(false)
                if (forceReload || attempt > 0) {
                    requestBuilder
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.DISABLED)
                }
                when (val result = picImageLoader.execute(requestBuilder.build())) {
                    is SuccessResult -> result.drawable.toBitmap()
                    is ErrorResult -> {
                        lastFailure = result.throwable
                        null
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                throw e
            } catch (e: Exception) {
                lastFailure = e
                null
            }

            if (bitmap != null) return bitmap

            // Coil 可能返回 ErrorResult，也可能在自定义 Fetcher/旧设备上直接抛异常。
            // 两种情况都要走内置 API 回退，否则手动重试只会重复显示同一个失败状态。
            val fallbackBitmap = try {
                val fetchedBytes = imageFetcher?.invoke()
                if (fetchedBytes == null) {
                    null
                } else {
                    val options = if (downscale) {
                        BitmapFactory.Options().apply { inSampleSize = 2 }
                    } else null
                    BitmapFactory.decodeByteArray(
                        fetchedBytes,
                        0,
                        fetchedBytes.size,
                        options,
                    )?.also {
                        lastFailure = null
                    } ?: run {
                        lastFailure = IllegalStateException("图片字节无法解码")
                        null
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                throw e
            } catch (e: Exception) {
                lastFailure = e
                logError("ComicPicImage", "imageFetcher 调用失败: ${e.message}")
                null
            }
            if (fallbackBitmap != null) return fallbackBitmap

            if (attempt < AUTO_RETRY_ATTEMPTS - 1) {
                logError(
                    "ComicPicImage",
                    "图片获取失败，自动重试第${attempt + 1}/${AUTO_RETRY_COUNT}次：${lastFailure?.message ?: "未知错误"}",
                )
                delay(AUTO_RETRY_DELAYS_MS[attempt])
            }
        }

        logError(
            "ComicPicImage",
            "图片获取失败，已自动重试${AUTO_RETRY_COUNT}次：${lastFailure?.stackTraceToString() ?: "未知错误"}",
        )
        return null
    }

    private fun decodeBitmap(originalBitmap: Bitmap, page: String): Bitmap {
        val naturalWidth = originalBitmap.width
        val naturalHeight = originalBitmap.height
        val seed = calculateSeed(comicId, page)
        val remainder = naturalHeight % seed

        val decodedBitmap =
            createBitmap(naturalWidth, naturalHeight)
        val canvas = Canvas(decodedBitmap.asImageBitmap())
        val paint = Paint().apply {
            this.isAntiAlias = false
        }
        val originImageBitmap = originalBitmap.asImageBitmap()

        for (i in 0 until seed) {
            var height = naturalHeight / seed
            var dy = height * i
            val sy = naturalHeight - height * (i + 1) - remainder
            if (i == 0) {
                height += remainder
            } else {
                dy += remainder
            }

            val srcOffset = IntOffset(0, sy)
            val srcSize = IntSize(naturalWidth, height)
            val destOffset = IntOffset(0, dy)
            val destSize = IntSize(naturalWidth, height)

            canvas.drawImageRect(
                originImageBitmap,
                srcOffset,
                srcSize,
                destOffset,
                destSize,
                paint
            )
        }

        return decodedBitmap
    }

    private fun calculateSeed(comicId: Int, pageStr: String): Int {
        val key = "$comicId$pageStr"
        val keyMd5 = md5(key)
        var charCodeOfLastChar = keyMd5.last().code
        val left = 268850
        val right = 421925

        when {
            comicId in left..right -> charCodeOfLastChar %= 10
            comicId >= right + 1 -> charCodeOfLastChar %= 8
        }

        return seedMap.getOrNull(charCodeOfLastChar) ?: 10
    }

    private fun extractPageFromUrl(): String {
        // A SAF document URI encodes the full folder hierarchy after its last
        // slash. Using it as a filename creates paths over Android's filename
        // limit (ENAMETOOLONG). Local pages are already ordered by index, so a
        // compact index is the correct decode-cache key.
        if (originSrc.startsWith("content://")) return index.toString()
        return originSrc.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')
    }

    private suspend fun saveBitmapAsWebp(bitmap: Bitmap, file: File) {
        withContext(Dispatchers.IO) {
            FileOutputStream(file).use { out ->
                bitmap.compressWebpCompat(50, out)
            }
        }
    }

    private fun isGif(): Boolean {
        return originSrc.substringBefore('?').endsWith(".gif", ignoreCase = true)
    }

}
