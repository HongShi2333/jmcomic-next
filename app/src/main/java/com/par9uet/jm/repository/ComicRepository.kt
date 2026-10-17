package com.par9uet.jm.repository

import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.data.models.HomeCategoryPageSource
import com.par9uet.jm.data.models.HomeComicPage
import com.par9uet.jm.retrofit.model.CollectComicResponse
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.ComicPicListResponse
import com.par9uet.jm.retrofit.model.CommentComicResponse
import com.par9uet.jm.retrofit.model.CommentListResponse
import com.par9uet.jm.retrofit.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.retrofit.model.LikeComicResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.WeekRecommendComicResponse
import com.par9uet.jm.retrofit.model.WeekResponse

interface ComicRepository {
    suspend fun getComicDetail(id: Int): NetWorkResult<ComicDetailResponse>
    suspend fun likeComic(id: Int): NetWorkResult<LikeComicResponse>
    suspend fun collectComic(id: Int): NetWorkResult<CollectComicResponse>
    suspend fun unCollectComic(id: Int): NetWorkResult<CollectComicResponse>
    suspend fun getHomeSwiperComicList(): NetWorkResult<List<HomeSwiperComicListItemResponse>>
    suspend fun getHomeCategoryPage(source: HomeCategoryPageSource, page: Int): NetWorkResult<HomeComicPage>
    suspend fun getComicPicList(id: Int, shunt: String): NetWorkResult<ComicPicListResponse>
    suspend fun downloadImageBytes(comicId: Int, imageIndex: Int): ByteArray?
    suspend fun getComicList(
        page: Int,
        order: ComicSearchOrderFilter,
        searchContent: String,
    ): NetWorkResult<ComicListResponse>

    suspend fun getWeekData(): NetWorkResult<WeekResponse>
    suspend fun getWeekRecommendComicList(
        page: Int,
        categoryId: String,
        typeId: String,
    ): NetWorkResult<WeekRecommendComicResponse>

    suspend fun getCommentList(
        page: Int,
        comicId: Int,
    ): NetWorkResult<CommentListResponse>

    suspend fun comment(
        content: String,
        comicId: Int,
        commentId: Int?
    ): NetWorkResult<CommentComicResponse>

    suspend fun likeComment(commentId: Int): NetWorkResult<CommentComicResponse>

    suspend fun createFavoriteFolder(name: String): NetWorkResult<Unit>
    suspend fun deleteFavoriteFolder(folderId: String): NetWorkResult<Unit>
    suspend fun renameFavoriteFolder(folderId: String, newName: String): NetWorkResult<Unit>
    suspend fun moveComicToFolder(comicId: Int, folderId: String): NetWorkResult<Unit>

    /**
     * 通过 JMComic 内置 API 按标签名搜索，返回该标签下的漫画 ID 集合。
     * 用于标签排除：获取所有排除标签下的漫画 ID 并集，从搜索结果中过滤掉。
     *
     * @param tagName 标签名（如 "催眠"）
     * @param maxPages 最多扫描的页数（每页约 20 条），默认 5 页
     * @return 该标签下的漫画 ID 集合；标签不存在或网络错误时返回空集合
     */
    suspend fun getComicIdsByTag(tagName: String, maxPages: Int = 5): Set<Int>
}
