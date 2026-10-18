package com.par9uet.jm.store

import com.par9uet.jm.storage.HistorySearchStorage
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.task.AppTaskInfo
import com.par9uet.jm.utils.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HistorySearchManager(
    private val historySearchStorage: HistorySearchStorage
) : AppInitTask {

    private val _historySearchState = MutableStateFlow(listOf<String>())
    val historySearchState = _historySearchState.asStateFlow()

    fun addItem(item: String) {
        val list = listOf(item) + _historySearchState.value.filterNot { it == item }
        _historySearchState.update {
            list
        }
        historySearchStorage.set(list)
    }

    fun clear() {
        historySearchStorage.remove()
        _historySearchState.update {
            historySearchStorage.get()
        }
    }

    override suspend fun init() {
        log("加载历史搜索数据")
        _historySearchState.update {
            historySearchStorage.get()
        }
        log("已加载历史搜索数据")
    }

    private var appTaskInfo = AppTaskInfo(
        taskName = "加载历史搜索数据",
        sort = 4,
    )

    override fun getAppTaskInfo(): AppTaskInfo = appTaskInfo
}