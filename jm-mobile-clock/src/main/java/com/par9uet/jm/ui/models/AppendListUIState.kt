package com.par9uet.jm.ui.models

data class AppendListUIState<T>(
    val isRefreshing: Boolean = false,
    val isMoreLoading: Boolean = false,
    val isError: Boolean = false,
    val list: List<T> = listOf(),
    val errorMsg: String? = null,
    val page: Int = 1,
    val pageSize: Int = 80,
    val total: Int = 0
) {
    val hasMore get() = list.size < total

    fun reset() = copy(
        isRefreshing = false,
        isMoreLoading = false,
        isError = false,
        list = list,
        errorMsg = "",
    )
}
