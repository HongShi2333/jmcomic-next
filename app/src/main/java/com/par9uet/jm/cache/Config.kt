package com.par9uet.jm.cache

import android.content.Context
import com.par9uet.jm.utils.tryCreateDir
import java.io.File

private const val DOWNLOAD_TREE_PREFERENCES = "download_storage"
private const val DOWNLOAD_TREE_URI_KEY = "tree_uri"
private const val CACHE_MIGRATION_RUNNING_KEY = "migration_running"

fun getCommonCacheDir(context: Context) = tryCreateDir(File(context.cacheDir, "common"))
/** Coil's disposable cover/image cache. It lives under cacheDir and may be evicted by Android. */
fun getComicCoverCacheDir(context: Context) = tryCreateDir(File(context.cacheDir, "comic_covers"))
fun getCommonPicDecodeCacheDir(context: Context, comicId: Int) = tryCreateDir(File(context.cacheDir, "pic_decode/$comicId"))

/**
 * Manga downloads are user data, not disposable process cache. Android may
 * delete cacheDir at any time, which used to remove config.json/cover.webp
 * while the database still pointed at those paths. Keep only transient image
 * and decode data in cacheDir; store manga files under filesDir instead.
 */
fun getDownloadDir(context: Context) = tryCreateDir(File(context.filesDir, "download"))

/** The pre-1.2.5 location, retained for one-time lazy migration. */
fun getLegacyDownloadDir(context: Context): File = File(context.cacheDir, "download")

/**
 * Move legacy manga roots out of cacheDir as early as possible. This runs in a
 * background application coroutine; per-comic access still has a lazy rename
 * fallback for the short window before this sweep finishes.
 */
fun migrateLegacyDownloadCache(context: Context): Map<String, String> {
    val legacyRoot = getLegacyDownloadDir(context)
    if (!legacyRoot.isDirectory) return emptyMap()
    val persistentRoot = getDownloadDir(context)
    val movedRoots = linkedMapOf<String, String>()
    legacyRoot.listFiles().orEmpty().forEach { source ->
        val target = File(persistentRoot, source.name)
        if (target.exists()) return@forEach
        if (!source.renameTo(target)) {
            // Keep the source intact when a provider/filesystem refuses the
            // rename; getComicDownloadRootDir will continue to read it.
            return@forEach
        }
        movedRoots[source.absolutePath] = target.absolutePath
    }
    if (legacyRoot.listFiles().isNullOrEmpty()) legacyRoot.delete()
    return movedRoots
}

fun getDownloadTreeUri(context: Context): android.net.Uri? =
    context.getSharedPreferences(DOWNLOAD_TREE_PREFERENCES, Context.MODE_PRIVATE)
        .getString(DOWNLOAD_TREE_URI_KEY, null)
        ?.takeIf { it.isNotBlank() }
        ?.let(android.net.Uri::parse)

fun setDownloadTreeUri(context: Context, uri: String) {
    context.getSharedPreferences(DOWNLOAD_TREE_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putString(DOWNLOAD_TREE_URI_KEY, uri)
        .apply()
}

fun isCacheMigrationRunning(context: Context): Boolean =
    context.getSharedPreferences(DOWNLOAD_TREE_PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(CACHE_MIGRATION_RUNNING_KEY, false)

fun setCacheMigrationRunning(context: Context, running: Boolean) {
    context.getSharedPreferences(DOWNLOAD_TREE_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(CACHE_MIGRATION_RUNNING_KEY, running)
        .apply()
}
