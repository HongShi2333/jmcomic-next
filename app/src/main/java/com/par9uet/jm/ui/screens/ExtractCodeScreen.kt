package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.ComicCoverImage
import com.par9uet.jm.ui.components.toDisplayText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin

/**
 * 提取编码页面
 *
 * 用户粘贴包含数字的文字（如分享文案），自动提取所有数字拼接为漫画编码，
 * 拉取漫画详情后弹窗展示封面/标题/作者/标签，确认后跳转详情页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractCodeScreen(
    comicRepository: ComicRepository = getKoin().get(),
    toastManager: ToastManager = getKoin().get(),
) {
    val mainNavController = LocalMainNavController.current
    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var extractedCode by remember { mutableStateOf<String?>(null) }
    var previewComic by remember { mutableStateOf<Comic?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun extractAndFetch(text: String) {
        val digits = text.filter { it.isDigit() }
        if (digits.isBlank()) {
            toastManager.showAsync("未检测到数字，无法提取编码")
            return
        }
        extractedCode = digits
        loading = true
        previewComic = null
    }

    // 提取后自动拉取详情
    LaunchedEffect(extractedCode) {
        val code = extractedCode ?: return@LaunchedEffect
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching { comicRepository.getComicDetail(code.toInt()) }
                .getOrNull()
        }
        when (result) {
            is NetWorkResult.Success<*> -> {
                @Suppress("UNCHECKED_CAST")
                previewComic = (result.data as com.par9uet.jm.retrofit.model.ComicDetailResponse).toComic()
            }
            is NetWorkResult.Error -> {
                toastManager.showAsync("获取漫画详情失败：${result.message}")
                extractedCode = null
            }
            null -> {
                toastManager.showAsync("获取漫画详情异常")
                extractedCode = null
            }
        }
        loading = false
    }

    CommonScaffold(title = "提取编码") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "粘贴包含数字的文字，自动提取所有数字拼成漫画编码",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                placeholder = { Text("例：加里奥在40岁的时候一拳撩到了8个闯入家中的恐怖分子获得了882万的悬赏金") },
                supportingText = {
                    Text("提取的数字：${extractedCode ?: "—"}")
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipText = clipboardManager.getText()?.text ?: ""
                        if (clipText.isNotBlank()) {
                            inputText = clipText
                            extractAndFetch(clipText)
                        } else {
                            toastManager.showAsync("剪切板为空")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null)
                    Text("粘贴", modifier = Modifier.padding(start = 4.dp))
                }
                Button(
                    onClick = { extractAndFetch(inputText) },
                    enabled = inputText.isNotBlank() && !loading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    Text("提取", modifier = Modifier.padding(start = 4.dp))
                }
            }
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            "正在获取漫画详情...",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // 详情预览弹窗（左侧封面小窗口 + 右侧信息，适配平板）
    val comic = previewComic
    if (comic != null) {
        AlertDialog(
            onDismissRequest = {
                previewComic = null
                extractedCode = null
            },
            title = { Text("找到漫画", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 左侧封面小窗口
                    ComicCoverImage(
                        comic = comic,
                        modifier = Modifier
                            .width(96.dp)
                            .height(128.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    // 右侧信息
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // JM 编码
                        AssistChip(
                            onClick = {},
                            label = { Text("JM${comic.id}") }
                        )
                        // 标题
                        Text(
                            text = comic.name.toDisplayText(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        // 作者
                        if (comic.authorList.isNotEmpty()) {
                            Text(
                                text = "作者：${comic.authorList.joinToString("、").toDisplayText()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        // 标签
                        if (comic.tagList.isNotEmpty()) {
                            Text(
                                text = "标签：${comic.tagList.take(10).joinToString("、")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    previewComic = null
                    extractedCode = null
                    inputText = ""
                    mainNavController.navigate("comicDetail/${comic.id}")
                }) { Text("跳转详情") }
            },
            dismissButton = {
                TextButton(onClick = {
                    previewComic = null
                    extractedCode = null
                }) { Text("取消") }
            }
        )
    }
}
