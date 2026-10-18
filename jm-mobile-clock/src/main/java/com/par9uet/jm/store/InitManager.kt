package com.par9uet.jm.store

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.utils.logError

class InitManager {
    val deferred = CompletableDeferred<String>()
    private val initialization = Mutex()

    /** A background cache worker can start the process without ever creating App(). */
    suspend fun initialize(tasks: List<AppInitTask>) = initialization.withLock {
        if (deferred.isCompleted) return@withLock
        for (task in tasks.sortedBy { it.getAppTaskInfo().sort }) {
            try {
                task.init()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                runCatching { logError("InitManager", "${task.getAppTaskInfo().taskName}: ${error.message}") }
            }
        }
        deferred.complete("")
        Unit
    }
}
