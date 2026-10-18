package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import com.par9uet.jm.ui.components.WatchLazyVerticalGrid as LazyVerticalGrid
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
    comicId: Int = -1,
    currentChapterId: Int = -1,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    readHistoryManager: ReadHistoryManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsState()
    val comic = comicDetailState.data?.takeIf { comicId <= 0 || it.id == comicId }
    val comicChapterList = comic?.let(::watchDetailChapters).orEmpty()
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
    LaunchedEffect(comicId) { if (comicId > 0 && comicDetailState.data?.id != comicId) comicDetailViewModel.getComicDetail(comicId) }
    val mainNavController = LocalMainNavController.current

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val currentIndex = remember(comicChapterList, currentChapterId) { comicChapterList.indexOfFirst { it.id == currentChapterId } }
    LaunchedEffect(currentIndex) { if (currentIndex >= 0) listState.scrollToItem(currentIndex) }
    CommonScaffold(title = "选择章节") {
        com.par9uet.jm.ui.components.WatchLazyColumn(Modifier.fillMaxSize(), state = listState) {
            if (comic == null) item {
                if (comicDetailState.isLoading) androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp))
                else {
                    Text(comicDetailState.errorMsg ?: "章节暂未加载", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.material3.Button(onClick = { if (comicId > 0) comicDetailViewModel.getComicDetail(comicId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("重新加载") }
                }
            }
            else if (comicChapterList.isEmpty()) item { Text("暂无章节", style = MaterialTheme.typography.bodySmall) }
            items(comicChapterList.size, key = { comicChapterList[it].id }) { index ->
                val chapter = comicChapterList[index]
                val current = chapter.id == currentChapterId
                val read = chapter.id in readChapterIds
                androidx.compose.material3.Surface(onClick = { mainNavController.navigate("comicRead/${chapter.id}") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.large,
                    color = if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(8.dp)) {
                        Text(chapter.name.ifBlank { "第 ${index + 1} 话" }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (current || read) Text(if (current) "当前阅读" else "已读", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
