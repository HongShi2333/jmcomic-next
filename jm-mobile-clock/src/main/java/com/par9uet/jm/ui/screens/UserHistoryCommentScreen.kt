package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.par9uet.jm.ui.components.WatchPageScaffold as Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import com.par9uet.jm.ui.components.WatchTopAppBar as TopAppBar
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

    com.par9uet.jm.ui.components.WatchScaffold("我的评论", onNavigateBack = { navController.popBackStack() }) {
        PullRefreshAndLoadMoreGrid(
            modifier = Modifier.fillMaxSize(), lazyPagingItems = historyCommentLazyPagingItems,
            key = { "${it.comicId}:${it.sourceChapterId}:${it.id}:${it.time}:${it.content.hashCode()}" },
            columns = GridCells.Fixed(1),
            headerContent = { TextButton(onClick = historyCommentLazyPagingItems::refresh, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("刷新评论") } },
        ) {
            Comment(comment = it, showSource = true,
                onClick = if (it.comicId > 0) ({ navController.navigate("comicDetail/${it.comicId}") }) else null)
        }
    }
}
