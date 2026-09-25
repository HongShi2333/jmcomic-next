package com.par9uet.jm.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
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
            .padding(14.dp)
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
    val folderList by userViewModel.folderList.collectAsState()
    val collectEditState by userViewModel.collectEditState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    var draftSelectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draftSelectedRoles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draftSelectedAuthors by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draftSelectedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draftTagLogic by remember { mutableStateOf(TagFilterLogic.AND) }
    var showFilterDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(isLogin) {
        if (isLogin) {
            userViewModel.preloadFavoritesAfterHome(context, imageLoader)
        }
    }

    // 主体内容：搜索栏 + 收藏夹 Chip + 排序 + 漫画网格
    val mainContent: @Composable () -> Unit = {
        PullToRefreshBox(
            isRefreshing = isCollectRefreshing,
            state = collectPullRefreshState,
            onRefresh = userViewModel::refreshCollectContent,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = toolbarVisible,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 0.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                            // 顶部搜索栏 + 筛选按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        BasicTextField(
                                            value = collectComicFilter.searchText,
                                            onValueChange = { userViewModel.updateCollectSearchText(it) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            decorationBox = { innerTextField ->
                                                if (collectComicFilter.searchText.isEmpty()) {
                                                    Text(
                                                        "搜索漫画名 / 作者 / 标签",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                innerTextField()
                                            },
                                        )
                                        if (collectComicFilter.searchText.isNotEmpty()) {
                                            IconButton(onClick = { userViewModel.updateCollectSearchText("") }) {
                                                Icon(Icons.Rounded.Close, contentDescription = "清除")
                                            }
                                        }
                                    }
                                }
                                FilledTonalIconButton(
                                    onClick = {
                                        draftSelectedTags = collectComicFilter.selectedTags
                                        draftSelectedRoles = collectComicFilter.selectedRoles
                                        draftSelectedAuthors = collectComicFilter.selectedAuthors
                                        draftSelectedTypes = collectComicFilter.selectedTypes
                                        draftTagLogic = collectComicFilter.tagLogic
                                        if (tagCountMap.isEmpty() && roleCountMap.isEmpty() && authorCountMap.isEmpty() && typeCountMap.isEmpty()) {
                                            userViewModel.refreshCollectTagCounts()
                                        }
                                        showFilterDialog = true
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (activeFilterCount > 0) Badge { Text(activeFilterCount.toString()) }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.FilterList,
                                            contentDescription = "筛选",
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            }
                                // 收藏夹切换栏
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    folders.forEach { (folderId, folderName) ->
                                        key(folderId) {
                                            FolderChip(
                                                folderName = folderName,
                                                isSelected = selectedFolderId == folderId.toIntOrNull(),
                                                isAll = folderId == "0",
                                                onClick = {
                                                    userViewModel.changeFolder(folderId.toIntOrNull() ?: 0)
                                                }
                                            )
                                        }
                                    }
                                    FilledTonalIconButton(onClick = {
                                        newFolderName = ""
                                        showCreateFolderDialog = true
                                    }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Rounded.Add, contentDescription = "新建收藏夹", modifier = Modifier.size(20.dp))
                                    }
                                    FilledTonalIconButton(
                                        onClick = { showFolderManageSheet = true },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(Icons.Rounded.Folder, contentDescription = "管理收藏夹", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                    // 排序：Material 3 单选 SegmentedButton
                    AnimatedVisibility(
                        visible = toolbarVisible,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "排序",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                                    CollectComicOrderFilter.entries.forEachIndexed { index, item ->
                                        SegmentedButton(
                                            selected = item == order,
                                            onClick = { userViewModel.changeCollectComicOrder(item) },
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index,
                                                CollectComicOrderFilter.entries.size
                                            )
                                        ) {
                                            Text(item.label)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }

                if (isCollectRefreshing && collectComicLazyPagingItems.itemCount == 0) {
                    UserCollectComicSkeleton(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    PullRefreshAndLoadMoreGrid(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(toolbarScrollConnection),
                        lazyPagingItems = collectComicLazyPagingItems,
                        key = { it.id },
                        columns = adaptiveComicGridCells(localSetting.collectGridColumns),
                        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Top),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(14.dp),
                        enablePullRefresh = false,
                    ) { comic ->
                        Comic(
                            comic = comic,
                            editing = collectEditState.editing,
                            selected = comic.id in collectEditState.selectedComicIds,
                            onLongClick = {
                                if (collectEditState.editing) {
                                    userViewModel.toggleCollectSelected(comic.id)
                                } else {
                                    userViewModel.enterCollectEdit(comic.id)
                                }
                            },
                            onToggleSelected = {
                                userViewModel.toggleCollectSelected(comic.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // 编辑模式下的底部工具栏
    val editBar: @Composable () -> Unit = {
        if (collectEditState.editing) {
            CollectEditBottomAppBar(
                selectedCount = collectEditState.selectedComicIds.size,
                onClose = userViewModel::clearCollectSelection,
                onCache = { userViewModel.cacheCollectedComics(selectedComics) },
                onDelete = { showDeleteCollectConfirmDialog = true },
                onMove = { showMoveFolderDialog = true }
            )
        }
    }

    if (useScaffold) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "我的收藏",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            bottomBar = { editBar() }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                mainContent()
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                mainContent()
            }
            editBar()
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            tagCountMap = tagCountMap,
            roleCountMap = roleCountMap,
            authorCountMap = authorCountMap,
            typeCountMap = typeCountMap,
            draftSelectedTags = draftSelectedTags,
            draftSelectedRoles = draftSelectedRoles,
            draftSelectedAuthors = draftSelectedAuthors,
            draftSelectedTypes = draftSelectedTypes,
            draftTagLogic = draftTagLogic,
            onTagToggle = { tag ->
                draftSelectedTags = if (tag in draftSelectedTags) {
                    draftSelectedTags - tag
                } else {
                    draftSelectedTags + tag
                }
            },
            onRoleToggle = { role ->
                draftSelectedRoles = if (role in draftSelectedRoles) {
                    draftSelectedRoles - role
                } else {
                    draftSelectedRoles + role
                }
            },
            onAuthorToggle = { author ->
                draftSelectedAuthors = if (author in draftSelectedAuthors) {
                    draftSelectedAuthors - author
                } else {
                    draftSelectedAuthors + author
                }
            },
            onTypeToggle = { type ->
                draftSelectedTypes = if (type in draftSelectedTypes) {
                    draftSelectedTypes - type
                } else {
                    draftSelectedTypes + type
                }
            },
            onTagLogicChange = { draftTagLogic = it },
            onConfirm = {
                userViewModel.updateCollectSelectedTags(draftSelectedTags)
                userViewModel.updateCollectSelectedRoles(draftSelectedRoles)
                userViewModel.updateCollectSelectedAuthors(draftSelectedAuthors)
                userViewModel.updateCollectSelectedTypes(draftSelectedTypes)
                userViewModel.updateCollectTagLogic(draftTagLogic)
                showFilterDialog = false
            },
            onClear = {
                draftSelectedTags = emptySet()
                draftSelectedRoles = emptySet()
                draftSelectedAuthors = emptySet()
                draftSelectedTypes = emptySet()
                draftTagLogic = TagFilterLogic.AND
                userViewModel.updateCollectSelectedTags(emptySet())
                userViewModel.updateCollectSelectedRoles(emptySet())
                userViewModel.updateCollectSelectedAuthors(emptySet())
                userViewModel.updateCollectSelectedTypes(emptySet())
                userViewModel.updateCollectTagLogic(TagFilterLogic.AND)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
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

    if (showFolderManageSheet) {
        val manageSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFolderManageSheet = false },
            sheetState = manageSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "管理收藏夹",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击文件夹名称可切换当前收藏夹，右侧按钮可重命名或删除。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(folders.entries.toList(), key = { it.key }) { (folderId, folderName) ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (selectedFolderId == folderId.toIntOrNull())
                                MaterialTheme.colorScheme.secondaryContainer
                            else androidx.compose.ui.graphics.Color.Transparent,
                            onClick = {
                                userViewModel.changeFolder(folderId.toIntOrNull() ?: 0)
                                showFolderManageSheet = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (folderId == "0") Icons.Rounded.Bookmarks else Icons.Rounded.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    folderName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (folderId != "0") {
                                    IconButton(onClick = {
                                        actionFolderId = folderId
                                        actionFolderName = folderName
                                        renameFolderName = folderName
                                        showRenameDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "重命名")
                                    }
                                    IconButton(onClick = {
                                        actionFolderId = folderId
                                        actionFolderName = folderName
                                        showDeleteConfirmDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "删除")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        newFolderName = ""
                        showCreateFolderDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("新建收藏夹")
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

// 筛选弹窗：ModalBottomSheet 支持上划全屏 + 统一逻辑门 + Tab（标签/角色/作者/类型）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    tagCountMap: Map<String, Int>,
    roleCountMap: Map<String, Int>,
    authorCountMap: Map<String, Int>,
    typeCountMap: Map<String, Int>,
    draftSelectedTags: Set<String>,
    draftSelectedRoles: Set<String>,
    draftSelectedAuthors: Set<String>,
    draftSelectedTypes: Set<String>,
    draftTagLogic: TagFilterLogic,
    onTagToggle: (String) -> Unit,
    onRoleToggle: (String) -> Unit,
    onAuthorToggle: (String) -> Unit,
    onTypeToggle: (String) -> Unit,
    onTagLogicChange: (TagFilterLogic) -> Unit,
    onConfirm: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    // 弹窗内的搜索文本，用于过滤当前页的标签或作者
    var filterQuery by remember { mutableStateOf("") }
    // 打开即全屏
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 根据搜索文本过滤当前页内容
    val query = filterQuery.trim()
    val filteredTagCountMap = remember(tagCountMap, query) {
        if (query.isBlank()) tagCountMap
        else tagCountMap.filterKeys { it.contains(query, ignoreCase = true) }
    }
    val filteredRoleCountMap = remember(roleCountMap, query) {
        if (query.isBlank()) roleCountMap
        else roleCountMap.filterKeys { it.contains(query, ignoreCase = true) }
    }
    val filteredAuthorCountMap = remember(authorCountMap, query) {
        if (query.isBlank()) authorCountMap
        else authorCountMap.filterKeys { it.contains(query, ignoreCase = true) }
    }
    val filteredTypeCountMap = remember(typeCountMap, query) {
        if (query.isBlank()) typeCountMap
        else typeCountMap.filterKeys { it.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // 标题
            Text(
                text = "筛选收藏",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // 逻辑门选择：对标签、角色、作者、类型的所有选择统一生效
            Text(
                text = "筛选逻辑",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TagFilterLogic.entries.forEach { logic ->
                    FilterChip(
                        selected = draftTagLogic == logic,
                        onClick = { onTagLogicChange(logic) },
                        label = {
                            Text(
                                text = logic.label,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 搜索框：搜索 tag 或作者
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    BasicTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (filterQuery.isEmpty()) {
                                Text(
                                    "搜索${listOf("标签", "角色", "作者", "类型").getOrElse(selectedTabIndex) { "筛选项" }}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        },
                    )
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "清除")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(8.dp))
            // Tab 行：增加底部留白，避免标题与第一行标签贴得太近
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        filterQuery = ""
                    },
                    text = { Text("标签 (${tagCountMap.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        filterQuery = ""
                    },
                    text = { Text("角色 (${roleCountMap.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                        filterQuery = ""
                    },
                    text = { Text("作者 (${authorCountMap.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = {
                        selectedTabIndex = 3
                        filterQuery = ""
                    },
                    text = { Text("类型 (${typeCountMap.size})") }
                )
            }
            // 内容区：可滚动，填满剩余空间
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        if (filteredTagCountMap.isEmpty()) {
                            Text(
                                if (tagCountMap.isEmpty()) "当前已加载收藏中没有可筛选的标签"
                                else "没有匹配「$query」的标签",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredTagCountMap.forEach { (tag, count) ->
                                    FilterChip(
                                        selected = tag in draftSelectedTags,
                                        onClick = { onTagToggle(tag) },
                                        label = { Text("$tag  $count") }
                                    )
                                }
                            }
                        }
                    }
                    1, 2, 3 -> {
                        val counts = when (selectedTabIndex) {
                            1 -> filteredRoleCountMap
                            2 -> filteredAuthorCountMap
                            else -> filteredTypeCountMap
                        }
                        val selected = when (selectedTabIndex) {
                            1 -> draftSelectedRoles
                            2 -> draftSelectedAuthors
                            else -> draftSelectedTypes
                        }
                        val onToggle = when (selectedTabIndex) {
                            1 -> onRoleToggle
                            2 -> onAuthorToggle
                            else -> onTypeToggle
                        }
                        if (counts.isEmpty()) {
                            Text(
                                if (counts.isEmpty() && query.isBlank()) "当前已加载收藏中没有可筛选的项目"
                                else "没有匹配「$query」的项目",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                counts.forEach { (value, count) ->
                                    FilterChip(
                                        selected = value in selected,
                                        onClick = { onToggle(value) },
                                        label = { Text("$value  $count") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // 底部操作栏（固定不随内容滚动消失）
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f)
                    ) { Text("清空") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) { Text("确定") }
                }
            }
        }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "移动到收藏夹",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (availableFolders.isEmpty()) {
                Text(
                    text = "暂无其他收藏夹，请先创建",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = adaptiveDialogMaxHeight(360.dp))
                ) {
                    items(availableFolders.entries.toList(), key = { it.key }) { (folderId, folderName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(onClick = { selectedFolderId = folderId })
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFolderId == folderId,
                                onClick = { selectedFolderId = folderId }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("取消") }
                Button(
                    onClick = { selectedFolderId?.let(onMove) },
                    enabled = selectedFolderId != null,
                    modifier = Modifier.weight(1f)
                ) { Text("移动") }
            }
        }
    }
}

// 编辑模式底部工具栏：Material 3 BottomAppBar
@Composable
private fun CollectEditBottomAppBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onCache: () -> Unit,
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
            IconButton(onClick = onCache) {
                Icon(Icons.Rounded.Download, contentDescription = "缓存")
            }
            IconButton(onClick = onMove) {
                Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = "移动")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "取消收藏")
            }
        }
    )
}
