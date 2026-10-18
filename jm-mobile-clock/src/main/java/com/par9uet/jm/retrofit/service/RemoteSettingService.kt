package com.par9uet.jm.retrofit.service

import com.par9uet.jm.retrofit.annotation.GInit
import com.par9uet.jm.retrofit.model.RemoteSettingResponse
import com.par9uet.jm.retrofit.model.ResponseWrapper
import retrofit2.http.GET

interface RemoteSettingService {
    @GInit
    @GET("setting")
    suspend fun getRemoteSetting(): ResponseWrapper<RemoteSettingResponse>
}