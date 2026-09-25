package com.par9uet.jm.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.utils.tryCreateDir
import java.io.File

private const val CONFIG_FILE_NAME = "config.json"
private const val COVER_FILE_NAME = "cover.webp"

data class DownloadComicCacheConfig(
    @SerializedName("formatVersion")
    val formatVersion: Int = 2,
    @SerializedName(value = "id", alternate = ["a"])
    val id: Int,
    @SerializedName(value = "title", alternate = ["b"])
    val title: String,
    @SerializedName(value = "authors", alternate = ["c"])
    val authors: List<String>,
    @SerializedName(value = "tags", alternate = ["d"])
    val tags: List<String>,
    @SerializedName(value = "cachePath", alternate = ["e"])
    val cachePath: String,
    @SerializedName(value = "coverPath", alternate = ["f"])
    val coverPath: String,
    @SerializedName(value = "configFileName", alternate = ["g"])
    val configFileName: String = CONFIG_FILE_NAME,
    @SerializedName(value = "coverFileName", alternate = ["h"])
    val coverFileName: String = COVER_FILE_NAME,
    @SerializedName(value = "chapterCount", alternate = ["i"])
    val chapterCount: Int = 0,
    @SerializedName(value = "imageCount", alternate = ["j"])
    val imageCount: Int = 0,
    @SerializedName(value = "chapters", alternate = ["k"])
    val chapters: List<DownloadComicCacheChapter>,
)

data class DownloadComicCacheChapter(
    @SerializedName(value = "id", alternate = ["a"])
    val id: Int,
    @SerializedName(value = "name", alternate = ["b"])
    val name: String,
    @SerializedName(value = "path", alternate = ["c"])
    val path: String,
    @SerializedName(value = "status", alternate = ["d"])
    val status: String,
    @SerializedName(value = "imageCount", alternate = ["e"])
    val imageCount: Int,
    @SerializedName(value = "imageFiles", alternate = ["f"])
    val imageFiles: List<String> = emptyList(),
)

fun getComicDownloadRootDir(context: Context, comic: DownloadComic): File {
    return getComicDownloadRootDir(context, comic.groupName.ifBlank { comic.name })
}

fun getComicDownloadRootDir(context: Context, comicName: String): File {
    val persistentRoot = File(getDownloadDir(context), safeCacheFileName(comicName))
    if (!persistentRoot.exists()) {
        val legacyRoot = File(getLegacyDownloadDir(context), safeCacheFileName(comicName))
        if (legacyRoot.exists()) {
            // The application startup sweep updates database paths together
            // with the directory move. Until that sweep completes, keep the
            // legacy root readable instead of moving it behind stale DAO paths.
            return legacyRoot
        }
    }
    return tryCreateDir(persistentRoot)
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
    val chapterTitle = comic.chapterName
        .ifBlank { comic.name }
        .ifBlank { "单篇" }
    // Chapter names are not guaranteed to be unique (some API responses leave
    // them blank). Include the chapter id so every multi-chapter task receives
    // its own directory instead of reusing the first chapter's 0.webp files.
    return safeCacheFileName("$chapterTitle-${comic.id}")
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
            imageFiles = listComicImageFiles(chapterDir).map(File::getName),
        )
    }
    val config = DownloadComicCacheConfig(
        id = comic.groupId.takeIf { it != 0 } ?: comic.id,
        title = comic.groupName.ifBlank { comic.name },
        authors = comic.authorList,
        tags = comic.tagList,
        cachePath = rootDir.absolutePath,
        coverPath = getComicCoverDownloadFile(context, comic).absolutePath,
        chapterCount = chapterConfigs.size,
        imageCount = chapterConfigs.sumOf { it.imageCount },
        chapters = chapterConfigs,
    )
    getComicConfigFile(context, comic).writeText(gson.toJson(config), Charsets.UTF_8)
}

fun buildComicCacheConfig(
    comic: DownloadComic,
    chapters: List<DownloadComic>,
    rootPath: String,
    coverPath: String,
    imageFiles: (String) -> List<String>,
): DownloadComicCacheConfig {
    val chapterConfigs = chapters.sortedBy { it.createTime }.map { chapter ->
        val path = chapter.zipPath.ifBlank { rootPath }
        val files = imageFiles(path)
        DownloadComicCacheChapter(
            id = chapter.id,
            name = chapter.chapterName.ifBlank { if (chapters.size > 1) chapter.name else "单篇" },
            path = path,
            status = chapter.status,
            imageCount = files.size,
            imageFiles = files,
        )
    }
    return DownloadComicCacheConfig(
        id = comic.groupId.takeIf { it != 0 } ?: comic.id,
        title = comic.groupName.ifBlank { comic.name },
        authors = comic.authorList,
        tags = comic.tagList,
        cachePath = rootPath,
        coverPath = coverPath,
        chapterCount = chapterConfigs.size,
        imageCount = chapterConfigs.sumOf { it.imageCount },
        chapters = chapterConfigs,
    )
}

fun safeCacheFileName(name: String): String {
    val cleaned = name
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trimEnd('.')
    return cleaned.ifBlank { "未命名漫画" }
}
