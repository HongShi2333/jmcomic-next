package com.par9uet.jm.retrofit.service

import com.par9uet.jm.retrofit.annotation.GInit
import com.par9uet.jm.retrofit.model.LoginResponse
import com.par9uet.jm.retrofit.model.ResponseWrapper
import com.par9uet.jm.retrofit.model.SignInDataResponse
import com.par9uet.jm.retrofit.model.SignInResponse
import com.par9uet.jm.retrofit.model.UserCollectComicListResponse
import com.par9uet.jm.retrofit.model.UserHistoryComicListResponse
import com.par9uet.jm.retrofit.model.UserHistoryCommentListResponse
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface UserService {

    @GInit
    @POST("login")
    @Multipart
    suspend fun login(
        @Part("username") username: String,
        @Part("password") password: String
    ): ResponseWrapper<LoginResponse>

    @GET("favorite")
    suspend fun getCollectComicList(
        @Query("page") page: Int,
        @Query("o") order: String,
        @Query("folder_id") folderId: Int = 0
    ): ResponseWrapper<UserCollectComicListResponse>

    @GET("watch_list")
    suspend fun getHistoryComicList(
        @Query("page") page: Int,
    ): ResponseWrapper<UserHistoryComicListResponse>

    @GET("forum")
    suspend fun getCommentList(
        @Query("page") page: Int,
        @Query("uid") userId: Int
    ): ResponseWrapper<UserHistoryCommentListResponse>

    @GET("daily")
    suspend fun getSignInData(
        @Query("user_id") userId: Int,
    ): ResponseWrapper<SignInDataResponse>

    @POST("daily_chk")
    @Multipart
    suspend fun signIn(
        @Part("user_id") userId: Int,
        @Part("daily_id") dailyId: Int,
    ): ResponseWrapper<SignInResponse>
}
