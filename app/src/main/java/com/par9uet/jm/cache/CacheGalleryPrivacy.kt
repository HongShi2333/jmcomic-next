package com.par9uet.jm.cache

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.par9uet.jm.utils.logError
import java.io.File

const val NOMEDIA_FILE_NAME = ".nomedia"

private const val EXTERNAL_STORAGE_DOCUMENTS = "com.android.externalstorage.documents"
private val hiddenDownloadTrees = mutableSetOf<String>()
private val cacheImageDisplayName = Regex("""^(cover|\d+)\.(webp|jpg|jpeg|png)$""", RegexOption.IGNORE_CASE)

/**
 * Hide a custom cache tree from the system gallery.
 *
 * `.nomedia` must exist before images are written; MediaStore will not drop
 * already-indexed photos just because the marker appears later, so this also
 * best-effort deletes matching cache image rows.
 */
fun ensureDownloadTreeHiddenFromGallery(context: Context, treeUri: String) {
    if (treeUri.isBlank()) return
    synchronized(hiddenDownloadTrees) {
        if (treeUri in hiddenDownloadTrees) return
    }
    runCatching {
        val tree = Uri.parse(treeUri)
        val documentId = DocumentsContract.getTreeDocumentId(tree)
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
        ensureNomediaMarker(context, root.toString())
        synchronized(hiddenDownloadTrees) { hiddenDownloadTrees.add(treeUri) }
        if (tree.authority == EXTERNAL_STORAGE_DOCUMENTS) {
            val relativePath = relativePathFromExternalStorageDocumentId(documentId)
            notifyMediaScannerOfNomedia(context, documentId, relativePath)
            relativePath?.let { removeIndexedCacheImagesFromGallery(context, it) }
        }
    }.onFailure { error ->
        logError(
            "CacheGalleryPrivacy",
            "无法在自定义缓存目录写入 .nomedia：${error.message ?: error::class.java.simpleName}",
        )
    }
}

fun ensureNomediaMarker(context: Context, directoryPath: String) {
    if (directoryPath.isBlank()) return
    if (isDocumentCachePath(directoryPath)) {
        val marker = requireNotNull(
            findOrCreateCacheDocument(
                context,
                Uri.parse(directoryPath),
                NOMEDIA_FILE_NAME,
                "application/octet-stream",
            )
        )
        runCatching { context.contentResolver.openOutputStream(marker, "wt")?.close() }
        return
    }
    val dir = File(directoryPath)
    if (!dir.exists()) dir.mkdirs()
    val marker = File(dir, NOMEDIA_FILE_NAME)
    if (!marker.exists()) marker.createNewFile()
}

internal fun relativePathFromExternalStorageDocumentId(documentId: String): String? {
    val separator = documentId.indexOf(':')
    if (separator < 0) return null
    val relative = documentId.substring(separator + 1).trim('/').replace('\\', '/')
    if (relative.isBlank()) return null
    return "$relative/"
}

internal fun isCacheImageDisplayName(name: String): Boolean = cacheImageDisplayName.matches(name)

private fun notifyMediaScannerOfNomedia(
    context: Context,
    documentId: String,
    relativePath: String?,
) {
    if (relativePath == null || !documentId.startsWith("primary:")) return
    val nomedia = File(Environment.getExternalStorageDirectory(), relativePath + NOMEDIA_FILE_NAME)
    runCatching {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(nomedia.absolutePath),
            arrayOf("application/octet-stream"),
            null,
        )
    }
}

private fun removeIndexedCacheImagesFromGallery(context: Context, relativePath: String) {
    runCatching {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            resolver.query(
                collection,
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME),
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf("$relativePath%"),
                null,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    if (!isCacheImageDisplayName(name)) continue
                    val item = ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                    runCatching { resolver.delete(item, null, null) }
                }
            }
            return
        }
        val prefix = File(Environment.getExternalStorageDirectory(), relativePath.trimEnd('/')).absolutePath
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME),
            "${MediaStore.MediaColumns.DATA} LIKE ?",
            arrayOf("$prefix/%"),
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue
                if (!isCacheImageDisplayName(name)) continue
                val item = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idCol),
                )
                runCatching { resolver.delete(item, null, null) }
            }
        }
    }
}
