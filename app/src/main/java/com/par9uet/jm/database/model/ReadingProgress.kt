package com.par9uet.jm.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
    indices = [Index(value = ["lastReadTime"])]
)
data class ReadingProgress(
    @PrimaryKey
    val comicId: Int,
    val chapterId: Int,
    val chapterName: String = "",
    val pageIndex: Int,
    val totalPages: Int,
    val lastReadTime: Long,
    val isLocal: Boolean,
    val comicName: String = "",
    val comicCover: String = ""
)
