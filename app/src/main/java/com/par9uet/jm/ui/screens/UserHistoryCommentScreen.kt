package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.ui.components.Comment
import com.par9uet.jm.ui.components.CommentSkeleton
import com.par9uet.jm.ui.components.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.viewModel.UserViewModel
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
private fun UserHistoryCommentSkeleton() {
    Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (i in 0 until 10) {
            key(i) {
                CommentSkeleton()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHistoryCommentScreen(
    userViewModel: UserViewModel = koinActivityViewModel()
) {
    val historyCommentLazyPagingItems = userViewModel.historyCommentPager.collectAsLazyPagingItems()
    val navController = LocalMainNavController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的评论") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = historyCommentLazyPagingItems.loadState.refresh is LoadState.Loading,
            state = rememberPullToRefreshState(),
            onRefresh = historyCommentLazyPagingItems::refresh,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (historyCommentLazyPagingItems.loadState.refresh is LoadState.Loading && historyCommentLazyPagingItems.itemCount == 0) {
                UserHistoryCommentSkeleton()
            } else {
                PullRefreshAndLoadMoreGrid(
                    modifier = Modifier.fillMaxSize(),
                    lazyPagingItems = historyCommentLazyPagingItems,
                    key = { "${it.comicId}:${it.sourceChapterId}:${it.id}:${it.time}:${it.content.hashCode()}" },
                    columns = GridCells.Fixed(1),
                    enablePullRefresh = false,
                ) {
                    Comment(
                        comment = it,
                        showSource = true,
                        onClick = if (it.comicId > 0) {
                            { navController.navigate("comicDetail/${it.comicId}") }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}
