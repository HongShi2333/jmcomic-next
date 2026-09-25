package com.par9uet.jm.cache

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.google.gson.Gson
import com.par9uet.jm.database.model.DownloadComic
import java.io.File
import java.io.OutputStream

fun isDocumentCachePath(path: String): Boolean = path.startsWith("content://")

fun getTreeUriForCachePath(path: String): Uri? = runCatching {
    if (!isDocumentCachePath(path)) return@runCatching null
    val uri = Uri.parse(path)
    DocumentsContract.buildTreeDocumentUri(
        uri.authority ?: return@runCatching null,
        DocumentsContract.getTreeDocumentId(uri),
    )
}.getOrNull()

fun getComicDownloadRootPath(context: Context, comic: DownloadComic): String {
    val treeUri = getDownloadTreeUri(context)
    if (treeUri == null) return getComicDownloadRootDir(context, comic).absolutePath
    ensureDownloadTreeHiddenFromGallery(context, treeUri.toString())
    val root = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri),
    )
    return requireNotNull(findOrCreateCacheDocument(
        context,
        root,
        safeCacheFileName(comic.groupName.ifBlank { comic.name }),
        DocumentsContract.Document.MIME_TYPE_DIR,
    )).toString()
}

fun getComicChapterDownloadPath(context: Context, comic: DownloadComic): String {
    val root = getComicDownloadRootPath(context, comic)
    if (!isDocumentCachePath(root)) return File(root, getChapterCacheName(comic)).also { it.mkdirs() }.absolutePath
    return requireNotNull(findOrCreateCacheDocument(
        context,
        Uri.parse(root),
        getChapterCacheName(comic),
        DocumentsContract.Document.MIME_TYPE_DIR,
    )).toString()
}

/**
 * Find an already migrated chapter directory without falling back to the
 * default cache. This also understands the directory names used before the
 * chapter-id suffix was introduced.
 */
fun findExistingComicChapterDownloadPath(context: Context, comic: DownloadComic): String? {
    val root = getComicDownloadRootPath(context, comic)
    val names = buildList {
        add(getChapterCacheName(comic))
        if (comic.chapterName.isNotBlank()) add(safeCacheFileName(comic.chapterName))
        // The old single-chapter layout used the comic name or "单篇". Do not
        // use these shared names for multi-chapter rows, otherwise every row
        // could resolve to the same legacy directory.
        if (comic.groupId == 0 || comic.groupId == comic.id) {
            add(safeCacheFileName(comic.name))
            add("单篇")
        }
    }.filter { it.isNotBlank() }.distinct()
    if (!isDocumentCachePath(root)) {
        return names.asSequence()
            .map { File(root, it) }
            .firstOrNull { it.isDirectory && listComicImageFiles(it).isNotEmpty() }
            ?.absolutePath
    }
    return runCatching {
        val parent = Uri.parse(root)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
        context.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                if (cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR && name in names) {
                    val path = DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(0)).toString()
                    if (listComicImageEntries(context, path).isNotEmpty()) return@use path
                }
            }
            null
        }
    }.getOrNull()
}

fun getOrCreateCacheFile(
    context: Context,
    directoryPath: String,
    name: String,
    mimeType: String,
): String {
    if (!isDocumentCachePath(directoryPath)) return File(directoryPath, name).absolutePath
    return requireNotNull(findOrCreateCacheDocument(context, Uri.parse(directoryPath), name, mimeType)).toString()
}

fun getComicCoverDownloadPath(context: Context, comic: DownloadComic): String =
    getOrCreateCacheFile(context, getComicDownloadRootPath(context, comic), "cover.webp", "image/webp")

fun openCacheOutputStream(context: Context, path: String): OutputStream =
    if (isDocumentCachePath(path)) {
        requireNotNull(context.contentResolver.openOutputStream(Uri.parse(path), "wt"))
    } else {
        File(path).also { it.parentFile?.mkdirs() }.outputStream()
    }

fun cachePathExists(context: Context, path: String): Boolean = if (isDocumentCachePath(path)) {
    runCatching {
        context.contentResolver.query(
            Uri.parse(path),
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null, null, null,
        )?.use { it.moveToFirst() } == true
    }.getOrDefault(false)
} else File(path).exists()

fun cachePathLength(context: Context, path: String): Long = if (isDocumentCachePath(path)) {
    runCatching {
        context.contentResolver.query(
            Uri.parse(path),
            arrayOf(DocumentsContract.Document.COLUMN_SIZE),
            null, null, null,
        )?.use { if (it.moveToFirst()) it.getLong(0) else 0L } ?: 0L
    }.getOrDefault(0L)
} else File(path).length()

/** Some SAF providers do not expose COLUMN_SIZE even for non-empty files. */
fun cachePathHasContent(context: Context, path: String): Boolean {
    if (path.isBlank()) return false
    if (!isDocumentCachePath(path)) return File(path).isFile && File(path).length() > 0L
    return runCatching {
        context.contentResolver.openInputStream(Uri.parse(path))?.use { it.read() >= 0 } == true
    }.getOrDefault(false)
}

fun cachePathSize(context: Context, path: String): Long {
    if (!isDocumentCachePath(path)) {
        val file = File(path)
        return if (file.isDirectory) {
            file.walkTopDown().filter(File::isFile).sumOf(File::length)
        } else {
            file.length()
        }
    }
    return runCatching { documentPathSize(context, Uri.parse(path)) }.getOrDefault(0L)
}

data class CacheImageEntry(
    val name: String,
    val path: String,
)

fun listComicImagePaths(context: Context, directoryPath: String): List<String> =
    listComicImageEntries(context, directoryPath).map(CacheImageEntry::path)

fun listComicImageEntries(context: Context, directoryPath: String): List<CacheImageEntry> {
    if (!isDocumentCachePath(directoryPath)) {
        return runCatching {
            listComicImageFiles(File(directoryPath)).map { CacheImageEntry(it.name, it.absolutePath) }
        }.getOrDefault(emptyList())
    }
    return runCatching {
        val parent = Uri.parse(directoryPath)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
        val result = mutableListOf<CacheImageEntry>()
        context.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                if (name.substringAfterLast('.', "").lowercase() in setOf("webp", "jpg", "jpeg", "png")) {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(0)).toString()
                    result += CacheImageEntry(name, uri)
                }
            }
        }
        result.sortedWith(
            compareBy<CacheImageEntry> { it.name.substringBeforeLast('.').toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.name }
        )
    }.getOrDefault(emptyList())
}

fun getCacheParentPath(path: String): String? = runCatching {
    if (!isDocumentCachePath(path)) {
        File(path).parentFile?.absolutePath
    } else {
        val uri = Uri.parse(path)
        val documentId = DocumentsContract.getDocumentId(uri)
        documentId.substringBeforeLast('/', "").takeIf(String::isNotBlank)
            ?.let { DocumentsContract.buildDocumentUriUsingTree(uri, it).toString() }
    }
}.getOrNull()

fun findCacheChildPath(context: Context, parentPath: String, name: String): String? {
    if (!isDocumentCachePath(parentPath)) {
        return File(parentPath, name).takeIf(File::exists)?.absolutePath
    }
    return runCatching {
        val parent = Uri.parse(parentPath)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
        context.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name) {
                    return@use DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(0)).toString()
                }
            }
            null
        }
    }.getOrNull()
}

fun openCacheInputStream(context: Context, path: String) =
    if (isDocumentCachePath(path)) context.contentResolver.openInputStream(Uri.parse(path)) else File(path).inputStream()

fun writeDocumentComicCacheConfig(
    context: Context,
    comic: DownloadComic,
    chapters: List<DownloadComic>,
    gson: Gson = Gson(),
) {
    val rootPath = getComicDownloadRootPath(context, comic)
    val coverPath = getComicCoverDownloadPath(context, comic)
    val config = buildComicCacheConfig(comic, chapters, rootPath, coverPath) { path ->
        listComicImageEntries(context, path).map(CacheImageEntry::name)
    }
    val configPath = getOrCreateCacheFile(context, rootPath, "config.json", "application/json")
    openCacheOutputStream(context, configPath).bufferedWriter(Charsets.UTF_8).use {
        it.write(gson.toJson(config))
    }
}

fun deleteCachePath(context: Context, path: String): Boolean = if (isDocumentCachePath(path)) {
    runCatching { DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(path)) }.getOrDefault(false)
} else {
    File(path).let { if (it.isDirectory) it.deleteRecursively() else it.delete() }
}

fun findOrCreateCacheDocument(context: Context, parent: Uri, name: String, mimeType: String): Uri? {
    findExistingCacheDocument(context, parent, name)?.let { return it }
    return runCatching {
        DocumentsContract.createDocument(context.contentResolver, parent, mimeType, name)
    }.getOrNull() ?: findExistingCacheDocument(context, parent, name)
}

private fun findExistingCacheDocument(context: Context, parent: Uri, name: String): Uri? {
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
    return context.contentResolver.query(
        children,
        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null, null, null,
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            if (cursor.getString(1) == name) {
                return@use DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(0))
            }
        }
        null
    }
}

private fun documentPathSize(context: Context, uri: Uri): Long {
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
    )
    val row = context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) null else Triple(
            cursor.getString(0),
            cursor.getString(1),
            if (cursor.isNull(2)) 0L else cursor.getLong(2),
        )
    } ?: return 0L
    if (row.second != DocumentsContract.Document.MIME_TYPE_DIR) return row.third

    val children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, row.first)
    return context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
        var total = 0L
        while (cursor.moveToNext()) {
            val childId = cursor.getString(0)
            val mimeType = cursor.getString(1)
            total += if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                documentPathSize(
                    context,
                    DocumentsContract.buildDocumentUriUsingTree(uri, childId),
                )
            } else if (cursor.isNull(2)) {
                0L
            } else {
                cursor.getLong(2)
            }
        }
        total
    } ?: 0L
}
