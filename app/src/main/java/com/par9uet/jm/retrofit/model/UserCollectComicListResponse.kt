package com.par9uet.jm.retrofit.model

import com.par9uet.jm.data.models.Comic

data class UserCollectComicListResponse(
    val count: Int,
//    val folder_list: List<*>,
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
                tagList = listOfNotNull(
                    it.category.title,
                    it.category_sub.title
                ).filter { title -> title.isNotBlank() }.distinct(),
                roleList = listOf(),
                workList = listOf(),
                isLike = false,
                isCollect = false,
                relateComicList = listOf(),
                comicChapterList = listOf(),
                price = 0,
                isBuy = false,
            )
        }
    }
}
