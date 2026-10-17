package com.par9uet.jm.retrofit.model

import com.google.gson.annotations.SerializedName
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.HomeComicSwiperItem
import com.par9uet.jm.data.models.HomeCategoryPageKind
import com.par9uet.jm.data.models.HomeCategoryPageSource

class HomeSwiperComicListItemResponse(
    val id: String,
    val title: String,
    val slug: String,
    val type: String,
    val filter_val: String,
    val content: List<ListItem>
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

    fun toHomeComicSwiperItem(): HomeComicSwiperItem {
        return HomeComicSwiperItem(
            id = id,
            title = title,
            list = content.map { it.toComic() },
            pageSource = when {
                type == "builtin" && id != "builtin_random" ->
                    HomeCategoryPageSource(HomeCategoryPageKind.BUILTIN, id, 2)
                type == "category_id" && slug.isNotBlank() ->
                    HomeCategoryPageSource(HomeCategoryPageKind.CATEGORY, slug, 1)
                id.removePrefix("pref_").toIntOrNull() != null ->
                    HomeCategoryPageSource(HomeCategoryPageKind.PROMOTE, id.removePrefix("pref_"), 0)
                else -> null
            },
        )
    }
}

fun HomeSwiperComicListItemResponse.ListItem.toComic(): Comic =
    Comic(
        id = id.toInt(),
        name = name,
        authorList = listOf(author),
        description = description ?: "",
        readCount = 0,
        likeCount = 0,
        commentCount = 0,
        tagList = listOfNotNull(tags, hidden_tags)
            .flatten()
            .filter { tag -> tag.isNotBlank() }
            .distinct(),
        roleList = listOf(),
        workList = listOf(),
        isLike = false,
        isCollect = false,
        relateComicList = listOf(),
        comicChapterList = listOf(),
        coverUrl = image,
        price = 0,
        isBuy = false,
    )
