package com.par9uet.jm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiChatMessage
import com.par9uet.jm.ui.viewModel.AiChatViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    aiChatViewModel: AiChatViewModel = koinActivityViewModel()
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
                isSending = uiState.isSending
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
                reasoningEnabled = uiState.reasoningEnabled,
                isSending = uiState.isSending,
                onInputChange = aiChatViewModel::changeInput,
                onReasoningChange = aiChatViewModel::changeReasoningEnabled,
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
                text = "chat-model-reasoning",
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
    isSending: Boolean
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (messages.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    modifier = Modifier.size(44.dp),
                    imageVector = Icons.Rounded.Psychology,
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
            ChatMessageBubble(message = message)
        }
        if (isSending) {
            item {
                Text(
                    text = "正在生成...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: AiChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 0.94f),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (isUser) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            shape = RoundedCornerShape(8.dp),
            tonalElevation = if (isUser) 0.dp else 1.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (isUser) {
                    Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                } else {
                    AssistantMessageContent(message = message)
                }
            }
        }
    }
}

@Composable
private fun AssistantMessageContent(message: AiChatMessage) {
    val parts = remember(message.content) { splitThinkContent(message.content) }
    if (parts.hasThink) {
        ThinkBlock(
            messageId = message.id,
            text = parts.think,
            complete = parts.complete
        )
        if (parts.answer.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    if (parts.answer.isNotBlank()) {
        MarkdownContent(text = parts.answer)
    }
}

@Composable
private fun ThinkBlock(
    messageId: String,
    text: String,
    complete: Boolean
) {
    var expanded by rememberSaveable(messageId, complete) { mutableStateOf(!complete) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = complete) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = if (complete) "深度思考" else "思考中",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (complete) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "收起思考" else "展开思考",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text.ifBlank { "等待模型输出思考内容..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    reasoningEnabled: Boolean,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onReasoningChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    val density = LocalDensity.current
    val keyboardHeight = with(density) {
        WindowInsets.ime.getBottom(density).toDp()
    }

    Surface(
        modifier = Modifier.padding(bottom = keyboardHeight),
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
                    selected = reasoningEnabled,
                    onClick = { onReasoningChange(!reasoningEnabled) },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Rounded.Psychology,
                            contentDescription = null
                        )
                    },
                    label = { Text("深度思考") }
                )
                Spacer(modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text("流式回复") })
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
}

@Composable
private fun MarkdownContent(text: String) {
    val lines = text.lines()
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

                line.startsWith("### ") -> MarkdownLine(line.removePrefix("### "), MarkdownKind.HeadingSmall)
                line.startsWith("## ") -> MarkdownLine(line.removePrefix("## "), MarkdownKind.HeadingMedium)
                line.startsWith("# ") -> MarkdownLine(line.removePrefix("# "), MarkdownKind.HeadingLarge)
                line.trim().startsWith(">") -> MarkdownLine(line.trim().removePrefix(">").trim(), MarkdownKind.Quote)
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> BulletLine(line.trim().drop(2))
                numberedListText(line) != null -> BulletLine(numberedListText(line).orEmpty(), ordered = true)
                line.isBlank() -> Spacer(modifier = Modifier.height(2.dp))
                else -> MarkdownLine(line, MarkdownKind.Body)
            }
            index++
        }
    }
}

@Composable
private fun MarkdownLine(text: String, kind: MarkdownKind) {
    val style = when (kind) {
        MarkdownKind.HeadingLarge -> MaterialTheme.typography.titleLarge
        MarkdownKind.HeadingMedium -> MaterialTheme.typography.titleMedium
        MarkdownKind.HeadingSmall -> MaterialTheme.typography.titleSmall
        MarkdownKind.Quote -> MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
        MarkdownKind.Body -> MaterialTheme.typography.bodyMedium
    }
    Text(
        text = inlineMarkdown(text),
        style = style,
        color = if (kind == MarkdownKind.Quote) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color.Unspecified
        }
    )
}

@Composable
private fun BulletLine(text: String, ordered: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.width(18.dp),
            text = if (ordered) "1." else "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            modifier = Modifier.weight(1f),
            text = inlineMarkdown(text),
            style = MaterialTheme.typography.bodyMedium
        )
    }
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
    var index = 0
    while (index < text.length) {
        when {
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

private data class ThinkParts(
    val hasThink: Boolean,
    val think: String,
    val answer: String,
    val complete: Boolean
)

private fun splitThinkContent(content: String): ThinkParts {
    val start = content.indexOf("<think>")
    if (start < 0) {
        if ("<think>".startsWith(content.trim()) && content.isNotBlank()) {
            return ThinkParts(hasThink = true, think = "", answer = "", complete = false)
        }
        return ThinkParts(hasThink = false, think = "", answer = content, complete = true)
    }
    val thinkStart = start + "<think>".length
    val end = content.lastIndexOf("</think>")
    if (end < 0) {
        return ThinkParts(
            hasThink = true,
            think = sanitizeThinkText(removeTrailingPartialThinkClose(content.substring(thinkStart))),
            answer = content.substring(0, start).trim(),
            complete = false
        )
    }
    return ThinkParts(
        hasThink = true,
        think = sanitizeThinkText(content.substring(thinkStart, end)),
        answer = (content.substring(0, start) + content.substring(end + "</think>".length)).trim(),
        complete = true
    )
}

private fun sanitizeThinkText(text: String): String {
    return text
        .replace("<think>", "")
        .replace("</think>", "")
        .trim()
}

private fun removeTrailingPartialThinkClose(text: String): String {
    val endTag = "</think>"
    for (length in endTag.length - 1 downTo 1) {
        val prefix = endTag.take(length)
        if (text.endsWith(prefix)) {
            return text.dropLast(length)
        }
    }
    return text
}

private fun numberedListText(line: String): String? {
    val match = Regex("^\\s*\\d+[.)]\\s+(.+)$").find(line) ?: return null
    return match.groupValues[1]
}

private fun formatTime(value: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(value))
}
