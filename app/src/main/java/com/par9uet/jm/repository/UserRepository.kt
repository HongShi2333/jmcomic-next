package com.par9uet.jm.repository

import com.par9uet.jm.data.models.CollectComicOrderFilter
import com.par9uet.jm.retrofit.model.LoginResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.SignInDataResponse
import com.par9uet.jm.retrofit.model.SignInResponse
import com.par9uet.jm.retrofit.model.UserCollectComicListResponse
import com.par9uet.jm.retrofit.model.UserHistoryComicListResponse
import com.par9uet.jm.retrofit.model.UserHistoryCommentListResponse

interface UserRepository {
    suspend fun login(username: String, password: String): NetWorkResult<LoginResponse>
    suspend fun getCollectComicList(
        page: Int = 1,
        order: CollectComicOrderFilter = CollectComicOrderFilter.COLLECT_TIME,
        folderId: Int = 0,
        enrichTags: Boolean = false,
    ): NetWorkResult<UserCollectComicListResponse>

    suspend fun getHistoryComicList(page: Int = 1): NetWorkResult<UserHistoryComicListResponse>
    suspend fun deleteHistoryComic(id: Int): NetWorkResult<Unit>
    suspend fun getHistoryCommentList(
        page: Int = 1,
        userId: Int
    ): NetWorkResult<UserHistoryCommentListResponse>

    suspend fun getSignData(
        userId: Int,
    ): NetWorkResult<SignInDataResponse>

    suspend fun signIn(
        userId: Int,
        dailyId: Int,
    ): NetWorkResult<SignInResponse>
}
