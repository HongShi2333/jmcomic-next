package com.par9uet.jm.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.DocumentsContract
import com.par9uet.jm.cache.getComicChapterDownloadDir
import com.par9uet.jm.cache.getDownloadDir
import com.par9uet.jm.cache.listComicImageFiles
import com.par9uet.jm.cache.cachePathLength
import com.par9uet.jm.cache.isDocumentCachePath
import com.par9uet.jm.cache.listComicImagePaths
import com.par9uet.jm.database.model.DownloadComic
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt

data class CachedComicInfo(
    val imageCount: Int,
    val totalBytes: Long,
    val imageDir: File?,
    val zipFile: File?
)

fun getCachedComicInfo(context: Context, comic: DownloadComic): CachedComicInfo {
    if (isDocumentCachePath(comic.zipPath)) {
        val paths = listComicImagePaths(context, comic.zipPath)
        return CachedComicInfo(
            imageCount = paths.size,
            totalBytes = paths.sumOf { cachePathLength(context, it) },
            imageDir = null,
            zipFile = null,
        )
    }
    val imageDir = getComicImageDir(context, comic)
    val imageFiles = imageDir?.let(::listComicImageFiles).orEmpty()
    val zipFile = comic.zipPath.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isFile && it.exists() }
    val totalBytes = imageFiles.sumOf { it.length() } + (zipFile?.length() ?: 0L)
    return CachedComicInfo(
        imageCount = imageFiles.size,
        totalBytes = totalBytes,
        imageDir = imageDir,
        zipFile = zipFile
    )
}

fun exportComicToPdf(
    context: Context,
    comic: DownloadComic,
    treeUri: Uri
): String {
    val imageFiles = getComicImagePaths(context, comic)
    if (imageFiles.isEmpty()) {
        throw IllegalStateException("未找到可导出的缓存图片")
    }

    val fileName = safeFileName("${comic.name}_${comic.id}.pdf")
    return writeImagesToPdf(context, treeUri, fileName, imageFiles)
}

fun exportComicsToMergedPdf(
    context: Context,
    comics: List<DownloadComic>,
    treeUri: Uri
): String {
    val imageFiles = comics.flatMap { comic ->
        getComicImagePaths(context, comic)
    }
    if (imageFiles.isEmpty()) {
        throw IllegalStateException("未找到可导出的缓存图片")
    }
    val groupName = comics.firstOrNull { it.groupName.isNotBlank() }?.groupName
        ?: comics.firstOrNull()?.name
        ?: "comic"
    val fileName = safeFileName("${groupName}_合并_${comics.size}章.pdf")
    return writeImagesToPdf(context, treeUri, fileName, imageFiles)
}

fun exportComicsToSeparatePdf(
    context: Context,
    comics: List<DownloadComic>,
    treeUri: Uri
): List<String> {
    if (comics.isEmpty()) {
        throw IllegalStateException("没有可导出的缓存章节")
    }
    return comics.map { exportComicToPdf(context, it, treeUri) }
}

private const val PDF_MAX_BITMAP_DIMENSION = 2000

private fun writeImagesToPdf(
    context: Context,
    treeUri: Uri,
    fileName: String,
    imageFiles: List<String>
): String {
    val parentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId)
    val outputUri = DocumentsContract.createDocument(
        context.contentResolver,
        parentUri,
        "application/pdf",
        fileName
    ) ?: throw IllegalStateException("无法创建 PDF 文件")

    val failedPages = mutableListOf<Int>()
    context.contentResolver.openOutputStream(outputUri)?.use { output ->
        val document = PdfDocument()
        var pageIndex = 0
        try {
            imageFiles.forEachIndexed { index, path ->
                try {
                    val bitmap = decodeBitmapForPdf(context, path)
                        ?: run {
                            failedPages.add(index + 1)
                            return@forEachIndexed
                        }
                    val pageWidth = bitmap.width.coerceAtLeast(1)
                    val pageHeight = bitmap.height.coerceAtLeast(1)
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                    pageIndex++
                    bitmap.recycle()
                } catch (e: OutOfMemoryError) {
                    System.gc()
                    failedPages.add(index + 1)
                } catch (e: Exception) {
                    failedPages.add(index + 1)
                }
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    } ?: throw IllegalStateException("无法写入 PDF 文件")

    if (failedPages.isNotEmpty() && failedPages.size == imageFiles.size) {
        throw IllegalStateException("所有图片导出失败，可能内存不足或图片损坏")
    }

    return outputUri.toString()
}

private fun decodeBitmapForPdf(context: Context, path: String): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    decodeBitmap(context, path, boundsOptions)
    val width = boundsOptions.outWidth
    val height = boundsOptions.outHeight
    if (width <= 0 || height <= 0) return null

    val sampleSize = calculateSampleSize(width, height)
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return decodeBitmap(context, path, options)
}

private fun decodeBitmap(context: Context, path: String, options: BitmapFactory.Options): Bitmap? =
    if (isDocumentCachePath(path)) {
        context.contentResolver.openInputStream(Uri.parse(path))?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } else {
        BitmapFactory.decodeFile(path, options)
    }

private fun calculateSampleSize(width: Int, height: Int): Int {
    var sampleSize = 1
    var maxDim = maxOf(width, height)
    while (maxDim / sampleSize > PDF_MAX_BITMAP_DIMENSION) {
        sampleSize *= 2
        maxDim /= 2
    }
    return sampleSize
}

private fun getComicImageDir(context: Context, comic: DownloadComic): File? {
    val directDir = comic.zipPath.takeIf { it.isNotBlank() }?.let(::File)
    if (directDir?.isDirectory == true && listComicImageFiles(directDir).isNotEmpty()) {
        return directDir
    }

    val namedDir = getComicChapterDownloadDir(context, comic)
    if (namedDir.exists() && listComicImageFiles(namedDir).isNotEmpty()) {
        return namedDir
    }

    val dir = File(getDownloadDir(context), "${comic.id}")
    if (dir.exists() && listComicImageFiles(dir).isNotEmpty()) {
        return dir
    }
    val zipFile = directDir?.takeIf { it.isFile } ?: return dir.takeIf { it.exists() }
    if (!zipFile.exists()) {
        return dir.takeIf { it.exists() }
    }
    dir.mkdirs()
    ZipInputStream(zipFile.inputStream()).use { zipIn ->
        while (true) {
            val entry = zipIn.nextEntry ?: break
            if (!entry.isDirectory) {
                val output = File(dir, File(entry.name).name)
                FileOutputStream(output).use { out -> zipIn.copyTo(out) }
            }
            zipIn.closeEntry()
        }
    }
    return dir.takeIf { it.exists() }
}

private fun getComicImagePaths(context: Context, comic: DownloadComic): List<String> {
    if (isDocumentCachePath(comic.zipPath)) {
        return listComicImagePaths(context, comic.zipPath)
    }
    return getComicImageDir(context, comic)?.let(::listComicImageFiles).orEmpty()
        .map(File::getAbsolutePath)
}

private fun safeFileName(name: String): String {
    return name.replace(Regex("""[\\/:*?"<>|]"""), "_")
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${value.roundToInt()} ${units[unitIndex]}"
    } else {
        String.format("%.1f %s", value, units[unitIndex])
    }
}
