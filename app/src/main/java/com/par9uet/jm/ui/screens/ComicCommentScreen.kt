package com.par9uet.jm.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
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
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.data.models.Comment
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.Comment
import com.par9uet.jm.ui.components.CommentSkeleton
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.getKoin

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
private fun ReplayComment(comment: Comment) {
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
        append(
            AnnotatedString.fromHtml(
                htmlString = comment.content,
            ).trim()
        )
    }
    Text(
        text = annotatedString,
        softWrap = true,
        fontSize = 12.sp
    )
}

@Composable
private fun CommentWithAction(comment: Comment, onReply: (() -> Unit)? = null) {
    Comment(comment) {
        Column {
            Row {
                TextButton(
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        onReply?.invoke()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Reply,
                        contentDescription = "",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "回复", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {

                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUpOffAlt,
                        contentDescription = "",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${comment.likeCount}", fontSize = 12.sp)
                }
            }
            if (comment.replyCommentList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        comment.replyCommentList.forEach {
                            key(it.id) {
                                ReplayComment(comment)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComicCommentScreen(
    comicId: Int,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    userManager: UserManager = getKoin().get(),
) {
    val focusManager = LocalFocusManager.current
    val mainNavController = LocalMainNavController.current
    val isLogin by userManager.isLoginState.collectAsState(false)
    val commentInputFocusRequester = remember { FocusRequester() }
    val commentLazyPagingItems = comicDetailViewModel.commentPager.collectAsLazyPagingItems()
    var replyComment by remember { mutableStateOf<Comment?>(null) }
    LaunchedEffect(Unit) {
        comicDetailViewModel.changeCommentComicId(comicId)
    }
    LaunchedEffect(isLogin) {
        if (!isLogin) {
            mainNavController.navigate("login")
        }
    }
    CommonScaffold(
        title = "评论",
        bottomBar = {
            if (!isLogin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 72.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(10.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "登录后发表评论",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { mainNavController.navigate("login") }) {
                        Text("登录")
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 80.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(10.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val textFieldState = rememberTextFieldState()
                    val comment = {
                        comicDetailViewModel.comment(
                            textFieldState.text.toString(),
                            comicId,
                            replyComment?.id
                        ) {
                            textFieldState.edit {
                                replace(0, length, "")
                            }
                            focusManager.clearFocus()
                            commentLazyPagingItems.refresh()
                        }
                    }
                    val commentComicState by comicDetailViewModel.commentComicState.collectAsState()
                    TextField(
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(commentInputFocusRequester),
                        state = textFieldState,
                        placeholder = {
                            Text(text = if (replyComment == null) "报告机长，这是我的起飞感想！" else "回复机长 ${replyComment!!.username}")
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            cursorColor = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        onKeyboardAction = {
                            comment()
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    if (replyComment != null) {
                        IconButton(
                            onClick = {
                                replyComment = null
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "")
                        }
                    }
                    IconButton(
                        enabled = !commentComicState.isLoading,
                        onClick = {
                            comment()
                        }
                    ) {
                        if (commentComicState.isLoading) {
                            CircularProgressIndicator(
                                color = ButtonDefaults.buttonColors().disabledContainerColor,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "")
                        }
                    }
                }
            }
        }
    ) {
        if (commentLazyPagingItems.loadState.refresh is LoadState.Loading && commentLazyPagingItems.itemCount == 0) {
            CommentListSkeleton()
            return@CommonScaffold
        }
        PullRefreshAndLoadMoreGrid(
            lazyPagingItems = commentLazyPagingItems,
            key = { it.id },
            columns = GridCells.Fixed(1)
        ) {
            CommentWithAction(it) {
                if (isLogin) {
                    // 强制清楚焦点
                    focusManager.clearFocus()
                    commentInputFocusRequester.requestFocus()
                    replyComment = it
                } else {
                    mainNavController.navigate("login")
                }
            }
        }
    }
}
