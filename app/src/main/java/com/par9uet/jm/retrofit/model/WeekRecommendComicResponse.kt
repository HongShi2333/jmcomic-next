package com.par9uet.jm.retrofit.model

import com.google.gson.annotations.SerializedName
import com.par9uet.jm.data.models.Comic

data class WeekRecommendComicResponse(
    val total: Int,
    val list: List<ListItem>
) {
    data class ListItem(
        val id: String,
        val author: String,
        val description: String?,
        val name: String,
        val image: String,
        val category: Category,
        val category_sub: Category,
        val liked: Boolean,
        val is_favorite: Boolean,
        val update_at: Int,
        val tags: List<String>? = null,
        @SerializedName(
            value = "hidden_tags",
            alternate = ["hiddenTags", "tags_hidden", "tagsHide", "tags_hide", "hidden_tag"]
        )
        val hidden_tags: List<String>? = null,
    ) {
        data class Category(
            val id: String?,
            val title: String?
        )
    }

    fun toComicList(): List<Comic> {
        return list.map {
            Comic(
                id = it.id.toInt(),
                name = it.name,
                authorList = listOf(it.author),
                description = it.description ?: "",
                readCount = 0,
                likeCount = 0,
                commentCount = 0,
                tagList = (listOfNotNull(it.tags, it.hidden_tags)
                    .flatten() + listOfNotNull(
                    it.category.title,
                    it.category_sub.title
                )).filter { title -> title.isNotBlank() }.distinct(),
                roleList = listOf(),
                workList = listOf(),
                isLike = false,
                isCollect = false,
                relateComicList = listOf(),
                comicChapterList = listOf(),
                coverUrl = it.image,
                price = 0,
                isBuy = false,
            )
        }
    }
}
