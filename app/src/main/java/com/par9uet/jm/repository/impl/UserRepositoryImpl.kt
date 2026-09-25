package com.par9uet.jm.repository.impl

import com.par9uet.jm.data.models.CollectComicOrderFilter
import com.par9uet.jm.data.models.COMIC_API_SOURCE_BUILTIN
import com.par9uet.jm.data.models.COMIC_API_SOURCE_MIXED
import com.par9uet.jm.repository.BaseRepository
import com.par9uet.jm.repository.UserRepository
import com.par9uet.jm.utils.log
import com.par9uet.jm.utils.logError
import com.par9uet.jm.retrofit.model.LoginResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.SignInDataResponse
import com.par9uet.jm.retrofit.model.SignInResponse
import com.par9uet.jm.retrofit.model.UserCollectComicListResponse
import com.par9uet.jm.retrofit.model.UserHistoryComicListResponse
import com.par9uet.jm.retrofit.model.UserHistoryCommentListResponse
import com.par9uet.jm.retrofit.service.UserService
import com.par9uet.jm.storage.CookieStorage
import com.par9uet.jm.store.InitManager
import com.par9uet.jm.store.LocalSettingManager
import io.github.jukomu.jmcomic.api.enums.ClientType
import io.github.jukomu.jmcomic.api.model.ForumQuery
import io.github.jukomu.jmcomic.api.model.FavoriteQuery
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta
import io.github.jukomu.jmcomic.api.model.JmCategoryMeta
import io.github.jukomu.jmcomic.api.model.JmComment
import io.github.jukomu.jmcomic.api.model.JmCommentList
import io.github.jukomu.jmcomic.api.model.JmDailyCheckInStatus
import io.github.jukomu.jmcomic.api.model.JmUserInfo
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import io.github.jukomu.jmcomic.core.config.JmConfiguration
import io.github.jukomu.jmcomic.core.net.OkHttpBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Cookie
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class UserRepositoryImpl(
    private val service: UserService,
    private val localSettingManager: LocalSettingManager,
    initManager: InitManager,
    private val cookieStorage: CookieStorage,
    private val embeddedClientManager: EmbeddedClientManager,
) : BaseRepository(initManager), UserRepository {

    private data class FavoritePageCacheEntry(
        val data: UserCollectComicListResponse,
        val expiresAt: Long,
        val hasDetailTags: Boolean,
    )

    // 首页预热、PagingSource 和文件夹列表可能在同一帧触发同一页请求。
    // 用短时缓存 + in-flight 合并，避免重复访问收藏接口，同时不长期持有用户数据。
    private val favoritePageCache = ConcurrentHashMap<String, FavoritePageCacheEntry>()
    private val favoritePageRequests = ConcurrentHashMap<String, CompletableDeferred<NetWorkResult<UserCollectComicListResponse>>>()

    override suspend fun login(username: String, password: String): NetWorkResult<LoginResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    val userInfo = withEmbeddedClient { client ->
                        client.login(username, password)
                    }
                    NetWorkResult.Success(userInfo.toLoginResponse())
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API登录失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.login(username, password)
        }
    }

    override suspend fun getCollectComicList(
        page: Int,
        order: CollectComicOrderFilter,
        folderId: Int,
        enrichTags: Boolean,
    ): NetWorkResult<UserCollectComicListResponse> {
        val cacheKey = favoritePageCacheKey(page, order, folderId)
        favoritePageCache[cacheKey]
            ?.takeIf { entry ->
                entry.expiresAt > System.currentTimeMillis() &&
                    (!enrichTags || entry.hasDetailTags)
            }
            ?.let { return NetWorkResult.Success(it.data) }

        val request = CompletableDeferred<NetWorkResult<UserCollectComicListResponse>>()
        val requestKey = "$cacheKey|${if (enrichTags) "details" else "summary"}"
        val activeRequest = favoritePageRequests.putIfAbsent(requestKey, request)
        if (activeRequest != null) return activeRequest.await()

        return try {
            val result = retryNetworkRequest("收藏漫画") {
                fetchCollectComicList(page, order, folderId, enrichTags)
            }
            if (result is NetWorkResult.Success) {
                val oldEntry = favoritePageCache[cacheKey]
                if (enrichTags || oldEntry?.hasDetailTags != true) {
                    favoritePageCache[cacheKey] = FavoritePageCacheEntry(
                        data = result.data,
                        expiresAt = System.currentTimeMillis() + if (enrichTags) {
                            FAVORITE_PAGE_DETAIL_CACHE_TTL_MS
                        } else {
                            FAVORITE_PAGE_CACHE_TTL_MS
                        },
                        hasDetailTags = enrichTags,
                    )
                }
            }
            request.complete(result)
            result
        } catch (error: Throwable) {
            request.completeExceptionally(error)
            throw error
        } finally {
            favoritePageRequests.remove(requestKey, request)
        }
    }

    private suspend fun fetchCollectComicList(
        page: Int,
        order: CollectComicOrderFilter,
        folderId: Int,
        enrichTags: Boolean,
    ): NetWorkResult<UserCollectComicListResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    currentCoroutineContext().ensureActive()
                    val client = embeddedClientManager.getClient()
                    embeddedClientManager.setFavoriteOrder(order.value)
                    val query = FavoriteQuery.Builder()
                        .folderId(folderId)
                        .page(page)
                        .build()
                    val favPage = client.getFavorites(query)
                    val metas = favPage.content().orEmpty()
                    // 首次列表请求只使用收藏接口元数据，保证列表和封面可以先展示。
                    // 预加载流程在封面完成后会再次以 enrichTags=true 补齐详情标签。
                    val listWithTags = if (enrichTags) {
                        coroutineScope {
                            metas.chunked(ALBUM_TAG_CONCURRENCY).flatMap { batch ->
                                batch.map { meta ->
                                    async {
                                        meta to withTimeoutOrNull(ALBUM_TAG_TIMEOUT_MS) {
                                            try {
                                                client.getAlbum(meta.id().orEmpty())
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }
                                    }
                                }.map { it.await() }
                            }
                        }.map { (meta, album) ->
                                meta.toListItem(
                                    fullTags = album?.tags().orEmpty().ifEmpty { meta.tags().orEmpty() },
                                    roles = album?.actors().orEmpty(),
                                    works = album?.works().orEmpty(),
                                )
                        }
                    } else {
                        metas.map { it.toListItem() }
                    }
                    NetWorkResult.Success(
                        UserCollectComicListResponse(
                            count = favPage.totalItems(),
                            folder_list = favPage.folderList(),
                            list = listWithTags,
                            total = favPage.totalItems()
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API获取收藏列表失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.getCollectComicList(page, order.value, folderId)
        }
    }

    private fun favoritePageCacheKey(
        page: Int,
        order: CollectComicOrderFilter,
        folderId: Int,
    ): String {
        val source = if (useEmbeddedApi()) "embedded" else "network"
        val sessionFingerprint = cookieStorage.get()
            .joinToString("|") { "${it.domain}:${it.path}:${it.name}=${it.value}" }
            .hashCode()
        return "$source|$page|${order.value}|$folderId|$sessionFingerprint"
    }

    private companion object {
        const val ALBUM_TAG_CONCURRENCY = 8
        const val ALBUM_TAG_TIMEOUT_MS = 4_000L
        const val FAVORITE_PAGE_CACHE_TTL_MS = 5_000L
        const val FAVORITE_PAGE_DETAIL_CACHE_TTL_MS = 120_000L
    }

    override suspend fun getHistoryComicList(page: Int): NetWorkResult<UserHistoryComicListResponse> {
        return retryNetworkRequest("历史漫画") {
            if (useEmbeddedApi()) {
                withContext(Dispatchers.IO) {
                    try {
                        currentCoroutineContext().ensureActive()
                        NetWorkResult.Success(withEmbeddedClient { client ->
                            val albumMetas = client.getWatchHistory(page)
                            UserHistoryComicListResponse(
                                list = albumMetas.map { it.toHistoryListItem() },
                                total = albumMetas.size
                            )
                        })
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        NetWorkResult.Error("内置API获取历史漫画失败：${e.message ?: "未知错误"}")
                    }
                }
            } else {
                safeApiCall {
                    service.getHistoryComicList(page)
                }
            }
        }
    }

    override suspend fun deleteHistoryComic(id: Int): NetWorkResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                withEmbeddedClient { client ->
                    client.deleteWatchHistory(id.toString())
                }
                NetWorkResult.Success(Unit)
            } catch (e: Exception) {
                logError("UserRepositoryImpl", "删除历史记录 id=$id 失败: ${e.message}")
                NetWorkResult.Error("删除历史记录失败：${e.message ?: "未知错误"}")
            }
        }
    }

    override suspend fun getHistoryCommentList(
        page: Int,
        userId: Int
    ): NetWorkResult<UserHistoryCommentListResponse> {
        return retryNetworkRequest("评论历史") {
            if (useEmbeddedApi()) {
                withContext(Dispatchers.IO) {
                    try {
                        currentCoroutineContext().ensureActive()
                        NetWorkResult.Success(withEmbeddedClient { client ->
                            val query = ForumQuery.user(userId.toString())
                                .page(page)
                                .build()
                            val commentList = client.getComments(query)
                            UserHistoryCommentListResponse(
                                list = commentList.list.map { it.toHistoryCommentListItem() },
                                total = commentList.total
                            )
                        })
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        NetWorkResult.Error("内置API获取评论历史失败：${e.message ?: "未知错误"}")
                    }
                }
            } else {
                safeApiCall {
                    service.getCommentList(page, userId)
                }
            }
        }
    }

    override suspend fun getSignData(userId: Int): NetWorkResult<SignInDataResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    NetWorkResult.Success(withEmbeddedClient { client ->
                        val status = client.getDailyCheckInStatus(userId.toString())
                        status.toSignInDataResponse()
                    })
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API获取签到数据失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.getSignInData(userId)
        }
    }

    override suspend fun signIn(userId: Int, dailyId: Int): NetWorkResult<SignInResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.doDailyCheckin(userId.toString(), dailyId.toString())
                    }
                    NetWorkResult.Success(SignInResponse(msg = "签到成功"))
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API签到失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.signIn(userId, dailyId)
        }
    }

    private fun useEmbeddedApi(): Boolean {
        val source = localSettingManager.localSettingState.value.comicApiSource
        return source == COMIC_API_SOURCE_BUILTIN || source == COMIC_API_SOURCE_MIXED
    }

    private fun <T> withEmbeddedClient(block: (JmApiClient) -> T): T {
        return block(embeddedClientManager.getClient())
    }

    private fun JmComment.toHistoryCommentListItem(): UserHistoryCommentListResponse.ListItem {
        return UserHistoryCommentListResponse.ListItem(
            AID = aid(),
            BID = bid(),
            CID = commentId(),
            UID = userId(),
            username = username(),
            nickname = nickname(),
            likes = likes().toString(),
            gender = gender(),
            update_at = updateAt(),
            addtime = postDate(),
            parent_CID = parentCommentId(),
            name = name(),
            content = content(),
            photo = photo() ?: "",
            spoiler = spoiler(),
            replys = replys()?.map { it.toHistoryCommentListItem() }
        )
    }

    private fun JmAlbumMeta.toListItem(
        fullTags: List<String> = tags().orEmpty(),
        roles: List<String> = emptyList(),
        works: List<String> = emptyList(),
    ): UserCollectComicListResponse.ListItem {
        return UserCollectComicListResponse.ListItem(
            id = id().orEmpty(),
            author = authors().orEmpty().firstOrNull().orEmpty(),
            description = description(),
            name = title().orEmpty(),
            image = image().orEmpty(),
            category = category().toCollectCategory(),
            category_sub = subCategory().toCollectCategory(),
            tags = if (fullTags.isEmpty()) null else fullTags,
            actors = roles.ifEmpty { null },
            works = works.ifEmpty { null },
        )
    }

    private fun JmAlbumMeta.toHistoryListItem(): UserHistoryComicListResponse.ListItem {
        return UserHistoryComicListResponse.ListItem(
            id = id().orEmpty(),
            author = authors().orEmpty().firstOrNull().orEmpty(),
            description = description(),
            name = title().orEmpty(),
            image = image().orEmpty(),
            category = category().toHistoryCategory(),
            category_sub = subCategory().toHistoryCategory()
        )
    }

    private fun JmCategoryMeta?.toHistoryCategory(): UserHistoryComicListResponse.ListItem.Category {
        return UserHistoryComicListResponse.ListItem.Category(
            id = this?.id(),
            title = this?.title()
        )
    }

    private fun JmCategoryMeta?.toCollectCategory(): UserCollectComicListResponse.ListItem.Category {
        return UserCollectComicListResponse.ListItem.Category(
            id = this?.id(),
            title = this?.title()
        )
    }

    private fun JmUserInfo.toLoginResponse(): LoginResponse {
        return LoginResponse(
            uid = uid.toIntOrNull() ?: 0,
            username = username,
            email = email,
            photo = avatarUrl,
            coin = coin.toString(),
            album_favorites = albumFavorites,
            level_name = levelName,
            level = level,
            nextLevelExp = nextLevelExp.toInt(),
            exp = currentExp.toInt(),
            expPercent = expPercent,
            album_favorites_max = maxAlbumFavorites,
        )
    }

    private fun JmDailyCheckInStatus.toSignInDataResponse(): SignInDataResponse {
        return SignInDataResponse(
            daily_id = dailyId,
            three_days_coin = threeDaysCoin,
            three_days_exp = threeDaysExp,
            seven_days_coin = sevenDaysCoin,
            seven_days_exp = sevenDaysExp,
            event_name = eventName,
            background_pc = backgroundPc,
            background_phone = backgroundPhone,
            currentProgress = currentProgress,
            record = record.map { week ->
                week.map { item ->
                    SignInDataResponse.RecordItem(
                        date = item.date,
                        signed = item.signed ?: false,
                        bonus = item.bonus,
                    )
                }
            }
        )
    }
}
