package com.par9uet.jm.store

import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.storage.ComicReadHistory
import com.par9uet.jm.storage.ReadHistoryStorage
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.task.AppTaskInfo
import com.par9uet.jm.utils.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReadHistoryManager(
    private val readHistoryStorage: ReadHistoryStorage
) : AppInitTask {
    private val _readHistoryState = MutableStateFlow<Map<Int, ComicReadHistory>>(emptyMap())
    val readHistoryState = _readHistoryState.asStateFlow()

    fun historyKey(comic: Comic?, fallbackId: Int): Int {
        return comic?.seriesId
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: comic?.id
            ?: fallbackId
    }

    fun markRead(comic: Comic?, chapterId: Int): Int {
        return markRead(historyKey(comic, chapterId), chapterId)
    }

    fun markRead(comicKey: Int, chapterId: Int): Int {
        val current = _readHistoryState.value.toMutableMap()
        val old = current[comicKey]
        val readIds = (old?.readChapterIds.orEmpty() + chapterId).distinct()
        current[comicKey] = ComicReadHistory(
            lastChapterId = chapterId,
            readChapterIds = readIds
        )
        _readHistoryState.update { current }
        readHistoryStorage.set(current)
        return comicKey
    }

    fun readChapterIds(
        comicKey: Int,
        history: Map<Int, ComicReadHistory> = _readHistoryState.value
    ): Set<Int> {
        return history[comicKey]?.readChapterIds.orEmpty().toSet()
    }

    fun lastReadChapterId(
        comic: Comic,
        history: Map<Int, ComicReadHistory> = _readHistoryState.value
    ): Int? {
        val lastId = history[historyKey(comic, comic.id)]?.lastChapterId?.takeIf { it > 0 }
        if (lastId == null || comic.comicChapterList.isEmpty()) {
            return lastId
        }
        return lastId.takeIf { id -> comic.comicChapterList.any { it.id == id } }
    }

    override suspend fun init() {
        log("加载阅读历史")
        _readHistoryState.update { readHistoryStorage.get() }
        log("阅读历史已加载")
    }

    private val appTaskInfo = AppTaskInfo(
        taskName = "加载阅读历史",
        sort = 5,
    )

    override fun getAppTaskInfo(): AppTaskInfo = appTaskInfo
}
