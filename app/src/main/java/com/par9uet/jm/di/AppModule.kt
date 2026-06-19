package com.par9uet.jm.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.par9uet.jm.repository.RemoteSettingRepository
import com.par9uet.jm.repository.AiChatRepository
import com.par9uet.jm.repository.impl.RemoteSettingRepositoryImpl
import com.par9uet.jm.storage.AiChatStorage
import com.par9uet.jm.storage.CookieStorage
import com.par9uet.jm.storage.HistorySearchStorage
import com.par9uet.jm.storage.LocalSettingStorage
import com.par9uet.jm.storage.SecureStorage
import com.par9uet.jm.storage.UserStorage
import com.par9uet.jm.store.HistorySearchManager
import com.par9uet.jm.store.InitManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.RemoteSettingManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.ui.viewModel.GlobalViewModel
import com.par9uet.jm.ui.viewModel.AiChatViewModel
import com.par9uet.jm.utils.log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            log("全局协程捕获到了异常: $throwable")
        })
    }

    single { SecureStorage(get()) }
    single { UserStorage(get()) }
    single { CookieStorage(get()) }
    single { LocalSettingStorage(get()) }
    single { HistorySearchStorage(get()) }
    single { AiChatStorage(get()) }

    single { RemoteSettingRepositoryImpl(get(), get()) } bind RemoteSettingRepository::class
    single { AiChatRepository(get()) }

    single { UserManager(get(), get(), get(), get()) } bind AppInitTask::class
    single { RemoteSettingManager(get()) } bind AppInitTask::class
    single { LocalSettingManager(get()) } bind AppInitTask::class
    single { HistorySearchManager(get()) } bind AppInitTask::class
    single { ToastManager() }
    single { InitManager() }

    single<Gson> { GsonBuilder().setStrictness(Strictness.LENIENT).serializeNulls().create() }

    viewModel { GlobalViewModel(getAll(), get()) }
    viewModel { AiChatViewModel(get(), get()) }
}
