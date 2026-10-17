package com.par9uet.jm.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.Comic
import com.par9uet.jm.ui.components.ComicSkeleton
import com.par9uet.jm.ui.components.ScrollToTopButton
import com.par9uet.jm.ui.components.TabSkeleton
import com.par9uet.jm.ui.components.adaptiveComicGridCells
import com.par9uet.jm.ui.state.rememberTabIndexState
import com.par9uet.jm.ui.viewModel.ComicViewModel
import com.par9uet.jm.ui.viewModel.UserViewModel
import com.par9uet.jm.utils.normalizeBlockedTagList
import coil.ImageLoader
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.math.abs

private const val TEXT_DISCOVER = "\u53d1\u73b0\u6f2b\u753b"
private const val TEXT_FEATURED = "\u7cbe\u9009\u63a8\u8350"
private const val TEXT_SEARCH_HINT = "\u641c\u7d22\u4f5c\u54c1\u3001\u4f5c\u8005\u6216 tag"
private const val TEXT_WEEKLY = "\u6bcf\u5468"
private const val TEXT_DOWNLOAD = "\u4e0b\u8f7d"
private const val TEXT_SIGN = "\u7b7e\u5230"
private const val TEXT_EXTRACT = "\u63d0\u53d6"

private data class HomeScrollAnchor(val comicIds: List<Int>, val index: Int, val offset: Int)

@Composable
private fun HomeSkeleton(
    onSearch: () -> Unit,
    onDownload: () -> Unit,
    onRecommend: () -> Unit,
    onExtract: () -> Unit,
    onSign: () -> Unit
) {
    val fakeTabSize = 6
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeHeader(
            categoryTitle = "",
            onSearch = onSearch,
            onDownload = onDownload,
            onRecommend = onRecommend,
            onExtract = onExtract,
            onSign = onSign
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 0 until fakeTabSize) {
                key(index) {
                    TabSkeleton(index)
                }
            }
        }
        HorizontalDivider()
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
        ) {
            for (i in 0 until 18) {
                key(i) {
                    ComicSkeleton(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    userViewModel: UserViewModel = koinActivityViewModel(),
    userManager: UserManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val homeComicState by comicViewModel.homeComicState.collectAsState()
    val homeFilteredState by comicViewModel.homeFilteredState.collectAsState()
    val isLogin by userManager.isLoginState.collectAsState(false)
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val context = LocalContext.current
    val imageLoader: ImageLoader = getKoin().get()
    val onSearch = { mainNavController.navigate("comicSearch") }
    val onDownload = { mainNavController.navigate("download") }
    val onRecommend = { mainNavController.navigate("comicRecommend") }
    val onExtract = { mainNavController.navigate("extractCode") }
    val onSign = {
        if (isLogin) {
            mainNavController.navigate("sign")
        } else {
            mainNavController.navigate("login")
        }
    }

    LaunchedEffect(localSetting.comicApiSource, localSetting.api, localSetting.preferenceRecommendEnabled) {
        comicViewModel.getHomeComic(forceRefresh = false)
    }

    if (homeComicState.list.isEmpty() && homeComicState.isLoading) {
        HomeSkeleton(
            onSearch = onSearch,
            onDownload = onDownload,
            onRecommend = onRecommend,
            onExtract = onExtract,
            onSign = onSign
        )
        return
    }

    val selectedTabIndexState = rememberTabIndexState()
    val gridState = rememberLazyGridState()
    val onTabClick: (index: Int) -> Unit = {
        selectedTabIndexState.value = it.coerceIn(0, (homeComicState.list.size - 1).coerceAtLeast(0))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val currentPageData = homeComicState.list.getOrNull(selectedTabIndexState.value)
        val allExcludedTags = remember(localSetting.blockedTagList, localSetting.homeExcludedTags) {
            normalizeBlockedTagList(localSetting.blockedTagList + localSetting.homeExcludedTags)
        }
        val filterRequest = remember(currentPageData, allExcludedTags, localSetting.comicApiSource, localSetting.api) {
            currentPageData?.let {
                ComicViewModel.HomeFilterRequest(it, allExcludedTags, "${localSetting.comicApiSource}|${localSetting.api}")
            }
        }
        var filterScrollAnchor by remember(filterRequest) {
            val previous = homeFilteredState.takeIf {
                filterRequest != null && it.request != filterRequest &&
                    it.request?.category == filterRequest.category && it.request.source == filterRequest.source
            }?.result?.comics.orEmpty()
            val index = gridState.firstVisibleItemIndex
            val comicIndex = (index - 1).coerceIn(0, previous.size)
            mutableStateOf(previous.takeIf { it.isNotEmpty() }?.let {
                HomeScrollAnchor(
                    (it.drop(comicIndex) + it.take(comicIndex).asReversed()).map { comic -> comic.id },
                    index, gridState.firstVisibleItemScrollOffset,
                )
            })
        }
        LaunchedEffect(filterRequest) {
            filterRequest?.let { comicViewModel.filterHomeCategory(it) }
        }
        val matchingFilterState = homeFilteredState.takeIf { it.request == filterRequest }
        val checkingTags = allExcludedTags.isNotEmpty() && filterRequest != null &&
            (matchingFilterState == null || matchingFilterState.isLoading)
        val comicList = if (allExcludedTags.isEmpty()) {
            currentPageData?.list.orEmpty()
        } else {
            matchingFilterState?.result?.comics.orEmpty()
        }
        val unresolvedCount = matchingFilterState?.result?.unresolvedCount ?: 0
        val filterError = matchingFilterState?.errorMsg.takeIf { allExcludedTags.isNotEmpty() }
        LaunchedEffect(filterScrollAnchor, matchingFilterState) {
            val anchor = filterScrollAnchor ?: return@LaunchedEffect
            if (matchingFilterState?.result != null && !matchingFilterState.isLoading) {
                val positions = comicList.mapIndexed { index, comic -> comic.id to index + 1 }.toMap()
                val index = if (anchor.index == 0) 0 else {
                    anchor.comicIds.firstNotNullOfOrNull { positions[it] } ?: anchor.index.coerceAtMost(comicList.size)
                }
                gridState.requestScrollToItem(index, anchor.offset)
                filterScrollAnchor = null
            }
        }
        // 标签检查完成后再预热收藏信息。
        LaunchedEffect(homeComicState.isLoading, checkingTags, isLogin, localSetting.comicApiSource) {
            if (!homeComicState.isLoading && !checkingTags && comicList.isNotEmpty() && isLogin) {
                userViewModel.preloadFavoritesAfterHome(context, imageLoader)
            }
        }
        val chipsScrollState = rememberScrollState()
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = homeComicState.isLoading,
            onRefresh = { comicViewModel.getHomeComic() }
        ) {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedTabIndexState.value, homeComicState.list.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val chipsScrollAtDown = chipsScrollState.value
                            var totalX = 0f
                            var totalY = 0f
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) break
                                totalX += change.position.x - change.previousPosition.x
                                totalY += change.position.y - change.previousPosition.y
                            } while (event.changes.any { it.pressed })

                            // 如果分类标签行发生了滚动，说明用户在滑动标签，不触发切换
                            val chipsScrolled = abs(chipsScrollState.value - chipsScrollAtDown) >= 2
                            if (!chipsScrolled &&
                                abs(totalX) > 72.dp.toPx() &&
                                abs(totalX) > abs(totalY) * 1.2f
                            ) {
                                if (totalX < 0) {
                                    onTabClick(selectedTabIndexState.value + 1)
                                } else {
                                    onTabClick(selectedTabIndexState.value - 1)
                                }
                            }
                        }
                },
                state = gridState,
                columns = adaptiveComicGridCells(localSetting.homeGridColumns),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeHeader(
                            categoryTitle = currentPageData?.title.orEmpty(),
                            onSearch = onSearch,
                            onDownload = onDownload,
                            onRecommend = onRecommend,
                            onExtract = onExtract,
                            onSign = onSign
                        )
                        HomeCategoryChips(
                            categories = homeComicState.list.map { it.title },
                            selectedIndex = selectedTabIndexState.value,
                            onSelect = onTabClick,
                            scrollState = chipsScrollState
                        )
                    }
                }
                items(items = comicList, key = { it.id }) {
                    Comic(it)
                }
                if (!homeComicState.isLoading && (checkingTags || comicList.isEmpty() || unresolvedCount > 0 || filterError != null)) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (checkingTags) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            } else Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = when {
                                    checkingTags -> "正在检查漫画标签 ${matchingFilterState?.checkedCount ?: 0}/${matchingFilterState?.totalCount ?: currentPageData?.list?.size ?: 0}…"
                                    filterError != null -> filterError
                                    homeComicState.isError -> homeComicState.errorMsg ?: "首页加载失败"
                                    unresolvedCount > 0 -> "$unresolvedCount 部漫画标签检查失败，暂未显示"
                                    allExcludedTags.isNotEmpty() -> "当前分类的漫画均被标签排除过滤"
                                    else -> "暂无漫画"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!checkingTags && (unresolvedCount > 0 || filterError != null) && filterRequest != null) {
                                TextButton(onClick = { comicViewModel.filterHomeCategory(filterRequest, forceRefresh = true) }) {
                                    Text("重新检查标签")
                                }
                            } else if (allExcludedTags.isNotEmpty() && !checkingTags && !homeComicState.isError) {
                                Text(
                                    text = "可在 设置 → 标签排除 中调整",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            ScrollToTopButton(
                gridState = gridState,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun HomeHeader(
    categoryTitle: String,
    onSearch: () -> Unit,
    onDownload: () -> Unit,
    onRecommend: () -> Unit,
    onExtract: () -> Unit,
    onSign: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = TEXT_DISCOVER,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = categoryTitle.ifBlank { TEXT_FEATURED },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable(onClick = onSearch),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = TEXT_SEARCH_HINT,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                label = TEXT_WEEKLY,
                onClick = onRecommend
            )
            HomeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Password,
                label = TEXT_EXTRACT,
                onClick = onExtract
            )
            HomeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Download,
                label = TEXT_DOWNLOAD,
                onClick = onDownload
            )
            HomeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarMonth,
                label = TEXT_SIGN,
                onClick = onSign
            )
        }
    }
}

/**
 * 首页分类按钮区。
 * 使用 Row + horizontalScroll，每个子组件用 pointerInput 手动检测点击。
 * 不使用 clickable（本地坐标位移为零导致误判 tap）。
 */
@Composable
private fun HomeCategoryChips(
    categories: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    scrollState: ScrollState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEachIndexed { index, title ->
            key(title) {
                CategoryChipItem(
                    title = title,
                    selected = selectedIndex == index,
                    scrollState = scrollState,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChipItem(
    title: String,
    selected: Boolean,
    scrollState: ScrollState,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val scrollAtDown = scrollState.value
                do {
                    val event = awaitPointerEvent()
                } while (event.changes.any { it.pressed })
                val scrollDelta = abs(scrollState.value - scrollAtDown)
                if (scrollDelta < 2) {
                    onClick()
                }
            }
        },
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = labelColor
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun HomeQuickAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
