package com.par9uet.jm.ui.models

data class ListData<T>(
    val mutableList: MutableList<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)
