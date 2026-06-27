package com.par9uet.jm.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.database.model.UpdateComicCover
import com.par9uet.jm.database.model.UpdateComicProgress
import com.par9uet.jm.database.model.UpdateComicStatus
import com.par9uet.jm.database.model.UpdateComicZipPath
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadComicDao {
    @Query("SELECT * FROM download_comics WHERE status = 'pending' OR status = 'downloading' ORDER BY createTime DESC")
    fun getDownloadingList(): PagingSource<Int, DownloadComic>

    @Query("SELECT * FROM download_comics WHERE status = 'complete' ORDER BY createTime DESC")
    fun getCompleteList(): PagingSource<Int, DownloadComic>

    @Query("SELECT * FROM download_comics WHERE status = 'error' ORDER BY createTime DESC")
    fun getErrorList(): PagingSource<Int, DownloadComic>

    @Query("SELECT * FROM download_comics WHERE status IN ('pending', 'downloading', 'paused') ORDER BY createTime DESC")
    fun getActiveList(): PagingSource<Int, DownloadComic>

    @Query("SELECT * FROM download_comics WHERE status = 'complete' ORDER BY createTime DESC")
    fun observeCompleteList(): Flow<List<DownloadComic>>

    @Query("SELECT * FROM download_comics WHERE status IN ('pending', 'downloading', 'paused') ORDER BY createTime DESC")
    fun observeActiveList(): Flow<List<DownloadComic>>

    @Query("SELECT * FROM download_comics WHERE status = 'error' ORDER BY createTime DESC")
    fun observeErrorList(): Flow<List<DownloadComic>>

    @Query("SELECT * FROM download_comics WHERE id = :comicId LIMIT 1")
    suspend fun getById(comicId: Int): DownloadComic?

    @Query(
        "SELECT * FROM download_comics " +
            "WHERE (groupId = :groupId OR (groupId = 0 AND id = :groupId)) " +
            "ORDER BY createTime ASC"
    )
    suspend fun getByGroupId(groupId: Int): List<DownloadComic>

    @Query(
        "SELECT * FROM download_comics " +
            "WHERE status = 'complete' AND (groupId = :groupId OR (groupId = 0 AND id = :groupId)) " +
            "ORDER BY createTime ASC"
    )
    suspend fun getCompleteByGroupId(groupId: Int): List<DownloadComic>

    @Query("SELECT id FROM download_comics WHERE id IN (:ids)")
    suspend fun getExistingIds(ids: List<Int>): List<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM download_comics WHERE id = :comicId)")
    fun isExist(comicId: Int): Flow<Boolean>

    @Update(entity = DownloadComic::class)
    suspend fun updateCover(updateComicCover: UpdateComicCover)

    @Update(entity = DownloadComic::class)
    suspend fun updateStatus(updateComicStatus: UpdateComicStatus)

    @Update(entity = DownloadComic::class)
    suspend fun updateProgress(updateComicProgress: UpdateComicProgress)

    @Update(entity = DownloadComic::class)
    suspend fun updateZipPath(updateComicZipPath: UpdateComicZipPath)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DownloadComic)

    @Update
    suspend fun update(task: DownloadComic)

    @Delete
    suspend fun delete(task: DownloadComic)

    @Query("DELETE FROM download_comics WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("UPDATE download_comics SET status = :status WHERE id IN (:ids)")
    suspend fun updateStatusByIds(ids: List<Int>, status: String)
}
