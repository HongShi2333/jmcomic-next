package com.par9uet.jm.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ReadHistoryManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.*
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicDetailScreen(
    id: Int,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    readHistoryManager: ReadHistoryManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get(),
    userManager: UserManager = getKoin().get(),
) {
    val nav = LocalMainNavController.current
    val detail by comicDetailViewModel.comicDetailState.collectAsState()
    val like by comicDetailViewModel.likeComicState.collectAsState()
    val collect by comicDetailViewModel.collectComicState.collectAsState()
    val history by readHistoryManager.readHistoryState.collectAsState()
    val settings by localSettingManager.localSettingState.collectAsState()
    val loggedIn by userManager.isLoginState.collectAsState(false)
    val pickerOpen by comicDetailViewModel.showFolderPicker.collectAsState()
    val folders by comicDetailViewModel.folderList.collectAsState()
    val clipboard = LocalClipboardManager.current
    val toast: ToastManager = getKoin().get()
    var cachePickerOpen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val comic = detail.data?.takeIf { it.id == id }
    val lastRead = comic?.takeIf { settings.comicReadingMemoryEnabled }?.let { readHistoryManager.lastReadChapterId(it, history) }
    val readIds = comic?.takeIf { settings.chapterReadingMemoryEnabled }?.let {
        readHistoryManager.readChapterIds(readHistoryManager.historyKey(it, it.id), history)
    }.orEmpty()
    fun authenticated(action: () -> Unit) { if (loggedIn) action() else nav.navigate("login") }
    fun openChapters(value: Comic) {
        nav.navigate("comicChapter/${value.id}?currentChapterId=${lastRead ?: -1}")
    }
    LaunchedEffect(id) { if (detail.data?.id != id) comicDetailViewModel.getComicDetail(id) }
    val listActive = LocalWatchListActive.current
    CompositionLocalProvider(LocalWatchListActive provides (listActive && !pickerOpen && !cachePickerOpen)) {
        WatchScaffold("漫画详情", onNavigateBack = { nav.popBackStack() }) {
            PullToRefreshBox(
                isRefreshing = detail.isLoading && comic != null,
                onRefresh = { comicDetailViewModel.getComicDetail(id) }, modifier = Modifier.fillMaxSize(),
            ) {
                if (comic == null) WatchLazyColumn(Modifier.fillMaxSize()) {
                    item { if (detail.isLoading) CircularProgressIndicator(Modifier.size(24.dp)) else Text(detail.errorMsg ?: "暂无漫画详情", style = MaterialTheme.typography.bodySmall) }
                    if (!detail.isLoading) item { Button(onClick = { comicDetailViewModel.getComicDetail(id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("重试") } }
                } else key(comic.id) {
                    WatchComicDetailContent(
                        comic, lastRead, readIds,
                        onRead = { nav.navigate("comicRead/$it") }, onChapters = { openChapters(comic) },
                        onSearch = { nav.navigate("comicSearchResult/${Uri.encode(it)}") },
                        onContentTag = { nav.navigate(comicTagSearchRoute(it)) },
                        onCopy = { clipboard.setText(AnnotatedString(it)); toast.showAsync("已复制") },
                        onLike = { authenticated { if (!comic.isLike) comicDetailViewModel.likeComic(comic.id) } },
                        onCollect = { authenticated {
                            if (comic.isCollect) comicDetailViewModel.unCollect(comic.id)
                            else if (comicDetailViewModel.shouldShowFolderPicker()) {
                                comicDetailViewModel.refreshFolderList(); comicDetailViewModel.showFolderPicker()
                            } else comicDetailViewModel.collect(comic.id)
                        } },
                        onComments = { nav.navigate("comment/${comic.id}") },
                        onRelated = { nav.navigate("comicRelate/${comic.id}") },
                        likeBusy = like.isLoading, collectBusy = collect.isLoading,
                        error = listOfNotNull(detail.errorMsg?.takeIf { detail.isError }, like.errorMsg?.takeIf { like.isError }, collect.errorMsg?.takeIf { collect.isError }).joinToString("\n").ifBlank { null },
                        cover = { modifier -> ComicCoverImage(comic, modifier = modifier, showIdChip = false) },
                        onCache = { cachePickerOpen = true },
                    )
                }
            }
        }
    }
    if (cachePickerOpen && comic != null) WatchCacheChaptersDialog(
        comic, onDismiss = { cachePickerOpen = false },
        onQueued = { cachePickerOpen = false; nav.navigate("download") },
    )
    if (pickerOpen && comic != null) SelectDialog(
        "选择收藏夹", null,
        (linkedMapOf("0" to "默认收藏夹") + folders).map { SelectOption(it.value, it.key) },
        onSelect = { comicDetailViewModel.collectWithFolder(comic.id, it) },
        onDismissRequest = { comicDetailViewModel.hideFolderPicker() },
    )
}
