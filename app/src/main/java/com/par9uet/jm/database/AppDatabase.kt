package com.par9uet.jm.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.par9uet.jm.database.converter.ListStringToStringConverter
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.database.dao.ReadingProgressDao
import com.par9uet.jm.database.dao.ChapterProgressDao
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.database.model.ReadingProgress
import com.par9uet.jm.database.model.ChapterProgress

@Database(entities = [DownloadComic::class, ReadingProgress::class, ChapterProgress::class], version = 5, exportSchema = false)
@TypeConverters(ListStringToStringConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadComicDao(): DownloadComicDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun chapterProgressDao(): ChapterProgressDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE download_comics ADD COLUMN parentId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE download_comics ADD COLUMN parentName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE download_comics ADD COLUMN chapterIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE download_comics ADD COLUMN chapterName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE download_comics ADD COLUMN chapterCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE download_comics SET parentId = id, parentName = name WHERE parentId = 0 AND parentName = ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reading_progress (
                        comicId INTEGER PRIMARY KEY NOT NULL,
                        chapterId INTEGER NOT NULL,
                        chapterName TEXT NOT NULL DEFAULT '',
                        pageIndex INTEGER NOT NULL,
                        totalPages INTEGER NOT NULL,
                        lastReadTime INTEGER NOT NULL,
                        isLocal INTEGER NOT NULL,
                        comicName TEXT NOT NULL DEFAULT '',
                        comicCover TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_reading_progress_lastReadTime
                    ON reading_progress(lastReadTime)
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create chapter_progress table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chapter_progress (
                        chapterId INTEGER PRIMARY KEY NOT NULL,
                        comicId INTEGER NOT NULL,
                        chapterName TEXT NOT NULL DEFAULT '',
                        pageIndex INTEGER NOT NULL,
                        totalPages INTEGER NOT NULL,
                        lastReadTime INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_chapter_progress_comicId
                    ON chapter_progress(comicId)
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_chapter_progress_lastReadTime
                    ON chapter_progress(lastReadTime)
                """.trimIndent())

                // Migrate existing reading_progress data to chapter_progress
                // Copy the last read chapter as the chapter progress
                db.execSQL("""
                    INSERT INTO chapter_progress (chapterId, comicId, chapterName, pageIndex, totalPages, lastReadTime, isCompleted)
                    SELECT chapterId, comicId, chapterName, pageIndex, totalPages, lastReadTime,
                           CASE WHEN pageIndex >= totalPages - 1 THEN 1 ELSE 0 END
                    FROM reading_progress
                """.trimIndent())
            }
        }
    }
}
