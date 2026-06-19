package com.par9uet.jm

import android.app.Application
import com.par9uet.jm.di.appModule
import com.par9uet.jm.di.coilModule
import com.par9uet.jm.di.comicModule
import com.par9uet.jm.di.databaseModule
import com.par9uet.jm.di.retrofitModule
import com.par9uet.jm.di.userModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

private val moduleList = listOf(
    appModule,
    coilModule,
    comicModule,
    retrofitModule,
    userModule,
    databaseModule
)

class JmApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@JmApplication)
            workManagerFactory()
            modules(moduleList)
        }
    }
}
