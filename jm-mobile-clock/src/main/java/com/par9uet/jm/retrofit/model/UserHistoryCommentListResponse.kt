package com.par9uet.jm.retrofit.model

import com.par9uet.jm.data.models.Comment
import com.par9uet.jm.utils.translateCommentTime

data class UserHistoryCommentListResponse(
    val list: List<ListItem> = emptyList(),
    val total: Int = 0,
) {
    data class ListItem(
        val AID: String? = null,
        val BID: String? = null,
        val CID: String? = null,
        val UID: String? = null,
        val username: String? = null,
        val nickname: String? = null,
        val likes: String? = null,
        val gender: String? = null,
        val update_at: String? = null,
        val addtime: String? = null,
        val parent_CID: String? = null,
        // 等级相关，这里不写，没啥意义
//        expinfo: {
//        level_name: string
//        level: number
//        nextLevelExp: number
//        exp: string
//        expPercent: number // 100
//        uid: string
//        badges: Array<{
//            content: string
//            name: string
//            id: string
//        }>
//    }
        val name: String? = null,
        val content: String? = null,
        val photo: String? = null,
        val spoiler: String? = null, // 是否剧透 1 和 0
        val replys: List<ListItem>? = null
    )

    fun toCommentList(): List<Comment> {
        return list.map {
            val username = it.username.orEmpty()
            val nickname = it.nickname.orEmpty().ifBlank { username }
            Comment(
                userId = it.UID.toIntOrZero(),
                comicId = it.AID.toIntOrZero(),
                id = it.CID.toIntOrZero(),
                time = translateCommentTime(it.addtime.orEmpty()),
                content = it.content.orEmpty(),
                likeCount = it.likes.toIntOrZero(),
                username = username,
                nickname = nickname,
                avatar = it.photo.orEmpty(),
                parentId = it.parent_CID.toIntOrZero(),
                spoiler = it.spoiler == "1",
                replyCommentList = listOf(),
                sourceComicName = it.name.orEmpty(),
                sourceChapterId = it.BID.orEmpty(),
                sourceBlogId = it.BID.orEmpty()
            )
        }
    }
}

private fun String?.toIntOrZero(): Int = this?.toIntOrNull() ?: 0
