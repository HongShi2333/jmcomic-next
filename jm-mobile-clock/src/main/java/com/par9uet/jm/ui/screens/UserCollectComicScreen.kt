package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.imePadding

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.par9uet.jm.ui.components.WatchLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import com.par9uet.jm.ui.components.watchVerticalScroll as verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import com.par9uet.jm.ui.components.WatchAlertDialog as AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.par9uet.jm.ui.components.WatchModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import com.par9uet.jm.ui.components.WatchPageScaffold as Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.par9uet.jm.ui.components.WatchTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import com.par9uet.jm.ui.components.WatchActionRow
import com.par9uet.jm.ui.components.WatchActionPair
import com.par9uet.jm.ui.components.WatchPanelDialog
import com.par9uet.jm.ui.components.WatchTextEditorDialog
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.ImageLoader
import com.par9uet.jm.data.models.CollectComicOrderFilter
import com.par9uet.jm.data.models.TagFilterLogic
import com.par9uet.jm.ui.components.Comic
import com.par9uet.jm.ui.components.ComicSkeleton
import com.par9uet.jm.ui.components.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.components.adaptiveComicGridCells
import com.par9uet.jm.ui.components.adaptiveDialogMaxHeight
import com.par9uet.jm.ui.viewModel.UserViewModel
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.UserManager
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

// 收藏列表加载中的骨架屏，2 列布局与正式网格保持一致
@Composable
private fun UserCollectComicSkeleton(
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Top)
    ) {
        for (i in 0 until 8) {
            key(i) {
                ComicSkeleton(
                    // FlowRow does not expose RowScope.weight on all Compose
                    // versions used by the project; use an adaptive fraction
                    // so the two-column skeleton remains tablet friendly.
                    modifier = Modifier.fillMaxWidth(0.48f)
                )
            }
        }
    }
}

// 收藏夹切换 Chip
@Composable
private fun FolderChip(
    folderName: String,
    isSelected: Boolean,
    isAll: Boolean = false,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(folderName) },
        leadingIcon = {
            Icon(
                imageVector = if (isAll) Icons.Rounded.Bookmarks else Icons.Rounded.Folder,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCollectComicScreen(
    userViewModel: UserViewModel = koinActivityViewModel(),
    useScaffold: Boolean = true,
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val navController = LocalMainNavController.current
    val context = LocalContext.current
    val imageLoader: ImageLoader = getKoin().get()
    val userManager: UserManager = getKoin().get()
    val collectComicLazyPagingItems = userViewModel.collectComicPager.collectAsLazyPagingItems()
    val isLogin by userManager.isLoginState.collectAsState(false)
    val order by userViewModel.collectComicOrder.collectAsState()
    val collectComicFilter by userViewModel.collectComicFilter.collectAsState()
    val tagCountMap by userViewModel.collectTagCounts.collectAsState()
    val roleCountMap by userViewModel.collectRoleCounts.collectAsState()
    val authorCountMap by userViewModel.collectAuthorCounts.collectAsState()
    val typeCountMap by userViewModel.collectTypeCounts.collectAsState()
    val selectedFolderId by userViewModel.selectedFolderId.collectAsState()
    val totalItems by userViewModel.collectTotalItems.collectAsState()
    val folderList by userViewModel.folderList.collectAsState()
    val collectEditState by userViewModel.collectEditState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    val filterLoading by userViewModel.collectFilterLoading.collectAsState()
    val filterError by userViewModel.collectFilterError.collectAsState()
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchDraft by rememberSaveable { mutableStateOf("") }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var actionFolderId by remember { mutableStateOf<String?>(null) }
    var actionFolderName by remember { mutableStateOf("") }
    var showFolderManageSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameFolderName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var showDeleteCollectConfirmDialog by remember { mutableStateOf(false) }
    var toolbarVisible by remember { mutableStateOf(true) }
    val toolbarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (kotlin.math.abs(available.y) >= 6f) {
                    val nextVisible = available.y > 0f
                    if (toolbarVisible != nextVisible) {
                        toolbarVisible = nextVisible
                    }
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    val folders = remember(folderList) {
        val result = linkedMapOf<String, String>()
        result["0"] = folderList["0"] ?: "全部"
        folderList.filterKeys { it != "0" }.forEach { (id, name) -> result[id] = name }
        result
    }

    val selectedComics: List<com.par9uet.jm.data.models.Comic> = remember(collectComicLazyPagingItems.itemSnapshotList, collectEditState.selectedComicIds) {
        collectComicLazyPagingItems.itemSnapshotList.filterNotNull().filter { it.id in collectEditState.selectedComicIds }
    }

    // 当前激活的筛选项数量，用于在筛选按钮上展示
    val activeFilterCount = collectComicFilter.selectedTags.size +
        collectComicFilter.selectedRoles.size +
        collectComicFilter.selectedAuthors.size +
        collectComicFilter.selectedTypes.size
    val collectPullRefreshState = rememberPullToRefreshState()
    val isCollectRefreshing = collectComicLazyPagingItems.loadState.refresh is LoadState.Loading

    var toolsOpen by rememberSaveable { mutableStateOf(false) }
    var folderOpen by rememberSaveable { mutableStateOf(false) }
    var orderOpen by rememberSaveable { mutableStateOf(false) }
    val mainContent: @Composable () -> Unit = {
        PullRefreshAndLoadMoreGrid(Modifier.fillMaxSize(), paginationEnabled = true, lazyPagingItems = collectComicLazyPagingItems,
            key = { it.id }, columns = adaptiveComicGridCells(), refreshAction = userViewModel::refreshCollectContent,
            totalItemsHint = totalItems,
            paginationKey = listOf(selectedFolderId, order, collectComicFilter),
            headerContent = {
                Column {
                    Text(folders[selectedFolderId.toString()] ?: "全部收藏", style = MaterialTheme.typography.labelMedium)
                    WatchActionPair("筛选 · $activeFilterCount", if (collectEditState.editing) "批选 · ${collectEditState.selectedComicIds.size}" else "收藏工具",
                        onFirst = { showFilterDialog = true }, onSecond = { toolsOpen = true })
                    if (activeFilterCount > 0) Text("${collectComicFilter.tagLogic.label} · $activeFilterCount 条条件", style = MaterialTheme.typography.labelSmall)
                }
            }) { comic ->
                Comic(comic, editing = collectEditState.editing, selected = comic.id in collectEditState.selectedComicIds,
                    onLongClick = { if (collectEditState.editing) userViewModel.toggleCollectSelected(comic.id) else userViewModel.enterCollectEdit(comic.id) },
                    onToggleSelected = { userViewModel.toggleCollectSelected(comic.id) })
            }
    }
    if (useScaffold) com.par9uet.jm.ui.components.CommonScaffold("我的收藏", content = mainContent) else mainContent()
    if (toolsOpen) WatchPanelDialog("收藏工具", onDismiss = { toolsOpen = false }) {
        item { WatchActionRow("搜索收藏", collectComicFilter.searchText.ifBlank { "名称、作者或标签" }) {
            searchDraft = collectComicFilter.searchText; toolsOpen = false; searchOpen = true
        } }
        item { WatchActionRow("切换收藏夹", folders[selectedFolderId.toString()]) { folderOpen = true; toolsOpen = false } }
        item { WatchActionRow("排序", order.label) { orderOpen = true; toolsOpen = false } }
        item { WatchActionRow("标签筛选", "$activeFilterCount 条条件") { showFilterDialog = true; toolsOpen = false } }
        item { WatchActionRow("新建收藏夹") { newFolderName = ""; showCreateFolderDialog = true; toolsOpen = false } }
        item { WatchActionRow("管理收藏夹") { showFolderManageSheet = true; toolsOpen = false } }
        if (collectEditState.editing) {
            item { WatchActionRow("移动所选漫画", enabled = selectedComics.isNotEmpty()) { showMoveFolderDialog = true; toolsOpen = false } }
            item { WatchActionRow("删除所选收藏", enabled = selectedComics.isNotEmpty()) { showDeleteCollectConfirmDialog = true; toolsOpen = false } }
            item { WatchActionRow("退出批量选择") { userViewModel.clearCollectSelection(); toolsOpen = false } }
        }
    }
    if (searchOpen) WatchTextEditorDialog(
        "搜索收藏", searchDraft, { searchDraft = it }, onDismiss = { searchOpen = false; toolsOpen = true },
        onConfirm = { userViewModel.applyCollectFilter(collectComicFilter.copy(searchText = searchDraft)); searchOpen = false },
        singleLine = true, confirmLabel = "搜索", hint = "留空可清除搜索条件",
    )
    if (folderOpen) com.par9uet.jm.ui.components.SelectDialog("切换收藏夹", selectedFolderId.toString(),
        folders.map { com.par9uet.jm.ui.components.SelectOption(it.value, it.key) },
        onSelect = { userViewModel.changeFolder(it.toIntOrNull() ?: 0); folderOpen = false }, onDismissRequest = { folderOpen = false })
    if (orderOpen) com.par9uet.jm.ui.components.SelectDialog("收藏排序", order.name,
        CollectComicOrderFilter.entries.map { com.par9uet.jm.ui.components.SelectOption(it.label, it.name) },
        onSelect = { userViewModel.changeCollectComicOrder(CollectComicOrderFilter.valueOf(it)); orderOpen = false }, onDismissRequest = { orderOpen = false })

    if (showFilterDialog) {
        DisposableEffect(userViewModel) {
            userViewModel.refreshCollectTagCounts()
            onDispose { userViewModel.stopCollectTagCounts() }
        }
        WatchCollectFilterDialog(
            initial = collectComicFilter, tagCounts = tagCountMap, roleCounts = roleCountMap,
            authorCounts = authorCountMap, typeCounts = typeCountMap,
            loading = filterLoading, error = filterError,
            onReload = { userViewModel.refreshCollectTagCounts(force = true) },
            onDismiss = { showFilterDialog = false },
            onApply = { userViewModel.applyCollectFilter(it); showFilterDialog = false },
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                newFolderName = ""
            },
            title = { Text("新建收藏夹") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    label = { Text("文件夹名称") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        userViewModel.createFolder(newFolderName.trim())
                        newFolderName = ""
                        showCreateFolderDialog = false
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newFolderName = ""
                    showCreateFolderDialog = false
                }) { Text("取消") }
            }
        )
    }

    if (showFolderManageSheet) androidx.compose.ui.window.Dialog(onDismissRequest = { showFolderManageSheet = false },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        com.par9uet.jm.ui.components.WatchScaffold("管理收藏夹", onNavigateBack = { showFolderManageSheet = false }) {
            LazyColumn(Modifier.fillMaxSize()) {
                item { Button(onClick = { newFolderName = ""; showCreateFolderDialog = true; showFolderManageSheet = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("新建收藏夹") } }
                items(folders.entries.toList(), key = { it.key }) { (folderId, folderName) ->
                    Column {
                        Button(onClick = { userViewModel.changeFolder(folderId.toIntOrNull() ?: 0); showFolderManageSheet = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(folderName, maxLines = 2) }
                        if (folderId != "0") {
                            TextButton(onClick = { actionFolderId = folderId; actionFolderName = folderName; renameFolderName = folderName; showRenameDialog = true; showFolderManageSheet = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("重命名") }
                            TextButton(onClick = { actionFolderId = folderId; actionFolderName = folderName; showDeleteConfirmDialog = true; showFolderManageSheet = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("删除收藏夹") }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名收藏夹") },
            text = {
                OutlinedTextField(
                    value = renameFolderName,
                    onValueChange = { renameFolderName = it },
                    singleLine = true,
                    label = { Text("新名称") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val folderId = actionFolderId
                    if (renameFolderName.isNotBlank() && folderId != null) {
                        userViewModel.renameFolder(folderId, renameFolderName.trim())
                        showRenameDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("删除收藏夹") },
            text = { Text("确定删除「${actionFolderName}」吗？\n注意：删除收藏夹不会删除其中的漫画，漫画会移至「全部」。") },
            confirmButton = {
                TextButton(onClick = {
                    val folderId = actionFolderId
                    if (folderId != null) {
                        userViewModel.deleteFolder(folderId)
                        showDeleteConfirmDialog = false
                    }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteCollectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCollectConfirmDialog = false },
            title = { Text("取消收藏") },
            text = { Text("确定取消收藏 ${collectEditState.selectedComicIds.size} 部漫画吗？") },
            confirmButton = {
                TextButton(onClick = {
                    userViewModel.deleteCollectedComics(selectedComics)
                    showDeleteCollectConfirmDialog = false
                }) { Text("取消收藏") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCollectConfirmDialog = false }) { Text("取消") }
            }
        )
    }

    if (showMoveFolderDialog) {
        MoveFolderSheet(
            folders = folders,
            currentFolderId = selectedFolderId,
            onMove = { folderId ->
                userViewModel.moveCollectedToFolder(selectedComics, folderId)
                showMoveFolderDialog = false
            },
            onDismiss = { showMoveFolderDialog = false }
        )
    }
}

// 移动到收藏夹：ModalBottomSheet + LazyColumn + Radio 选择
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MoveFolderSheet(
    folders: Map<String, String>,
    currentFolderId: Int?,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 排除「全部」与当前所在收藏夹
    val availableFolders = remember(folders, currentFolderId) {
        folders.filterKeys { it != "0" && it.toIntOrNull() != currentFolderId }
    }

    com.par9uet.jm.ui.components.SelectDialog("移动到收藏夹", null,
        availableFolders.map { com.par9uet.jm.ui.components.SelectOption(it.value, it.key) },
        onSelect = onMove, onDismissRequest = onDismiss)
}

@Composable
private fun CollectEditBottomAppBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        actions = {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "退出编辑")
            }
            Text(
                text = "已选择 $selectedCount 项",
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onMove) {
                Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = "移动")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "取消收藏")
            }
        }
    )
}
