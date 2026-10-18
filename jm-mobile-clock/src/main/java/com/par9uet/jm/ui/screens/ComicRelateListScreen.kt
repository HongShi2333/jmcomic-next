package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import com.par9uet.jm.ui.components.WatchLazyVerticalGrid as LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.Comic
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.adaptiveComicGridCells
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import com.par9uet.jm.utils.filterBlockedTags
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun ComicRelateListScreen(
    comicId: Int = -1,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(comicId) { if (comicId > 0 && comicDetailState.data?.id != comicId) comicDetailViewModel.getComicDetail(comicId) }
    val comic = comicDetailState.data?.takeIf { comicId <= 0 || it.id == comicId }
    val relate = remember(comic, localSetting.blockedTagList) { comic?.relateComicList?.filterBlockedTags(localSetting.blockedTagList).orEmpty() }
    val pages = com.par9uet.jm.ui.components.rememberWatchListPages(relate.size, key = comicId)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val window = pages.window
    androidx.compose.runtime.LaunchedEffect(window.page) { listState.scrollToItem(0) }
    CommonScaffold(title = "相关漫画") {
        com.par9uet.jm.ui.components.WatchLazyColumn(androidx.compose.ui.Modifier.fillMaxSize(), state = listState) {
            if (pages.enabled) item { com.par9uet.jm.ui.components.WatchPagination(window, onPrevious = pages.previous, onNext = pages.next) }
            if (comicDetailState.isLoading) item { androidx.compose.material3.CircularProgressIndicator(androidx.compose.ui.Modifier.size(24.dp)) }
            else if (comicDetailState.isError) item {
                androidx.compose.material3.Text(comicDetailState.errorMsg ?: "加载失败")
                androidx.compose.material3.Button(onClick = { if (comicId > 0) comicDetailViewModel.getComicDetail(comicId) }, modifier = androidx.compose.ui.Modifier.fillMaxWidth().heightIn(min = 48.dp)) { androidx.compose.material3.Text("重试") }
            }
            if (!comicDetailState.isLoading && !comicDetailState.isError && relate.isEmpty()) item { androidx.compose.material3.Text("暂无相关漫画") }
            items(window.end - window.start, key = { relate[window.start + it].id }) { index -> Comic(relate[window.start + index]) }
            if (pages.enabled) item { com.par9uet.jm.ui.components.WatchPagination(window, onPrevious = pages.previous, onNext = pages.next) }
        }
    }
}
