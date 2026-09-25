package com.par9uet.jm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiChatModel
import com.par9uet.jm.data.models.AiChatMessage
import com.par9uet.jm.data.models.AiPersona
import com.par9uet.jm.data.models.AiSearchEngineProvider
import com.par9uet.jm.data.models.AiSearchSettings
import com.par9uet.jm.repository.WebSearchResult
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.viewModel.AiChatViewModel
import com.par9uet.jm.ui.components.adaptiveDialogMaxHeight
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
    toastManager: ToastManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val uiState by aiChatViewModel.uiState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val activeConversation = uiState.conversations.firstOrNull {
        it.id == uiState.activeConversationId
    }
    var personaSwitchOpen by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 700.dp
        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                ConversationPanel(
                    modifier = Modifier.width(300.dp),
                    conversations = uiState.conversations,
                    activeConversationId = uiState.activeConversationId,
                    onNewConversation = aiChatViewModel::createConversation,
                    onSelectConversation = aiChatViewModel::selectConversation,
                    onDeleteConversation = aiChatViewModel::deleteConversation
                )
                VerticalDivider()
                AiChatContent(
                    modifier = Modifier.weight(1f),
                    conversation = activeConversation,
                    uiState = uiState,
                    showDrawerButton = false,
                    onOpenDrawer = {},
                    onNewConversation = aiChatViewModel::createConversation,
                    onOpenPersonaSwitch = { personaSwitchOpen = true },
                    onRetry = aiChatViewModel::retry,
                    onEditUserMessage = aiChatViewModel::editUserMessage,
                    onSwitchUserBranch = aiChatViewModel::switchUserBranch,
                    onInputChange = aiChatViewModel::changeInput,
                    onWebSearchChange = aiChatViewModel::changeWebSearchEnabled,
                    onDeepThinkingChange = aiChatViewModel::changeDeepThinkingEnabled,
                    onSearchSettingsChange = aiChatViewModel::changeSearchSettings,
                    onSend = aiChatViewModel::send,
                    onStop = aiChatViewModel::stopGenerating,
                    toastManager = toastManager,
                    modelId = localSetting.aiModel,
                    onModelChange = localSettingManager::updateAiModel
                )
            }
        } else {
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
                AiChatContent(
                    modifier = Modifier.fillMaxSize(),
                    conversation = activeConversation,
                    uiState = uiState,
                    showDrawerButton = true,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onNewConversation = aiChatViewModel::createConversation,
                    onOpenPersonaSwitch = { personaSwitchOpen = true },
                    onRetry = aiChatViewModel::retry,
                    onEditUserMessage = aiChatViewModel::editUserMessage,
                    onSwitchUserBranch = aiChatViewModel::switchUserBranch,
                    onInputChange = aiChatViewModel::changeInput,
                    onWebSearchChange = aiChatViewModel::changeWebSearchEnabled,
                    onDeepThinkingChange = aiChatViewModel::changeDeepThinkingEnabled,
                    onSearchSettingsChange = aiChatViewModel::changeSearchSettings,
                    onSend = aiChatViewModel::send,
                    onStop = aiChatViewModel::stopGenerating,
                    toastManager = toastManager,
                    modelId = localSetting.aiModel,
                    onModelChange = localSettingManager::updateAiModel
                )
            }
        }
    }

    if (personaSwitchOpen) {
        PersonaSwitchDialog(
            personas = uiState.personas,
            activePersonaId = uiState.activePersonaId,
            onDismiss = { personaSwitchOpen = false },
            onSelect = { id ->
                if (uiState.activeConversationId.isNotBlank()) {
                    aiChatViewModel.bindConversationPersona(uiState.activeConversationId, id)
                }
                aiChatViewModel.setActivePersona(id)
                personaSwitchOpen = false
            }
        )
    }
}

@Composable
private fun AiChatContent(
    modifier: Modifier,
    conversation: AiChatConversation?,
    uiState: AiChatViewModel.AiChatUiState,
    showDrawerButton: Boolean,
    onOpenDrawer: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenPersonaSwitch: () -> Unit,
    onRetry: (String, AiChatViewModel.RetryMode) -> Unit,
    onEditUserMessage: (String, String) -> Unit,
    onSwitchUserBranch: (String, Int) -> Unit,
    onInputChange: (String) -> Unit,
    onWebSearchChange: (Boolean) -> Unit,
    onDeepThinkingChange: (Boolean) -> Unit,
    onSearchSettingsChange: (AiSearchSettings) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    toastManager: ToastManager,
    modelId: String,
    onModelChange: (String) -> Unit
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        AiChatHeader(
            title = conversation?.title ?: "AI 对话",
            personaName = uiState.activePersona?.name,
            showDrawerButton = showDrawerButton,
            onOpenDrawer = onOpenDrawer,
            onNewConversation = onNewConversation,
            onOpenPersonaSwitch = onOpenPersonaSwitch,
            modelId = modelId,
            onModelChange = onModelChange
        )
        MessageList(
            modifier = Modifier.weight(1f),
            messages = conversation?.messages.orEmpty(),
            isSending = uiState.isSending,
            searchUiState = uiState.searchUiState,
            lastSearchResults = uiState.lastSearchResults,
            onRetry = onRetry,
            onEditUserMessage = onEditUserMessage,
            onSwitchUserBranch = onSwitchUserBranch,
            toastManager = toastManager
        )
        uiState.errorMessage?.let {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = it,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        ChatInputBar(
            input = uiState.input,
            webSearchEnabled = uiState.webSearchEnabled,
            deepThinkingEnabled = uiState.deepThinkingEnabled,
            searchSettings = uiState.searchSettings,
            isSending = uiState.isSending,
            onInputChange = onInputChange,
            onWebSearchChange = onWebSearchChange,
            onDeepThinkingChange = onDeepThinkingChange,
            onSearchSettingsChange = onSearchSettingsChange,
            onSend = onSend,
            onStop = onStop
        )
    }
}

@Composable
private fun AiChatHeader(
    title: String,
    personaName: String?,
    showDrawerButton: Boolean,
    onOpenDrawer: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenPersonaSwitch: () -> Unit,
    modelId: String,
    onModelChange: (String) -> Unit
) {
    var modelMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showDrawerButton) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Rounded.Menu, contentDescription = "对话列表")
                }
            }

            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = buildString {
                        append(AiChatModel.fromId(modelId).label)
                        if (!personaName.isNullOrBlank()) append(" · 人格：$personaName")
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                val model = AiChatModel.fromId(modelId)
                AssistChip(
                    onClick = { modelMenuExpanded = true },
                    label = { Text(model.label, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (model == AiChatModel.NoTrackAi) {
                                Icons.Rounded.TravelExplore
                            } else {
                                Icons.Rounded.AutoAwesome
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false }
                ) {
                    AiChatModel.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(option.label)
                                    Text(
                                        option.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (option == AiChatModel.NoTrackAi) {
                                        Icons.Rounded.TravelExplore
                                    } else {
                                        Icons.Rounded.AutoAwesome
                                    },
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onModelChange(option.id)
                                modelMenuExpanded = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = onOpenPersonaSwitch) {
                Icon(Icons.Rounded.Face, contentDescription = "切换人格面具")
            }
            IconButton(onClick = onNewConversation) {
                Icon(Icons.Rounded.Add, contentDescription = "新建对话")
            }
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
        ConversationPanel(
            conversations = conversations,
            activeConversationId = activeConversationId,
            onNewConversation = onNewConversation,
            onSelectConversation = onSelectConversation,
            onDeleteConversation = onDeleteConversation
        )
    }
}

@Composable
private fun ConversationPanel(
    modifier: Modifier = Modifier,
    conversations: List<AiChatConversation>,
    activeConversationId: String,
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Article, contentDescription = null)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI 工作区", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${conversations.size} 个对话",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNewConversation) {
                    Icon(Icons.Rounded.Add, contentDescription = "新建对话")
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
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
        shape = RoundedCornerShape(16.dp)
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
    searchUiState: AiChatViewModel.SearchUiState,
    lastSearchResults: List<WebSearchResult>,
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

    // 搜索进度卡片显示在最后一条用户消息之后、AI 回复之前
    val showSearchCard = searchUiState !is AiChatViewModel.SearchUiState.Idle

    LaunchedEffect(messages.size, showSearchCard) {
        if (messages.isNotEmpty() || showSearchCard) {
            followOutput = true
            val targetIndex = messages.lastIndex + if (showSearchCard) 1 else 0
            listState.scrollToItem(targetIndex.coerceAtLeast(0))
        }
    }

    LaunchedEffect(messages.lastOrNull()?.content, isSending, followOutput, showSearchCard) {
        if (followOutput && !listState.isScrollInProgress) {
            val targetIndex = messages.lastIndex + if (showSearchCard) 1 else 0
            if (targetIndex >= 0) listState.scrollToItem(targetIndex)
        }
    }

    if (messages.isEmpty() && !showSearchCard) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                modifier = Modifier.size(30.dp),
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                            )
                        }
                    }
                    Text(
                        text = "开始一段 AI 对话",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "在下方输入问题，也可以随时切换模型、人格面具和联网搜索。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        if (showSearchCard) {
            item(key = "search-progress-card") {
                SearchProgressCard(
                    state = searchUiState,
                    results = lastSearchResults
                )
            }
        }
    }
}

/**
 * 联网搜索进度卡片：类似 Lobe Chat 的过程可视化。
 * 显示阶段：正在搜索 → 找到 N 条 → 完成 / 失败。
 * 完成后展示引用来源列表，可点击跳转外部浏览器。
 */
@Composable
private fun SearchProgressCard(
    state: AiChatViewModel.SearchUiState,
    results: List<WebSearchResult>
) {
    val uriHandler = LocalUriHandler.current
    val displayResults = when (state) {
        is AiChatViewModel.SearchUiState.Found -> state.results
        is AiChatViewModel.SearchUiState.Done -> state.results
        else -> results
    }
    val (containerColor, contentColor) = when (state) {
        is AiChatViewModel.SearchUiState.Failed ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (state) {
                    is AiChatViewModel.SearchUiState.Querying -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "正在联网搜索",
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = state.query,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    is AiChatViewModel.SearchUiState.Found -> {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = contentColor)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "已找到 ${state.count} 条结果",
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    is AiChatViewModel.SearchUiState.Done -> {
                        Icon(Icons.Rounded.TravelExplore, contentDescription = null, tint = contentColor)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "联网搜索完成（${state.results.size} 条引用）",
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    is AiChatViewModel.SearchUiState.Failed -> {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = contentColor)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "搜索失败：${state.message}",
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    AiChatViewModel.SearchUiState.Idle -> Unit
                }
            }
            // 引用来源列表
            if (displayResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                displayResults.take(5).forEachIndexed { index, result ->
                    CitationRow(
                        index = index + 1,
                        result = result,
                        contentColor = contentColor,
                        onClick = {
                            runCatching { uriHandler.openUri(result.url) }
                        }
                    )
                    if (index < minOf(displayResults.size, 5) - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = contentColor.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CitationRow(
    index: Int,
    result: WebSearchResult,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = contentColor.copy(alpha = 0.15f)
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                text = index.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (result.snippet.isNotBlank()) {
                Text(
                    text = result.snippet,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(12.dp),
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = displayLinkText(result.url),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = TextDecoration.Underline
                )
            }
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
        if (isUser) message.content else cleanAssistantAnswer(message.content)
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
                    shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
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
                    shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
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
                        .heightIn(max = adaptiveDialogMaxHeight(420.dp))
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            .clip(RoundedCornerShape(12.dp))
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
    val parsed = remember(message.content) { parseThinkContent(message.content) }
    if (parsed.blocks.isEmpty() && parsed.answer.isBlank()) {
        AssistantLoadingContent()
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            parsed.blocks.forEachIndexed { index, block ->
                ThinkBlockView(
                    block = block,
                    isThinking = index == parsed.blocks.lastIndex && parsed.isThinking
                )
            }
            if (parsed.answer.isNotBlank()) {
                MarkdownContent(text = parsed.answer)
            }
        }
    }
}

@Composable
private fun ThinkBlockView(
    block: ThinkBlockContent,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!isThinking) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = if (isThinking) "正在思考..." else "思考过程",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded || isThinking,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = block.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
    deepThinkingEnabled: Boolean,
    searchSettings: AiSearchSettings,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onWebSearchChange: (Boolean) -> Unit,
    onDeepThinkingChange: (Boolean) -> Unit,
    onSearchSettingsChange: (AiSearchSettings) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    var searchSettingsOpen by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = deepThinkingEnabled,
                        onClick = { onDeepThinkingChange(!deepThinkingEnabled) },
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null
                            )
                        },
                        label = { Text("深度思考") }
                    )
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
                    if (searchSettings.aiAutoSearch) {
                        AssistChip(
                            onClick = { searchSettingsOpen = true },
                            label = { Text("AI 自主") },
                            leadingIcon = {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
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
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSending || input.isNotBlank()) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ) {
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

/**
 * 人格切换对话框：在 AI 对话页快速切换或停用人格面具。
 */
@Composable
private fun PersonaSwitchDialog(
    personas: List<AiPersona>,
    activePersonaId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("切换人格面具") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = adaptiveDialogMaxHeight(480.dp))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (personas.isEmpty()) {
                    Text(
                        text = "尚未创建任何人格面具。请到「设置 → AI 设置 → 人格面具管理」新建。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    PersonaOptionRow(
                        title = "默认（无人格）",
                        description = "不注入任何人格设定",
                        selected = activePersonaId.isNullOrBlank(),
                        onClick = { onSelect(null) }
                    )
                    personas.forEach { persona ->
                        PersonaOptionRow(
                            title = persona.name.ifBlank { "未命名人格" },
                            description = listOfNotNull(
                                persona.profession.takeIf { it.isNotBlank() },
                                persona.bio.takeIf { it.isNotBlank() }
                            ).joinToString(" · "),
                            selected = persona.id == activePersonaId,
                            onClick = { onSelect(persona.id) }
                        )
                    }
                }
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
private fun PersonaOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Face,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
                    .heightIn(max = adaptiveDialogMaxHeight(560.dp))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "搜索引擎",
                    style = MaterialTheme.typography.labelLarge
                )
                SearchEngineProviderChips(
                    value = draft.provider,
                    onChange = { draft = draft.copy(provider = it) }
                )
                // Tavily Key 配置
                if (draft.provider == AiSearchEngineProvider.TAVILY || draft.provider == AiSearchEngineProvider.AUTO) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft.tavilyApiKey,
                        onValueChange = { draft = draft.copy(tavilyApiKey = it) },
                        label = { Text("Tavily API Key（可选）") },
                        placeholder = { Text("tvly-xxxxxx") },
                        singleLine = true,
                        supportingText = {
                            Text(
                                if (draft.provider == AiSearchEngineProvider.TAVILY && draft.tavilyApiKey.isBlank())
                                    "Tavily 需要填写 Key 才能使用"
                                else "留空则回退到其他引擎"
                            )
                        }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "搜索深度：${if (draft.tavilySearchDepth == "advanced") "深度" else "基础"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = draft.tavilySearchDepth == "advanced",
                            onCheckedChange = {
                                draft = draft.copy(tavilySearchDepth = if (it) "advanced" else "basic")
                            }
                        )
                    }
                }
                // AI 自主搜索开启
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI 自主决策搜索",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "检测到时间敏感词时自动联网",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = draft.aiAutoSearch,
                        onCheckedChange = { draft = draft.copy(aiAutoSearch = it) }
                    )
                }
                HorizontalDivider()
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
                // SearXNG 配置
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = draft.searxngBaseUrl,
                    onValueChange = { draft = draft.copy(searxngBaseUrl = it) },
                    label = { Text("SearXNG 地址（可选）") },
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
private fun SearchEngineProviderChips(
    value: AiSearchEngineProvider,
    onChange: (AiSearchEngineProvider) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                AiSearchEngineProvider.AUTO,
                AiSearchEngineProvider.TAVILY,
                AiSearchEngineProvider.DUCKDUCKGO
            ).forEach { SearchEngineProviderChip(it, value == it, onChange) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                AiSearchEngineProvider.BING,
                AiSearchEngineProvider.SOGOU,
                AiSearchEngineProvider.BAIDU,
                AiSearchEngineProvider.SEARXNG
            ).forEach { SearchEngineProviderChip(it, value == it, onChange) }
        }
    }
}

@Composable
private fun SearchEngineProviderChip(
    provider: AiSearchEngineProvider,
    selected: Boolean,
    onChange: (AiSearchEngineProvider) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { onChange(provider) },
        label = { Text(provider.label) }
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
            .clip(RoundedCornerShape(12.dp))
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
private const val THINK_OPEN_TAG = "<" + "think" + ">"
private const val THINK_CLOSE_TAG = "</" + "think" + ">"

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
        val open = result.indexOf(THINK_OPEN_TAG)
        if (open < 0) break
        val close = result.indexOf(THINK_CLOSE_TAG, startIndex = open + THINK_OPEN_TAG.length)
        result = if (close >= 0) {
            result.removeRange(open, close + THINK_CLOSE_TAG.length)
        } else {
            result.substring(0, open)
        }
    }
    val strayClose = result.indexOf(THINK_CLOSE_TAG)
    if (strayClose >= 0) {
        result = result.substring(strayClose + THINK_CLOSE_TAG.length)
    }
    return result
        .replace(THINK_OPEN_TAG, "")
        .replace(THINK_CLOSE_TAG, "")
        .trim()
}

data class ThinkBlockContent(val content: String)

data class ParsedThinkContent(
    val blocks: List<ThinkBlockContent>,
    val answer: String,
    val isThinking: Boolean
)

fun parseThinkContent(content: String): ParsedThinkContent {
    val blocks = mutableListOf<ThinkBlockContent>()
    val answer = StringBuilder()
    var remaining = content

    while (true) {
        val open = remaining.indexOf(THINK_OPEN_TAG)
        if (open < 0) {
            answer.append(remaining)
            break
        }
        val thinkStart = open + THINK_OPEN_TAG.length
        val close = remaining.indexOf(THINK_CLOSE_TAG, thinkStart)
        if (close < 0) {
            // 未闭合：思考进行中，把 open 之前的内容加入正文，思考内容放入块
            answer.append(remaining.substring(0, open))
            blocks.add(ThinkBlockContent(remaining.substring(thinkStart)))
            return ParsedThinkContent(blocks, answer.toString().trim(), isThinking = true)
        }
        val nextOpen = remaining.indexOf(THINK_OPEN_TAG, thinkStart)
        if (nextOpen in 0 until close) {
            // 嵌套 open 视为不闭合：去除所有 think 标签后作为普通正文
            answer.append(remaining.replace(THINK_OPEN_TAG, "").replace(THINK_CLOSE_TAG, ""))
            break
        }
        answer.append(remaining.substring(0, open))
        blocks.add(ThinkBlockContent(remaining.substring(thinkStart, close)))
        remaining = remaining.substring(close + THINK_CLOSE_TAG.length)
    }

    return ParsedThinkContent(blocks, answer.toString().trim(), isThinking = false)
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
