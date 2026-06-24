package com.par9uet.jm.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.ui.components.ComicContentTag
import com.par9uet.jm.ui.components.ComicCoverImage
import com.par9uet.jm.ui.components.ComicRoleTag
import com.par9uet.jm.ui.components.ComicWorkTag
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import com.par9uet.jm.utils.shimmer
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
private fun ComicInfoListItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AssistChip(
            border = null,
            modifier = Modifier
                .width(50.dp)
                .height(50.dp),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            onClick = {},
            label = {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        Column {
            Text(text = label, fontSize = 14.sp)
            Text(text = value)
        }
    }
}

@Composable
private fun ComicDetailSkeleton() {
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .shimmer()
            )
            Column(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f) // 标题长度通常不到头
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f) // 标题长度通常不到头
                        .height(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ComicInfoListItem(
                        modifier = Modifier.weight(.5f),
                        icon = Icons.Default.Favorite,
                        label = "喜爱人数",
                        value = "0"
                    )
                    ComicInfoListItem(
                        modifier = Modifier.weight(.5f),
                        icon = Icons.Default.RemoveRedEye,
                        label = "浏览量",
                        value = "0"
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val list = listOf(40.dp, 60.dp, 50.dp)
                    for (i in 0 until 6) {
                        key(i) {
                            Box(
                                modifier = Modifier
                                    .width(list[i % list.size])
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmer()
                            )
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val list = listOf(80.dp, 60.dp, 70.dp)
                    for (i in 0 until 4) {
                        key(i) {
                            Box(
                                modifier = Modifier
                                    .width(list[i % list.size])
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmer()
                            )
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val list = listOf(70.dp, 50.dp, 60.dp)
                    for (i in 0 until 5) {
                        key(i) {
                            Box(
                                modifier = Modifier
                                    .width(list[i % list.size])
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmer()
                            )
                        }
                    }
                }
                Box {}
            }
        }
    }
}

// https://cdn-msp3.jmapinodeudzn.net/media/albums/467243_3x4.jpg
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ComicDetailScreen(
    id: Int,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    downloadManager: DownloadManager = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val scrollState = rememberScrollState()
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsState()
    val readingProgress by comicDetailViewModel.readingProgressState.collectAsState()
    var showDownloadDialog by remember { mutableStateOf(false) }
    var selectedDownloadChapterIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        if (comicDetailState.data != null) {
            return@LaunchedEffect
        }
        comicDetailViewModel.getComicDetail(id)
        comicDetailViewModel.loadReadingProgress(id)
    }

    if (comicDetailState.isLoading && comicDetailState.data == null) {
        ComicDetailSkeleton()
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (comicDetailState.data != null) {
                val comic = comicDetailState.data!!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 80.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        10.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(
                            onClick = {
                                if (!comic.isLike) {
                                    comicDetailViewModel.likeComic(comic.id)
                                }
                            }
                        ) {
                            if (comic.isLike) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "已喜欢",
                                    tint = Color.Red
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = "喜欢",
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                if (comic.isCollect) {
                                    comicDetailViewModel.unCollect(comic.id)
                                } else {
                                    comicDetailViewModel.collect(comic.id)
                                }
                            },
                        ) {
                            if (comic.isCollect) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = "收藏",
                                    tint = Color.Yellow
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.BookmarkBorder,
                                    contentDescription = "收藏",
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                mainNavController.navigate("comment/${comic.id}")
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Message,
                                contentDescription = "评论",
                            )
                        }
                        IconButton(
                            onClick = {
                                mainNavController.navigate("comicRelate/${comic.id}")
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "相关本子",
                            )
                        }
                        IconButton(
                            onClick = {
                                if (comic.comicChapterList.isEmpty()) {
                                    downloadManager.downloadComic(comic)
                                } else {
                                    selectedDownloadChapterIds = comic.comicChapterList.map { it.id }.toSet()
                                    showDownloadDialog = true
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "下载",
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // Show "Continue Reading" if progress exists, otherwise show "Start Reading" or chapter buttons
                    val progress = readingProgress
                    if (progress != null && progress.pageIndex > 0) {
                        // Has reading progress: show "Continue Reading"
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "上次读到 ${progress.chapterName} 第${progress.pageIndex + 1}/${progress.totalPages}页",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = {
                                val route = if (progress.isLocal) {
                                    "localComicRead/${progress.chapterId}"
                                } else {
                                    "comicRead/${progress.chapterId}"
                                }
                                mainNavController.navigate(route)
                            }) {
                                Text("继续阅读")
                            }
                        }
                    } else if (comic.comicChapterList.isEmpty()) {
                        Button(onClick = {
                            mainNavController.navigate("comicRead/${comic.id}")
                        }) {
                            Text("开始阅读")
                        }
                    } else {
                        Row {
                            Button(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                onClick = {
                                    mainNavController.navigate("comicChapter/${comic.id}")
                                },
                                shape = RoundedCornerShape(
                                    topStart = 25.dp,
                                    bottomStart = 25.dp,
                                    topEnd = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            ) {
                                Text("章节")
                            }
                            VerticalDivider(modifier = Modifier.height(40.dp))
                            Button(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                onClick = {
                                    val firstChapterId = comic.comicChapterList.firstOrNull()?.id ?: comic.id
                                    mainNavController.navigate("comicRead/$firstChapterId")
                                },
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    bottomStart = 0.dp,
                                    topEnd = 25.dp,
                                    bottomEnd = 25.dp
                                )
                            ) {
                                Text("第1话")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (comicDetailState.data != null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                PullToRefreshBox(
                    isRefreshing = comicDetailState.isLoading,
                    state = rememberPullToRefreshState(),
                    onRefresh = {
                        comicDetailViewModel.getComicDetail(id)
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val comic = comicDetailState.data!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                    ) {
                        ComicCoverImage(
                            comic = comic,
                            showIdChip = true
                        )
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                        // comic name
                        Text(
                            modifier = Modifier.padding(top = 10.dp),
                            text = comic.name,
                            fontSize = 18.sp,
                            lineHeight = 1.5.em,
                            fontWeight = FontWeight.Bold,
                        )
                        // comic author list
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            comic.authorList.forEach {
                                key(it) {
                                    Text(
                                        modifier = Modifier.clickable(onClick = {
                                            mainNavController.navigate("comicSearchResult/$it")
                                        }),
                                        text = it,
                                        color = Color.Gray,
                                        fontSize = 18.sp,
                                        lineHeight = 27.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ComicInfoListItem(
                                modifier = Modifier.weight(.5f),
                                icon = Icons.Default.Favorite,
                                label = "喜爱人数",
                                value = comic.likeCount.toString()
                            )
                            ComicInfoListItem(
                                modifier = Modifier.weight(.5f),
                                icon = Icons.Default.RemoveRedEye,
                                label = "浏览量",
                                value = comic.readCount.toString()
                            )
                        }
                        if (comic.tagList.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                comic.tagList.forEach {
                                    key(it) {
                                        ComicContentTag(it)
                                    }
                                }
                            }
                        }

                        // comic role list
                        if (comic.roleList.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                comic.roleList.forEach {
                                    key(it) {
                                        ComicRoleTag(it)
                                    }
                                }
                            }
                        }
                        if (comic.workList.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                comic.workList.forEach {
                                    key(it) {
                                        ComicWorkTag(it)
                                    }
                                }
                            }

                        }
                        Box {}
                    }
                }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp
                ) {
                    IconButton(onClick = { mainNavController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "退出详情"
                        )
                    }
                }
            }
        }
    }
    val currentComic = comicDetailState.data
    if (showDownloadDialog && currentComic != null) {
        DownloadChapterPickerDialog(
            chapters = currentComic.comicChapterList,
            selectedChapterIds = selectedDownloadChapterIds,
            onSelectedChange = { selectedDownloadChapterIds = it },
            onDismiss = { showDownloadDialog = false },
            onDownload = {
                val selectedChapters = currentComic.comicChapterList.filter { chapter ->
                    chapter.id in selectedDownloadChapterIds
                }
                downloadManager.downloadComicChapters(currentComic, selectedChapters)
                showDownloadDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DownloadChapterPickerDialog(
    chapters: List<ComicChapter>,
    selectedChapterIds: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    val allIds = chapters.map { it.id }.toSet()
    val firstPageIds = chapters.take(20).map { it.id }.toSet()
    val lastPageIds = chapters.takeLast(20).map { it.id }.toSet()

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "选择下载章节 (${selectedChapterIds.size}/${chapters.size})",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                HorizontalDivider()
                FlowRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { onSelectedChange(allIds) }) {
                        Text("全选")
                    }
                    TextButton(onClick = { onSelectedChange(emptySet()) }) {
                        Text("清空")
                    }
                    if (chapters.size > 20) {
                        TextButton(onClick = { onSelectedChange(firstPageIds) }) {
                            Text("前20话")
                        }
                        TextButton(onClick = { onSelectedChange(lastPageIds) }) {
                            Text("后20话")
                        }
                    }
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    itemsIndexed(chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                        val checked = chapter.id in selectedChapterIds
                        Row(
                            modifier = Modifier
                                .clickable {
                                    onSelectedChange(
                                        if (checked) {
                                            selectedChapterIds - chapter.id
                                        } else {
                                            selectedChapterIds + chapter.id
                                        }
                                    )
                                }
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    onSelectedChange(
                                        if (isChecked) {
                                            selectedChapterIds + chapter.id
                                        } else {
                                            selectedChapterIds - chapter.id
                                        }
                                    )
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "第${index + 1}话", fontWeight = FontWeight.Bold)
                                if (chapter.name.isNotBlank()) {
                                    Text(
                                        text = chapter.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Button(
                        enabled = selectedChapterIds.isNotEmpty(),
                        onClick = onDownload
                    ) {
                        Text("下载")
                    }
                }
            }
        }
    }
}
