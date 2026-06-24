package com.par9uet.jm.di

import androidx.room.Room
import com.par9uet.jm.database.AppDatabase
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.ui.viewModel.DownloadComicDetailViewModel
import com.par9uet.jm.ui.viewModel.DownloadViewModel
import com.par9uet.jm.worker.DownloadComicWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .fallbackToDestructiveMigration(false)
            .build()
    }
    single { get<AppDatabase>().downloadComicDao() }
    single { get<AppDatabase>().readingProgressDao() }
    single { get<AppDatabase>().chapterProgressDao() }
    single { DownloadManager(get(), get(), get(), get()) }
    viewModel { DownloadViewModel(get()) }
    viewModel { DownloadComicDetailViewModel(get()) }

    worker { DownloadComicWorker(get(), get(), get(), get(), get(), get(), get()) }
}
