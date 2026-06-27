package com.par9uet.jm.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration(false)
            .build()
    }
    single { get<AppDatabase>().downloadComicDao() }
    single { DownloadManager(get(), get(), get(), get()) }
    viewModel { DownloadViewModel(get()) }
    viewModel { DownloadComicDetailViewModel(get()) }

    worker { DownloadComicWorker(get(), get(), get(), get(), get(), get(), get()) }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE download_comics ADD COLUMN groupId INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE download_comics ADD COLUMN groupName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE download_comics ADD COLUMN chapterName TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE download_comics ADD COLUMN tagList TEXT NOT NULL DEFAULT '[]'")
    }
}
