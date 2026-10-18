package com.par9uet.jm.retrofit.model

import com.par9uet.jm.data.models.Comment
import com.par9uet.jm.utils.translateCommentTime

data class CommentListResponse(
    val list: List<ListItem>,
    val total: String,
) {
    data class ListItem(
        val AID: String?,
        val BID: String?,
        val CID: String?,
        val UID: String?,
        val username: String?,
        val nickname: String?,
        val likes: String?,
        val gender: String?,
        val update_at: String?,
        val addtime: String?,
        val parent_CID: String?,
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
        val name: String?,
        val content: String?,
        val photo: String?,
        val spoiler: String?, // 是否剧透 1 和 0
        val replys: List<ListItem>?
    ) {
        fun toComment(): Comment = Comment(
            userId = UID.toIntOrZero(),
            comicId = AID.toIntOrZero(),
            id = CID.toIntOrZero(),
            time = translateCommentTime(addtime.orEmpty()),
            content = content.orEmpty(),
            likeCount = likes.toIntOrZero(),
            username = username.orEmpty(),
            nickname = nickname.orEmpty().ifBlank { username.orEmpty() },
            avatar = photo.orEmpty(),
            parentId = parent_CID.toIntOrZero(),
            spoiler = spoiler == "1",
            replyCommentList = replys?.map { it.toComment() } ?: listOf(),
            sourceComicName = name.orEmpty(),
            sourceChapterId = BID.orEmpty(),
            sourceBlogId = BID.orEmpty()
        )
    }

    fun toCommentList(): List<Comment> {
        return list.map {
            it.toComment()
        }
    }
}

private fun String?.toIntOrZero(): Int = this?.toIntOrNull() ?: 0
