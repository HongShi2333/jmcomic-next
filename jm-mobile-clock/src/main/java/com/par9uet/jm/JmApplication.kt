package com.par9uet.jm

import android.app.Application
import android.content.ComponentCallbacks2
import coil.ImageLoader
import com.par9uet.jm.di.appModule
import com.par9uet.jm.di.coilModule
import com.par9uet.jm.di.comicModule
import com.par9uet.jm.di.databaseModule
import com.par9uet.jm.di.retrofitModule
import com.par9uet.jm.di.userModule
import com.par9uet.jm.utils.ensureAppNotificationChannels
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
        ensureAppNotificationChannels(this)
        startKoin {
            androidContext(this@JmApplication)
            workManagerFactory()
            modules(moduleList)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // A watch can report pressure before the process is killed. Drop
            // only transient decoded images; downloaded manga files live in
            // filesDir and must remain untouched.
            runCatching {
                org.koin.core.context.GlobalContext.get().get<ImageLoader>().memoryCache?.clear()
            }
        }
    }
}
