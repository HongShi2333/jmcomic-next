package com.par9uet.jm.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.par9uet.jm.database.model.ChapterProgress

@Dao
interface ChapterProgressDao {
    @Query("SELECT * FROM chapter_progress WHERE chapterId = :chapterId")
    suspend fun getProgress(chapterId: Int): ChapterProgress?

    @Query("SELECT * FROM chapter_progress WHERE comicId = :comicId ORDER BY lastReadTime DESC")
    suspend fun getProgressByComic(comicId: Int): List<ChapterProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ChapterProgress)

    @Query("DELETE FROM chapter_progress WHERE chapterId = :chapterId")
    suspend fun deleteProgress(chapterId: Int)

    @Query("DELETE FROM chapter_progress WHERE lastReadTime < :threshold")
    suspend fun cleanOldProgress(threshold: Long)
}
