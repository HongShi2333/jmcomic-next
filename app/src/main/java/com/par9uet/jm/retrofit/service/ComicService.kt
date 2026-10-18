package com.par9uet.jm.retrofit.service

import com.par9uet.jm.retrofit.model.CollectComicResponse
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.CommentComicResponse
import com.par9uet.jm.retrofit.model.CommentListResponse
import com.par9uet.jm.retrofit.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.retrofit.model.HomeComicPageResponse
import com.par9uet.jm.retrofit.model.LikeComicResponse
import com.par9uet.jm.retrofit.model.ResponseWrapper
import com.par9uet.jm.retrofit.model.WeekRecommendComicResponse
import com.par9uet.jm.retrofit.model.WeekResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ComicService {

    @GET("album")
    suspend fun getComicDetail(
        @Query("id") id: Int,
    ): ResponseWrapper<ComicDetailResponse>

    @POST("like")
    @Multipart
    suspend fun likeComic(
        @Part("id") id: Int,
    ): ResponseWrapper<LikeComicResponse>

    @POST("favorite")
    @Multipart
    suspend fun collectComic(
        @Part("aid") id: Int,
    ): ResponseWrapper<CollectComicResponse>

    @GET("promote")
    suspend fun getHomeSwiperComicList(): ResponseWrapper<List<HomeSwiperComicListItemResponse>>

    @GET("promote_list")
    suspend fun getHomePromotePage(@Query("id") id: String, @Query("page") page: Int): ResponseWrapper<HomeComicPageResponse>

    @GET("categories/filter")
    suspend fun getHomeCategoryPage(@Query("c") category: String, @Query("page") page: Int): ResponseWrapper<HomeComicPageResponse>

    @GET("chapter_view_template")
    suspend fun getComicPicList(
        @Query("id") id: Int,
        // TODO 图片设置
        @Query("app_img_shunt") shunt: String,
        @Query("mode") mode: String = "vertical",
        @Query("page") page: Int = 0,
        @Query("express") express: String = "off",
        @Query("v") v: Long = System.currentTimeMillis() / 1000,
    ): String

    @GET("search")
    suspend fun getComicList(
        @Query("page") page: Int,
        @Query("o") order: String,
        @Query("search_query") searchContent: String,
    ): ResponseWrapper<ComicListResponse>

    @GET("week")
    suspend fun getWeekData(): ResponseWrapper<WeekResponse>

    @GET("week/filter")
    suspend fun getWeekRecommendComicList(
        @Query("page") page: Int,
        @Query("id") categoryId: String,
        @Query("type") typeId: String,
    ): ResponseWrapper<WeekRecommendComicResponse>

    @GET("forum")
    suspend fun getCommentList(
        @Query("page") page: Int,
        @Query("aid") comicId: Int,
        @Query("mode") mode: String = "manhua",
    ): ResponseWrapper<CommentListResponse>

    @POST("comment")
    @FormUrlEncoded
    suspend fun comment(
        @Field("comment") content: String,
        @Field("aid") id: Int,
        @Field("comment_id") commentId: Int = 0,
    ): ResponseWrapper<CommentComicResponse>

    @POST("comment_vote")
    @FormUrlEncoded
    suspend fun likeComment(
        @Field("comment_id") commentId: Int,
        @Field("vote_type") voteType: String = "up",
    ): ResponseWrapper<CommentComicResponse>
}
