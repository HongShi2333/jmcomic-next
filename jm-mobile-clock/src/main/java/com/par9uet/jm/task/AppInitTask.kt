package com.par9uet.jm.task

data class AppTaskInfo(
    val taskName: String,
    val sort: Int,
    val isError: Boolean = false,
    val errorMsg: String = ""
)

interface AppInitTask {
    suspend fun init()

    fun getAppTaskInfo(): AppTaskInfo
}