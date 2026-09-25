package com.par9uet.jm.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.par9uet.jm.MainActivity
import com.par9uet.jm.R
import com.par9uet.jm.cache.NOMEDIA_FILE_NAME
import com.par9uet.jm.cache.ensureDownloadTreeHiddenFromGallery
import com.par9uet.jm.cache.cachePathExists
import com.par9uet.jm.cache.cachePathLength
import com.par9uet.jm.cache.cachePathSize
import com.par9uet.jm.cache.deleteCachePath
import com.par9uet.jm.cache.findOrCreateCacheDocument
import com.par9uet.jm.cache.findCacheChildPath
import com.par9uet.jm.cache.getDownloadDir
import com.par9uet.jm.cache.getChapterCacheName
import com.par9uet.jm.cache.isDocumentCachePath
import com.par9uet.jm.cache.openCacheOutputStream
import com.par9uet.jm.cache.safeCacheFileName
import com.par9uet.jm.cache.setCacheMigrationRunning
import com.par9uet.jm.cache.setDownloadTreeUri
import com.par9uet.jm.cache.writeDocumentComicCacheConfig
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.DownloadProgressMessageStore
import com.par9uet.jm.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

const val CACHE_MIGRATION_WORK_NAME = "cache_path_migration"
const val CACHE_MIGRATION_TARGET_URI = "target_tree_uri"
const val CACHE_MIGRATION_PROGRESS = "progress"
const val CACHE_MIGRATION_STAGE = "stage"
const val CACHE_MIGRATION_ERROR = "error"
private const val CACHE_MIGRATION_NOTIFICATION_ID = 19_940

class CacheMigrationWorker(
    private val appContext: Context,
    params: WorkerParameters,
    private val downloadComicDao: DownloadComicDao,
    private val localSettingManager: LocalSettingManager,
) : CoroutineWorker(appContext, params) {
    private var lastReportedPercent = -1

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // WorkManager may restart this task after the UI process has been reclaimed.
        // Load persisted settings before resolving either source or target storage.
        localSettingManager.init()
        val targetTreeUri = inputData.getString(CACHE_MIGRATION_TARGET_URI).orEmpty()
        val sourceTreeUri = localSettingManager.localSettingState.value.downloadTreeUri
        if (sourceTreeUri == targetTreeUri) return@withContext Result.success()

        setCacheMigrationRunning(appContext, true)
        val progressGroupIds = mutableSetOf<Int>()
        try {
            if (targetTreeUri.isNotBlank()) {
                ensureDownloadTreeHiddenFromGallery(appContext, targetTreeUri)
            }
            updateProgress(0, "正在等待当前缓存任务结束")
            while (downloadComicDao.getAll().any { it.status == "downloading" }) delay(750)

            val records = downloadComicDao.getAll()
            val sourcePaths = records.flatMap { listOf(it.coverPath, it.zipPath) }
                .filter { it.isNotBlank() && cachePathExists(appContext, it) }
                .distinct()
            val totalBytes = sourcePaths.sumOf { cachePathSize(appContext, it) }.coerceAtLeast(1L)
            var copiedBytes = 0L
            val copiedCoverGroups = mutableSetOf<Int>()
            val usableCoverPaths = records.groupBy { it.groupId.takeIf { id -> id != 0 } ?: it.id }
                .mapValues { (_, chapters) ->
                    chapters.firstNotNullOfOrNull { chapter ->
                        chapter.coverPath.takeIf { path ->
                            path.isNotBlank() &&
                                cachePathExists(appContext, path) &&
                                cachePathLength(appContext, path) > 0L
                        }
                    }
                }

            fun reportCopied(delta: Long) {
                copiedBytes += delta
                val percent = ((copiedBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                updateProgressBlocking(percent, "正在迁移缓存文件 · $percent%")
            }

            val migrated = records.map { record ->
                val groupId = record.groupId.takeIf { it != 0 } ?: record.id
                progressGroupIds += groupId
                DownloadProgressMessageStore.update(groupId, "移动缓存文件")
                val rootPath = destinationComicRoot(record, targetTreeUri)
                val sourceCoverPath = usableCoverPaths[groupId]
                val coverPath = sourceCoverPath?.let {
                    destinationFile(rootPath, "cover.webp", "image/webp")
                }.orEmpty()
                if (sourceCoverPath == null) {
                    findCacheChildPath(appContext, rootPath, "cover.webp")
                        ?.takeIf { cachePathLength(appContext, it) == 0L }
                        ?.let { deleteCachePath(appContext, it) }
                }
                val chapterPath = destinationDirectory(rootPath, getChapterCacheName(record))
                if (sourceCoverPath != null && copiedCoverGroups.add(groupId)) {
                    copyFile(sourceCoverPath, coverPath, ::reportCopied)
                }
                if (record.zipPath.isNotBlank() && cachePathExists(appContext, record.zipPath)) {
                    copyDirectory(record.zipPath, chapterPath, ::reportCopied)
                }
                record.copy(
                    coverPath = coverPath,
                    zipPath = if (record.zipPath.isBlank()) "" else chapterPath,
                )
            }

            updateProgress(99, "正在更新缓存索引")
            for (record in migrated) {
                downloadComicDao.update(record)
            }
            setDownloadTreeUri(appContext, targetTreeUri)
            localSettingManager.updateDownloadTreeUri(targetTreeUri)
            migrated.groupBy { it.groupId.takeIf { id -> id != 0 } ?: it.id }.values.forEach { chapters ->
                DownloadProgressMessageStore.update(
                    chapters.first().groupId.takeIf { it != 0 } ?: chapters.first().id,
                    "生成 JSON"
                )
                runCatching { writeDocumentComicCacheConfig(appContext, chapters.first(), chapters) }
            }
            sourcePaths.sortedByDescending { it.length }.forEach { path ->
                runCatching { deleteCachePath(appContext, path) }
            }
            removeSourceMetadata(records)
            removeEmptyDefaultCacheDirectories()
            updateProgress(100, "迁移完成")
            Result.success(workDataOf(CACHE_MIGRATION_PROGRESS to 100, CACHE_MIGRATION_STAGE to "迁移完成"))
        } catch (throwable: Throwable) {
            Result.failure(workDataOf(CACHE_MIGRATION_ERROR to (throwable.message ?: "缓存迁移失败，原路径未切换")))
        } finally {
            progressGroupIds.forEach(DownloadProgressMessageStore::clear)
            setCacheMigrationRunning(appContext, false)
        }
    }


    private suspend fun updateProgress(percent: Int, stage: String) {
        setProgress(workDataOf(CACHE_MIGRATION_PROGRESS to percent, CACHE_MIGRATION_STAGE to stage))
        if (localSettingManager.localSettingState.value.showCacheMigrationNotification) {
            setForeground(createForegroundInfo(percent, stage))
        }
    }

    private fun updateProgressBlocking(percent: Int, stage: String) {
        if (percent == lastReportedPercent) return
        lastReportedPercent = percent
        runCatching {
            setProgressAsync(workDataOf(CACHE_MIGRATION_PROGRESS to percent, CACHE_MIGRATION_STAGE to stage)).get()
            if (localSettingManager.localSettingState.value.showCacheMigrationNotification) {
                setForegroundAsync(createForegroundInfo(percent, stage)).get()
            }
        }
    }

    private fun createForegroundInfo(percent: Int, stage: String): ForegroundInfo {
        val openApp = PendingIntent.getActivity(
            appContext,
            CACHE_MIGRATION_NOTIFICATION_ID,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_notification)
            .setContentTitle("正在迁移漫画缓存")
            .setContentText(stage)
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, percent.coerceIn(0, 100), false)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(CACHE_MIGRATION_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(CACHE_MIGRATION_NOTIFICATION_ID, notification)
        }
    }

    private fun destinationComicRoot(record: DownloadComic, treeUri: String): String {
        val comicName = safeCacheFileName(record.groupName.ifBlank { record.name })
        if (treeUri.isBlank()) return File(getDownloadDir(appContext), comicName).also(File::mkdirs).absolutePath
        val tree = Uri.parse(treeUri)
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        return requireNotNull(findOrCreateCacheDocument(appContext, root, comicName, DocumentsContract.Document.MIME_TYPE_DIR)).toString()
    }

    private fun destinationDirectory(parentPath: String, name: String): String {
        if (!isDocumentCachePath(parentPath)) return File(parentPath, name).also(File::mkdirs).absolutePath
        return requireNotNull(findOrCreateCacheDocument(appContext, Uri.parse(parentPath), name, DocumentsContract.Document.MIME_TYPE_DIR)).toString()
    }

    private fun destinationFile(parentPath: String, name: String, mimeType: String): String {
        if (!isDocumentCachePath(parentPath)) return File(parentPath, name).absolutePath
        return requireNotNull(findOrCreateCacheDocument(appContext, Uri.parse(parentPath), name, mimeType)).toString()
    }

    private fun copyDirectory(sourcePath: String, destinationPath: String, onBytes: (Long) -> Unit) {
        if (!isDocumentCachePath(sourcePath)) {
            val source = File(sourcePath)
            if (!source.isDirectory) return
            source.listFiles().orEmpty().forEach { child ->
                if (child.name == NOMEDIA_FILE_NAME) return@forEach
                if (child.isDirectory) copyDirectory(child.absolutePath, destinationDirectory(destinationPath, child.name), onBytes)
                else copyFile(child.absolutePath, destinationFile(destinationPath, child.name, mimeType(child.name)), onBytes)
            }
            return
        }
        val source = Uri.parse(sourcePath)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(source, DocumentsContract.getDocumentId(source))
        appContext.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val child = DocumentsContract.buildDocumentUriUsingTree(source, cursor.getString(0)).toString()
                val name = cursor.getString(1)
                val type = cursor.getString(2)
                if (name == NOMEDIA_FILE_NAME) continue
                if (type == DocumentsContract.Document.MIME_TYPE_DIR) copyDirectory(child, destinationDirectory(destinationPath, name), onBytes)
                else copyFile(child, destinationFile(destinationPath, name, type ?: mimeType(name)), onBytes)
            }
        }
    }

    private fun copyFile(sourcePath: String, destinationPath: String, onBytes: (Long) -> Unit) {
        val input = if (isDocumentCachePath(sourcePath)) requireNotNull(appContext.contentResolver.openInputStream(Uri.parse(sourcePath)))
        else File(sourcePath).inputStream()
        input.use { source ->
            openCacheOutputStream(appContext, destinationPath).use { destination -> copyWithProgress(source, destination, onBytes) }
        }
    }

    private fun copyWithProgress(input: InputStream, output: OutputStream, onBytes: (Long) -> Unit) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
            onBytes(read.toLong())
        }
    }

    private fun removeEmptyDefaultCacheDirectories() {
        val root = getDownloadDir(appContext)
        root.walkBottomUp().forEach { file ->
            if (file != root && file.isDirectory && file.listFiles().isNullOrEmpty()) file.delete()
        }
    }

    private fun removeSourceMetadata(records: List<DownloadComic>) {
        val roots = records.flatMap { listOf(it.coverPath, it.zipPath) }
            .filter(String::isNotBlank)
            .mapNotNull(::sourceComicRoot)
            .distinct()
        roots.forEach(::removeMetadataFromRoot)
    }

    private fun sourceComicRoot(path: String): String? = runCatching {
        if (!isDocumentCachePath(path)) {
            val file = File(path)
            (if (file.isDirectory) file.parentFile else file.parentFile)?.absolutePath
        } else {
            val uri = Uri.parse(path)
            val documentId = DocumentsContract.getDocumentId(uri)
            val parentId = documentId.substringBeforeLast('/', missingDelimiterValue = "")
            parentId.takeIf(String::isNotBlank)
                ?.let { DocumentsContract.buildDocumentUriUsingTree(uri, it).toString() }
        }
    }.getOrNull()

    private fun removeMetadataFromRoot(rootPath: String) {
        if (!isDocumentCachePath(rootPath)) {
            val root = File(rootPath)
            File(root, "config.json").delete()
            File(root, "cover.webp").takeIf { it.length() == 0L }?.delete()
            if (root.isDirectory && root.listFiles().isNullOrEmpty()) root.delete()
            return
        }
        val root = Uri.parse(rootPath)
        runCatching {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(root, DocumentsContract.getDocumentId(root))
            appContext.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_SIZE,
                ),
                null, null, null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1)
                    val size = if (cursor.isNull(2)) 0L else cursor.getLong(2)
                    if (name == "config.json" || (name == "cover.webp" && size == 0L)) {
                        val child = DocumentsContract.buildDocumentUriUsingTree(root, cursor.getString(0))
                        DocumentsContract.deleteDocument(appContext.contentResolver, child)
                    }
                }
            }
            val remainingChildren = DocumentsContract.buildChildDocumentsUriUsingTree(root, DocumentsContract.getDocumentId(root))
            val isEmpty = appContext.contentResolver.query(
                remainingChildren,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null, null, null,
            )?.use { cursor -> !cursor.moveToFirst() } ?: false
            if (isEmpty) DocumentsContract.deleteDocument(appContext.contentResolver, root)
        }
    }

    private fun mimeType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "webp" -> "image/webp"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "json" -> "application/json"
        else -> "application/octet-stream"
    }
}
