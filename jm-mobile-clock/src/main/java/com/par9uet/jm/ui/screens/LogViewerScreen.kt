package com.par9uet.jm.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.par9uet.jm.ui.components.WatchLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.utils.LogBuffer
import com.par9uet.jm.utils.LogEntry
import kotlinx.coroutines.delay

@Composable
fun LogViewerScreen() {
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    var logs by remember { mutableStateOf(LogBuffer.getLogs()) }
    var autoScroll by remember { mutableStateOf(false) }

    // Auto-refresh every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            logs = LogBuffer.getLogs()
            if (autoScroll && logs.isNotEmpty()) {
                listState.scrollToItem(logs.size)
            }
        }
    }

    CommonScaffold(title = "日志") {
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            item {
                Button(onClick = { logs = LogBuffer.getLogs(); autoScroll = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("刷新日志") }
                Button(onClick = { clipboardManager.setText(AnnotatedString(LogBuffer.getLogText())) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("复制日志") }
                Button(onClick = { LogBuffer.clear(); logs = emptyList() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("清空日志") }
                Button(onClick = { autoScroll = !autoScroll }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("自动跟随 · ${if (autoScroll) "开" else "关"}") }
            }
            if (logs.isEmpty()) item { Text("暂无日志", style = MaterialTheme.typography.bodySmall) }
            itemsIndexed(logs, key = { index, _ -> index }) { _, entry -> LogItem(entry) }
        }
    }
}

@Composable
private fun LogItem(entry: LogEntry) {
    val bgColor = when (entry.level) {
        "E" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val textColor = when (entry.level) {
        "E" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            text = entry.formatted,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = textColor
        )
    }
}
