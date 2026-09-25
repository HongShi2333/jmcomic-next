package com.par9uet.jm.retrofit.model

import com.google.gson.annotations.SerializedName
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
    val tags: List<String> = emptyList(),
    @SerializedName(
        value = "hidden_tags",
        alternate = ["hiddenTags", "tags_hidden", "tagsHide", "tags_hide", "hidden_tag"]
    )
    val hidden_tags: List<String>? = null,
    val actors: List<String>,
    val works: List<String>,
    val is_favorite: Boolean,
    val liked: Boolean,
    val related_list: List<ComicDetailRelatedListItemResponse>,
    val series: List<ComicDetailSeriesListItemResponse>,
    val series_id: String,
    val price: String,
    val purchased: Boolean,
    // Some API responses include the cover directly. Keep it as a fallback when
    // the configured image host is unavailable (for example during DoH failures).
    val image: String? = null,
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
            tagList = listOfNotNull(tags, hidden_tags)
                .flatten()
                .filter { it.isNotBlank() }
                .distinct(),
            roleList = actors,
            workList = works,
            isLike = liked,
            isCollect = is_favorite,
            relateComicList = related_list.map {
                Comic.create(
                    it.id.toInt(),
                    it.name,
                    listOf(it.author),
                    coverUrl = it.image,
                )
            },
            comicChapterList = series.map { ComicChapter(it.id.toInt(), it.name) },
            seriesId = series_id,
            coverUrl = image.orEmpty(),
            price = price.toIntOrNull() ?: 0,
            isBuy = purchased
        )
    }
}
