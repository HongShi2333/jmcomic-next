package com.par9uet.jm.cache

import android.content.Context
import com.google.gson.Gson
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.utils.tryCreateDir
import java.io.File

private const val CONFIG_FILE_NAME = "config.json"
private const val COVER_FILE_NAME = "cover.webp"

data class DownloadComicCacheConfig(
    val id: Int,
    val title: String,
    val authors: List<String>,
    val tags: List<String>,
    val cachePath: String,
    val coverPath: String,
    val chapters: List<DownloadComicCacheChapter>,
)

data class DownloadComicCacheChapter(
    val id: Int,
    val name: String,
    val path: String,
    val status: String,
    val imageCount: Int,
)

fun getComicDownloadRootDir(context: Context, comic: DownloadComic): File {
    return getComicDownloadRootDir(context, comic.groupName.ifBlank { comic.name })
}

fun getComicDownloadRootDir(context: Context, comicName: String): File {
    return tryCreateDir(File(getDownloadDir(context), safeCacheFileName(comicName)))
}

fun getComicChapterDownloadDir(context: Context, comic: DownloadComic): File {
    return tryCreateDir(File(getComicDownloadRootDir(context, comic), getChapterCacheName(comic)))
}

fun getComicCoverDownloadFile(context: Context, comic: DownloadComic): File {
    return File(getComicDownloadRootDir(context, comic), COVER_FILE_NAME)
}

fun getComicConfigFile(context: Context, comic: DownloadComic): File {
    return File(getComicDownloadRootDir(context, comic), CONFIG_FILE_NAME)
}

fun getChapterCacheName(comic: DownloadComic): String {
    return safeCacheFileName(comic.chapterName.ifBlank { "单篇" })
}

fun listComicImageFiles(dir: File): List<File> {
    return dir.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in setOf("webp", "jpg", "jpeg", "png") }
        ?.sortedWith(compareBy<File> { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.name })
        .orEmpty()
}

fun writeComicCacheConfig(
    context: Context,
    comic: DownloadComic,
    chapters: List<DownloadComic>,
    gson: Gson = Gson()
) {
    val rootDir = getComicDownloadRootDir(context, comic)
    val chapterConfigs = chapters.sortedBy { it.createTime }.map { chapter ->
        val chapterDir = chapter.zipPath.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isDirectory }
            ?: File(rootDir, getChapterCacheName(chapter))
        DownloadComicCacheChapter(
            id = chapter.id,
            name = chapter.chapterName.ifBlank { if (chapters.size > 1) chapter.name else "单篇" },
            path = chapterDir.absolutePath,
            status = chapter.status,
            imageCount = listComicImageFiles(chapterDir).size,
        )
    }
    val config = DownloadComicCacheConfig(
        id = comic.groupId.takeIf { it != 0 } ?: comic.id,
        title = comic.groupName.ifBlank { comic.name },
        authors = comic.authorList,
        tags = comic.tagList,
        cachePath = rootDir.absolutePath,
        coverPath = getComicCoverDownloadFile(context, comic).absolutePath,
        chapters = chapterConfigs,
    )
    getComicConfigFile(context, comic).writeText(gson.toJson(config), Charsets.UTF_8)
}

fun safeCacheFileName(name: String): String {
    val cleaned = name
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trimEnd('.')
    return cleaned.ifBlank { "未命名漫画" }
}
