package com.par9uet.jm.repository

import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.UserCollectComicListResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class FavoriteWarmupSessionTest {
    @Test
    fun foregroundSharesStartupRequestAndSuccessfulPages() = runBlocking {
        val calls = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val session = FavoriteWarmupSession(this) {
            calls.incrementAndGet()
            started.complete(Unit)
            gate.await()
            NetWorkResult.Success(response(100))
        }
        try {
            val background = async { session.page(1) }
            withTimeout(1_000) { started.await() }
            val foreground = async { session.page(1) }
            gate.complete(Unit)
            assertTrue(background.await() is NetWorkResult.Success)
            assertTrue(foreground.await() is NetWorkResult.Success)
            session.page(1)
            assertEquals(1, calls.get())
        } finally { session.close() }
    }

    @Test
    fun cancellingBackgroundWaiterDoesNotCancelForegroundRequest() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val calls = AtomicInteger()
        val session = FavoriteWarmupSession(this) {
            calls.incrementAndGet()
            started.complete(Unit)
            gate.await()
            NetWorkResult.Success(response(100))
        }
        try {
            val background = async { session.page(1) }
            withTimeout(1_000) { started.await() }
            background.cancelAndJoin()
            val foreground = async { session.page(1) }
            gate.complete(Unit)
            assertTrue(withTimeout(1_000) { foreground.await() } is NetWorkResult.Success)
            assertEquals(1, calls.get())
        } finally { session.close() }
    }

    @Test
    fun onlyFirstDisplayPageIsWarmed() = runBlocking {
        listOf(10 to listOf(1), 40 to listOf(1, 2), 45 to listOf(1, 2, 3), 500 to listOf(1, 2)).forEach { (total, expected) ->
            val calls = java.util.Collections.synchronizedList(mutableListOf<Int>())
            val session = FavoriteWarmupSession(this) {
                calls += it
                NetWorkResult.Success(response(total))
            }
            try {
                session.firstDisplayPage()
                assertEquals(expected, calls)
            } finally { session.close() }
        }
    }

    @Test
    fun failedWarmupCanRetryWithoutDroppingSuccessfulPages() = runBlocking {
        val calls = AtomicInteger()
        val session = FavoriteWarmupSession(this) {
            if (calls.incrementAndGet() == 1) NetWorkResult.Error("offline")
            else NetWorkResult.Success(response(20))
        }
        try {
            assertTrue(session.page(1) is NetWorkResult.Error)
            assertTrue(session.page(1) is NetWorkResult.Success)
            session.page(1)
            assertEquals(2, calls.get())
        } finally { session.close() }
    }

    private fun response(total: Int) = UserCollectComicListResponse(total, list = emptyList(), total = total)
}
