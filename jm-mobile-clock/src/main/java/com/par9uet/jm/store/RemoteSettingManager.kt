package com.par9uet.jm.store

import com.par9uet.jm.data.models.RemoteSetting
import com.par9uet.jm.repository.RemoteSettingRepository
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.RemoteSettingResponse
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.task.AppTaskInfo
import com.par9uet.jm.utils.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RemoteSettingManager(
    private val remoteSettingRepository: RemoteSettingRepository
) : AppInitTask {
    private val _remoteSettingState = MutableStateFlow(RemoteSetting(
        imgHost = ""
    ))
    val remoteSettingState = _remoteSettingState.asStateFlow()

    private var appTaskInfo = AppTaskInfo(
        taskName = "加载 app 远端应用数据",
        sort = 2,
    )

    private suspend fun getRemoteSetting() {
        when (val data = remoteSettingRepository.getRemoteSetting()) {
            is NetWorkResult.Error -> {
                log("获取远程应用设置失败")
                appTaskInfo = appTaskInfo.copy(
                    isError = true,
                    errorMsg = data.message
                )
            }

            is NetWorkResult.Success<RemoteSettingResponse> -> {
                log("获取远程应用设置成功")
                _remoteSettingState.update {
                    data.data.toRemoteSetting()
                }
                appTaskInfo = appTaskInfo.copy(
                    isError = false,
                    errorMsg = "",
                )
            }
        }
    }

    override suspend fun init() {
        log("远程应用设置开始初始化")
        getRemoteSetting()
        log("远程应用设置初始化结束")
    }

    override fun getAppTaskInfo(): AppTaskInfo = appTaskInfo
}