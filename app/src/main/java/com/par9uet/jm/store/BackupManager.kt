package com.par9uet.jm.store

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiPersona
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.utils.logError
import java.security.MessageDigest

// 备份保护方式
const val BACKUP_PROTECTION_NONE = "none"
const val BACKUP_PROTECTION_PASSWORD = "password"
const val BACKUP_PROTECTION_PATTERN = "pattern"
const val BACKUP_PROTECTION_BOTH = "both"

// 备份文件格式版本（v4 新增可选的本机生物识别绑定）
const val BACKUP_FORMAT_VERSION = 4

/**
 * 用户选择要备份的内容类型。
 */
data class BackupContentOptions(
    val includeLocalSetting: Boolean = true,
    val includeDownloadPath: Boolean = false,
    val includeAiChats: Boolean = false,
    val includePersonas: Boolean = false,
    val includeComicCache: Boolean = false,
) {
    val isEmpty: Boolean get() = !includeLocalSetting && !includeDownloadPath && !includeAiChats && !includePersonas && !includeComicCache
}

/**
 * 备份文件元信息：包含版本、时间戳、保护方式与备份内容标记。
 */
data class BackupMeta(
    val version: Int = BACKUP_FORMAT_VERSION,
    val timestamp: Long = System.currentTimeMillis(),
    val protectionType: String = BACKUP_PROTECTION_NONE,
    val passwordHash: String? = null,
    val patternHash: String? = null,
    // 可选的本机生物识别快捷验证绑定；密码/图形仍是跨设备恢复的保底凭据
    val biometricBinding: String? = null,
    val includeLocalSetting: Boolean = true,
    val includeDownloadPath: Boolean = false,
    val includeAiChats: Boolean = false,
    val includePersonas: Boolean = false,
    val includeComicCache: Boolean = false,
    val comicCacheCount: Int = 0,
)

/**
 * 缓存目录备份：单个章节的信息。
 * 不包含图片文件，只保留章节 ID（即漫画 ID）和排序信息。
 */
data class ChapterBackup(
    val id: Int,
    val name: String,
    val sortOrder: Long,
)

/**
 * 缓存目录备份：一个漫画组的信息。
 * 多章节漫画会包含多个 ChapterBackup；单篇漫画 chapters 列表只有一个元素（id 与 groupId 相同）。
 */
data class ComicGroupBackup(
    val id: Int,
    val name: String,
    val authors: List<String>,
    val tags: List<String>,
    val chapters: List<ChapterBackup>,
) {
    val chapterCount: Int get() = chapters.size
}

/**
 * 缓存目录备份整体结构。
 */
data class ComicCacheBackup(
    val groups: List<ComicGroupBackup> = emptyList(),
)

/**
 * 备份文件结构：meta + data。
 * v2: data 下分 localSetting / aiChats / aiPersonas 三段。
 * v1（兼容旧文件）: data 直接是 LocalSetting 的 JSON。
 */
data class BackupFile(
    val meta: BackupMeta,
    val data: JsonObject,
)

class BackupManager {
    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    /**
     * 创建备份 JSON 字符串。
     */
    fun createBackup(
        localSetting: LocalSetting?,
        aiChats: List<AiChatConversation>?,
        personas: List<AiPersona>?,
        comicCache: ComicCacheBackup? = null,
        options: BackupContentOptions,
        protectionType: String = BACKUP_PROTECTION_PASSWORD,
        password: String? = null,
        pattern: String? = null,
        biometricBinding: String? = null,
    ): String {
        require(!options.isEmpty) { "至少需要选择一项备份内容" }
        require(protectionType != BACKUP_PROTECTION_NONE) { "备份必须设置密码或图形保护" }
        val meta = BackupMeta(
            version = BACKUP_FORMAT_VERSION,
            timestamp = System.currentTimeMillis(),
            protectionType = protectionType,
            passwordHash = when (protectionType) {
                BACKUP_PROTECTION_PASSWORD, BACKUP_PROTECTION_BOTH -> {
                    requireNotNull(password) { "password must not be null for protection $protectionType" }
                    sha256(password)
                }
                else -> null
            },
            patternHash = when (protectionType) {
                BACKUP_PROTECTION_PATTERN, BACKUP_PROTECTION_BOTH -> {
                    requireNotNull(pattern) { "pattern must not be null for protection $protectionType" }
                    sha256(pattern)
                }
                else -> null
            },
            biometricBinding = biometricBinding,
            includeLocalSetting = options.includeLocalSetting,
            includeDownloadPath = options.includeDownloadPath,
            includeAiChats = options.includeAiChats,
            includePersonas = options.includePersonas,
            includeComicCache = options.includeComicCache && comicCache != null,
            comicCacheCount = comicCache?.groups?.size ?: 0,
        )

        val data = JsonObject()
        if ((options.includeLocalSetting || options.includeDownloadPath) && localSetting != null) {
            val sanitized = localSetting.copy(
                appLockPassword = "",
                appLockPattern = "",
                appLockDuressEnabled = false,
                appLockDuressPassword = "",
                appLockDuressPattern = "",
                downloadTreeUri = if (options.includeDownloadPath) localSetting.downloadTreeUri else "",
            )
            data.add("localSetting", gson.toJsonTree(sanitized))
        }
        if (options.includeAiChats && aiChats != null) {
            data.add("aiChats", gson.toJsonTree(aiChats))
        }
        if (options.includePersonas && personas != null) {
            data.add("aiPersonas", gson.toJsonTree(personas))
        }
        if (options.includeComicCache && comicCache != null) {
            data.add("comicCache", gson.toJsonTree(comicCache))
        }

        val backup = BackupFile(meta = meta, data = data)
        return gson.toJson(backup)
    }

    /**
     * 解析备份 JSON 字符串，兼容 v1/v2。
     */
    fun parseBackup(json: String): Result<BackupFile> = runCatching {
        val obj = JsonParser.parseString(json).asJsonObject
        val meta = gson.fromJson(obj.getAsJsonObject("meta"), BackupMeta::class.java)
            ?: error("备份文件缺少 meta 字段")
        val data = obj.getAsJsonObject("data") ?: error("备份文件缺少 data 字段")
        BackupFile(meta = meta, data = data)
    }

    /**
     * 从备份中提取 [LocalSetting]，兼容 v1 旧格式。
     */
    fun extractLocalSetting(backup: BackupFile): LocalSetting? {
        // v2 格式：data.localSetting
        val obj = backup.data.getAsJsonObject("localSetting")
        if (obj != null) return gson.fromJson(obj, LocalSetting::class.java)
        // v1 旧格式：data 直接是 LocalSetting
        if (backup.meta.version <= 1) {
            return runCatching { gson.fromJson(backup.data, LocalSetting::class.java) }.getOrNull()
        }
        return null
    }

    /**
     * 从备份中提取 AI 聊天记录。
     */
    fun extractAiChats(backup: BackupFile): List<AiChatConversation> {
        val arr = backup.data.getAsJsonArray("aiChats") ?: return emptyList()
        return runCatching {
            gson.fromJson(arr, Array<AiChatConversation>::class.java)?.toList() ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * 从备份中提取 AI 人格面具。
     */
    fun extractPersonas(backup: BackupFile): List<AiPersona> {
        val arr = backup.data.getAsJsonArray("aiPersonas") ?: return emptyList()
        return runCatching {
            gson.fromJson(arr, Array<AiPersona>::class.java)?.toList() ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * 从备份中提取缓存目录备份信息。
     * v1/v2 旧备份无此段，返回空。
     */
    fun extractComicCache(backup: BackupFile): ComicCacheBackup {
        val obj = backup.data.getAsJsonObject("comicCache") ?: return ComicCacheBackup()
        return runCatching {
            gson.fromJson(obj, ComicCacheBackup::class.java) ?: ComicCacheBackup()
        }.getOrDefault(ComicCacheBackup())
    }

    fun needsPassword(backup: BackupFile): Boolean {
        return backup.meta.protectionType == BACKUP_PROTECTION_PASSWORD ||
            backup.meta.protectionType == BACKUP_PROTECTION_BOTH
    }

    fun needsPattern(backup: BackupFile): Boolean {
        return backup.meta.protectionType == BACKUP_PROTECTION_PATTERN ||
            backup.meta.protectionType == BACKUP_PROTECTION_BOTH
    }

    /**
     * 校验密码（用于恢复时的核验）。
     */
    fun verifyPassword(backup: BackupFile, password: String): Boolean {
        val expected = backup.meta.passwordHash ?: return false
        return constantEquals(expected, sha256(password))
    }

    fun verifyPattern(backup: BackupFile, pattern: String): Boolean {
        val expected = backup.meta.patternHash ?: return false
        return constantEquals(expected, sha256(pattern))
    }

    fun readFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrElse {
            logError("BackupManager", "读取备份文件失败: ${it.message}")
            null
        }
    }

    /**
     * 从 DownloadComicDao 的数据生成缓存目录备份。
     * 只备份漫画编号与章节信息，不备份图片文件本身。
     */
    suspend fun buildComicCacheBackup(
        allDownloads: List<com.par9uet.jm.database.model.DownloadComic>
    ): ComicCacheBackup {
        // 按 groupId 聚合（单篇漫画 groupId=0，以自身 id 作为组 ID）
        val grouped = allDownloads.groupBy { it.groupId.takeIf { g -> g != 0 } ?: it.id }
        val groups = grouped.map { (groupId, items) ->
            val first = items.first()
            val chapters = items
                .sortedBy { it.createTime }
                .map { item ->
                    ChapterBackup(
                        id = item.id,
                        name = item.chapterName,
                        sortOrder = item.createTime,
                    )
                }
            ComicGroupBackup(
                id = groupId,
                name = first.groupName.ifBlank { first.name },
                authors = first.authorList,
                tags = first.tagList,
                chapters = chapters,
            )
        }.sortedBy { it.id }
        return ComicCacheBackup(groups = groups)
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                true
            } ?: false
        }.getOrElse {
            logError("BackupManager", "写入备份文件失败: ${it.message}")
            false
        }
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
