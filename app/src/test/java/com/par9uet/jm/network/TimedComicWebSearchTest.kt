package com.par9uet.jm.network

import com.par9uet.jm.data.models.ComicRankCategory
import com.par9uet.jm.data.models.ComicRankPeriod
import com.par9uet.jm.retrofit.model.NetWorkResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TimedComicWebSearchTest {
    @Test
    fun periodQueriesKeepPageTagAndExclusions() {
        val urls = ComicRankPeriod.entries.map { period ->
            buildTimedComicSearchUrl("example.test", 2, "tag +artist -excluded", 3, period)
        }
        assertEquals(listOf("t", "w", "m"), urls.map { it.queryParameter("t") })
        urls.forEach {
            assertEquals("/search/photos", it.encodedPath)
            assertEquals("2", it.queryParameter("page"))
            assertEquals("mv", it.queryParameter("o"))
            assertEquals("3", it.queryParameter("main_tag"))
            assertEquals("tag +artist -excluded", it.queryParameter("search_query"))
        }
    }

    @Test
    fun categoryExclusionsKeepCategoryInWebsitePath() {
        val url = buildTimedComicSearchUrl(
            "https://example.test/", 3, "-excluded", 3, ComicRankPeriod.WEEK, ComicRankCategory.DOUJIN,
        )
        assertEquals("/search/photos/doujin", url.encodedPath)
        assertEquals("-excluded", url.queryParameter("search_query"))
        assertEquals("0", url.queryParameter("main_tag"))
        assertEquals("3", url.queryParameter("page"))
    }

    @Test
    fun parsesServerOrderMetadataAndRelativeCoversWithoutRequestingDetails() {
        val url = buildTimedComicSearchUrl("example.test", 1, "tag", 3, ComicRankPeriod.DAY)
        val parsed = parseTimedComicSearchPage(resultHtml(), url, "tag")
        assertEquals("81", parsed.total)
        assertEquals(listOf("102", "101"), parsed.content.map { it.id })
        assertEquals(listOf("Author", "Author"), parsed.content.map { it.author })
        assertEquals(listOf("tag"), parsed.content.first().tags)
        assertEquals("https://example.test/media/102.jpg", parsed.content.first().image)
    }

    @Test
    fun explicitZeroCountIsAnEmptyResult() {
        val url = buildTimedComicSearchUrl("example.test", 1, "tag", 3, ComicRankPeriod.DAY)
        val parsed = parseTimedComicSearchPage(resultHtml(total = 0, cards = ""), url, "tag")
        assertEquals("0", parsed.total)
        assertTrue(parsed.content.isEmpty())
    }

    @Test(expected = IOException::class)
    fun verificationPageIsNotAnEmptyRanking() {
        val url = buildTimedComicSearchUrl("example.test", 1, "tag", 3, ComicRankPeriod.DAY)
        parseTimedComicSearchPage("<form id='challenge-form'>Checking browser</form>", url, "tag")
    }

    @Test(expected = IOException::class)
    fun unrecognizedMarkupIsNotAnEmptyRanking() {
        val url = buildTimedComicSearchUrl("example.test", 1, "tag", 3, ComicRankPeriod.DAY)
        parseTimedComicSearchPage("<html><body>Sign in</body></html>", url, "tag")
    }

    @Test(expected = IOException::class)
    fun partiallyParsedCardsCannotHideMissingComics() {
        val url = buildTimedComicSearchUrl("example.test", 1, "tag", 3, ComicRankPeriod.DAY)
        parseTimedComicSearchPage(resultHtml(cards = card(102) + """
            <div class="list-col"><div class="well"><div class="thumb-overlay"><img></div></div></div>
        """), url, "tag")
    }

    @Test
    fun transportPreservesPeriodAndDoesNotUseMobileSearch() = runBlocking {
        val paths = mutableListOf<String>()
        val periods = mutableListOf<String?>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            paths += chain.request().url.encodedPath
            periods += chain.request().url.queryParameter("t")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(resultHtml().toResponseBody()).build()
        }.build()
        val source = TimedComicWebSearch(client)
        ComicRankPeriod.entries.forEach {
            val result = source.load("example.test", 1, "tag", 3, it)
            assertTrue(result is NetWorkResult.Success)
        }
        assertEquals(List(3) { "/search/photos" }, paths)
        assertEquals(listOf("t", "w", "m"), periods)
    }

    @Test
    fun failureDoesNotFallbackToAllTimeApi() = runBlocking {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(503).message("Unavailable").body("Unavailable".toResponseBody()).build()
        }.build()
        val result = TimedComicWebSearch(client, fallbackHosts = { emptyList() }).load("example.test", 1, "tag", 3, ComicRankPeriod.WEEK)
        assertTrue(result is NetWorkResult.Error)
        assertEquals(1, calls)
    }

    @Test
    fun redirectThatDropsTimeFilterFailsInsteadOfShowingAllTimeResults() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val redirected = chain.request().newBuilder().url(
                chain.request().url.newBuilder().setQueryParameter("t", "a").build(),
            ).build()
            Response.Builder().request(redirected).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(resultHtml().toResponseBody()).build()
        }.build()
        val result = TimedComicWebSearch(client, fallbackHosts = { emptyList() }).load("example.test", 1, "tag", 3, ComicRankPeriod.MONTH)
        assertTrue(result is NetWorkResult.Error)
    }

    @Test
    fun emptySearchNeverReturnsRecommendationsBelowResults() {
        val html = """
            <div class="col-xs-12 col-md-9 col-sm-8">
              <div class="well well-sm">
                <span class="title-truncate">搜寻: <span class="text-white">cosplay</span></span>
                <span class="text-danger">没发现A漫.</span>
                <h3>找不到和<span>cosplay</span>相符的內容</h3>
              </div>
            </div>
            <h4 class="talk-title">猜你喜欢</h4>
            ${card(999)}
        """.trimIndent()
        ComicRankPeriod.entries.forEach { period ->
            val url = buildTimedComicSearchUrl("example.test", 1, "cosplay", 3, period)
            val parsed = parseTimedComicSearchPage(html, url, "cosplay")
            assertEquals("0", parsed.total)
            assertTrue(parsed.content.isEmpty())
        }
    }

    @Test
    fun sidebarAndRecommendationsDoNotContaminateNonemptyRanking() {
        val url = buildTimedComicSearchUrl("example.test", 1, "tag", 3, ComicRankPeriod.WEEK)
        val html = resultHtml() + "<aside>${card(998)}</aside><h4>猜你喜欢</h4>${card(999)}"
        assertEquals(listOf("102", "101"), parseTimedComicSearchPage(html, url, "tag").content.map { it.id })
    }

    @Test
    fun numericSearchTermDoesNotBecomeResultTotal() {
        val url = buildTimedComicSearchUrl("example.test", 1, "12345", 0, ComicRankPeriod.WEEK)
        val html = resultHtml().replace("搜寻: tag", """搜寻: <span class="text-white">12345</span>""")
        assertEquals("81", parseTimedComicSearchPage(html, url, "12345").total)
    }

    @Test
    fun recoversWithPublishedHostAndReusesItOnNextPage() = runBlocking {
        val hosts = java.util.Collections.synchronizedList(mutableListOf<String>())
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val host = chain.request().url.host
            hosts += host
            if (host == "failed.test") throw IOException("unavailable")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(resultHtml().toResponseBody()).build()
        }.build()
        val source = TimedComicWebSearch(client, fallbackHosts = { listOf("working.test") })
        assertTrue(source.load("failed.test", 1, "tag", 3, ComicRankPeriod.WEEK) is NetWorkResult.Success)
        assertTrue(source.load("failed.test", 2, "tag", 3, ComicRankPeriod.WEEK) is NetWorkResult.Success)
        assertEquals(listOf("failed.test", "working.test", "working.test"), hosts)
    }

    private fun resultHtml(total: Int = 81, cards: String = card(102) + card(101)): String = """
        <html><body>
        <div class="col-xs-12 col-md-9 col-sm-8">
          <div class="well well-sm"><span>搜寻: tag</span><span class="text-white">$total</span> 搜索結果</div>
          <div class="row m-0">$cards</div>
        </div>
        </body></html>
    """.trimIndent()

    private fun card(id: Int): String = """
        <div class="list-col"><div class="well">
          <div class="thumb-overlay"><a href="/album/$id"><img title="Comic $id" src="/blank.jpg" data-original="/media/$id.jpg"></a></div>
          <div class="video-title title-truncate">Comic $id</div>
          <div class="title-truncate"><a href="/author">Author</a></div>
          <div class="tags title-truncate"><a href="/tag">tag</a></div>
        </div></div>
    """.trimIndent()
}
