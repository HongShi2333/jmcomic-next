package com.par9uet.jm.di

import com.par9uet.jm.retrofit.Retrofit
import com.par9uet.jm.retrofit.converter.PrimitiveToRequestBodyConverterFactory
import com.par9uet.jm.retrofit.converter.ResponseConverterFactory
import com.par9uet.jm.retrofit.interceptor.BaseUrlInterceptor
import com.par9uet.jm.retrofit.interceptor.InitInterceptor
import com.par9uet.jm.retrofit.interceptor.ToastInterceptor
import com.par9uet.jm.retrofit.interceptor.TokenInterceptor
import com.par9uet.jm.retrofit.service.ComicService
import com.par9uet.jm.retrofit.service.RemoteSettingService
import com.par9uet.jm.retrofit.service.UserService
import com.par9uet.jm.task.AppInitTask
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.converter.scalars.ScalarsConverterFactory

val retrofitModule = module {
    single {
        Retrofit(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind AppInitTask::class
    single<ComicService> { get<Retrofit>().createService(ComicService::class.java) }
    single<RemoteSettingService> { get<Retrofit>().createService(RemoteSettingService::class.java) }
    single<UserService> { get<Retrofit>().createService(UserService::class.java) }
    single { BaseUrlInterceptor(get()) }
    single { TokenInterceptor(get()) }
    single { InitInterceptor(get()) }
    single { ToastInterceptor(get()) }
    single { ResponseConverterFactory(get()) }
    single { PrimitiveToRequestBodyConverterFactory() }
    single { ScalarsConverterFactory.create() }
}
