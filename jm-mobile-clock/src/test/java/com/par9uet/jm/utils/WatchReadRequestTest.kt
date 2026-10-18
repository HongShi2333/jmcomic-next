package com.par9uet.jm.utils
import com.par9uet.jm.retrofit.model.NetWorkResult
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
class WatchReadRequestTest {
    @Test fun successPassesThrough() = runBlocking { assertEquals(7,(watchReadRequest("test") { NetWorkResult.Success(7) } as NetWorkResult.Success).data) }
    @Test fun exceptionsBecomeReadableErrors() = runBlocking { val result=watchReadRequest<Int>("test") { throw IllegalStateException("bad") };assertTrue(result is NetWorkResult.Error);assertTrue((result as NetWorkResult.Error).message.contains("bad")) }
    @Test fun stalledRequestTimesOut() = runBlocking { val result=watchReadRequest("test",20) { delay(1000);NetWorkResult.Success(7) };assertTrue(result is NetWorkResult.Error) }
    @Test fun externalCancellationIsNotSwallowed() = runBlocking { var cancelled=false;val job=launch { try { watchReadRequest("test") { delay(10000);NetWorkResult.Success(7) } } catch (_:CancellationException) { cancelled=true } };yield();job.cancelAndJoin();assertTrue(cancelled) }
    @Test fun categoriesRemainCompleteAndUnique() { val all=com.par9uet.jm.data.models.watchHomeCategories;assertEquals(14,all.size);assertEquals(14,all.map { it.id }.toSet().size);assertFalse(all.first { it.id=="builtin_random" }.pageable) }
}
