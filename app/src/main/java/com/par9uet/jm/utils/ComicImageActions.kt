package com.par9uet.jm.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

suspend fun saveComicPageToGallery(context: Context, bitmap: Bitmap, comicId: Int, page: Int): Uri =
    withContext(Dispatchers.IO) {
        val name = "JM${comicId}_${page}_${UUID.randomUUID()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/JMComic")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("无法创建相册图片")
            try {
                resolver.openOutputStream(uri)?.use {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("保存图片失败")
                } ?: throw IOException("无法写入相册")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }
        } else {
            @Suppress("DEPRECATION")
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "JMComic")
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("无法创建相册目录")
            val file = File(directory, name)
            try {
                file.outputStream().use {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("保存图片失败")
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                Uri.fromFile(file)
            } catch (error: Exception) {
                file.delete()
                throw error
            }
        }
    }

suspend fun copyComicImage(context: Context, label: String, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "clipboard-images")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("无法创建图片副本")
        val file = File(directory, "${UUID.randomUUID()}.png")
        try {
            file.outputStream().use {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("复制图片失败")
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }
    withContext(Dispatchers.Main) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, label, uri))
    }
}
