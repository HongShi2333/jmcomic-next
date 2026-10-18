package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.AiChatModel
import com.par9uet.jm.data.models.AiChatMessage
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.components.*
import com.par9uet.jm.ui.viewModel.AiChatViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
internal fun WatchAiChatScreen(
    viewModel: AiChatViewModel,
    toastManager: ToastManager,
    settingsManager: LocalSettingManager,
) {
    val state by viewModel.uiState.collectAsState()
    val settings by settingsManager.localSettingState.collectAsState()
    val conversation = state.conversations.firstOrNull { it.id == state.activeConversationId }
    val messages = conversation?.messages.orEmpty()
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var historyQuery by rememberSaveable { mutableStateOf("") }
    var historyQueryDraft by rememberSaveable { mutableStateOf("") }
    var editingHistorySearch by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var followOutput by remember(state.activeConversationId) { mutableStateOf(true) }
    val active = LocalWatchListActive.current
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val showSearch = state.searchUiState != AiChatViewModel.SearchUiState.Idle || state.lastSearchResults.isNotEmpty()
    val tailIndex = 1 + (if (messages.isEmpty()) 1 else messages.size) + (if (showSearch) 1 else 0) + (if (state.errorMessage != null) 1 else 0)
    fun latest() { followOutput = true; scope.launch { listState.scrollToItem(tailIndex) } }
    fun send() { if (!state.isSending && state.input.isNotBlank()) { followOutput = true; viewModel.send(); panel = null } }
    fun openPanelAnimated(next: String) {
        // Keep the pressed state on screen long enough for the scale-down to
        // be perceptible before the full-screen watch panel replaces it.
        scope.launch {
            delay(90)
            panel = next
        }
    }

    LaunchedEffect(state.activeConversationId) { listState.scrollToItem(if (messages.isEmpty()) 0 else tailIndex) }
    LaunchedEffect(messages.lastOrNull()?.content, messages.size, panel, active, windowFocused) {
        if (active && windowFocused && panel == null && followOutput && !listState.isScrollInProgress && messages.isNotEmpty()) {
            listState.scrollToItem(tailIndex)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val info = listState.layoutInfo
                if (info.visibleItemsInfo.any { it.index == info.totalItemsCount - 1 }) followOutput = true
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalWatchListActive provides (active && panel == null)) {
            WatchLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("watch-ai-messages")
                    .then(if (panel != null) Modifier.clearAndSetSemantics { } else Modifier)
                    .onPreRotaryScrollEvent { if (it.verticalScrollPixels < 0f) followOutput = false; false }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                if (awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }) followOutput = false
                            }
                        }
                    },
                contentPadding = PaddingValues(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "summary") {
                    WatchActionRow(conversation?.title?.takeUnless { it == "新对话" } ?: "AI 对话",
                        icon = Icons.Rounded.MoreHoriz) { panel = "tools" }
                }
                if (messages.isEmpty()) item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("想聊点什么？", style = MaterialTheme.typography.titleSmall)
                    }
                }
                items(messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message, retryEnabled = !state.isSending, onRetry = viewModel::retry,
                        onEditUserMessage = viewModel::editUserMessage, onSwitchUserBranch = viewModel::switchUserBranch,
                        toastManager = toastManager,
                    )
                }
                if (showSearch) item(key = "search") { SearchProgressCard(state.searchUiState, state.lastSearchResults) }
                state.errorMessage?.let { error -> item(key = "error") {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } }
                item(key = "tail") { Spacer(Modifier.height(4.dp)) }
            }
        }
        // Restore the earlier watch layout: two always-visible, 48dp circular
        // controls. The message viewport stays full-size and the actions are
        // recognizable without opening a cramped capsule.
        if (panel == null) {
            val composeSource = remember { MutableInteractionSource() }
            val toolsSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (watchIsRound()) 14.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = { if (state.isSending) viewModel.stopGenerating() else openPanelAnimated("write") },
                    interactionSource = composeSource,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("watch-ai-compose")
                        .watchPressFeedback(composeSource),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (state.isSending) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (state.isSending) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (state.isSending) Icons.Rounded.Stop else Icons.Rounded.Edit,
                            contentDescription = if (state.isSending) "停止生成" else "写消息",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Surface(
                    onClick = { openPanelAnimated("tools") },
                    interactionSource = toolsSource,
                    modifier = Modifier
                        .size(48.dp)
                        .watchPressFeedback(toolsSource),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MoreHoriz, contentDescription = "对话工具", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

    }

    when (panel) {
        "write" -> WatchTextEditorDialog(
            title = "写消息", value = state.input, onValueChange = viewModel::changeInput,
            onDismiss = { panel = null }, onConfirm = ::send, confirmLabel = "发送",
            confirmEnabled = !state.isSending && state.input.isNotBlank(),
            hint = "返回自动保留草稿",
        )
        "tools" -> WatchPanelDialog("对话工具", onDismiss = { panel = null }) {
            if (state.isSending) item { WatchActionRow("停止生成", icon = Icons.Rounded.Stop) { viewModel.stopGenerating() } }
            item { WatchActionRow("写消息", if (state.input.isBlank()) null else "有未发送草稿", icon = Icons.Rounded.Edit,
                enabled = !state.isSending) { panel = "write" } }
            item { WatchActionRow("回到最新消息", icon = Icons.Rounded.ArrowDownward) { panel = null; latest() } }
            item { WatchActionRow("历史对话", "${state.conversations.size} 个", enabled = !state.isSending) { panel = "history" } }
            item { WatchActionRow("新建对话", icon = Icons.Rounded.Add, enabled = !state.isSending) {
                viewModel.createConversation(); panel = "write"
            } }
            item { WatchActionRow("模型与人格", AiChatModel.fromId(settings.aiModel).label) { panel = "preferences" } }
            item { WatchActionRow("联网与思考", "搜索设置、深度思考") { panel = "search-options" } }
        }
        "history" -> if (editingHistorySearch) {
            WatchTextEditorDialog("查找对话", historyQueryDraft, { historyQueryDraft = it },
                onDismiss = { editingHistorySearch = false },
                onConfirm = { historyQuery = historyQueryDraft.trim(); editingHistorySearch = false }, singleLine = true)
        } else WatchPanelDialog("历史对话", onDismiss = { panel = "tools" }) {
            item { WatchActionRow("查找对话", historyQuery.ifBlank { "按标题搜索" }, icon = Icons.Rounded.Search) {
                historyQueryDraft = historyQuery; editingHistorySearch = true
            } }
            val filtered = state.conversations.filter { it.title.contains(historyQuery, ignoreCase = true) }
            if (filtered.isEmpty()) item { Text("没有匹配的对话", style = MaterialTheme.typography.bodySmall) }
            items(filtered, key = { it.id }) { item ->
                WatchActionRow(item.title, "${item.messages.size} 条消息",
                    selected = item.id == state.activeConversationId, enabled = !state.isSending) {
                    viewModel.selectConversation(item.id); panel = null
                }
            }
            item { WatchActionRow("删除当前对话", conversation?.title, enabled = !state.isSending) { deleteId = state.activeConversationId; panel = "delete" } }
        }
        "delete" -> WatchPanelDialog("删除对话？", onDismiss = { panel = "history"; deleteId = null }) {
            item { Text(state.conversations.firstOrNull { it.id == deleteId }?.title.orEmpty(), style = MaterialTheme.typography.bodySmall) }
            item { Text("删除后无法恢复。", style = MaterialTheme.typography.bodySmall) }
            item { WatchActionPair("取消", "删除", secondEnabled = !state.isSending,
                onFirst = { deleteId = null; panel = "history" },
                onSecond = { deleteId?.let(viewModel::deleteConversation); deleteId = null; panel = "history" }) }
        }
        "preferences" -> WatchPanelDialog("模型与人格", onDismiss = { panel = "tools" }) {
            item { WatchActionRow("模型", AiChatModel.fromId(settings.aiModel).label, enabled = !state.isSending) { panel = "model" } }
            item { WatchActionRow("人格面具", viewModel.resolvePersonaForConversation(state.activeConversationId)?.name ?: "默认",
                enabled = !state.isSending) { panel = "persona" } }
        }
        "model" -> SelectDialog("选择模型", settings.aiModel,
            AiChatModel.entries.map { SelectOption(it.label, it.id) },
            onSelect = { settingsManager.updateAiModel(it); panel = "preferences" }, onDismissRequest = { panel = "preferences" })
        "persona" -> WatchPanelDialog("人格面具", onDismiss = { panel = "preferences" }) {
            fun select(id: String?) {
                viewModel.bindConversationPersona(state.activeConversationId, id)
                viewModel.setActivePersona(id)
                panel = "preferences"
            }
            item { WatchActionRow("默认（无人格）", selected = viewModel.resolvePersonaForConversation(state.activeConversationId) == null) { select(null) } }
            items(state.personas, key = { it.id }) { persona ->
                WatchActionRow(persona.name.ifBlank { "未命名人格" }, persona.profession,
                    selected = viewModel.resolvePersonaForConversation(state.activeConversationId)?.id == persona.id) { select(persona.id) }
            }
            if (state.personas.isEmpty()) item { Text("可到“设置 → 账户与工具”创建人格面具。", style = MaterialTheme.typography.labelSmall) }
        }
        "search-options" -> WatchPanelDialog("联网与思考", onDismiss = { panel = "tools" }) {
            item { WatchActionRow("联网搜索", selected = state.webSearchEnabled, enabled = !state.isSending) { viewModel.changeWebSearchEnabled(!state.webSearchEnabled) } }
            item { WatchActionRow("深度思考", selected = state.deepThinkingEnabled, enabled = !state.isSending) { viewModel.changeDeepThinkingEnabled(!state.deepThinkingEnabled) } }
            item { WatchActionRow("搜索配置", state.searchSettings.provider.label, enabled = !state.isSending) { panel = "search-settings" } }
        }
        "search-settings" -> SearchSettingsDialog(state.searchSettings, onDismiss = { panel = "search-options" },
            onSave = { viewModel.changeSearchSettings(it); panel = "search-options" })
    }
}
