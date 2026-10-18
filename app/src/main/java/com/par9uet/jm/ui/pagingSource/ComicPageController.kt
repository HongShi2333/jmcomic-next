package com.par9uet.jm.ui.pagingSource

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.data.models.Comic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

const val COMIC_DISPLAY_PAGE_SIZE = 40
const val COMIC_PAGINATION_THRESHOLD = 50
const val COMIC_REMOTE_PAGE_SIZE = 20
private const val MAX_PRELOAD_REMOTE_REQUESTS = 2
private const val PAGE_PRELOAD_TIMEOUT_MS = 8_000L

internal fun comicDisplayPagingConfig() = PagingConfig(
    pageSize = COMIC_DISPLAY_PAGE_SIZE,
    initialLoadSize = COMIC_DISPLAY_PAGE_SIZE,
    // With placeholders disabled Paging requires a positive prefetch distance.
    // Null append/prepend keys enforce the manual display-page limit.
    prefetchDistance = 1,
    enablePlaceholders = false,
)

data class ComicPageState(
    val page: Int = 1,
    val totalCount: Int? = null,
    val enabled: Boolean = false,
    val hasNext: Boolean = false,
    val isLoading: Boolean = true,
) {
    val pageCount: Int?
        get() = totalCount?.let { ((it.toLong() + COMIC_DISPLAY_PAGE_SIZE - 1) / COMIC_DISPLAY_PAGE_SIZE).toInt().coerceAtLeast(1) }
}

/** Remote counts describe the unfiltered result, never Paging's requested loadSize. */
abstract class ComicRemotePagingSource : PagingSource<Int, Comic>() {
    abstract val hasLocalFilters: Boolean
    open val remotePageSize: Int = COMIC_REMOTE_PAGE_SIZE
    // A metadata filter can issue many album-detail requests for one remote page.
    // Such sources may reuse cached pages, but must not start that work speculatively.
    open val supportsNetworkPreload: Boolean = true
    var remoteTotalCount: Int? = null
        private set

    protected fun remotePage(
        page: Int,
        data: List<Comic>,
        remoteCount: Int,
        total: Int?,
    ): LoadResult.Page<Int, Comic> {
        val offset = (page.toLong() - 1) * remotePageSize
        remoteTotalCount = total?.takeIf { it >= 0 && it.toLong() >= offset + remoteCount }
        val hasMore = remoteCount >= remotePageSize &&
            (remoteTotalCount == null || offset + remoteCount < remoteTotalCount!!)
        return LoadResult.Page(
            data = data,
            prevKey = if (page == 1) null else page - 1,
            nextKey = if (hasMore) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null
}

internal data class ComicDisplayPage(val comics: List<Comic>, val state: ComicPageState)

/** Keeps only requested remote pages; revisiting a display page doesn't request them again. */
internal class ComicPageSession(
    private val source: ComicRemotePagingSource,
    private val preloadTimeoutMillis: Long = PAGE_PRELOAD_TIMEOUT_MS,
) {
    private data class CachedPage(
        val comics: List<Comic>,
        val nextKey: Int?,
        val total: Int?,
    )

    private val cache = mutableMapOf<Int, CachedPage>()
    private val loadMutex = Mutex()
    private var knownTotal: Int? = null
    private class RequestBudget(var remaining: Int)

    fun clear() {
        cache.clear()
        knownTotal = null
    }

    private suspend fun fetch(page: Int, budget: RequestBudget?): CachedPage? {
        cache[page]?.let { return it }
        if (budget != null) {
            if (budget.remaining == 0) return null
            budget.remaining--
        }
        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = page,
                loadSize = source.remotePageSize,
                placeholdersEnabled = false,
            )
        )
        currentCoroutineContext().ensureActive()
        return when (result) {
            is PagingSource.LoadResult.Error -> throw result.throwable
            is PagingSource.LoadResult.Invalid -> error("漫画列表已失效，请下拉刷新")
            is PagingSource.LoadResult.Page -> CachedPage(
                comics = result.data,
                nextKey = result.nextKey,
                total = source.remoteTotalCount,
            ).also { cache[page] = it }
        }
    }

    suspend fun load(requestedPage: Int): ComicDisplayPage = loadMutex.withLock {
        checkNotNull(loadPage(requestedPage, budget = null))
    }

    /** Warm just the next display page; partial successes remain reusable after a timeout/error. */
    suspend fun preloadNextPage(current: ComicPageState): List<Comic> {
        if (current.isLoading || !current.enabled || !current.hasNext) return emptyList()
        return try {
            withTimeoutOrNull(preloadTimeoutMillis) {
                loadMutex.withLock {
                    val target = current.page + 1
                    val budget = RequestBudget(
                        if (source.supportsNetworkPreload) MAX_PRELOAD_REMOTE_REQUESTS else 0,
                    )
                    loadPage(target, budget)?.takeIf { it.state.page == target }?.comics.orEmpty()
                }
            }.orEmpty()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Speculative failures must not replace the visible page with an error.
            emptyList()
        }
    }

    private suspend fun loadPage(requestedPage: Int, budget: RequestBudget?): ComicDisplayPage? {
        var page = requestedPage.coerceAtLeast(1)
        if (!source.hasLocalFilters) {
            knownTotal?.let { total ->
                page = page.coerceAtMost(((total.toLong() + 39) / 40).toInt().coerceAtLeast(1))
            }
        }
        val start = (page - 1) * COMIC_DISPLAY_PAGE_SIZE
        // Local filters must run before slicing, including pages that filter to zero.
        var remotePage: Int? = if (source.hasLocalFilters) 1 else start / source.remotePageSize + 1
        var skipped = if (source.hasLocalFilters) start else start % source.remotePageSize
        val candidates = mutableListOf<Comic>()
        val seenIds = mutableSetOf<Int>()
        var reachedEnd = false
        var visibleTotal = 0
        while (remotePage != null) {
            currentCoroutineContext().ensureActive()
            val result = fetch(remotePage, budget) ?: return null
            if (!source.hasLocalFilters) knownTotal = result.total ?: knownTotal
            val comics = result.comics.filter { seenIds.add(it.id) }
            if (source.hasLocalFilters) {
                visibleTotal += comics.size
                val toSkip = skipped.coerceAtMost(comics.size)
                skipped -= toSkip
                candidates += comics.drop(toSkip)
            } else {
                val toSkip = skipped.coerceAtMost(comics.size)
                skipped -= toSkip
                candidates += comics.drop(toSkip)
            }
            remotePage = result.nextKey
            reachedEnd = remotePage == null
            if (reachedEnd) break
            // A warm-up only needs the next page's items, not another page's lookahead.
            val required = if (budget != null) COMIC_DISPLAY_PAGE_SIZE else when {
                source.hasLocalFilters || knownTotal == null ->
                    if (page == 1) COMIC_PAGINATION_THRESHOLD else COMIC_DISPLAY_PAGE_SIZE + 1
                knownTotal!! < COMIC_PAGINATION_THRESHOLD && page == 1 -> knownTotal!!
                else -> COMIC_DISPLAY_PAGE_SIZE
            }
            if (candidates.size >= required) break
        }
        val total = when {
            source.hasLocalFilters -> if (reachedEnd) visibleTotal else null
            knownTotal != null -> knownTotal
            reachedEnd -> start + candidates.size
            else -> null
        }
        if (page > 1 && total != null && total < COMIC_PAGINATION_THRESHOLD) {
            return loadPage(1, budget)
        }
        // A delete/refresh can invalidate the old last page. Recover without trapping the user.
        if (page > 1 && reachedEnd && candidates.isEmpty()) {
            val lastPage = (((total ?: 0).toLong() + 39) / 40).toInt().coerceAtLeast(1)
            if (lastPage < page) return loadPage(lastPage, budget)
        }
        val enabled = page > 1 || (total ?: candidates.size) >= COMIC_PAGINATION_THRESHOLD
        val limit = if (enabled) COMIC_DISPLAY_PAGE_SIZE else COMIC_PAGINATION_THRESHOLD
        val hasNext = if (total != null) {
            total.toLong() > start.toLong() + limit
        } else {
            candidates.size > limit || !reachedEnd
        }
        return ComicDisplayPage(
            comics = candidates.take(limit),
            state = ComicPageState(page, total, enabled, hasNext, isLoading = false),
        )
    }
}

class ComicPageController {
    private val requestedPage = MutableStateFlow(1)
    private val refreshVersion = MutableStateFlow(0)
    private val _state = MutableStateFlow(ComicPageState())
    val state = _state.asStateFlow()
    private var activeSession: Any? = null
    private var currentSession: ComicPageSession? = null
    private var preloadJob: Job? = null

    private fun cancelPreload() {
        preloadJob?.cancel()
        preloadJob = null
    }

    fun selectPage(page: Int) {
        val current = _state.value
        if (current.isLoading || page < 1 || page == current.page) return
        if (page > current.page && !current.hasNext) return
        if (current.pageCount?.let { page > it } == true) return
        cancelPreload()
        _state.value = current.copy(page = page, isLoading = true)
        requestedPage.value = page
    }

    fun refresh() {
        cancelPreload()
        _state.update { it.copy(isLoading = true) }
        refreshVersion.update { it + 1 }
    }

    /**
     * Called only by a visible screen after its items have arrived. The caller's
     * lifecycle owns this job, so leaving the screen also stops speculative work.
     */
    suspend fun preloadNextPage(): List<Comic> {
        val current = _state.value
        val session = currentSession ?: return emptyList()
        val token = activeSession
        if (current.isLoading || !current.enabled || !current.hasNext) return emptyList()
        val job = currentCoroutineContext()[Job] ?: return emptyList()
        if (preloadJob?.isActive == true && preloadJob !== job) return emptyList()
        preloadJob = job
        return try {
            // Keep controller ownership/validation on the caller (UI) context;
            // only the mutex-protected session work belongs on the background dispatcher.
            val comics = withContext(Dispatchers.IO) { session.preloadNextPage(current) }
            currentCoroutineContext().ensureActive()
            if (activeSession === token && currentSession === session && _state.value == current) {
                comics
            } else {
                emptyList()
            }
        } finally {
            if (preloadJob === job) preloadJob = null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <F : Any> pagingFlow(
        filters: Flow<F>,
        sourceFactory: (F) -> ComicRemotePagingSource,
    ): Flow<PagingData<Comic>> = filters.distinctUntilChanged().flatMapLatest { filter ->
        cancelPreload()
        val token = Any()
        activeSession = token
        requestedPage.value = 1
        _state.value = ComicPageState()
        var session = ComicPageSession(sourceFactory(filter))
        currentSession = session
        var lastRefreshVersion = refreshVersion.value
        combine(requestedPage, refreshVersion) { page, refresh ->
            page to refresh
        }.flatMapLatest { (page, refresh) ->
            if (refresh != lastRefreshVersion) {
                cancelPreload()
                session = ComicPageSession(sourceFactory(filter))
                currentSession = session
                lastRefreshVersion = refresh
            }
            val pageSession = session
            Pager(
                config = comicDisplayPagingConfig(),
                pagingSourceFactory = {
                    object : PagingSource<Int, Comic>() {
                        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
                            try {
                                if (activeSession === token && requestedPage.value == page && refreshVersion.value == refresh) {
                                    _state.value = _state.value.copy(isLoading = true)
                                }
                                val result = pageSession.load(page)
                                currentCoroutineContext().ensureActive()
                                if (activeSession === token && requestedPage.value == page && refreshVersion.value == refresh) {
                                    _state.value = result.state
                                }
                                // No append/prepend keys: scrolling cannot fetch the next display page.
                                return LoadResult.Page(result.comics, prevKey = null, nextKey = null)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                if (activeSession === token && requestedPage.value == page && refreshVersion.value == refresh) {
                                    _state.value = _state.value.copy(isLoading = false)
                                }
                                return LoadResult.Error(error)
                            }
                        }

                        override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null
                    }
                },
            ).flow
        }
    }
}
