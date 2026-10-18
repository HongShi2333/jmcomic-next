package com.par9uet.jm.storage

import android.content.Context
import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiSearchSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class AiChatStorage(
    private val secureStorage: SecureStorage,
    context: Context,
) {
    companion object {
        private const val STORAGE_KEY = "aiChatConversations"
        private const val SEARCH_SETTINGS_KEY = "aiSearchSettings"
    }

    private val _state = MutableStateFlow<List<AiChatConversation>?>(null)
    val state = _state.asStateFlow()
    private val _searchSettingsState = MutableStateFlow<AiSearchSettings?>(null)
    val searchSettingsState = _searchSettingsState.asStateFlow()
    private val directory = File(context.filesDir, "ai-history")
    private val gson = Gson()
    private val crypto = CryptoManager()
    private val writes = Channel<List<AiChatConversation>>(Channel.CONFLATED)
    private val _writeError = MutableStateFlow<String?>(null)
    val writeError = _writeError.asStateFlow()
    private var persisted = emptyMap<String, AiChatConversation>()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            for (snapshot in writes) {
                try {
                    val next = snapshot.associateBy { it.id }
                    next.values.forEach { conversation ->
                        if (persisted[conversation.id] !== conversation) {
                            writeFile(conversationFile(conversation.id), gson.toJson(conversation.toArchive()))
                        }
                    }
                    writeFile(File(directory, "index"), gson.toJson(snapshot.map { it.id }))
                    // Migration is committed only after the durable index exists.
                    secureStorage.remove(STORAGE_KEY)
                    (persisted.keys - next.keys).forEach { AtomicFile(conversationFile(it)).delete() }
                    persisted = next
                    _writeError.value = null
                } catch (_: OutOfMemoryError) {
                    _writeError.value = "聊天记录保存时内存不足，原记录仍保留，请稍后重试"
                } catch (_: StackOverflowError) {
                    _writeError.value = "聊天分支过深，原记录仍保留"
                } catch (error: Exception) {
                    _writeError.value = error.message ?: "聊天记录保存失败，原记录仍保留"
                }
            }
        }
    }

    @Synchronized
    fun get(): List<AiChatConversation> {
        if (_state.value == null) {
            val index = File(directory, "index")
            val conversations = if (index.exists() || File(directory, "index.bak").exists()) {
                val ids = gson.fromJson(readFile(index), Array<String>::class.java)
                ids.map { id ->
                    gson.fromJson(readFile(conversationFile(id)), AiChatArchive::class.java)
                        .toConversation().also { require(it.id == id) }
                }.also { persisted = it.associateBy { conversation -> conversation.id } }
            } else {
                // Keep legacy data intact if decoding fails; do not overwrite it with an empty list.
                val legacy = secureStorage.getString(STORAGE_KEY)
                if (legacy == null && secureStorage.sharedPreferences.contains(STORAGE_KEY)) {
                    throw IOException("旧聊天记录无法解密，原记录已保留")
                }
                legacy?.let { json ->
                    gson.fromJson<List<AiChatConversation>>(
                        json, object : TypeToken<List<AiChatConversation>>() {}.type,
                    ).map { it.toArchive().toConversation() }
                } ?: emptyList()
            }
            _state.value = conversations
        }
        return _state.value.orEmpty()
    }

    fun set(conversations: List<AiChatConversation>) {
        _state.update { conversations }
        writes.trySend(conversations)
    }

    fun getSearchSettings(): AiSearchSettings {
        if (_searchSettingsState.value == null) {
            _searchSettingsState.update {
                runCatching {
                    secureStorage.get<AiSearchSettings>(
                        SEARCH_SETTINGS_KEY,
                        object : TypeToken<AiSearchSettings>() {}.type
                    )
                }.getOrNull()?.normalized() ?: AiSearchSettings()
            }
        }
        return _searchSettingsState.value ?: AiSearchSettings()
    }

    fun setSearchSettings(settings: AiSearchSettings) {
        val normalized = settings.normalized()
        _searchSettingsState.update { normalized }
        secureStorage.set(SEARCH_SETTINGS_KEY, normalized)
    }

    fun remove() {
        set(emptyList())
    }

    private fun conversationFile(id: String): File {
        val key = MessageDigest.getInstance("SHA-256").digest(id.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$key.chat")
    }

    private fun readFile(file: File): String =
        crypto.decrypt(AtomicFile(file).openRead().bufferedReader().use { it.readText() })
            ?: throw IOException("无法读取聊天记录，原文件已保留")

    private fun writeFile(file: File, json: String) {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("无法创建聊天记录目录")
        val data = crypto.encrypt(json).toByteArray()
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(data)
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }
}
