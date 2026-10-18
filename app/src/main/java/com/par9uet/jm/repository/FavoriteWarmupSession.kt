package com.par9uet.jm.repository

import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.UserCollectComicListResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

/** A short startup window shared by the home warm-up and foreground favorites. */
internal class FavoriteWarmupSession(
    parentScope: CoroutineScope,
    private val fetch: suspend (Int) -> NetWorkResult<UserCollectComicListResponse>,
) {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job + Dispatchers.IO)
    private val pages = ConcurrentHashMap<Int, Deferred<NetWorkResult<UserCollectComicListResponse>>>()

    suspend fun page(number: Int): NetWorkResult<UserCollectComicListResponse> {
        require(number in 1..3)
        val created = scope.async(start = CoroutineStart.LAZY) { fetch(number) }
        val existing = pages.putIfAbsent(number, created)
        val request = existing ?: created
        if (existing != null) created.cancel()
        request.start()
        request.invokeOnCompletion { error -> if (error != null) pages.remove(number, request) }
        return request.await().also { if (it is NetWorkResult.Error) pages.remove(number, request) }
    }

    suspend fun firstDisplayPage(): List<UserCollectComicListResponse> {
        val first = when (val result = page(1)) {
            is NetWorkResult.Success -> result.data
            is NetWorkResult.Error -> return emptyList()
        }
        val result = mutableListOf(first)
        if (first.total > 20) {
            (page(2) as? NetWorkResult.Success<UserCollectComicListResponse>)?.data?.let(result::add)
        }
        // Lists below the pagination threshold are shown together (up to 49).
        if (first.total in 41..49) {
            (page(3) as? NetWorkResult.Success<UserCollectComicListResponse>)?.data?.let(result::add)
        }
        return result
    }

    fun close() = job.cancel()
}
