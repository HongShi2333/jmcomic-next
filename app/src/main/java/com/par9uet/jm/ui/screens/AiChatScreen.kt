package com.par9uet.jm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiChatMessage
import com.par9uet.jm.data.models.AiSearchEngine
import com.par9uet.jm.data.models.AiSearchSettings
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.viewModel.AiChatViewModel
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    aiChatViewModel: AiChatViewModel = koinActivityViewModel(),
    toastManager: ToastManager = getKoin().get()
) {
    val uiState by aiChatViewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val activeConversation = uiState.conversations.firstOrNull {
        it.id == uiState.activeConversationId
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawer(
                conversations = uiState.conversations,
                activeConversationId = uiState.activeConversationId,
                onNewConversation = {
                    aiChatViewModel.createConversation()
                    coroutineScope.launch { drawerState.close() }
                },
                onSelectConversation = {
                    aiChatViewModel.selectConversation(it)
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteConversation = aiChatViewModel::deleteConversation
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AiChatHeader(
                title = activeConversation?.title ?: "AI 对话",
                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                onNewConversation = aiChatViewModel::createConversation
            )
            HorizontalDivider()
            MessageList(
                modifier = Modifier.weight(1f),
                messages = activeConversation?.messages.orEmpty(),
                isSending = uiState.isSending,
                onRetry = aiChatViewModel::retry,
                onEditUserMessage = aiChatViewModel::editUserMessage,
                onSwitchUserBranch = aiChatViewModel::switchUserBranch,
                toastManager = toastManager
            )
            uiState.errorMessage?.let {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            ChatInputBar(
                input = uiState.input,
                webSearchEnabled = uiState.webSearchEnabled,
                searchSettings = uiState.searchSettings,
                isSending = uiState.isSending,
                onInputChange = aiChatViewModel::changeInput,
                onWebSearchChange = aiChatViewModel::changeWebSearchEnabled,
                onSearchSettingsChange = aiChatViewModel::changeSearchSettings,
                onSend = aiChatViewModel::send,
                onStop = aiChatViewModel::stopGenerating
            )
        }
    }
}

@Composable
private fun AiChatHeader(
    title: String,
    onOpenDrawer: () -> Unit,
    onNewConversation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenDrawer) {
            Icon(Icons.Rounded.Menu, contentDescription = "对话列表")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "chat-model",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onNewConversation) {
            Icon(Icons.Rounded.Add, contentDescription = "新建对话")
        }
    }
}

@Composable
private fun ConversationDrawer(
    conversations: List<AiChatConversation>,
    activeConversationId: String,
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "对话管理",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onNewConversation) {
                Icon(Icons.Rounded.Add, contentDescription = "新建对话")
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversations, key = { it.id }) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    selected = conversation.id == activeConversationId,
                    onClick = { onSelectConversation(conversation.id) },
                    onDelete = { onDeleteConversation(conversation.id) }
                )
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: AiChatConversation,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTime(conversation.updatedAt),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "删除对话",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    modifier: Modifier,
    messages: List<AiChatMessage>,
    isSending: Boolean,
    onRetry: (String, AiChatViewModel.RetryMode) -> Unit,
    onEditUserMessage: (String, String) -> Unit,
    onSwitchUserBranch: (String, Int) -> Unit,
    toastManager: ToastManager
) {
    val listState = rememberLazyListState()
    var followOutput by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    followOutput = listState.isNearBottom()
                }
            }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            followOutput = true
            listState.scrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(messages.lastOrNull()?.content, isSending, followOutput) {
        if (messages.isNotEmpty() && followOutput && !listState.isScrollInProgress) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    if (messages.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    modifier = Modifier.size(44.dp),
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "开始一段 AI 对话",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            ChatMessageBubble(
                message = message,
                retryEnabled = !isSending,
                onRetry = onRetry,
                onEditUserMessage = onEditUserMessage,
                onSwitchUserBranch = onSwitchUserBranch,
                toastManager = toastManager
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: AiChatMessage,
    retryEnabled: Boolean,
    onRetry: (String, AiChatViewModel.RetryMode) -> Unit,
    onEditUserMessage: (String, String) -> Unit,
    onSwitchUserBranch: (String, Int) -> Unit,
    toastManager: ToastManager
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    val visibleText = remember(message.content, isUser) {
        if (isUser) {
            message.content
        } else {
            cleanAssistantAnswer(message.content)
        }
    }
    var actionDialog by rememberSaveable(message.id) { mutableStateOf<MessageActionDialog?>(null) }
    var editDialogOpen by rememberSaveable(message.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Column(
                modifier = Modifier.fillMaxWidth(0.86f),
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                UserMessageMeta(
                    message = message,
                    editEnabled = retryEnabled,
                    onEditClick = { editDialogOpen = true },
                    onSwitchBranch = onSwitchUserBranch
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(0.94f)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        AssistantMessageContent(message = message)
                    }
                }
                AssistantMessageMeta(
                    durationMs = message.durationMs,
                    retryEnabled = retryEnabled,
                    onCopyClick = { actionDialog = MessageActionDialog.Copy },
                    onRetryClick = { actionDialog = MessageActionDialog.Retry }
                )
            }
        }
    }

    when (actionDialog) {
        MessageActionDialog.Copy -> {
            MessageCopyDialog(
                text = visibleText,
                onDismiss = { actionDialog = null },
                onCopyAll = {
                    clipboardManager.setText(AnnotatedString(visibleText))
                    toastManager.showAsync("已复制")
                    actionDialog = null
                },
                onSelectCopy = { actionDialog = MessageActionDialog.SelectCopy }
            )
        }

        MessageActionDialog.SelectCopy -> {
            SelectCopyDialog(
                text = visibleText,
                onDismiss = { actionDialog = null },
                onCopyAll = {
                    clipboardManager.setText(AnnotatedString(visibleText))
                    toastManager.showAsync("已复制")
                    actionDialog = null
                }
            )
        }

        MessageActionDialog.Retry -> {
            MessageRetryDialog(
                enabled = retryEnabled,
                onDismiss = { actionDialog = null },
                onRetry = {
                    onRetry(message.id, it)
                    actionDialog = null
                }
            )
        }

        null -> Unit
    }

    if (editDialogOpen) {
        EditUserMessageDialog(
            originalText = message.content,
            enabled = retryEnabled,
            onDismiss = { editDialogOpen = false },
            onConfirm = {
                onEditUserMessage(message.id, it)
                editDialogOpen = false
            }
        )
    }
}

private enum class MessageActionDialog {
    Copy,
    SelectCopy,
    Retry
}

@Composable
private fun UserMessageMeta(
    message: AiChatMessage,
    editEnabled: Boolean,
    onEditClick: () -> Unit,
    onSwitchBranch: (String, Int) -> Unit
) {
    val branchCount = message.branches.size
    val activeBranch = message.activeBranchIndex.coerceIn(0, (branchCount - 1).coerceAtLeast(0))
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (branchCount > 0) {
            TextButton(
                enabled = editEnabled && activeBranch > 0,
                onClick = { onSwitchBranch(message.id, activeBranch - 1) }
            ) {
                Text("<")
            }
            Text(
                text = "${activeBranch + 1}/$branchCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                enabled = editEnabled && activeBranch < branchCount - 1,
                onClick = { onSwitchBranch(message.id, activeBranch + 1) }
            ) {
                Text(">")
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        TextButton(
            enabled = editEnabled,
            onClick = onEditClick
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Rounded.Edit,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("编辑")
        }
    }
}

@Composable
private fun EditUserMessageDialog(
    originalText: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draft by rememberSaveable(originalText) { mutableStateOf(originalText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑消息") },
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp),
                value = draft,
                onValueChange = { draft = it },
                textStyle = MaterialTheme.typography.bodyMedium,
                minLines = 4,
                maxLines = 10
            )
        },
        confirmButton = {
            TextButton(
                enabled = enabled && draft.isNotBlank() && draft != originalText,
                onClick = { onConfirm(draft.trim()) }
            ) {
                Text("重新回答")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AssistantMessageMeta(
    durationMs: Long?,
    retryEnabled: Boolean,
    onCopyClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = durationMs?.let { "响应 ${formatDuration(it)}" } ?: "正在响应",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onCopyClick) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "复制"
            )
        }
        IconButton(
            onClick = onRetryClick,
            enabled = retryEnabled
        ) {
            Icon(
                imageVector = Icons.Rounded.Replay,
                contentDescription = "重试"
            )
        }
    }
}

@Composable
private fun MessageCopyDialog(
    text: String,
    onDismiss: () -> Unit,
    onCopyAll: () -> Unit,
    onSelectCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("复制") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageActionRow(
                    icon = Icons.Rounded.ContentCopy,
                    title = "全部复制",
                    description = "复制当前回复正文",
                    onClick = onCopyAll
                )
                MessageActionRow(
                    icon = Icons.Rounded.SelectAll,
                    title = "选择复制",
                    description = "打开可选择文本，自行选择片段",
                    onClick = onSelectCopy
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun SelectCopyDialog(
    text: String,
    onDismiss: () -> Unit,
    onCopyAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择复制") },
        text = {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    text = text.ifBlank { "暂无可复制内容" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopyAll) {
                Text("全部复制")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun MessageRetryDialog(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onRetry: (AiChatViewModel.RetryMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重试") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageActionRow(
                    icon = Icons.Rounded.Refresh,
                    title = "重新回答",
                    description = "重新生成当前回复",
                    enabled = enabled,
                    onClick = { onRetry(AiChatViewModel.RetryMode.Regenerate) }
                )
                MessageActionRow(
                    icon = Icons.Rounded.Article,
                    title = "更详细",
                    description = "增加细节、步骤和上下文",
                    enabled = enabled,
                    onClick = { onRetry(AiChatViewModel.RetryMode.Detailed) }
                )
                MessageActionRow(
                    icon = Icons.Rounded.ShortText,
                    title = "更精简",
                    description = "压缩为只保留重点的版本",
                    enabled = enabled,
                    onClick = { onRetry(AiChatViewModel.RetryMode.Concise) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun MessageActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AssistantMessageContent(message: AiChatMessage) {
    val answer = remember(message.content) { cleanAssistantAnswer(message.content) }
    if (answer.isBlank()) {
        AssistantLoadingContent()
    } else {
        MarkdownContent(text = answer)
    }
}

@Composable
private fun AssistantLoadingContent() {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "正在生成",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    webSearchEnabled: Boolean,
    searchSettings: AiSearchSettings,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onWebSearchChange: (Boolean) -> Unit,
    onSearchSettingsChange: (AiSearchSettings) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    var searchSettingsOpen by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = webSearchEnabled,
                    onClick = { onWebSearchChange(!webSearchEnabled) },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Rounded.TravelExplore,
                            contentDescription = null
                        )
                    },
                    label = { Text("联网搜索") }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { searchSettingsOpen = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "联网搜索设置"
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = input,
                    onValueChange = onInputChange,
                    minLines = 1,
                    maxLines = 5,
                    placeholder = { Text("输入消息") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (!isSending && input.isNotBlank()) {
                                onSend()
                            }
                        }
                    ),
                    enabled = !isSending
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = if (isSending) onStop else onSend,
                    enabled = isSending || input.isNotBlank()
                ) {
                    Icon(
                        imageVector = if (isSending) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                        contentDescription = if (isSending) "停止生成" else "发送"
                    )
                }
            }
        }
    }
    if (searchSettingsOpen) {
        SearchSettingsDialog(
            settings = searchSettings,
            onDismiss = { searchSettingsOpen = false },
            onSave = {
                onSearchSettingsChange(it)
                searchSettingsOpen = false
            }
        )
    }
}

@Composable
private fun SearchSettingsDialog(
    settings: AiSearchSettings,
    onDismiss: () -> Unit,
    onSave: (AiSearchSettings) -> Unit
) {
    var draft by remember(settings) { mutableStateOf(settings.normalized()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("联网搜索设置") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "搜索引擎",
                    style = MaterialTheme.typography.labelLarge
                )
                SearchEngineChips(
                    value = draft.engine,
                    onChange = { draft = draft.copy(engine = it) }
                )
                Text(
                    text = "搜索条数：${draft.resultCount}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = draft.resultCount.toFloat(),
                    onValueChange = {
                        draft = draft.copy(resultCount = it.roundToInt().coerceIn(1, 10))
                    },
                    valueRange = 1f..10f,
                    steps = 8
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = draft.searxngBaseUrl,
                    onValueChange = { draft = draft.copy(searxngBaseUrl = it) },
                    label = { Text("SearXNG 地址") },
                    placeholder = { Text("https://search.example.com") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = draft.searxngLanguage,
                        onValueChange = { draft = draft.copy(searxngLanguage = it) },
                        label = { Text("语言") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = draft.searxngCategories,
                        onValueChange = { draft = draft.copy(searxngCategories = it) },
                        label = { Text("分类") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft.normalized()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SearchEngineChips(
    value: AiSearchEngine,
    onChange: (AiSearchEngine) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(AiSearchEngine.AUTO, AiSearchEngine.BING, AiSearchEngine.SOGOU).forEach {
                SearchEngineChip(engine = it, selected = value == it, onChange = onChange)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(AiSearchEngine.BAIDU, AiSearchEngine.SEARXNG).forEach {
                SearchEngineChip(engine = it, selected = value == it, onChange = onChange)
            }
        }
    }
}

@Composable
private fun SearchEngineChip(
    engine: AiSearchEngine,
    selected: Boolean,
    onChange: (AiSearchEngine) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { onChange(engine) },
        label = { Text(engine.label) }
    )
}

@Composable
private fun MarkdownContent(text: String) {
    val lines = text.lines()
    val uriHandler = LocalUriHandler.current
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    var index = 0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        while (index < lines.size) {
            val line = lines[index]
            when {
                line.trim().startsWith("```") -> {
                    val codeLines = mutableListOf<String>()
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith("```")) {
                        codeLines.add(lines[index])
                        index++
                    }
                    CodeBlock(codeLines.joinToString("\n"))
                }

                line.startsWith("### ") -> MarkdownLine(line.removePrefix("### "), MarkdownKind.HeadingSmall) { pendingUrl = it }
                line.startsWith("## ") -> MarkdownLine(line.removePrefix("## "), MarkdownKind.HeadingMedium) { pendingUrl = it }
                line.startsWith("# ") -> MarkdownLine(line.removePrefix("# "), MarkdownKind.HeadingLarge) { pendingUrl = it }
                line.trim().startsWith(">") -> MarkdownLine(line.trim().removePrefix(">").trim(), MarkdownKind.Quote) { pendingUrl = it }
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> BulletLine(line.trim().drop(2), onLinkClick = { pendingUrl = it })
                numberedListText(line) != null -> BulletLine(numberedListText(line).orEmpty(), ordered = true, onLinkClick = { pendingUrl = it })
                line.isBlank() -> Spacer(modifier = Modifier.height(2.dp))
                else -> MarkdownLine(line, MarkdownKind.Body) { pendingUrl = it }
            }
            index++
        }
    }
    pendingUrl?.let { url ->
        ExternalLinkDialog(
            url = url,
            onDismiss = { pendingUrl = null },
            onConfirm = {
                pendingUrl = null
                uriHandler.openUri(url)
            }
        )
    }
}

@Composable
private fun MarkdownLine(text: String, kind: MarkdownKind, onLinkClick: (String) -> Unit) {
    val style = when (kind) {
        MarkdownKind.HeadingLarge -> MaterialTheme.typography.titleLarge
        MarkdownKind.HeadingMedium -> MaterialTheme.typography.titleMedium
        MarkdownKind.HeadingSmall -> MaterialTheme.typography.titleSmall
        MarkdownKind.Quote -> MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
        MarkdownKind.Body -> MaterialTheme.typography.bodyMedium
    }
    val color = if (kind == MarkdownKind.Quote) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    LinkText(
        text = inlineMarkdown(text),
        style = style.copy(color = color),
        onLinkClick = onLinkClick
    )
}

@Composable
private fun BulletLine(
    text: String,
    ordered: Boolean = false,
    onLinkClick: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.width(18.dp),
            text = if (ordered) "1." else "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinkText(
            modifier = Modifier.weight(1f),
            text = inlineMarkdown(text),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            onLinkClick = onLinkClick
        )
    }
}

@Composable
private fun LinkText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit
) {
    ClickableText(
        modifier = modifier,
        text = text,
        style = style,
        onClick = { offset ->
            text.getStringAnnotations(tag = LINK_TAG, start = offset, end = offset)
                .firstOrNull()
                ?.let { onLinkClick(it.item) }
        }
    )
}

@Composable
private fun ExternalLinkDialog(
    url: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("即将离开应用") },
        text = {
            Text("外部网站不受应用控制，无法保障网站安全。\n\n$url")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("继续跳转")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(10.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private enum class MarkdownKind {
    HeadingLarge,
    HeadingMedium,
    HeadingSmall,
    Quote,
    Body
}

@Composable
private fun inlineMarkdown(text: String) = buildAnnotatedString {
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val linkColor = MaterialTheme.colorScheme.primary
    var index = 0
    while (index < text.length) {
        val markdownLink = MARKDOWN_LINK_REGEX.find(text, index)
            ?.takeIf { it.range.first == index }
        val rawLink = RAW_URL_REGEX.find(text, index)
            ?.takeIf { it.range.first == index }
        when {
            markdownLink != null -> {
                val label = markdownLink.groupValues[1]
                val url = normalizeLinkUrl(markdownLink.groupValues[2])
                pushStringAnnotation(tag = LINK_TAG, annotation = url)
                pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                append(label)
                pop()
                pop()
                index = markdownLink.range.last + 1
            }

            rawLink != null -> {
                val url = normalizeLinkUrl(rawLink.value.trimEnd('.', ',', ';', ')', ']', '}'))
                pushStringAnnotation(tag = LINK_TAG, annotation = url)
                pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                append(displayLinkText(url))
                pop()
                pop()
                index += rawLink.value.length
            }

            text.startsWith("**", index) -> {
                val end = text.indexOf("**", startIndex = index + 2)
                if (end > index) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                } else {
                    append(text[index])
                    index++
                }
            }

            text[index] == '`' -> {
                val end = text.indexOf('`', startIndex = index + 1)
                if (end > index) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground
                        )
                    )
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                } else {
                    append(text[index])
                    index++
                }
            }

            else -> {
                append(text[index])
                index++
            }
        }
    }
}

private const val LINK_TAG = "URL"
private val MARKDOWN_LINK_REGEX = Regex("""\[([^\]]+)]\(((?:https?://|www\.)[^)\s]+)\)""")
private val RAW_URL_REGEX = Regex("""(?:https?://|www\.)[^\s<>()]+""")
private const val THINK_OPEN = "<think>"
private const val THINK_CLOSE = "</think>"

private fun normalizeLinkUrl(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

private fun displayLinkText(url: String): String {
    return runCatching {
        java.net.URI(url).host
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: "来源链接"
}

private fun cleanAssistantAnswer(content: String): String {
    var result = content
    while (true) {
        val open = result.indexOf(THINK_OPEN)
        if (open < 0) break
        val close = result.indexOf(THINK_CLOSE, startIndex = open + THINK_OPEN.length)
        result = if (close >= 0) {
            result.removeRange(open, close + THINK_CLOSE.length)
        } else {
            result.substring(0, open)
        }
    }
    val strayClose = result.indexOf(THINK_CLOSE)
    if (strayClose >= 0) {
        result = result.substring(strayClose + THINK_CLOSE.length)
    }
    return result
        .replace(THINK_OPEN, "")
        .replace(THINK_CLOSE, "")
        .trim()
}

private fun numberedListText(line: String): String? {
    val match = Regex("^\\s*\\d+[.)]\\s+(.+)$").find(line) ?: return null
    return match.groupValues[1]
}

private fun LazyListState.isNearBottom(): Boolean {
    val info = layoutInfo
    val totalItems = info.totalItemsCount
    if (totalItems == 0) return true
    val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return true
    return lastVisible >= totalItems - 2
}

private fun formatTime(value: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(value))
}

private fun formatDuration(value: Long): String {
    return if (value < 1000) {
        "${value}ms"
    } else {
        val seconds = value / 1000.0
        String.format(Locale.getDefault(), "%.1fs", seconds)
    }
}
