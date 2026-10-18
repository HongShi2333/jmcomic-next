package com.par9uet.jm.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore

/** One decoder/writer at a time on a watch; deletion waits for that writer to release its permit. */
internal object WatchCacheJobs {
    val writer = Semaphore(1)
    val mutations = Mutex()
}

internal fun watchCacheWorkName(chapterId: Int): String = "watch-comic-cache-$chapterId"
internal fun canRunWatchCache(status: String?): Boolean = status in setOf("pending", "downloading")
