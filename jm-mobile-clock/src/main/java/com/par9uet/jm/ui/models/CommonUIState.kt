package com.par9uet.jm.ui.models

data class CommonUIState<T>(
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val data: T? = null,
    val errorMsg: String? = null
) {}
