package com.par9uet.jm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.par9uet.jm.cache.comicCoverCacheKey
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.network.ComicCoverUrlResolver
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.store.RemoteSettingManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin

/**
 * 用于联网详情和本地缓存详情的统一漫画编号交互：短按复制，长按加载详情。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComicIdChip(
    comicId: Int,
    modifier: Modifier = Modifier,
    toastManager: ToastManager = getKoin().get(),
) {
    val clipboardManager = LocalClipboardManager.current
    val comicRepository: ComicRepository = getKoin().get()
    val scope = rememberCoroutineScope()
    var showDetailDialog by remember { mutableStateOf(false) }
    var detailInfoText by remember { mutableStateOf("") }
    var detailLoading by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.combinedClickable(
            onClick = {
                clipboardManager.setText(AnnotatedString(comicId.toString()))
                toastManager.showAsync("已复制漫画编码：$comicId")
            },
            onLongClick = {
                detailLoading = true
                showDetailDialog = true
                scope.launch {
                    detailInfoText = buildComicDetailText(comicRepository, comicId)
                    detailLoading = false
                }
            },
        ),
    ) {
        Text(
            text = "JM$comicId",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }

    if (showDetailDialog) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = { Text("漫画详情 (JM$comicId)", fontWeight = FontWeight.Bold) },
            text = {
                if (detailLoading) {
                    Text("加载中…")
                } else {
                    Text(
                        text = detailInfoText,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = adaptiveDialogMaxHeight(400.dp))
                            .verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(detailInfoText))
                    toastManager.showAsync("已复制详情信息")
                }) { Text("复制") }
            },
            dismissButton = {
                TextButton(onClick = { showDetailDialog = false }) { Text("关闭") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComicCoverImage(
    comic: Comic,
    modifier: Modifier = Modifier.fillMaxWidth(),
    showIdChip: Boolean = false,
    remoteSettingManager: RemoteSettingManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get(),
    imageLoader: ImageLoader = getKoin().get(),
    toastManager: ToastManager = getKoin().get(),
) {
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val comicRepository: ComicRepository = getKoin().get()
    val scope = rememberCoroutineScope()
    var showDetailDialog by remember { mutableStateOf(false) }
    var detailInfoText by remember { mutableStateOf("") }
    var detailLoading by remember { mutableStateOf(false) }
    val coverUrls = ComicCoverUrlResolver.resolve(
        comicId = comic.id,
        apiImage = comic.coverUrl,
        configuredImageHost = remoteSetting.imgHost,
    )

    Box(modifier = modifier) {
        FallbackAsyncImage(
            coverUrls = coverUrls,
            cacheKey = comicCoverCacheKey(comic.id, localSetting.coverCacheDurationHours),
            cacheEnabled = localSetting.coverCacheDurationHours > 0,
            imageLoader = imageLoader,
            contentDescription = "${comic.name}的封面",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(3f / 4f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        if (showIdChip) {
            // 右上角 JM{id} 标签：用 Surface + combinedClickable 实现
            // 不能用 AssistChip，因为它的 onClick 会消费点击事件导致 combinedClickable 不触发
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 10.dp)
                    .combinedClickable(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(comic.id.toString()))
                            toastManager.showAsync("已复制漫画编码：${comic.id}")
                        },
                        onLongClick = {
                            detailLoading = true
                            showDetailDialog = true
                            scope.launch {
                                val text = buildComicDetailText(comicRepository, comic.id)
                                detailInfoText = text
                                detailLoading = false
                            }
                        }
                    ),
            ) {
                Text(
                    text = "JM${comic.id}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    // 漫画详情对话框（长按 JM{id} 标签触发）
    if (showDetailDialog) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = { Text("漫画详情 (JM${comic.id})", fontWeight = FontWeight.Bold) },
            text = {
                if (detailLoading) {
                    Text("加载中...")
                } else {
                    Text(
                        text = detailInfoText,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = adaptiveDialogMaxHeight(400.dp))
                            .verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(detailInfoText))
                    toastManager.showAsync("已复制详情信息")
                }) { Text("复制") }
            },
            dismissButton = {
                TextButton(onClick = { showDetailDialog = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
fun FallbackAsyncImage(
    coverUrls: List<String>,
    cacheKey: String? = null,
    cacheEnabled: Boolean = true,
    imageLoader: ImageLoader,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var coverIndex by remember(coverUrls) { mutableStateOf(0) }
    val coverUrl = coverUrls.getOrNull(coverIndex)
    if (coverUrl != null) {
        val request = remember(context, coverUrl, cacheKey, cacheEnabled) {
            ImageRequest.Builder(context)
                .data(coverUrl)
                .memoryCachePolicy(if (cacheEnabled) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .diskCachePolicy(if (cacheEnabled) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .apply {
                    cacheKey?.let {
                        memoryCacheKey(it)
                        diskCacheKey(it)
                    }
                }
                .build()
        }
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onError = {
                if (coverIndex < coverUrls.lastIndex) {
                    coverIndex += 1
                }
            },
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier)
    }
}

private suspend fun buildComicDetailText(repository: ComicRepository, comicId: Int): String {
    return withContext(Dispatchers.IO) {
        when (val result = repository.getComicDetail(comicId)) {
            is NetWorkResult.Error -> "获取详情失败：${result.message}"
            is NetWorkResult.Success -> {
                val detail: ComicDetailResponse = result.data
                buildString {
                    appendLine("=== 基础信息 ===")
                    appendLine("ID: ${detail.id}")
                    appendLine("名称: ${detail.name}")
                    appendLine("作者: ${detail.author.joinToString(", ")}")
                    appendLine("简介: ${detail.description.ifBlank { "无" }}")
                    appendLine("阅读次数: ${detail.total_views}")
                    appendLine("喜欢数: ${detail.likes}")
                    appendLine("评论数: ${detail.comment_total}")
                    appendLine("标签: ${detail.tags.joinToString(", ").ifBlank { "无" }}")
                    appendLine("角色: ${detail.actors.joinToString(", ").ifBlank { "无" }}")
                    appendLine("作品: ${detail.works.joinToString(", ").ifBlank { "无" }}")
                    appendLine()
                    appendLine("=== 详情扩展 ===")
                    appendLine("已喜欢: ${if (detail.liked) "是" else "否"}")
                    appendLine("已收藏: ${if (detail.is_favorite) "是" else "否"}")
                    appendLine("系列ID: ${detail.series_id.ifBlank { "无" }}")
                    appendLine("价格: ${detail.price}")
                    appendLine("已购买: ${if (detail.purchased) "是" else "否"}")
                    appendLine()
                    appendLine("=== 章节 ===")
                    detail.series.forEachIndexed { i, chapter ->
                        appendLine("${i + 1}. ${chapter.name}")
                    }
                    appendLine()
                    appendLine("=== 相关漫画 ===")
                    detail.related_list.forEachIndexed { i, related ->
                        appendLine("${i + 1}. JM${related.id} - ${related.name} (${related.author})")
                    }
                }
            }
        }
    }
}
