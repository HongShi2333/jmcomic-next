package com.par9uet.jm.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.storage.ComicReadHistory
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ReadHistoryManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.ChapterMultiSelectDialog
import com.par9uet.jm.ui.components.ComicCoverImage
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import com.par9uet.jm.utils.shimmer
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
private fun ComicStatItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
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
                        .clip(MaterialTheme.shapes.extraSmall)
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(34.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .shimmer()
                )
            }
        }
    }
}

@Composable
private fun ComicDetailErrorPage(
    errorMessage: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "加载失败",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = errorMessage ?: "加载失败，请稍后重试",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("重试")
            }
            TextButton(onClick = onBack) {
                Text("返回")
            }
        }
    }
}

@Composable
private fun ComicHeaderInfo(
    comic: Comic,
    onTagSearch: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = comic.name,
            style = MaterialTheme.typography.headlineSmall,
            lineHeight = 1.25.em,
            fontWeight = FontWeight.Bold,
        )
        if (comic.authorList.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                comic.authorList.distinct().forEach { author ->
                    key(author) {
                        AssistChip(
                            onClick = { onTagSearch(author) },
                            label = { Text(author, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComicStatItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Favorite,
                label = "\u559c\u6b22",
                value = comic.likeCount.toString(),
            )
            ComicStatItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.RemoveRedEye,
                label = "\u6d4f\u89c8",
                value = comic.readCount.toString(),
            )
        }
    }
}

@Composable
private fun ComicTagGroup(
    title: String,
    tags: List<String>,
    content: @Composable (String) -> Unit,
) {
    val distinctTags = tags.distinct().filter { it.isNotBlank() }
    if (distinctTags.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            distinctTags.forEach { tag ->
                key(tag) { content(tag) }
            }
        }
    }
}

@Composable
private fun ComicDetailTag(
    label: String,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun ComicTagCollectionCard(
    comic: Comic,
    onTagSearch: (String) -> Unit,
) {
    if (comic.tagList.isEmpty() && comic.roleList.isEmpty() && comic.workList.isEmpty()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "内容信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ComicTagGroup("标签", comic.tagList) { tag ->
                ComicDetailTag(tag) { onTagSearch(tag) }
            }
            ComicTagGroup("角色", comic.roleList) { tag ->
                ComicDetailTag(tag) { onTagSearch(tag) }
            }
            ComicTagGroup("作品", comic.workList) { tag ->
                ComicDetailTag(tag) { onTagSearch(tag) }
            }
        }
    }
}

@Composable
private fun ComicDescriptionCard(comic: Comic) {
    if (comic.description.isBlank()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("简介", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = comic.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComicHeroCard(
    comic: Comic,
    onTagSearch: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ComicCoverImage(
                comic = comic,
                modifier = Modifier.width(142.dp),
                showIdChip = true,
            )
            Column(modifier = Modifier.weight(1f)) {
                ComicHeaderInfo(comic, onTagSearch)
            }
        }
    }
}

@Composable
private fun ComicChapterDirectoryCard(
    comic: Comic,
    readHistoryManager: ReadHistoryManager,
    readHistory: Map<Int, ComicReadHistory>,
    comicReadingMemoryEnabled: Boolean,
    chapterReadingMemoryEnabled: Boolean,
    onOpenChapters: () -> Unit,
    onRead: (Int) -> Unit,
) {
    val lastReadChapterId = if (comicReadingMemoryEnabled) {
        readHistoryManager.lastReadChapterId(comic, readHistory)
    } else {
        null
    }
    val readChapterIds = if (chapterReadingMemoryEnabled) {
        readHistoryManager.readChapterIds(
            readHistoryManager.historyKey(comic, comic.id),
            readHistory,
        )
    } else {
        emptySet()
    }
    val isSingleChapter = comic.comicChapterList.isEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = if (isSingleChapter) {
            { onRead(comic.id) }
        } else {
            onOpenChapters
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("章节目录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (isSingleChapter) "单篇漫画，点击开始阅读" else "${comic.comicChapterList.size} 个章节",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = if (isSingleChapter) {
                        { onRead(comic.id) }
                    } else {
                        onOpenChapters
                    },
                ) {
                    Text(if (isSingleChapter) "开始阅读" else "章节选择")
                }
            }
            if (isSingleChapter) {
                Text("暂无章节目录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                comic.comicChapterList.forEachIndexed { index, chapter ->
                    val isLastRead = chapterReadingMemoryEnabled && chapter.id == lastReadChapterId
                    val isRead = chapter.id in readChapterIds
                    val containerColor = when {
                        isLastRead -> MaterialTheme.colorScheme.primaryContainer
                        isRead -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    }
                    val contentColor = when {
                        isLastRead -> MaterialTheme.colorScheme.onPrimaryContainer
                        isRead -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = containerColor,
                        contentColor = contentColor,
                        onClick = { onRead(chapter.id) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor,
                                modifier = Modifier.width(28.dp),
                            )
                            Text(
                                text = chapter.name.ifBlank { "第 ${index + 1} 章" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (isLastRead) {
                                Text("继续", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicDetailScreen(
    id: Int,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    readHistoryManager: ReadHistoryManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get(),
    downloadManager: DownloadManager = getKoin().get(),
    userManager: UserManager = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val scrollState = rememberScrollState()
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsState()
    val readHistory by readHistoryManager.readHistoryState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val isLogin by userManager.isLoginState.collectAsState(false)
    var showDownloadChapterDialog by remember { mutableStateOf(false) }
    var selectedChapterIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    fun requireLogin(action: () -> Unit) {
        if (isLogin) action() else mainNavController.navigate("login")
    }

    fun searchTag(tag: String) {
        mainNavController.navigate("comicSearchResult/${Uri.encode(tag)}")
    }

    fun openChapters(comic: Comic) {
        val currentChapterId = if (localSetting.comicReadingMemoryEnabled) {
            readHistoryManager.lastReadChapterId(comic, readHistory) ?: -1
        } else {
            -1
        }
        mainNavController.navigate("comicChapter/${comic.id}?currentChapterId=$currentChapterId")
    }

    LaunchedEffect(id) {
        if (comicDetailState.data?.id != id) {
            comicDetailViewModel.getComicDetail(id)
        }
    }

    // Error state: no data and error occurred
    if (comicDetailState.isError && comicDetailState.data == null) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
                    navigationIcon = {
                        IconButton(onClick = { mainNavController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "\u8fd4\u56de"
                            )
                        }
                    },
                    title = {}
                )
            }
        ) { innerPadding ->
            ComicDetailErrorPage(
                errorMessage = comicDetailState.errorMsg,
                onRetry = { comicDetailViewModel.getComicDetail(id) },
                onBack = { mainNavController.popBackStack() },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    // Loading skeleton
    if (comicDetailState.isLoading && comicDetailState.data == null) {
        ComicDetailSkeleton()
        return
    }

    // 详情页滚动时收起顶部栏，向上轻推即可恢复，避免顶部栏长期遮挡内容。
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val comicTitle = comicDetailState.data?.name ?: ""
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = { mainNavController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "\u8fd4\u56de"
                        )
                    }
                },
                title = {
                    Text(
                        text = comicTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            val comic = comicDetailState.data ?: return@Scaffold
            ComicDetailBottomBar(
                comic = comic,
                readHistoryManager = readHistoryManager,
                readHistory = readHistory,
                comicReadingMemoryEnabled = localSetting.comicReadingMemoryEnabled,
                onLike = {
                    requireLogin {
                        if (!comic.isLike) comicDetailViewModel.likeComic(comic.id)
                    }
                },
                onCollect = {
                    requireLogin {
                        if (comic.isCollect) {
                            comicDetailViewModel.unCollect(comic.id)
                        } else if (comicDetailViewModel.shouldShowFolderPicker()) {
                            // 内置 API 模式：弹出收藏夹选择
                            comicDetailViewModel.refreshFolderList()
                            comicDetailViewModel.showFolderPicker()
                        } else {
                            // 网络 API 模式：直接收藏到默认夹
                            comicDetailViewModel.collect(comic.id)
                        }
                    }
                },
                onRelated = { mainNavController.navigate("comicRelate/${comic.id}") },
                onComments = { mainNavController.navigate("comment/${comic.id}") },
                onDownload = {
                    if (comic.comicChapterList.isEmpty()) {
                        downloadManager.downloadComic(comic)
                    } else {
                        selectedChapterIds = comic.comicChapterList.map { it.id }.toSet()
                        showDownloadChapterDialog = true
                    }
                },
                onRead = { targetId -> mainNavController.navigate("comicRead/$targetId") },
                onChapters = { openChapters(comic) }
            )
        }
    ) { innerPadding ->
        val comic = comicDetailState.data ?: return@Scaffold

        if (showDownloadChapterDialog) {
            ChapterMultiSelectDialog(
                title = "\u9009\u62e9\u7f13\u5b58\u7ae0\u8282",
                chapters = comic.comicChapterList,
                selectedChapterIds = selectedChapterIds,
                onSelectedChange = { selectedChapterIds = it },
                onDismiss = { showDownloadChapterDialog = false },
                confirmText = "\u5f00\u59cb\u7f13\u5b58",
                onConfirm = {
                    val selectedChapters = comic.comicChapterList.filter { it.id in selectedChapterIds }
                    downloadManager.downloadChapters(comic, selectedChapters)
                    showDownloadChapterDialog = false
                }
            )
        }

        PullToRefreshBox(
            isRefreshing = comicDetailState.isLoading,
            state = rememberPullToRefreshState(),
            onRefresh = { comicDetailViewModel.getComicDetail(id) },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val isTabletLayout = maxWidth >= 600.dp
                if (isTabletLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.36f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            ComicCoverImage(
                                comic = comic,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 300.dp),
                                showIdChip = true,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.64f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 760.dp)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        ComicHeaderInfo(comic, ::searchTag)
                                    }
                                }
                                ComicTagCollectionCard(comic, ::searchTag)
                                ComicDescriptionCard(comic)
                                ComicChapterDirectoryCard(
                                    comic = comic,
                                    readHistoryManager = readHistoryManager,
                                    readHistory = readHistory,
                                    comicReadingMemoryEnabled = localSetting.comicReadingMemoryEnabled,
                                    chapterReadingMemoryEnabled = localSetting.chapterReadingMemoryEnabled,
                                    onOpenChapters = { openChapters(comic) },
                                    onRead = { targetId -> mainNavController.navigate("comicRead/$targetId") },
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ComicHeroCard(comic, ::searchTag)
                        ComicTagCollectionCard(comic, ::searchTag)
                        ComicDescriptionCard(comic)
                        ComicChapterDirectoryCard(
                            comic = comic,
                            readHistoryManager = readHistoryManager,
                            readHistory = readHistory,
                            comicReadingMemoryEnabled = localSetting.comicReadingMemoryEnabled,
                            chapterReadingMemoryEnabled = localSetting.chapterReadingMemoryEnabled,
                            onOpenChapters = { openChapters(comic) },
                            onRead = { targetId -> mainNavController.navigate("comicRead/$targetId") },
                        )
                    }
                }
            }
        }

        // 收藏夹选择弹窗（仅内置 API 模式）
        val showFolderPicker by comicDetailViewModel.showFolderPicker.collectAsState()
        val folderList by comicDetailViewModel.folderList.collectAsState()
        if (showFolderPicker) {
            FolderPickerSheet(
                comicId = comic.id,
                folderList = folderList,
                onSelect = { folderId -> comicDetailViewModel.collectWithFolder(comic.id, folderId) },
                onDismiss = { comicDetailViewModel.hideFolderPicker() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPickerSheet(
    comicId: Int,
    folderList: Map<String, String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.extraLarge,
            ) { Box(Modifier.width(32.dp).height(4.dp)) }
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(Icons.Rounded.Bookmarks, contentDescription = null, modifier = Modifier.padding(10.dp))
                }
                Column {
                    Text("选择收藏夹", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("收藏后仍可在收藏页移动位置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // "0"（全部）排第一，其余按原序
            val sortedFolders = linkedMapOf<String, String>().apply {
                folderList["0"]?.let { put("0", it) }
                folderList.filterKeys { it != "0" }.forEach { (id, name) -> put(id, name) }
                if (containsKey("0").not() && folderList.isNotEmpty()) {
                    put("0", "\u5168\u90e8")
                }
            }
            items(sortedFolders.size) { index ->
                val entry = sortedFolders.entries.elementAt(index)
                val selected = entry.key == "0"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(entry.key) },
                    shape = MaterialTheme.shapes.large,
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Icon(
                                if (entry.key == "0") Icons.Rounded.Bookmarks else Icons.Rounded.Folder,
                                contentDescription = null,
                                modifier = Modifier.padding(9.dp),
                            )
                        }
                        Text(entry.value, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        if (selected) Icon(Icons.Rounded.Check, contentDescription = "默认收藏夹", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicDetailBottomBar(
    comic: Comic,
    readHistoryManager: ReadHistoryManager,
    readHistory: Map<Int, ComicReadHistory>,
    comicReadingMemoryEnabled: Boolean,
    onLike: () -> Unit,
    onCollect: () -> Unit,
    onRelated: () -> Unit,
    onComments: () -> Unit,
    onDownload: () -> Unit,
    onRead: (Int) -> Unit,
    onChapters: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .navigationBarsPadding()
                .defaultMinSize(minHeight = 64.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            val lastReadChapterId = if (comicReadingMemoryEnabled) {
                readHistoryManager.lastReadChapterId(comic, readHistory)
            } else {
                null
            }
            val compact = maxWidth < 430.dp
            val veryCompact = maxWidth < 360.dp
            val actionButtonSize = when {
                veryCompact -> 34.dp
                compact -> 40.dp
                else -> 48.dp
            }
            val readingButtonPadding = PaddingValues(horizontal = if (veryCompact) 8.dp else 12.dp)
            val readingButtonSpacing = if (compact) 4.dp else 10.dp
            val actionButtons: @Composable () -> Unit = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (veryCompact) 0.dp else 2.dp),
                ) {
                    IconButton(onClick = onLike, modifier = Modifier.size(actionButtonSize)) {
                        if (comic.isLike) {
                            Icon(Icons.Default.Favorite, contentDescription = "\u5df2\u559c\u6b22", tint = MaterialTheme.colorScheme.error)
                        } else {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "\u559c\u6b22")
                        }
                    }
                    IconButton(onClick = onCollect, modifier = Modifier.size(actionButtonSize)) {
                        if (comic.isCollect) {
                            Icon(Icons.Filled.Bookmark, contentDescription = "\u5df2\u6536\u85cf", tint = MaterialTheme.colorScheme.tertiary)
                        } else {
                            Icon(Icons.Filled.BookmarkBorder, contentDescription = "\u6536\u85cf")
                        }
                    }
                    IconButton(onClick = onRelated, modifier = Modifier.size(actionButtonSize)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "\u76f8\u5173")
                    }
                    IconButton(onClick = onComments, modifier = Modifier.size(actionButtonSize)) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "\u8bc4\u8bba")
                    }
                    IconButton(onClick = onDownload, modifier = Modifier.size(actionButtonSize)) {
                        Icon(Icons.Default.Download, contentDescription = "\u7f13\u5b58")
                    }
                }
            }
            val readingButtons: @Composable () -> Unit = {
                if (comic.comicChapterList.isEmpty()) {
                    Button(
                        contentPadding = readingButtonPadding,
                        onClick = { onRead(lastReadChapterId ?: comic.id) },
                        shape = CircleShape,
                    ) {
                        Text(if (lastReadChapterId != null) "\u7ee7\u7eed\u9605\u8bfb" else "\u5f00\u59cb\u9605\u8bfb")
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(readingButtonSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            contentPadding = readingButtonPadding,
                            onClick = onChapters,
                            shape = CircleShape,
                        ) {
                            Text("\u7ae0\u8282")
                        }
                        Button(
                            contentPadding = readingButtonPadding,
                            onClick = {
                                val targetChapterId = lastReadChapterId
                                    ?: comic.comicChapterList.firstOrNull()?.id
                                    ?: comic.id
                                onRead(targetChapterId)
                            },
                            shape = CircleShape,
                        ) {
                            Text(if (lastReadChapterId != null) "\u7ee7\u7eed" else "\u9605\u8bfb")
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actionButtons()
                Spacer(modifier = Modifier.weight(1f))
                readingButtons()
            }
        }
    }
}
