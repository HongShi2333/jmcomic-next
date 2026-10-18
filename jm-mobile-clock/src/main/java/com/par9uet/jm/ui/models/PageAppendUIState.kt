package com.par9uet.jm.ui.models

data class PageAppendUIState<T>(
    val isInitial: Boolean = true,
    val isLoading: Boolean = false,
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Int = 0,
    val list: MutableList<T> = mutableListOf(),
    val isError: Boolean = false,
    val errMsg: String = ""
) {
    // 已有数据，刷新页面
    val isRefreshing: Boolean get() = list.isNotEmpty() && isLoading && page == 1
    val hasData: Boolean get() = list.isNotEmpty()
    val hasMore: Boolean get() = list.size < total

    fun append(appendList: List<T>): PageAppendUIState<T> {
        list.addAll(appendList)
        return copy(
            list = list,
            isLoading = false,
            isError = false,
            total = total + appendList.size,
        )
    }

    fun startLoading(): PageAppendUIState<T> = copy(
        isLoading = true,
        isError = false,
        errMsg = ""
    )

    fun setError(msg: String): PageAppendUIState<T> = copy(
        isError = true,
        errMsg = msg
    )

    fun nextPage(): PageAppendUIState<T> = setPage(page + 1)

    fun setPage(page: Int): PageAppendUIState<T> = copy(
        page = page
    )
}
