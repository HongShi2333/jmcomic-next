package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ReadHistoryManager
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun ComicChapterScreen(
    currentChapterId: Int = -1,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    readHistoryManager: ReadHistoryManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsState()
    val comic = comicDetailState.data
    val comicChapterList = comic?.comicChapterList ?: listOf()
    val readHistory by readHistoryManager.readHistoryState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val readChapterIds = remember(comic, readHistory, localSetting.chapterReadingMemoryEnabled) {
        if (localSetting.chapterReadingMemoryEnabled) {
            comic?.let {
                readHistoryManager.readChapterIds(
                    readHistoryManager.historyKey(it, it.id),
                    readHistory
                )
            } ?: emptySet()
        } else {
            emptySet()
        }
    }
    val mainNavController = LocalMainNavController.current

    // 自动滚动到当前阅读章节
    val gridState = rememberLazyGridState()
    val currentIndex = remember(comicChapterList, currentChapterId) {
        if (currentChapterId > 0) {
            comicChapterList.indexOfFirst { it.id == currentChapterId }
        } else -1
    }
    LaunchedEffect(currentIndex, comicChapterList.size) {
        if (currentIndex >= 0) {
            val spanCount = 4
            val targetRow = currentIndex / spanCount
            // 让当前章节显示在视口中部偏上的位置
            val scrollTarget = ((targetRow - 2).coerceAtLeast(0)) * spanCount
            gridState.scrollToItem(scrollTarget)
        }
    }

    CommonScaffold(title = "选择章节") {
        LazyVerticalGrid(
            state = gridState,
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            columns = GridCells.Fixed(4)
        ) {
            itemsIndexed(comicChapterList, key = { _, item -> item.id }) { index, item ->
                val read = item.id in readChapterIds
                val isCurrent = localSetting.chapterReadingMemoryEnabled &&
                    currentChapterId > 0 && item.id == currentChapterId
                AssistChip(
                    modifier = Modifier.fillMaxSize(),
                    colors = when {
                        isCurrent -> AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        read -> AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        else -> AssistChipDefaults.assistChipColors()
                    },
                    onClick = {
                        mainNavController.navigate("comicRead/${item.id}")
                    },
                    label = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "第${index + 1}话",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    })
            }
        }
    }
}
