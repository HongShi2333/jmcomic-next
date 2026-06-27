package com.par9uet.jm.retrofit.model

import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicChapter

data class ComicDetailResponse(
    val id: Int,
    val name: String,
    val description: String,
    val author: List<String>,
    val total_views: Int,
    val likes: Int,
    val comment_total: Int,
    val tags: List<String>,
    val actors: List<String>,
    val works: List<String>,
    val is_favorite: Boolean,
    val liked: Boolean,
    val related_list: List<ComicDetailRelatedListItemResponse>,
    val series: List<ComicDetailSeriesListItemResponse>,
    val series_id: String,
    val price: String,
    val purchased: Boolean,
) {
    fun toComic(): Comic {
        return Comic(
            id = id,
            name = name,
            authorList = author,
            description = description,
            readCount = total_views,
            likeCount = likes,
            commentCount = comment_total,
            tagList = tags,
            roleList = actors,
            workList = works,
            isLike = liked,
            isCollect = is_favorite,
            relateComicList = related_list.map {
                Comic.create(
                    it.id.toInt(),
                    it.name,
                    listOf(it.author)
                )
            },
            comicChapterList = series.map { ComicChapter(it.id.toInt(), it.name) },
            seriesId = series_id,
            price = price.toIntOrNull() ?: 0,
            isBuy = purchased
        )
    }
}
