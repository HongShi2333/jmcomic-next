package com.par9uet.jm.repository.impl

import com.par9uet.jm.repository.BaseRepository
import com.par9uet.jm.repository.RemoteSettingRepository
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.RemoteSettingResponse
import com.par9uet.jm.retrofit.service.RemoteSettingService
import com.par9uet.jm.store.InitManager

class RemoteSettingRepositoryImpl(
    private val service: RemoteSettingService,
    initManager: InitManager
) : BaseRepository(initManager), RemoteSettingRepository {
    override suspend fun getRemoteSetting(): NetWorkResult<RemoteSettingResponse> {
        return safeApiCall {
            service.getRemoteSetting()
        }
    }
}