package com.par9uet.jm.ui.viewModel

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.par9uet.jm.cache.getComicChapterDownloadDir
import com.par9uet.jm.cache.getComicChapterDownloadPath
import com.par9uet.jm.cache.getDownloadDir
import com.par9uet.jm.cache.getDownloadTreeUri
import com.par9uet.jm.cache.findExistingComicChapterDownloadPath
import com.par9uet.jm.cache.getTreeUriForCachePath
import com.par9uet.jm.cache.setDownloadTreeUri
import com.par9uet.jm.cache.listComicImageFiles
import com.par9uet.jm.cache.isDocumentCachePath
import com.par9uet.jm.cache.listComicImagePaths
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.data.models.ComicPicImageState
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.CollectComicResponse
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.ComicPicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ReadHistoryManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.models.CommonUIState
import com.par9uet.jm.utils.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import com.par9uet.jm.utils.watchReadRequest
import com.par9uet.jm.data.models.ImageResultState
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min

class ComicReadViewModel(
    private val comicRepository: ComicRepository,
    private val picImageLoader: ImageLoader,
    private val localSettingManager: LocalSettingManager,
    private val downloadComicDao: DownloadComicDao,
    private val toastManager: ToastManager,
    private val readHistoryManager: ReadHistoryManager,
) : ViewModel() {
    var isShowToolBar = mutableStateOf(false)
    var currentIndexState = mutableIntStateOf(0)
    var loadedComicId = mutableIntStateOf(-1)
    var readHistoryComicId = mutableIntStateOf(-1)
    private val _comicPicState = MutableStateFlow(
        CommonUIState<List<ComicPicImageState>>(
            isLoading = true
        )
    )
    val comicPicState = _comicPicState.asStateFlow()
    private val _comicDetailState = MutableStateFlow(CommonUIState<Comic>())
    val comicDetailState = _comicDetailState.asStateFlow()
    private val _localChapterList = MutableStateFlow<List<ComicChapter>>(emptyList())
    val localChapterList = _localChapterList.asStateFlow()

    val size: Int get() = _comicPicState.value.data?.size ?: 0

    private val prefetchSet = mutableSetOf<Int>()
    private var decodeSemaphore = Semaphore(2)
    private var decodeConcurrency = 2
    private fun currentDecodeSemaphore(): Semaphore {
        val target = localSettingManager.localSettingState.value.readDecodeConcurrency.coerceIn(1, 2)
        if (target != decodeConcurrency) { decodeConcurrency = target; decodeSemaphore = Semaphore(target) }
        return decodeSemaphore
    }
    private val decodeJobs = mutableMapOf<Int, Job>()
    private var chapterJob: Job? = null
    private var detailJob: Job? = null

    fun getComicDetail(comicId: Int) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _comicDetailState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = watchReadRequest("漫画详情") { comicRepository.getComicDetail(comicId) }) {
                is NetWorkResult.Error -> {
                    _comicDetailState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<ComicDetailResponse> -> {
                    val comic = data.data.toComic()
                    readHistoryComicId.intValue = readHistoryManager.markRead(comic, comicId)
                    _comicDetailState.update {
                        it.copy(
                            data = comic
                        )
                    }
                }
            }
            _comicDetailState.update {
                it.copy(isLoading = false)
            }
        }
    }

    fun clearComicDetail() {
        _comicDetailState.update { CommonUIState() }
    }

    fun collect(comicId: Int) {
        updateCollectState(comicId, true)
    }

    fun unCollect(comicId: Int) {
        updateCollectState(comicId, false)
    }

    private fun updateCollectState(comicId: Int, targetCollect: Boolean) {
        viewModelScope.launch {
            when (val data: NetWorkResult<CollectComicResponse> = if (targetCollect) {
                comicRepository.collectComic(comicId)
            } else {
                comicRepository.unCollectComic(comicId)
            }) {
                is NetWorkResult.Error -> {
                    toastManager.showAsync(data.message)
                }

                is NetWorkResult.Success<CollectComicResponse> -> {
                    toastManager.showAsync(if (targetCollect) "收藏成功" else "取消收藏成功")
                    _comicDetailState.update {
                        it.copy(
                            data = it.data?.copy(isCollect = targetCollect)
                        )
                    }
                }
            }
        }
    }

    fun getComicPicList(comicId: Int, shunt: String, onSuccess: (() -> Unit)? = null) {
        chapterJob?.cancel()
        decodeJobs.values.toList().forEach { it.cancel() }; decodeJobs.clear()
        chapterJob = viewModelScope.launch {
            _localChapterList.value = emptyList()
            _comicPicState.value = CommonUIState(isLoading = true)
            _comicPicState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            prefetchSet.clear()
            when (val data = watchReadRequest("章节图片列表") { comicRepository.getComicPicList(comicId, shunt) }) {
                is NetWorkResult.Error -> {
                    _comicPicState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<ComicPicListResponse> -> {
                    _comicPicState.update {
                        it.copy(
                            data = data.data.list.mapIndexed { index, item ->
                                ComicPicImageState(
                                    index,
                                    comicId,
                                    item,
                                    data.data.__scrambleId,
                                    data.data.__speed,
                                    picImageLoader,
                                    imageFetcher = {
                                        comicRepository.downloadImageBytes(comicId, index)
                                    }
                                )
                            }
                        )
                    }
                    onSuccess?.invoke()
                }
            }
            _comicPicState.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    /**
     * Start metadata and page-list requests together, but calculate recovery from
     * the persisted chapter key before either response can overwrite reader state.
     */
    fun loadRemoteComicChapter(
        comicId: Int,
        shunt: String,
        restoreReadingMemory: Boolean,
        onSuccess: (savedPageIndex: Int) -> Unit,
    ) {
        val historyComicKey = readHistoryManager.historyKeyForChapter(comicId)
        val savedPageIndex = if (restoreReadingMemory) {
            readHistoryManager.lastReadPageIndex(historyComicKey, comicId)
        } else {
            0
        }
        readHistoryComicId.intValue = historyComicKey
        getComicDetail(comicId)
        getComicPicList(comicId, shunt) {
            onSuccess(savedPageIndex)
        }
    }

    fun getLocalComicPicList(
        comicId: Int,
        context: Context,
        restoreReadingMemory: Boolean,
        onSuccess: ((savedPageIndex: Int) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            _comicPicState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            prefetchSet.clear()
            val downloadComic = downloadComicDao.getById(comicId)
            val groupId = downloadComic?.groupId?.takeIf { it != 0 } ?: comicId
            val savedPageIndex = if (restoreReadingMemory) {
                readHistoryManager.lastReadPageIndex(groupId, comicId)
            } else {
                0
            }
            readHistoryComicId.intValue = readHistoryManager.markRead(groupId, comicId)
            loadLocalChapterList(comicId, downloadComic)
            val storedPath = downloadComic?.zipPath.orEmpty()
            if (getDownloadTreeUri(context) == null && downloadComic != null) {
                val inferredTree = downloadComicDao.getByGroupId(groupId)
                    .asSequence()
                    .flatMap { sequenceOf(it.zipPath, it.coverPath) }
                    .mapNotNull(::getTreeUriForCachePath)
                    .firstOrNull()
                if (inferredTree != null) {
                    setDownloadTreeUri(context, inferredTree.toString())
                    localSettingManager.updateDownloadTreeUri(inferredTree.toString())
                }
            }
            val customStorageEnabled = getDownloadTreeUri(context) != null
            // Once a custom tree is selected it is the source of truth. Reading
            // the old default path here made multi-page chapters appear to work
            // while still pointing at files that had already been migrated.
            var files = if (customStorageEnabled && downloadComic != null) {
                runCatching {
                    val currentPath = findExistingComicChapterDownloadPath(context, downloadComic)
                        ?: getComicChapterDownloadPath(context, downloadComic)
                    listLocalImages(context, currentPath)
                }.getOrDefault(emptyList())
                    .ifEmpty {
                        // Do not use a stale default-path URI once a custom tree
                        // is active. The migrated tree is the only source here.
                        emptyList()
                    }
            } else {
                storedPath.takeIf(::isDocumentCachePath)?.let { listLocalImages(context, it) }
                    ?: ensureLocalImageDir(context, comicId, downloadComic)
                        ?.let(::listComicImageFiles)
                        .orEmpty()
                        .map(File::getAbsolutePath)
            }

            if (files.isEmpty()) {
                _comicPicState.update {
                    it.copy(
                        isLoading = false,
                        isError = true,
                        errorMsg = "未找到本地缓存图片"
                    )
                }
                return@launch
            }

            _comicPicState.update {
                it.copy(
                    data = files.mapIndexed { index, path ->
                        ComicPicImageState(
                            index = index,
                            comicId = comicId,
                            originSrc = path,
                            __scrambleId = Int.MAX_VALUE,
                            __speed = "1",
                            picImageLoader = picImageLoader
                        )
                    },
                    isLoading = false
                )
            }
            onSuccess?.invoke(savedPageIndex)
        }
    }

    private fun listLocalImages(context: Context, path: String): List<String> {
        if (path.isBlank()) return emptyList()
        return if (isDocumentCachePath(path)) {
            listComicImagePaths(context, path)
        } else {
            listComicImageFiles(File(path)).map(File::getAbsolutePath)
        }
    }

    private suspend fun loadLocalChapterList(comicId: Int, currentComic: DownloadComic?) {
        val groupId = currentComic?.groupId?.takeIf { it != 0 } ?: comicId
        val chapters = downloadComicDao.getCompleteByGroupId(groupId)
        _localChapterList.value = chapters.mapIndexed { index, item ->
            ComicChapter(
                id = item.id,
                name = item.chapterName.ifBlank {
                    if (chapters.size > 1) "第 ${index + 1} 章" else item.name
                }
            )
        }
    }

    private fun ensureLocalImageDir(context: Context, comicId: Int, downloadComic: DownloadComic?): File? {
        val zipPath = downloadComic?.zipPath.orEmpty()
        val directDir = zipPath.takeIf { it.isNotBlank() }?.let(::File)
        if (directDir?.isDirectory == true && listComicImageFiles(directDir).isNotEmpty()) {
            return directDir
        }

        if (downloadComic != null) {
            val namedDir = getComicChapterDownloadDir(context, downloadComic)
            if (namedDir.exists() && listComicImageFiles(namedDir).isNotEmpty()) {
                return namedDir
            }
        }

        val dir = File(getDownloadDir(context), "$comicId")
        if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) {
            return dir
        }
        if (zipPath.isBlank()) {
            return dir.takeIf { it.exists() }
        }
        val zipFile = File(zipPath)
        if (!zipFile.exists()) {
            return dir.takeIf { it.exists() }
        }
        dir.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zipIn ->
            while (true) {
                val entry = zipIn.nextEntry ?: break
                if (!entry.isDirectory) {
                    val output = File(dir, File(entry.name).name)
                    FileOutputStream(output).use { out ->
                        zipIn.copyTo(out)
                    }
                }
                zipIn.closeEntry()
            }
        }
        return dir
    }

    fun decodeIndex(index: Int, context: Context) {
        if (size <= 0 || index !in 0 until size) return
        log("decode index $index")
        val count = localSettingManager.localSettingState.value.prefetchCount.coerceIn(0, 1)
        val start = max(0, index - count)
        val end = min(size - 1, index + count)
        trimDecodedWindow(max(0, index - 2), min(size - 1, index + 2))
        decode(index, context) {
            for (i in index + 1..end) {
                log("pre decode index $i")
                decode(i, context)
            }
            for (i in index - 1 downTo start) {
                log("pre decode index $i")
                decode(i, context)
            }
        }
    }

    fun decodeVisibleRange(firstIndex: Int, lastIndex: Int, context: Context) {
        if (size <= 0) return
        val count = localSettingManager.localSettingState.value.prefetchCount.coerceIn(0, 1)
        val start = max(0, min(firstIndex, lastIndex) - count)
        val end = min(size - 1, max(firstIndex, lastIndex) + count)
        trimDecodedWindow(max(0, start - 1), min(size - 1, end + 1))
        for (i in start..end) {
            decode(i, context)
        }
    }

    fun prev(context: Context) {
        if (size <= 0) return
        hideToolBar()
        val index = max(0, currentIndexState.intValue - 1)
        currentIndexState.intValue = index
        decodeIndex(index, context)
    }

    fun next(context: Context) {
        if (size <= 0) return
        hideToolBar()
        val index = min(size - 1, currentIndexState.intValue + 1)
        currentIndexState.intValue = index
        decodeIndex(index, context)
    }

    private fun trimDecodedWindow(first: Int, last: Int) {
        comicPicState.value.data.orEmpty().forEachIndexed { index, image ->
            if (index !in first..last) {
                decodeJobs.remove(index)?.cancel()
                prefetchSet.remove(index)
                // Drop references; never recycle a bitmap still used by a Compose frame.
                image.imageResultState = ImageResultState.Loading
            }
        }
    }

    private fun decode(index: Int, context: Context, onComplete: (() -> Unit)? = null) {
        val image = comicPicState.value.data?.getOrNull(index) ?: return
        if (image.imageResultState is ImageResultState.Success) { onComplete?.invoke(); return }
        if (decodeJobs[index]?.isActive == true || image.imageResultState is ImageResultState.Failure) return
        val job = viewModelScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try {
                currentDecodeSemaphore().withPermit { image.decode(context, downscale = localSettingManager.localSettingState.value.readMemoryOptEnabled) }
                if (image.imageResultState is ImageResultState.Success) {
                    prefetchSet.add(index)
                    onComplete?.invoke()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { image.imageResultState = ImageResultState.Failure("图片加载失败，请重试") }
            finally { if (decodeJobs[index] === kotlinx.coroutines.currentCoroutineContext()[Job]) decodeJobs.remove(index) }
        }
        decodeJobs[index] = job
        job.start()
    }

    fun triggerToolBar() {
        isShowToolBar.value = !isShowToolBar.value
    }

    fun hideToolBar() {
        isShowToolBar.value = false
    }

    fun showToolBar() {
        isShowToolBar.value = true
    }
}
