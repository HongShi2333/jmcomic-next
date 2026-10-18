package com.par9uet.jm.ui.models

data class ListUIState<T>(
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val list: List<T> = listOf(),
    val errorMsg: String? = null
) {
    val isRefreshing get() = list.isNotEmpty() && isLoading
    val isFirstInit get() = list.isEmpty() && isLoading
}
