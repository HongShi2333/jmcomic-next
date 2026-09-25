package com.par9uet.jm.repository.impl

import com.par9uet.jm.data.models.COMIC_API_SOURCE_BUILTIN
import com.par9uet.jm.data.models.COMIC_API_SOURCE_MIXED
import com.par9uet.jm.data.models.COMIC_API_SOURCE_NETWORK
import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.repository.BaseRepository
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.CollectComicResponse
import com.par9uet.jm.retrofit.model.ComicDetailRelatedListItemResponse
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.ComicDetailSeriesListItemResponse
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.ComicPicListResponse
import com.par9uet.jm.retrofit.model.CommentComicResponse
import com.par9uet.jm.retrofit.model.CommentListResponse
import com.par9uet.jm.retrofit.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.retrofit.model.LikeComicResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.WeekRecommendComicResponse
import com.par9uet.jm.retrofit.model.WeekResponse
import com.par9uet.jm.retrofit.parseHtml
import com.par9uet.jm.retrofit.parseRange
import com.par9uet.jm.retrofit.parseSpeed
import com.par9uet.jm.retrofit.service.ComicService
import com.par9uet.jm.storage.CookieStorage
import com.par9uet.jm.store.InitManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.network.DohManager
import com.par9uet.jm.utils.applyTlsCompat
import com.par9uet.jm.utils.log
import com.par9uet.jm.utils.logError
import io.github.jukomu.jmcomic.api.enums.ClientType
import io.github.jukomu.jmcomic.api.enums.Category
import io.github.jukomu.jmcomic.api.enums.FavoriteFolderType
import io.github.jukomu.jmcomic.api.enums.ForumMode
import io.github.jukomu.jmcomic.api.enums.OrderBy
import io.github.jukomu.jmcomic.api.enums.SearchMainTag
import io.github.jukomu.jmcomic.api.enums.TimeOption
import io.github.jukomu.jmcomic.api.model.ForumQuery
import io.github.jukomu.jmcomic.api.model.JmAlbum
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta
import io.github.jukomu.jmcomic.api.model.JmCategoryMeta
import io.github.jukomu.jmcomic.api.model.JmComment
import io.github.jukomu.jmcomic.api.model.JmCommentList
import io.github.jukomu.jmcomic.api.model.JmImage
import io.github.jukomu.jmcomic.api.model.JmSearchPage
import io.github.jukomu.jmcomic.api.model.JmWeeklyPicksDetail
import io.github.jukomu.jmcomic.api.model.JmWeeklyPicksCategory
import io.github.jukomu.jmcomic.api.model.JmWeeklyPicksList
import io.github.jukomu.jmcomic.api.model.JmWeeklyPicksType
import io.github.jukomu.jmcomic.api.model.SearchQuery
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import io.github.jukomu.jmcomic.core.config.JmConfiguration
import io.github.jukomu.jmcomic.core.net.OkHttpBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.util.concurrent.TimeUnit

class ComicRepositoryImpl(
    private val service: ComicService,
    initManager: InitManager,
    private val localSettingManager: LocalSettingManager,
    private val cookieStorage: CookieStorage,
    private val embeddedClientManager: EmbeddedClientManager,
    private val dohManager: DohManager,
) : BaseRepository(initManager), ComicRepository {

    companion object {
        private val imageCache = mutableMapOf<Int, List<JmImage>>()
        private val imageUrlCache = mutableMapOf<Int, List<String>>()
        private val imageCdnHosts = listOf(
            "cdn-msp.jmapiproxy1.cc",
            "cdn-msp.jmapiproxy2.cc",
            "cdn-msp2.jmapiproxy2.cc",
            "cdn-msp3.jmapiproxy2.cc",
            "cdn-msp.jmapinodeudzn.net",
            "cdn-msp3.jmapinodeudzn.net",
        )
        private val comicPicListCache = java.util.LinkedHashMap<String, TimedComicPicList>(
            COMIC_PIC_LIST_CACHE_MAX_ENTRIES,
            0.75f,
            true,
        )
        private val comicPicListInFlight = mutableMapOf<String, CompletableDeferred<NetWorkResult<ComicPicListResponse>>>()
        private const val COMIC_PIC_LIST_CACHE_TTL_MS = 5 * 60 * 1000L
        private const val COMIC_PIC_LIST_CACHE_MAX_ENTRIES = 16
    }

    private data class TimedComicPicList(
        val response: ComicPicListResponse,
        val expiresAtMillis: Long,
    )

    private val cleanHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .dns(dohManager)
            .applyTlsCompat()
            .build()
    }

    private fun fixImageUrl(url: String): String {
        // 库的 buildImageList 会将域名前缀拼接到 filename 前。
        // 但 API 有时返回的 image 字段本身就是完整 URL，导致双重拼接：
        // https://cdn-msp.jmapiproxy1.cc/media/photos/1452549/https://tencent.jmdanjonproxy.xyz/media/photos/1452549/00004.webp?t=...
        // 正确的 URL 应该从第二个 https:// 开始
        val trimmed = url.trim()
        val nestedHttps = trimmed.lastIndexOf("https://")
        val nestedHttp = trimmed.lastIndexOf("http://")
        val nestedProtocol = if (nestedHttps > nestedHttp) nestedHttps else nestedHttp
        return if (nestedProtocol > 0) trimmed.substring(nestedProtocol) else trimmed
    }

    private fun imageUrlCandidates(url: String): List<String> {
        val normalized = fixImageUrl(url)
        val parsed = normalized.toHttpUrlOrNull() ?: return listOf(normalized)
        if (!parsed.encodedPath.contains("/media/photos/")) return listOf(normalized)

        return buildList {
            add(normalized)
            imageCdnHosts.forEach { host ->
                if (!host.equals(parsed.host, ignoreCase = true)) {
                    add(parsed.newBuilder().host(host).build().toString())
                }
            }
        }.distinct()
    }

    private fun buildImageRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; V1938CT Build/PQ3A.190705.11211812; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Safari/537.36")
            .header("Referer", "https://18comic.vip")
            .build()
    }

    override suspend fun getComicDetail(id: Int): NetWorkResult<ComicDetailResponse> {
        return retryNetworkRequest("漫画详情") {
            if (useEmbeddedApi()) {
                getComicDetailFromEmbeddedApi(id)
            } else {
                safeApiCall {
                    service.getComicDetail(id)
                }
            }
        }
    }

    override suspend fun likeComic(id: Int): NetWorkResult<LikeComicResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.toggleAlbumLike(id.toString())
                    }
                    NetWorkResult.Success(LikeComicResponse(code = 200, msg = "success", status = "ok"))
                } catch (e: Exception) {
                    NetWorkResult.Error("内置 API 点赞失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.likeComic(id)
        }
    }

    override suspend fun collectComic(id: Int): NetWorkResult<CollectComicResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.toggleAlbumFavorite(id.toString(), "0")
                    }
                    NetWorkResult.Success(CollectComicResponse(msg = "success", status = "ok", type = "collect"))
                } catch (e: Exception) {
                    NetWorkResult.Error("内置 API 收藏失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.collectComic(id)
        }
    }

    override suspend fun unCollectComic(id: Int): NetWorkResult<CollectComicResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.toggleAlbumFavorite(id.toString(), "0")
                    }
                    NetWorkResult.Success(CollectComicResponse(msg = "success", status = "ok", type = "uncollect"))
                } catch (e: Exception) {
                    NetWorkResult.Error("内置 API 取消收藏失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.collectComic(id)
        }
    }

    override suspend fun getHomeSwiperComicList(): NetWorkResult<List<HomeSwiperComicListItemResponse>> {
        return retryNetworkRequest("首页漫画") {
            if (useEmbeddedApi()) {
                getHomeSwiperComicListFromEmbeddedApi()
            } else {
                safeApiCall {
                    service.getHomeSwiperComicList()
                }
            }
        }
    }

    override suspend fun getComicPicList(id: Int, shunt: String): NetWorkResult<ComicPicListResponse> {
        val source = localSettingManager.localSettingState.value.comicApiSource
        val cacheKey = "$source:$shunt:$id"
        synchronized(comicPicListCache) {
            val cached = comicPicListCache[cacheKey]
            if (cached != null && cached.expiresAtMillis > System.currentTimeMillis()) {
                synchronized(imageUrlCache) {
                    imageUrlCache[id] = cached.response.list
                }
                return NetWorkResult.Success(cached.response)
            }
            if (cached != null) comicPicListCache.remove(cacheKey)
        }

        val pendingResult = CompletableDeferred<NetWorkResult<ComicPicListResponse>>()
        val existingRequest = synchronized(comicPicListInFlight) {
            comicPicListInFlight[cacheKey].also { existing ->
                if (existing == null) comicPicListInFlight[cacheKey] = pendingResult
            }
        }
        if (existingRequest != null) return existingRequest.await()

        try {
            val result = retryNetworkRequest("章节图片列表") {
                try {
                    if (useEmbeddedApi()) {
                        val embeddedResult = getComicPicListFromEmbeddedApi(id)
                        if (embeddedResult is NetWorkResult.Success<ComicPicListResponse>) {
                            embeddedResult
                        } else {
                            // The embedded client can fail while parsing the second scramble
                            // request even though the chapter HTML endpoint is reachable. Use
                            // the chapter-specific network endpoint as a compatibility path;
                            // this is still keyed by the selected chapter id and cannot reuse
                            // the album's first chapter.
                            getComicPicListFromNetworkApi(id, shunt)
                        }
                    } else {
                        getComicPicListFromNetworkApi(id, shunt)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    NetWorkResult.Error("获取章节图片列表失败：${error.message ?: "未知错误"}")
                }
            }
            if (result is NetWorkResult.Success<ComicPicListResponse>) {
                // Keep a URL-only copy as well. HTML/network image lists do not
                // have JmImage objects, but the reader still needs a direct
                // download fallback when Coil cannot decode the page.
                synchronized(imageUrlCache) {
                    imageUrlCache[id] = result.data.list
                }
                synchronized(comicPicListCache) {
                    comicPicListCache[cacheKey] = TimedComicPicList(
                        response = result.data,
                        expiresAtMillis = System.currentTimeMillis() + COMIC_PIC_LIST_CACHE_TTL_MS,
                    )
                    while (comicPicListCache.size > COMIC_PIC_LIST_CACHE_MAX_ENTRIES) {
                        comicPicListCache.entries.iterator().run {
                            if (hasNext()) {
                                next()
                                remove()
                            }
                        }
                    }
                }
            }
            pendingResult.complete(result)
            return result
        } catch (error: CancellationException) {
            pendingResult.cancel(error)
            throw error
        } finally {
            synchronized(comicPicListInFlight) {
                if (comicPicListInFlight[cacheKey] === pendingResult) {
                    comicPicListInFlight.remove(cacheKey)
                }
            }
        }
    }

    private suspend fun getComicPicListFromNetworkApi(id: Int, shunt: String): NetWorkResult<ComicPicListResponse> {
        return when (val res = safeStringCall {
            service.getComicPicList(id, shunt)
        }) {
            is NetWorkResult.Success<String> -> {
                val htmlStr = res.data
                val pair = parseRange(htmlStr)
                val imageList = parseHtml(htmlStr).map(::fixImageUrl)
                if (imageList.isEmpty()) {
                    NetWorkResult.Error("从 HTML 解析图片列表为空")
                } else {
                    NetWorkResult.Success(
                        ComicPicListResponse(
                            list = imageList,
                            __aId = pair.first,
                            __scrambleId = pair.second,
                            __speed = parseSpeed(htmlStr)
                        )
                    )
                }
            }

            else -> {
                NetWorkResult.Error("从 HTML 解析图片列表失败")
            }
        }
    }

    override suspend fun getComicList(
        page: Int,
        order: ComicSearchOrderFilter,
        searchContent: String,
    ): NetWorkResult<ComicListResponse> {
        return retryNetworkRequest("漫画列表") {
            if (useEmbeddedApi()) {
                getComicListFromEmbeddedApi(page, order, searchContent)
            } else {
                safeApiCall {
                    service.getComicList(page, order.value, searchContent)
                }
            }
        }
    }

    override suspend fun getWeekData(): NetWorkResult<WeekResponse> {
        return retryNetworkRequest("周刊数据") {
            if (useEmbeddedApi()) {
                withContext(Dispatchers.IO) {
                    try {
                        NetWorkResult.Success(withEmbeddedClient { client ->
                            val picks = client.getWeeklyPicksList()
                            WeekResponse(
                                categories = picks.categories.map { category ->
                                    WeekResponse.CategoryItem(
                                        id = category.id(),
                                        time = category.time(),
                                        title = category.title()
                                    )
                                },
                                type = picks.type.map { type ->
                                    WeekResponse.TypeItem(
                                        id = type.id(),
                                        title = type.title()
                                    )
                                }
                            )
                        })
                    } catch (e: Exception) {
                        NetWorkResult.Error("内置 API 获取周刊数据失败：${e.message ?: "未知错误"}")
                    }
                }
            } else {
                safeApiCall {
                    service.getWeekData()
                }
            }
        }
    }

    override suspend fun getWeekRecommendComicList(
        page: Int,
        categoryId: String,
        typeId: String,
    ): NetWorkResult<WeekRecommendComicResponse> {
        return retryNetworkRequest("周刊漫画") {
            if (useEmbeddedApi()) {
                withContext(Dispatchers.IO) {
                    try {
                        NetWorkResult.Success(withEmbeddedClient { client ->
                            val detail = client.getWeeklyPicksDetail(categoryId)
                            WeekRecommendComicResponse(
                                total = detail.list.size,
                                list = detail.list.map { albumMeta ->
                                    WeekRecommendComicResponse.ListItem(
                                        id = albumMeta.id(),
                                        author = albumMeta.authors().joinToString(", "),
                                        description = albumMeta.description(),
                                        name = albumMeta.title(),
                                        image = albumMeta.image() ?: "",
                                        category = WeekRecommendComicResponse.ListItem.Category(
                                            id = albumMeta.category()?.id(),
                                            title = albumMeta.category()?.title()
                                        ),
                                        category_sub = WeekRecommendComicResponse.ListItem.Category(
                                            id = albumMeta.subCategory()?.id(),
                                            title = albumMeta.subCategory()?.title()
                                        ),
                                        liked = false,
                                        is_favorite = false,
                                        update_at = 0,
                                        tags = albumMeta.tags().orEmpty().takeIf { it.isNotEmpty() }
                                    )
                                }
                            )
                        })
                    } catch (e: Exception) {
                        NetWorkResult.Error("内置 API 获取周刊详情失败：${e.message ?: "未知错误"}")
                    }
                }
            } else {
                safeApiCall {
                    service.getWeekRecommendComicList(
                        page,
                        categoryId,
                        typeId
                    )
                }
            }
        }
    }

    override suspend fun getCommentList(
        page: Int,
        comicId: Int
    ): NetWorkResult<CommentListResponse> {
        return retryNetworkRequest("评论列表") {
            if (useEmbeddedApi()) {
                withContext(Dispatchers.IO) {
                    try {
                        NetWorkResult.Success(withEmbeddedClient { client ->
                            val query = ForumQuery.album(comicId.toString())
                                .mode(ForumMode.ALL)
                                .page(page)
                                .build()
                            val commentList = client.getComments(query)
                            CommentListResponse(
                                list = commentList.list.map { it.toCommentListItem() },
                                total = commentList.total.toString()
                            )
                        })
                    } catch (e: Exception) {
                        NetWorkResult.Error("内置 API 获取评论列表失败：${e.message ?: "未知错误"}")
                    }
                }
            } else {
                safeApiCall {
                    service.getCommentList(
                        page,
                        comicId,
                        "manhua"
                    )
                }
            }
        }
    }

    override suspend fun comment(
        content: String,
        comicId: Int,
        commentId: Int?
    ): NetWorkResult<CommentComicResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        if (commentId != null) {
                            client.replyToComment(comicId.toString(), content, commentId.toString())
                        } else {
                            client.postComment(comicId.toString(), content)
                        }
                    }
                    NetWorkResult.Success(
                        CommentComicResponse(
                            msg = "success",
                            status = "ok",
                            aid = comicId,
                            cid = 0,
                            spoiler = "0"
                        )
                    )
                } catch (e: Exception) {
                    NetWorkResult.Error("内置 API 评论失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.comment(
                content,
                comicId,
                commentId ?: 0,
            )
        }
    }

    override suspend fun likeComment(commentId: Int): NetWorkResult<CommentComicResponse> {
        return safeApiCall {
            service.likeComment(commentId)
        }
    }

    override suspend fun createFavoriteFolder(name: String): NetWorkResult<Unit> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.manageFavoriteFolder(FavoriteFolderType.ADD, "0", name, "")
                    }
                    NetWorkResult.Success(Unit)
                } catch (e: Exception) {
                    logError("ComicRepositoryImpl", "创建收藏夹失败：${e.message}")
                    NetWorkResult.Error("内置API创建收藏夹失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return NetWorkResult.Error("网络API暂不支持收藏夹管理")
    }

    override suspend fun deleteFavoriteFolder(folderId: String): NetWorkResult<Unit> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.manageFavoriteFolder(FavoriteFolderType.DELETE, folderId, "", "")
                    }
                    NetWorkResult.Success(Unit)
                } catch (e: Exception) {
                    logError("ComicRepositoryImpl", "删除收藏夹失败：${e.message}")
                    NetWorkResult.Error("内置API删除收藏夹失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return NetWorkResult.Error("网络API暂不支持收藏夹管理")
    }

    override suspend fun renameFavoriteFolder(folderId: String, newName: String): NetWorkResult<Unit> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.manageFavoriteFolder(FavoriteFolderType.EDIT, folderId, newName, "")
                    }
                    NetWorkResult.Success(Unit)
                } catch (e: Exception) {
                    logError("ComicRepositoryImpl", "重命名收藏夹失败：${e.message}")
                    NetWorkResult.Error("内置API重命名收藏夹失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return NetWorkResult.Error("网络API暂不支持收藏夹管理")
    }

    override suspend fun moveComicToFolder(comicId: Int, folderId: String): NetWorkResult<Unit> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.manageFavoriteFolder(FavoriteFolderType.MOVE, folderId, "", comicId.toString())
                    }
                    NetWorkResult.Success(Unit)
                } catch (e: Exception) {
                    logError("ComicRepositoryImpl", "移动漫画到收藏夹失败：${e.message}")
                    NetWorkResult.Error("内置API移动漫画到收藏夹失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return NetWorkResult.Error("网络API暂不支持收藏夹管理")
    }

    override suspend fun getComicIdsByTag(tagName: String, maxPages: Int): Set<Int> {
        if (tagName.isBlank()) return emptySet()
        return withContext(Dispatchers.IO) {
            try {
                val client = getEmbeddedClient()
                val ids = mutableSetOf<Int>()
                for (page in 1..maxPages.coerceAtLeast(1)) {
                    val query = SearchQuery.Builder()
                        .text(tagName)
                        .mainTag(SearchMainTag.TAG)
                        .page(page)
                        .build()
                    val result = retryNetworkRequest("标签[$tagName]第${page}页") {
                        try {
                            NetWorkResult.Success(client.search(query))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            NetWorkResult.Error("搜索标签失败：${e.message ?: "未知错误"}")
                        }
                    }
                    val searchPage = when (result) {
                        is NetWorkResult.Success -> result.data
                        is NetWorkResult.Error -> {
                            logError("ComicRepositoryImpl", "搜索标签 [$tagName] 第${page}页失败：${result.message}")
                            null
                        }
                    }
                    if (searchPage == null) break
                    val content = searchPage.content().orEmpty()
                    if (content.isEmpty()) break
                    content.forEach { meta ->
                        meta.id().toIntOrNull()?.let { ids += it }
                    }
                    val total = searchPage.totalItems()
                    if (total <= page * 20) break
                    if (page < maxPages) delay(150)
                }
                log("ComicRepositoryImpl", "标签 [$tagName] 获取到 ${ids.size} 个漫画ID")
                ids
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError("ComicRepositoryImpl", "获取标签 [$tagName] 漫画ID失败：${e.message}")
                emptySet()
            }
        }
    }

    private fun useEmbeddedApi(): Boolean {
        val source = localSettingManager.localSettingState.value.comicApiSource
        return source == COMIC_API_SOURCE_BUILTIN || source == COMIC_API_SOURCE_MIXED
    }

    private fun getEmbeddedClient(): JmApiClient = embeddedClientManager.getClient()

    private fun <T> withEmbeddedClient(block: (JmApiClient) -> T): T {
        return block(getEmbeddedClient())
    }

    private suspend fun getComicDetailFromEmbeddedApi(id: Int): NetWorkResult<ComicDetailResponse> {
        return withContext(Dispatchers.IO) {
            try {
                currentCoroutineContext().ensureActive()
                NetWorkResult.Success(withEmbeddedClient { client ->
                    client.getAlbum(id.toString()).toComicDetailResponse()
                })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 获取漫画详情失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun getHomeSwiperComicListFromEmbeddedApi(): NetWorkResult<List<HomeSwiperComicListItemResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                currentCoroutineContext().ensureActive()
                val client = getEmbeddedClient()
                coroutineScope {
                    suspend fun loadCategoryPages(buildQuery: (Int) -> SearchQuery): List<HomeSwiperComicListItemResponse.ListItem> {
                        return (1..2).flatMap { page ->
                            try {
                                client.getCategories(buildQuery(page)).content().orEmpty().map { it.toHomeListItem() }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }.distinctBy { it.id }
                    }
                    // 并发拉取各分类首页数据，单个分类失败不影响其他分类
                    val latestDeferred = async {
                        try {
                            client.getLatest(1).content().orEmpty().map { it.toHomeListItem() }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                    val randomDeferred = async {
                        try {
                            client.getRandomRecommend().orEmpty().map { it.toHomeListItem() }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                    val serializationDeferred = async {
                        try {
                            client.getSerialization(1).content().orEmpty().map { it.toHomeListItem() }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                    // 按分类 + 排序维度
                    val doujinDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().category(Category.DOUJIN).page(page).build() } }
                    val singleDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().category(Category.SINGLE).page(page).build() } }
                    val shortDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().category(Category.SHORT).page(page).build() } }
                    val koreanDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().category(Category.KOREAN).page(page).build() } }
                    val americanDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().category(Category.AMERICAN).page(page).build() } }
                    val cosplayDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().category(Category.COSPLAY).page(page).build() } }
                    val image3dDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().category(Category.IMAGE_3D).page(page).build() } }
                    // 按排序维度
                    val weekHotDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().orderBy(OrderBy.MOST_VIEWED).time(TimeOption.WEEK).page(page).build() } }
                    val monthHotDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().orderBy(OrderBy.MOST_VIEWED).time(TimeOption.MONTH).page(page).build() } }
                    val mostLikedDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().orderBy(OrderBy.MOST_LIKED).time(TimeOption.ALL).page(page).build() } }
                    val mostImagesDeferred = async { loadCategoryPages { page -> SearchQuery.Builder().orderBy(OrderBy.MOST_IMAGES).time(TimeOption.ALL).page(page).build() } }

                    val builtinCategories = listOf(
                        HomeSwiperComicListItemResponse("builtin_latest", "最新上架", "builtin_latest", "builtin", "", latestDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_week_hot", "本周热门", "builtin_week_hot", "builtin", "", weekHotDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_month_hot", "本月热门", "builtin_month_hot", "builtin", "", monthHotDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_most_liked", "最多喜欢", "builtin_most_liked", "builtin", "", mostLikedDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_random", "随机推荐", "builtin_random", "builtin", "", randomDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_serialization", "连载系列", "builtin_serialization", "builtin", "", serializationDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_doujin", "同人", "builtin_doujin", "builtin", "", doujinDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_single", "单本", "builtin_single", "builtin", "", singleDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_short", "短篇", "builtin_short", "builtin", "", shortDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_korean", "韩漫", "builtin_korean", "builtin", "", koreanDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_american", "美漫", "builtin_american", "builtin", "", americanDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_cosplay", "Cosplay", "builtin_cosplay", "builtin", "", cosplayDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_3d", "3D", "builtin_3d", "builtin", "", image3dDeferred.await()),
                        HomeSwiperComicListItemResponse("builtin_most_images", "图片最多", "builtin_most_images", "builtin", "", mostImagesDeferred.await()),
                    ).filter { it.content.isNotEmpty() }

                    // 偏好推荐开关开启时，额外请求网络 API 获取基于登录账号的个性化推荐
                    val preferenceEnabled = localSettingManager.localSettingState.value.preferenceRecommendEnabled
                    val preferenceCategories: List<HomeSwiperComicListItemResponse> = if (preferenceEnabled) {
                        try {
                            val networkResponse = service.getHomeSwiperComicList()
                            if (networkResponse.code == 200) {
                                networkResponse.data.orEmpty()
                                    .filter { it.content.isNotEmpty() }
                                    .map { item ->
                                        HomeSwiperComicListItemResponse(
                                            id = "pref_${item.id}",
                                            title = item.title,
                                            slug = item.slug,
                                            type = "preference",
                                            filter_val = item.filter_val,
                                            content = item.content
                                        )
                                    }
                            } else {
                                emptyList()
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }

                    val allCategories = preferenceCategories + builtinCategories
                    if (allCategories.isEmpty()) {
                        NetWorkResult.Error("内置 API 未返回首页漫画")
                    } else {
                        NetWorkResult.Success(allCategories)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 获取首页数据失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun getComicPicListFromEmbeddedApi(id: Int): NetWorkResult<ComicPicListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                currentCoroutineContext().ensureActive()
                withEmbeddedClient { client ->
                    val photo = try {
                        client.getPhoto(id.toString())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    val chapterImages = photo?.images().orEmpty()
                    // Some domains intermittently reject `/chapter`; retain a
                    // compatibility fallback, but accept it only when every
                    // returned image explicitly belongs to this chapter. This
                    // prevents an album-level response from silently becoming
                    // chapter one's cache while keeping downloads functional.
                    val validForChapter: (List<io.github.jukomu.jmcomic.api.model.JmImage>) -> Boolean = { images ->
                        images.isNotEmpty() && images.all { it.photoId() == id.toString() }
                    }
                    val images = if (validForChapter(chapterImages)) {
                        chapterImages
                    } else {
                        val fallbackImages = try {
                            client.getComicRead(id.toString()).images().orEmpty()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            emptyList()
                        }
                        fallbackImages
                            .takeIf(validForChapter)
                            .orEmpty()
                    }
                    if (images.isEmpty()) {
                        NetWorkResult.Error("内置 API 未返回章节图片（章节 ID：$id）")
                    } else {
                        synchronized(imageCache) {
                            imageCache[id] = images
                        }
                        NetWorkResult.Success(
                            ComicPicListResponse(
                                list = images.map { fixImageUrl(it.getDownloadUrl()) },
                                __aId = photo?.albumId()?.toIntOrNull() ?: id,
                                __scrambleId = photo?.scrambleId()?.toIntOrNull()
                                    ?: images.firstOrNull()?.scrambleId()?.toIntOrNull()
                                    ?: 0,
                                __speed = "0"
                            )
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 获取图片列表失败：${e.message ?: "未知错误"}")
            }
        }
    }

    override suspend fun downloadImageBytes(comicId: Int, imageIndex: Int): ByteArray? {
        // Prefer the URL list that created the current reader state. The JmImage
        // cache can belong to an earlier API source or a previous retry.
        val imageUrl = synchronized(imageUrlCache) {
            imageUrlCache[comicId]?.getOrNull(imageIndex)?.let(::fixImageUrl)
        } ?: synchronized(imageCache) {
            imageCache[comicId]?.getOrNull(imageIndex)?.let { fixImageUrl(it.getDownloadUrl()) }
        }
            ?: return null
        return withContext(Dispatchers.IO) {
            for ((candidateIndex, candidateUrl) in imageUrlCandidates(imageUrl).withIndex()) {
                try {
                    logError(
                        "ComicRepositoryImpl",
                        "下载图片 comicId=$comicId index=$imageIndex candidate=$candidateIndex URL=$candidateUrl",
                    )
                    val request = buildImageRequest(candidateUrl)
                    cleanHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            logError(
                                "ComicRepositoryImpl",
                                "下载图片失败 comicId=$comicId index=$imageIndex candidate=$candidateIndex: HTTP ${response.code} URL=$candidateUrl",
                            )
                        } else {
                            val bytes = response.body?.bytes()
                            if (bytes == null || bytes.isEmpty()) {
                                logError(
                                    "ComicRepositoryImpl",
                                    "下载图片返回空内容 comicId=$comicId index=$imageIndex candidate=$candidateIndex URL=$candidateUrl",
                                )
                            } else if (isHtmlImageResponse(response.header("Content-Type"), bytes)) {
                                logError(
                                    "ComicRepositoryImpl",
                                    "下载图片返回网页内容 comicId=$comicId index=$imageIndex candidate=$candidateIndex URL=$candidateUrl",
                                )
                            } else {
                                return@withContext bytes
                            }
                        }
                    }
                } catch (e: Exception) {
                    logError(
                        "ComicRepositoryImpl",
                        "下载图片异常 comicId=$comicId index=$imageIndex candidate=$candidateIndex: ${e.message} URL=$candidateUrl",
                    )
                }
            }
            null
        }
    }

    private fun isHtmlImageResponse(contentType: String?, bytes: ByteArray): Boolean {
        if (contentType?.contains("text/html", ignoreCase = true) == true) return true
        val prefix = String(bytes, 0, minOf(bytes.size, 64), Charsets.UTF_8).trimStart()
        return prefix.startsWith("<!doctype", ignoreCase = true) ||
            prefix.startsWith("<html", ignoreCase = true)
    }

    private suspend fun getComicListFromEmbeddedApi(
        page: Int,
        order: ComicSearchOrderFilter,
        searchContent: String,
    ): NetWorkResult<ComicListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                currentCoroutineContext().ensureActive()
                NetWorkResult.Success(withEmbeddedClient { client ->
                    val query = SearchQuery.Builder()
                        .text(searchContent)
                        .page(page)
                        .orderBy(order.toEmbeddedOrderBy())
                        .build()
                    client.search(query).toComicListResponse(searchContent)
                })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 搜索漫画失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private fun ComicSearchOrderFilter.toEmbeddedOrderBy(): OrderBy {
        return when (this) {
            ComicSearchOrderFilter.NEWEST -> OrderBy.LATEST
            ComicSearchOrderFilter.MOST_COLLECT_COUNT -> OrderBy.MOST_VIEWED
            ComicSearchOrderFilter.MOST_PIC_COUNT -> OrderBy.MOST_IMAGES
            ComicSearchOrderFilter.MOST_LIKE_COUNT -> OrderBy.MOST_LIKED
        }
    }

    private fun JmAlbum.toComicDetailResponse(): ComicDetailResponse {
        return ComicDetailResponse(
            id = id().toIntOrNull() ?: 0,
            name = title().orEmpty(),
            description = description().orEmpty(),
            author = authors().orEmpty(),
            total_views = views().toDisplayCount(),
            likes = likes().toDisplayCount(),
            comment_total = commentCount(),
            tags = tags().orEmpty(),
            actors = actors().orEmpty(),
            works = works().orEmpty(),
            is_favorite = isFavorite,
            liked = liked(),
            related_list = relatedAlbums().orEmpty().map {
                ComicDetailRelatedListItemResponse(
                    id = it.id().orEmpty(),
                    name = it.title().orEmpty(),
                    author = it.authors().orEmpty().firstOrNull().orEmpty(),
                    image = it.image().orEmpty()
                )
            },
            series = photoMetas().orEmpty().map {
                ComicDetailSeriesListItemResponse(
                    id = it.id().orEmpty(),
                    name = it.title().orEmpty(),
                    sort = it.sortOrder().toString()
                )
            },
            series_id = seriesId().orEmpty(),
            price = price().orEmpty(),
            purchased = purchased().equals("true", ignoreCase = true),
            image = image().orEmpty(),
        )
    }

    private fun JmSearchPage.toComicListResponse(searchContent: String): ComicListResponse {
        return ComicListResponse(
            search_query = searchContent,
            total = totalItems().toString(),
            redirect_aid = null,
            content = content().orEmpty().map { it.toContentListItem() }
        )
    }

    private fun JmAlbumMeta.toContentListItem(): ComicListResponse.ContentListItem {
        return ComicListResponse.ContentListItem(
            id = id().orEmpty(),
            author = authors().orEmpty().firstOrNull().orEmpty(),
            description = description(),
            name = title().orEmpty(),
            image = image().orEmpty(),
            category = category().toContentCategory(),
            category_sub = subCategory().toContentCategory(),
            liked = false,
            is_favorite = false,
            update_at = 0,
            tags = tags().orEmpty().takeIf { it.isNotEmpty() }
        )
    }

    private fun JmAlbumMeta.toHomeListItem(): HomeSwiperComicListItemResponse.ListItem {
        return HomeSwiperComicListItemResponse.ListItem(
            id = id().orEmpty(),
            author = authors().orEmpty().firstOrNull().orEmpty(),
            description = description(),
            name = title().orEmpty(),
            image = image().orEmpty(),
            category = category().toHomeCategory(),
            category_sub = subCategory().toHomeCategory(),
            liked = false,
            is_favorite = false,
            update_at = 0,
            tags = tags().orEmpty().takeIf { it.isNotEmpty() }
        )
    }

    private fun JmCategoryMeta?.toContentCategory(): ComicListResponse.ContentListItem.Category {
        return ComicListResponse.ContentListItem.Category(
            id = this?.id(),
            title = this?.title()
        )
    }

    private fun JmCategoryMeta?.toHomeCategory(): HomeSwiperComicListItemResponse.ListItem.Category {
        return HomeSwiperComicListItemResponse.ListItem.Category(
            id = this?.id(),
            title = this?.title()
        )
    }

    private fun JmComment.toCommentListItem(): CommentListResponse.ListItem {
        return CommentListResponse.ListItem(
            AID = null,
            BID = commentId(),
            CID = commentId(),
            UID = userId(),
            username = username(),
            nickname = nickname(),
            likes = likes.toString(),
            gender = gender(),
            update_at = updateAt(),
            addtime = postDate(),
            parent_CID = parentCommentId(),
            name = nickname(),
            content = content(),
            photo = photo() ?: "",
            spoiler = spoiler(),
            replys = replys().orEmpty().map { it.toCommentListItem() }
        )
    }

    private fun String?.toDisplayCount(): Int {
        val text = this?.trim().orEmpty()
        if (text.isBlank()) return 0
        val multiplier = when {
            text.endsWith("K", ignoreCase = true) -> 1_000
            text.endsWith("M", ignoreCase = true) -> 1_000_000
            else -> 1
        }
        val numeric = if (multiplier == 1) text else text.dropLast(1)
        return (numeric.toDoubleOrNull()?.times(multiplier)
            ?: text.filter(Char::isDigit).toDoubleOrNull()
            ?: 0.0).toInt()
    }
}
