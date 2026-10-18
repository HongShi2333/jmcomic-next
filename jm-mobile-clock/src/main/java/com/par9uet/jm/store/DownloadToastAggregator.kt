package com.par9uet.jm.store

class DownloadToastAggregator(
    private val toastManager: ToastManager
) {
    private val batches = mutableMapOf<String, BatchState>()

    fun report(batchId: String, total: Int, comicId: Int, success: Boolean) {
        if (batchId.isBlank() || total <= 1) {
            toastManager.showAsync(if (success) "下载成功" else "下载失败")
            return
        }

        val message = synchronized(this) {
            val state = batches.getOrPut(batchId) { BatchState(total.coerceAtLeast(1)) }
            if (!state.reportedIds.add(comicId)) {
                return
            }
            if (success) {
                state.successCount++
            } else {
                state.failureCount++
            }

            if (state.reportedIds.size >= state.total) {
                batches.remove(batchId)
                batchDownloadMessage(state.successCount, state.failureCount)
            } else {
                null
            }
        }

        if (message != null) {
            toastManager.showAsync(message)
        }
    }

    private fun batchDownloadMessage(successCount: Int, failureCount: Int): String {
        return when {
            failureCount == 0 -> "下载成功 $successCount 个"
            successCount == 0 -> "下载失败 $failureCount 个"
            else -> "下载完成 $successCount 个，失败 $failureCount 个"
        }
    }

    private data class BatchState(
        val total: Int,
        val reportedIds: MutableSet<Int> = mutableSetOf(),
        var successCount: Int = 0,
        var failureCount: Int = 0
    )
}
