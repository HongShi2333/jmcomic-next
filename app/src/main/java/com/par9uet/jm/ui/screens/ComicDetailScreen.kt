package com.par9uet.jm.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.store.ReadHistoryManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.ChapterMultiSelectDialog
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
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                        .fillMaxWidth(0.8f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ComicInfoListItem(
                        modifier = Modifier.weight(.5f),
                        icon = Icons.Default.Favorite,
                        label = "喜欢人数",
                        value = "0"
                    )
                    ComicInfoListItem(
                        modifier = Modifier.weight(.5f),
                        icon = Icons.Default.RemoveRedEye,
                        label = "浏览量",
                        value = "0"
                    )
                }
                repeat(3) { rowIndex ->
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val widths = when (rowIndex) {
                            0 -> listOf(40.dp, 60.dp, 50.dp)
                            1 -> listOf(80.dp, 60.dp, 70.dp)
                            else -> listOf(70.dp, 50.dp, 60.dp)
                        }
                        repeat(5) { index ->
                            key("$rowIndex-$index") {
                                Box(
                                    modifier = Modifier
                                        .width(widths[index % widths.size])
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmer()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicMetadataContent(
    comic: Comic,
    onTagSearch: (String) -> Unit,
) {
    Text(
        modifier = Modifier.padding(top = 10.dp),
        text = comic.name,
        fontSize = 18.sp,
        lineHeight = 1.5.em,
        fontWeight = FontWeight.Bold,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        comic.authorList.forEach {
            key(it) {
                Text(
                    modifier = Modifier.clickable(onClick = { onTagSearch(it) }),
                    text = it,
                    color = Color.Gray,
                    fontSize = 18.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ComicInfoListItem(
            modifier = Modifier.weight(.5f),
            icon = Icons.Default.Favorite,
            label = "喜欢人数",
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
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ComicDetailScreen(
    id: Int,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    readHistoryManager: ReadHistoryManager = getKoin().get(),
    downloadManager: DownloadManager = getKoin().get(),
    userManager: UserManager = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val scrollState = rememberScrollState()
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsState()
    val readHistory by readHistoryManager.readHistoryState.collectAsState()
    val isLogin by userManager.isLoginState.collectAsState(false)
    var showDownloadChapterDialog by remember { mutableStateOf(false) }
    var selectedChapterIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    fun requireLogin(action: () -> Unit) {
        if (isLogin) {
            action()
        } else {
            mainNavController.navigate("login")
        }
    }

    LaunchedEffect(Unit) {
        if (comicDetailState.data == null) {
            comicDetailViewModel.getComicDetail(id)
        }
    }

    if (comicDetailState.isLoading && comicDetailState.data == null) {
        ComicDetailSkeleton()
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val comic = comicDetailState.data ?: return@Scaffold
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 80.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(
                        onClick = {
                            requireLogin {
                                if (!comic.isLike) {
                                    comicDetailViewModel.likeComic(comic.id)
                                }
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
                            requireLogin {
                                if (comic.isCollect) {
                                    comicDetailViewModel.unCollect(comic.id)
                                } else {
                                    comicDetailViewModel.collect(comic.id)
                                }
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
                                selectedChapterIds = comic.comicChapterList.map { it.id }.toSet()
                                showDownloadChapterDialog = true
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "缓存",
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                val lastReadChapterId = readHistoryManager.lastReadChapterId(comic, readHistory)
                if (comic.comicChapterList.isEmpty()) {
                    Button(onClick = {
                        mainNavController.navigate("comicRead/${lastReadChapterId ?: comic.id}")
                    }) {
                        Text(if (lastReadChapterId != null) "继续阅读" else "开始阅读")
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
                                val targetChapterId = lastReadChapterId
                                    ?: comic.comicChapterList.firstOrNull()?.id
                                    ?: comic.id
                                mainNavController.navigate("comicRead/$targetChapterId")
                            },
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = 25.dp,
                                bottomEnd = 25.dp
                            )
                        ) {
                            Text(if (lastReadChapterId != null) "继续" else "阅读")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val comic = comicDetailState.data ?: return@Scaffold

        if (showDownloadChapterDialog) {
            ChapterMultiSelectDialog(
                title = "选择缓存章节",
                chapters = comic.comicChapterList,
                selectedChapterIds = selectedChapterIds,
                onSelectedChange = { selectedChapterIds = it },
                onDismiss = { showDownloadChapterDialog = false },
                confirmText = "开始缓存",
                onConfirm = {
                    val selectedChapters = comic.comicChapterList
                        .filter { it.id in selectedChapterIds }
                    downloadManager.downloadChapters(comic, selectedChapters)
                    showDownloadChapterDialog = false
                }
            )
        }

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
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                ) {
                    val isTabletLayout = maxWidth >= 700.dp
                    if (isTabletLayout) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            ComicCoverImage(
                                comic = comic,
                                modifier = Modifier
                                    .widthIn(max = 320.dp)
                                    .weight(0.42f),
                                showIdChip = true
                            )
                            Column(
                                modifier = Modifier.weight(0.58f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ComicMetadataContent(comic) {
                                    mainNavController.navigate("comicSearchResult/$it")
                                }
                            }
                        }
                    } else {
                        Column {
                            ComicCoverImage(
                                comic = comic,
                                showIdChip = true
                            )
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ComicMetadataContent(comic) {
                                    mainNavController.navigate("comicSearchResult/$it")
                                }
                            }
                        }
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
                        contentDescription = "返回详情"
                    )
                }
            }
        }
    }
}
