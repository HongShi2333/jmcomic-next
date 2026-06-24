package com.par9uet.jm.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.par9uet.jm.database.model.ReadingProgress

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE comicId = :comicId")
    suspend fun getProgress(comicId: Int): ReadingProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ReadingProgress)

    @Query("SELECT * FROM reading_progress ORDER BY lastReadTime DESC LIMIT :limit")
    suspend fun getRecentProgress(limit: Int = 20): List<ReadingProgress>

    @Query("DELETE FROM reading_progress WHERE comicId = :comicId")
    suspend fun deleteProgress(comicId: Int)

    @Query("DELETE FROM reading_progress WHERE lastReadTime < :threshold")
    suspend fun cleanOldProgress(threshold: Long)
}
