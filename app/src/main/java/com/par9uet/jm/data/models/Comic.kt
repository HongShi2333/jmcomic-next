package com.par9uet.jm.data.models

data class Comic(
    val id: Int,
    val name: String,
    val authorList: List<String> = listOf(),
    val description: String,
    // 阅读次数
    val readCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val tagList: List<String>,
    // 相关角色
    val roleList: List<String>,
    // 相关作品
    val workList: List<String>,
    // 是否喜爱
    val isLike: Boolean = false,
    // 是否收藏
    val isCollect: Boolean = false,
    // 相关漫画
    val relateComicList: List<Comic> = listOf(),
    // 话数
    val comicChapterList: List<ComicChapter> = listOf(),
    val seriesId: String = "",
    // 价格
    val price: Int,
    // 是否购买
    val isBuy: Boolean = false,
) {
    companion object {
        fun create(
            id: Int,
            name: String,
            authorList: List<String>,
        ): Comic {
            return Comic(
                id = id,
                name = name,
                authorList = authorList,
                description = "",
                readCount = 0,
                likeCount = 0,
                commentCount = 0,
                tagList = listOf(),
                roleList = listOf(),
                workList = listOf(),
                isLike = false,
                isCollect = false,
                relateComicList = listOf(),
                comicChapterList = listOf(),
                seriesId = "",
                price = 0,
                isBuy = false,
            )
        }
    }
}
