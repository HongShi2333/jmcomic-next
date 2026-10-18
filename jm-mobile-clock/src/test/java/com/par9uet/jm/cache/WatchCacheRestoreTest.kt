package com.par9uet.jm.cache

import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.store.InitManager
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.task.AppTaskInfo
import com.par9uet.jm.ui.viewModel.groupDownloads
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class WatchCacheRestoreTest {
    private fun item(id: Int, group: Int, status: String) = DownloadComic(
        id, "漫画$id", emptyList(), emptyList(), "", "/cache/$group/$id",
        if (status == "complete") 1f else .5f, status, id.toLong(), group, "系列$group", "章节$id",
    )
    @Test fun partiallyCachedSeriesAppearsOnlyOnceAndKeepsAllChapterIds() {
        val groups = groupDownloads(listOf(item(1, 10, "complete"), item(2, 10, "downloading"), item(3, 20, "complete")))
        assertEquals(2, groups.size)
        val series = groups.single { it.id == 10 }
        assertEquals(setOf(1, 2), series.itemIds)
        assertEquals("downloading", series.status)
        assertEquals(.75f, series.progress, .001f)
        assertEquals("complete", groups.single { it.id == 20 }.status)
    }
    @Test fun pausedCompleteAndDeletedTasksCannotWrite() {
        assertTrue(canRunWatchCache("pending"))
        assertTrue(canRunWatchCache("downloading"))
        for (state in listOf(null, "paused", "complete", "error")) assertFalse(canRunWatchCache(state))
    }
    @Test fun concurrentAppAndWorkerInitializeOnlyOnceInOrder() = runBlocking {
        val manager = InitManager()
        val order = mutableListOf<Int>()
        fun task(sort: Int) = object : AppInitTask {
            override fun getAppTaskInfo() = AppTaskInfo("test$sort", sort)
            override suspend fun init() { delay(5); order += sort }
        }
        val tasks = listOf(task(2), task(0), task(1))
        coroutineScope { repeat(3) { launch { manager.initialize(tasks) } } }
        assertEquals(listOf(0, 1, 2), order)
        assertTrue(manager.deferred.isCompleted)
    }
    @Test fun cancelledWorkerDoesNotPublishInitializationAsComplete() = runBlocking {
        val manager = InitManager()
        val started = CompletableDeferred<Unit>()
        val task = object : AppInitTask {
            override fun getAppTaskInfo() = AppTaskInfo("cancel", 0)
            override suspend fun init() { started.complete(Unit); awaitCancellation() }
        }
        val job = launch { manager.initialize(listOf(task)) }
        started.await()
        job.cancelAndJoin()
        assertFalse(manager.deferred.isCompleted)
        manager.initialize(emptyList())
        assertTrue(manager.deferred.isCompleted)
    }
}
