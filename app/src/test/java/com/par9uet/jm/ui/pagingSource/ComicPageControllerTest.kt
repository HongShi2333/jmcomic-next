package com.par9uet.jm.ui.pagingSource

import androidx.paging.PagingSource
import com.par9uet.jm.data.models.Comic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicPageControllerTest {
    @Test
    fun twelveItemWebsitePagesFillFortyItemDisplayPagesWithoutSkipping() = runBlocking {
        val source = FakeSource(134, 12)
        val session = ComicPageSession(source)
        assertEquals((1..40).toList(), session.load(1).comics.map { it.id })
        assertEquals(listOf(1, 2, 3, 4), source.requests)
        assertEquals((41..80).toList(), session.load(2).comics.map { it.id })
        assertEquals((1..7).toList(), source.requests)
        assertEquals((81..120).toList(), session.load(3).comics.map { it.id })
        val last = session.load(4)
        assertEquals((121..134).toList(), last.comics.map { it.id })
        assertFalse(last.state.hasNext)
    }

    @Test
    fun largeFavoritesOnlyLoadFortyAndRevisitUsesCache() = runBlocking {
        val source = FakeSource(total = 125, pageSize = 20)
        val session = ComicPageSession(source)
        val first = session.load(1)
        assertEquals((1..40).toList(), first.comics.map { it.id })
        assertEquals(listOf(1, 2), source.requests)
        assertTrue(first.state.enabled)
        assertEquals(4, first.state.pageCount)
        assertTrue(first.state.hasNext)
        assertEquals((41..80).toList(), session.load(2).comics.map { it.id })
        assertEquals(listOf(1, 2, 3, 4), source.requests)
        session.load(1)
        assertEquals(listOf(1, 2, 3, 4), source.requests)
    }

    @Test
    fun eightyItemSearchPageIsSplitWithoutDroppingSecondHalf() = runBlocking {
        val source = FakeSource(total = 165, pageSize = 80)
        val session = ComicPageSession(source)
        assertEquals((1..40).toList(), session.load(1).comics.map { it.id })
        assertEquals((41..80).toList(), session.load(2).comics.map { it.id })
        assertEquals(listOf(1), source.requests)
        assertEquals((81..120).toList(), session.load(3).comics.map { it.id })
        assertEquals((121..160).toList(), session.load(4).comics.map { it.id })
        val last = session.load(5)
        assertEquals((161..165).toList(), last.comics.map { it.id })
        assertFalse(last.state.hasNext)
        assertEquals(listOf(1, 2, 3), source.requests)
    }

    @Test
    fun fewerThanFiftyItemsRemainInOneList() = runBlocking {
        listOf(0, 1, 20, 40, 41, 49).forEach { total ->
            val first = ComicPageSession(FakeSource(total, 20)).load(1)
            assertEquals(total, first.comics.size)
            assertFalse(first.state.enabled)
            assertFalse(first.state.hasNext)
        }
        val first = ComicPageSession(FakeSource(50, 20)).load(1)
        assertEquals(40, first.comics.size)
        assertTrue(first.state.enabled)
        assertTrue(first.state.hasNext)
    }

    @Test
    fun unknownHistoryTotalKeepsLoadingBeyondFirstRemotePage() = runBlocking {
        val session = ComicPageSession(FakeSource(65, 20, reportTotal = false))
        val first = session.load(1)
        assertEquals(40, first.comics.size)
        assertTrue(first.state.hasNext)
        val second = session.load(2)
        assertEquals((41..65).toList(), second.comics.map { it.id })
        assertEquals(65, second.state.totalCount)
        assertFalse(second.state.hasNext)
    }

    @Test
    fun filteringRunsBeforePaginationIncludingEmptyRemotePages() = runBlocking {
        val source = FakeSource(240, 20, filter = { it > 40 && it % 2 == 0 })
        val session = ComicPageSession(source)
        val first = session.load(1)
        assertEquals((42..120 step 2).toList(), first.comics.map { it.id })
        val second = session.load(2)
        assertEquals((122..200 step 2).toList(), second.comics.map { it.id })
        val last = session.load(3)
        assertEquals((202..240 step 2).toList(), last.comics.map { it.id })
        assertEquals(100, last.state.totalCount)
        assertFalse(last.state.hasNext)
    }

    @Test
    fun smallFilteredListDoesNotEnablePaginationFromRawTotal() = runBlocking {
        val first = ComicPageSession(FakeSource(160, 80, filter = { it <= 45 })).load(1)
        assertEquals(45, first.comics.size)
        assertEquals(45, first.state.totalCount)
        assertFalse(first.state.enabled)
    }

    @Test
    fun clearRefetchesRemotePagesAndShrinkRecoversLastPage() = runBlocking {
        val source = FakeSource(100, 20)
        val session = ComicPageSession(source)
        session.load(1)
        session.clear()
        session.load(1)
        assertEquals(listOf(1, 2, 1, 2), source.requests)
        session.load(3)
        source.total = 45
        session.clear()
        val recovered = session.load(3)
        assertEquals(1, recovered.state.page)
        assertEquals((1..45).toList(), recovered.comics.map { it.id })
        assertFalse(recovered.state.enabled)
    }

    @Test
    fun failedPageCanBeRetriedWithoutReloadingSuccessfulPages() = runBlocking {
        val source = FakeSource(100, 20)
        val session = ComicPageSession(source)
        session.load(1)
        source.failedPage = 4
        var failed = false
        try {
            session.load(2)
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue(failed)
        source.failedPage = null
        assertEquals((41..80).toList(), session.load(2).comics.map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 4), source.requests)
    }

    @Test
    fun controllerCreatesLegalBoundedPagingConfig() {
        val config = comicDisplayPagingConfig()
        assertEquals(40, config.pageSize)
        assertEquals(40, config.initialLoadSize)
        assertEquals(1, config.prefetchDistance)
        assertFalse(config.enablePlaceholders)
    }

    @Test
    fun firstFavoritesPagePreloadsOnlyTheSecondPageAndReusesIt() = runBlocking {
        val source = FakeSource(200, 20)
        val session = ComicPageSession(source)
        val first = session.load(1)
        assertEquals(listOf(1, 2), source.requests)
        assertEquals((41..80).toList(), session.preloadNextPage(first.state).map { it.id })
        assertEquals(listOf(1, 2, 3, 4), source.requests)
        assertEquals(1, first.state.page)
        assertEquals((1..40).toList(), first.comics.map { it.id })
        assertEquals((41..80).toList(), session.load(2).comics.map { it.id })
        assertEquals(listOf(1, 2, 3, 4), source.requests)
    }

    @Test
    fun secondSearchPagePreloadUsesAlreadyLoadedRemotePage() = runBlocking {
        val source = FakeSource(200, 80)
        val session = ComicPageSession(source)
        val first = session.load(1)
        assertEquals((41..80).toList(), session.preloadNextPage(first.state).map { it.id })
        assertEquals(listOf(1), source.requests)
        session.load(2)
        assertEquals(listOf(1), source.requests)
    }

    @Test
    fun loadingSmallAndLastPagesDoNotPreload() = runBlocking {
        val smallSource = FakeSource(49, 20)
        val smallSession = ComicPageSession(smallSource)
        val small = smallSession.load(1)
        assertTrue(smallSession.preloadNextPage(small.state).isEmpty())
        assertEquals(listOf(1, 2, 3), smallSource.requests)

        val source = FakeSource(85, 20)
        val session = ComicPageSession(source)
        val first = session.load(1)
        assertTrue(session.preloadNextPage(first.state.copy(isLoading = true)).isEmpty())
        assertEquals(listOf(1, 2), source.requests)
        val last = session.load(3)
        assertTrue(session.preloadNextPage(last.state).isEmpty())
        assertEquals(listOf(1, 2, 5), source.requests)
    }

    @Test
    fun sparseFilterPreloadHasTwoRequestBudgetAndKeepsPartialCache() = runBlocking {
        val source = FakeSource(600, 20, filter = { it % 5 == 0 })
        val session = ComicPageSession(source)
        val first = session.load(1)
        assertEquals((1..13).toList(), source.requests)
        assertTrue(session.preloadNextPage(first.state).isEmpty())
        assertEquals((1..15).toList(), source.requests)
        assertEquals((205..400 step 5).toList(), session.load(2).comics.map { it.id })
        assertEquals((1..21).toList(), source.requests)
    }

    @Test
    fun metadataFilterPreloadOnlyUsesCacheWithoutNewDetailRequests() = runBlocking {
        val source = FakeSource(200, 80, filter = { true }, allowNetworkPreload = false)
        val session = ComicPageSession(source)
        val first = session.load(1)
        assertEquals((41..80).toList(), session.preloadNextPage(first.state).map { it.id })
        assertEquals(listOf(1), source.requests)
        val second = session.load(2)
        val before = source.requests.toList()
        // The filtered foreground load has lookahead; this may already include remote page 2.
        session.preloadNextPage(second.state)
        assertEquals(before, source.requests)
    }

    @Test
    fun preloadFailureIsSilentAndSuccessfulHalfIsStillReusable() = runBlocking {
        val source = FakeSource(200, 20)
        val session = ComicPageSession(source)
        val first = session.load(1)
        source.failedPage = 4
        assertTrue(session.preloadNextPage(first.state).isEmpty())
        assertEquals((1..40).toList(), session.load(1).comics.map { it.id })
        source.failedPage = null
        assertEquals((41..80).toList(), session.load(2).comics.map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 4), source.requests)
    }

    @Test
    fun preloadTimeoutKeepsSuccessfulRemotePages() = runBlocking {
        val source = FakeSource(200, 20)
        val session = ComicPageSession(source, preloadTimeoutMillis = 100)
        val first = session.load(1)
        source.beforeLoad = { if (it == 4) delay(10_000) }
        assertTrue(session.preloadNextPage(first.state).isEmpty())
        source.beforeLoad = null
        assertEquals((41..80).toList(), session.load(2).comics.map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 4), source.requests)
    }

    @Test
    fun cancelledPreloadReleasesSessionForForeground() = runBlocking {
        val source = FakeSource(200, 20)
        val session = ComicPageSession(source)
        val first = session.load(1)
        val started = CompletableDeferred<Unit>()
        source.beforeLoad = {
            if (it == 4) {
                started.complete(Unit)
                CompletableDeferred<Unit>().await()
            }
        }
        val preload = async { session.preloadNextPage(first.state) }
        withTimeout(1_000) { started.await() }
        preload.cancelAndJoin()
        source.beforeLoad = null
        val second = withTimeout(1_000) { session.load(2) }
        assertEquals((41..80).toList(), second.comics.map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 4), source.requests)
    }

    private class FakeSource(
        var total: Int,
        private val pageSize: Int,
        private val reportTotal: Boolean = true,
        private val filter: ((Int) -> Boolean)? = null,
        private val allowNetworkPreload: Boolean = true,
    ) : ComicRemotePagingSource() {
        val requests = mutableListOf<Int>()
        var failedPage: Int? = null
        var beforeLoad: (suspend (Int) -> Unit)? = null
        override val hasLocalFilters: Boolean get() = filter != null
        override val remotePageSize: Int get() = pageSize
        override val supportsNetworkPreload: Boolean get() = allowNetworkPreload

        override suspend fun load(params: PagingSource.LoadParams<Int>): PagingSource.LoadResult<Int, Comic> {
            val page = params.key ?: 1
            requests += page
            beforeLoad?.invoke(page)
            if (page == failedPage) return LoadResult.Error(IllegalStateException("test network error"))
            val start = (page - 1) * pageSize + 1
            val ids = if (start > total) emptyList() else (start..minOf(start + pageSize - 1, total)).toList()
            return remotePage(
                page = page,
                data = ids.filter { filter?.invoke(it) != false }.map { Comic.create(it, "Comic $it", emptyList()) },
                remoteCount = ids.size,
                total = if (reportTotal) total else null,
            )
        }
    }
}
