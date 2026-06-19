package com.par9uet.jm.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.DocumentsContract
import com.par9uet.jm.cache.getDownloadDir
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
    val imageDir = getComicImageDir(context, comic)
    val imageFiles = imageDir?.let(::listComicImageFiles).orEmpty()
    val zipFile = comic.zipPath.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() }
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
    val imageDir = getComicImageDir(context, comic)
        ?: throw IllegalStateException("未找到本地缓存图片")
    val imageFiles = listComicImageFiles(imageDir)
    if (imageFiles.isEmpty()) {
        throw IllegalStateException("未找到可导出的缓存图片")
    }

    val fileName = safeFileName("${comic.name}_${comic.id}.pdf")
    val parentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId)
    val outputUri = DocumentsContract.createDocument(
        context.contentResolver,
        parentUri,
        "application/pdf",
        fileName
    ) ?: throw IllegalStateException("无法创建 PDF 文件")

    context.contentResolver.openOutputStream(outputUri)?.use { output ->
        val document = PdfDocument()
        try {
            imageFiles.forEachIndexed { index, file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ?: return@forEachIndexed
                val pageWidth = bitmap.width.coerceAtLeast(1)
                val pageHeight = bitmap.height.coerceAtLeast(1)
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
                bitmap.recycle()
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    } ?: throw IllegalStateException("无法写入 PDF 文件")

    return outputUri.toString()
}

private fun getComicImageDir(context: Context, comic: DownloadComic): File? {
    val dir = File(getDownloadDir(context), "${comic.id}")
    if (dir.exists() && listComicImageFiles(dir).isNotEmpty()) {
        return dir
    }
    val zipFile = comic.zipPath.takeIf { it.isNotBlank() }?.let(::File) ?: return dir.takeIf { it.exists() }
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

private fun listComicImageFiles(dir: File): List<File> {
    return dir.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in setOf("webp", "jpg", "jpeg", "png") }
        ?.sortedWith(compareBy<File> { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.name })
        .orEmpty()
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
