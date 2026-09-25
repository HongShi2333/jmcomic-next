package com.par9uet.jm

import android.app.Application
import com.par9uet.jm.di.appModule
import com.par9uet.jm.di.coilModule
import com.par9uet.jm.di.comicModule
import com.par9uet.jm.di.databaseModule
import com.par9uet.jm.di.retrofitModule
import com.par9uet.jm.di.userModule
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.cache.migrateLegacyDownloadCache
import com.par9uet.jm.cache.setCacheMigrationRunning
import com.par9uet.jm.utils.ensureAppNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.io.File

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

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            setCacheMigrationRunning(this@JmApplication, true)
            try {
                val movedRoots = migrateLegacyDownloadCache(this@JmApplication)
                if (movedRoots.isNotEmpty()) {
                    val dao = GlobalContext.get().get<DownloadComicDao>()
                    dao.getAll().forEach { record ->
                        val remap: (String) -> String = { path ->
                            movedRoots.entries.firstOrNull { (oldRoot, _) ->
                                path == oldRoot || path.startsWith(oldRoot + File.separator)
                            }?.let { (oldRoot, newRoot) -> newRoot + path.removePrefix(oldRoot) }
                                ?: path
                        }
                        val newCoverPath = remap(record.coverPath)
                        val newZipPath = remap(record.zipPath)
                        if (newCoverPath != record.coverPath || newZipPath != record.zipPath) {
                            dao.update(record.copy(coverPath = newCoverPath, zipPath = newZipPath))
                        }
                    }
                }
            } finally {
                setCacheMigrationRunning(this@JmApplication, false)
            }
        }
    }
}
