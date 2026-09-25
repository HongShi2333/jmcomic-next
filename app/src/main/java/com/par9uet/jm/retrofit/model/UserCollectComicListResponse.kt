package com.par9uet.jm.retrofit.model

import com.google.gson.annotations.SerializedName
import com.par9uet.jm.data.models.Comic

data class UserCollectComicListResponse(
    val count: Int,
    val folder_list: Map<String, String>? = null,
    val list: List<ListItem>,
    val total: Int,
) {
    data class ListItem(
        val id: String,
        val author: String,
        val description: String?,
        val name: String,
        val image: String,
        val category: Category,
        val category_sub: Category,
        val tags: List<String>? = null,
        @SerializedName(value = "actors", alternate = ["actor", "roles", "role"])
        val actors: List<String>? = null,
        @SerializedName(value = "works", alternate = ["work"])
        val works: List<String>? = null,
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
                tagList = listOfNotNull(it.tags, it.hidden_tags)
                    .flatten()
                    .filter { t -> t.isNotBlank() }
                    .distinct(),
                roleList = it.actors.orEmpty().filter { actor -> actor.isNotBlank() }.distinct(),
                workList = it.works.orEmpty().filter { work -> work.isNotBlank() }.distinct(),
                typeList = listOfNotNull(it.category.title, it.category_sub.title)
                    .filter { title -> title.isNotBlank() }
                    .distinct(),
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
