package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.data.models.Comment
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.Comment
import com.par9uet.jm.ui.components.CommentSkeleton
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
private fun CommentListSkeleton() {
    FlowRow(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (i in 0 until 10) {
            key(i) {
                CommentSkeleton()
            }
        }
    }
}

@Composable
private fun ReplyComment(
    comment: Comment,
    onReply: () -> Unit,
) {
    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        ) {
            append(comment.username)
        }
        append(": ")
        append(AnnotatedString.fromHtml(htmlString = comment.content).trim())
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = annotatedString,
            softWrap = true,
            fontSize = 12.sp
        )
        TextButton(
            modifier = Modifier.height(28.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            onClick = onReply
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Reply,
                contentDescription = "回复 ${comment.username}",
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text("回复", fontSize = 11.sp)
        }
    }
}

@Composable
private fun CommentWithAction(
    comment: Comment,
    isLiked: Boolean,
    onReply: ((Comment) -> Unit)? = null,
    onLike: (() -> Unit)? = null,
) {
    var repliesExpanded by remember { mutableStateOf(false) }
    val replyCount = comment.replyCommentList.size

    Comment(comment) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = { onReply?.invoke(comment) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Reply,
                        contentDescription = "回复",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "回复", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = { onLike?.invoke() }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Default.ThumbUpOffAlt,
                        contentDescription = "点赞",
                        modifier = Modifier.size(14.dp),
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${comment.likeCount + (if (isLiked) 1 else 0)}",
                        fontSize = 12.sp,
                        color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 有回复时显示展开/折叠按钮
            if (replyCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = { repliesExpanded = !repliesExpanded }
                ) {
                    Icon(
                        imageVector = if (repliesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (repliesExpanded) "收起回复" else "展开 $replyCount 条回复",
                        fontSize = 12.sp
                    )
                }
                if (repliesExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            comment.replyCommentList.forEach {
                                key(it.id) {
                                    ReplyComment(
                                        comment = it,
                                        onReply = { onReply?.invoke(it) }
                                    )
                                    if (it != comment.replyCommentList.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComicCommentArea(
    comicId: Int,
    modifier: Modifier = Modifier,
    useScaffold: Boolean = false,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    userManager: UserManager = getKoin().get(),
) {
    val focusManager = LocalFocusManager.current
    val mainNavController = LocalMainNavController.current
    val isLogin by userManager.isLoginState.collectAsState(false)
    val commentInputFocusRequester = remember { FocusRequester() }
    val commentLazyPagingItems = comicDetailViewModel.commentPager.collectAsLazyPagingItems()
    val likedCommentIds by comicDetailViewModel.likedCommentIds.collectAsState()
    var replyComment by remember(comicId) { mutableStateOf<Comment?>(null) }
    // 评论列表拥有自己的滚动容器。评论列表到达边界时，不把剩余手势继续传给详情页，
    // 避免用户在评论区滑动时详情信息同步位移造成嵌套滚动冲突。
    val isolateParentScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = available
        }
    }

    // 评论页独立使用时拉取漫画详情，用于在标题栏显示漫画标题与 JM 编码
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsState()
    LaunchedEffect(comicId) {
        comicDetailViewModel.changeCommentComicId(comicId)
        // 仅当当前详情不是该漫画时才拉取
        if (comicDetailState.data?.id != comicId) {
            comicDetailViewModel.getComicDetail(comicId)
        }
    }

    val inputBar: @Composable () -> Unit = {
        CommentInputBar(
            comicId = comicId,
            isLogin = isLogin,
            replyComment = replyComment,
            onReplyCancel = { replyComment = null },
            commentLazyPagingItems = commentLazyPagingItems,
            commentInputFocusRequester = commentInputFocusRequester,
            comicDetailViewModel = comicDetailViewModel,
            onLogin = { mainNavController.navigate("login") },
            onSuccess = {
                replyComment = null
                focusManager.clearFocus()
            }
        )
    }

    if (useScaffold) {
        LaunchedEffect(isLogin) {
            if (!isLogin) {
                mainNavController.navigate("login")
            }
        }
        // 标题：优先显示漫画标题，否则显示"评论"
        val comicTitle = comicDetailState.data?.let { "${it.name} · JM${it.id}" } ?: "评论"
        CommonScaffold(title = comicTitle, bottomBar = inputBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { commentLazyPagingItems.refresh() },
                    enabled = commentLazyPagingItems.loadState.refresh !is LoadState.Loading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "\u5237\u65b0\u8bc4\u8bba")
                }
            }
            CommentList(
                modifier = modifier,
                commentLazyPagingItems = commentLazyPagingItems,
                isLogin = isLogin,
                likedCommentIds = likedCommentIds,
                onLogin = { mainNavController.navigate("login") },
                onReply = {
                    focusManager.clearFocus()
                    commentInputFocusRequester.requestFocus()
                    replyComment = it
                },
                onLike = { commentId ->
                    if (isLogin) {
                        comicDetailViewModel.likeComment(commentId)
                    } else {
                        mainNavController.navigate("login")
                    }
                }
            )
        }
    } else {
        Column(
            modifier = modifier.nestedScroll(isolateParentScroll),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "\u8bc4\u8bba",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { commentLazyPagingItems.refresh() },
                    enabled = commentLazyPagingItems.loadState.refresh !is LoadState.Loading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "\u5237\u65b0\u8bc4\u8bba")
                }
            }
            HorizontalDivider()
            CommentList(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                commentLazyPagingItems = commentLazyPagingItems,
                isLogin = isLogin,
                likedCommentIds = likedCommentIds,
                onLogin = { mainNavController.navigate("login") },
                onReply = {
                    focusManager.clearFocus()
                    commentInputFocusRequester.requestFocus()
                    replyComment = it
                },
                onLike = { commentId ->
                    if (isLogin) {
                        comicDetailViewModel.likeComment(commentId)
                    } else {
                        mainNavController.navigate("login")
                    }
                }
            )
            inputBar()
        }
    }
}

@Composable
private fun CommentList(
    modifier: Modifier = Modifier,
    commentLazyPagingItems: LazyPagingItems<Comment>,
    isLogin: Boolean,
    likedCommentIds: Set<Int>,
    onLogin: () -> Unit,
    onReply: (Comment) -> Unit,
    onLike: (Int) -> Unit
) {
    if (commentLazyPagingItems.loadState.refresh is LoadState.Loading && commentLazyPagingItems.itemCount == 0) {
        Column(modifier = modifier) {
            CommentListSkeleton()
        }
        return
    }
    PullRefreshAndLoadMoreGrid(
        modifier = modifier,
        lazyPagingItems = commentLazyPagingItems,
        key = { it.id },
        columns = GridCells.Fixed(1),
        enablePullRefresh = false
    ) {
        CommentWithAction(
            comment = it,
            isLiked = it.id in likedCommentIds,
            onReply = { targetComment ->
                if (isLogin) {
                    onReply(targetComment)
                } else {
                    onLogin()
                }
            },
            onLike = { onLike(it.id) }
        )
    }
}

@Composable
private fun CommentInputBar(
    comicId: Int,
    isLogin: Boolean,
    replyComment: Comment?,
    onReplyCancel: () -> Unit,
    commentLazyPagingItems: LazyPagingItems<Comment>,
    commentInputFocusRequester: FocusRequester,
    comicDetailViewModel: ComicDetailViewModel,
    onLogin: () -> Unit,
    onSuccess: () -> Unit,
) {
    if (!isLogin) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 72.dp)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "\u767b\u5f55\u540e\u53d1\u8868\u8bc4\u8bba",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onLogin) {
                    Text("\u767b\u5f55")
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            val textFieldState = rememberTextFieldState()
            val commentComicState by comicDetailViewModel.commentComicState.collectAsState()
            fun comment() {
                val content = textFieldState.text.toString().trim()
                if (content.isBlank()) return
                comicDetailViewModel.comment(content, comicId, replyComment?.id) {
                    textFieldState.edit {
                        replace(0, length, "")
                    }
                    commentLazyPagingItems.refresh()
                    onSuccess()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 80.dp)
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(commentInputFocusRequester),
                    state = textFieldState,
                    placeholder = {
                        Text(
                            text = if (replyComment == null) {
                                "\u53d1\u8868\u8bc4\u8bba"
                            } else {
                                "\u56de\u590d ${replyComment.username}"
                            }
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    onKeyboardAction = { comment() }
                )
                if (replyComment != null) {
                    IconButton(onClick = onReplyCancel) {
                        Icon(Icons.Default.Close, contentDescription = "\u53d6\u6d88\u56de\u590d")
                    }
                }
                IconButton(enabled = !commentComicState.isLoading, onClick = { comment() }) {
                    if (commentComicState.isLoading) {
                        CircularProgressIndicator(
                            color = ButtonDefaults.buttonColors().disabledContainerColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "\u53d1\u9001")
                    }
                }
            }
        }
    }
}

@Composable
fun ComicCommentScreen(comicId: Int) {
    ComicCommentArea(comicId = comicId, useScaffold = true)
}
