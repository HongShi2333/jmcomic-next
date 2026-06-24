package com.par9uet.jm.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter_progress",
    indices = [
        Index(value = ["comicId"]),
        Index(value = ["lastReadTime"])
    ]
)
data class ChapterProgress(
    @PrimaryKey
    val chapterId: Int,              // 章节ID作为主键
    val comicId: Int,                // 所属漫画ID（用于查询某本漫画的所有章节进度）
    val chapterName: String = "",    // 章节名称
    val pageIndex: Int,              // 当前页码（从0开始）
    val totalPages: Int,             // 总页数
    val lastReadTime: Long,          // 最后阅读时间戳
    val isCompleted: Boolean = false // 是否已读完
)
