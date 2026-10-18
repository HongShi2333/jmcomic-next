package com.par9uet.jm.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * 轻量级 Markdown 解析器，用于渲染更新日志（类似 版本更改.md 的内容）。
 * 支持：标题（#、##、###）、无序列表（- 或 *）、粗体、行内代码、链接、分隔线（---）。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(
        modifier = modifier
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                is MdBlock.Paragraph -> Text(
                    text = parseInline(block.text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                is MdBlock.ListItem -> Text(
                    text = parseInline("• ${block.text}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )

                is MdBlock.Divider -> Spacer(
                    modifier = Modifier
                        .height(1.dp)
                        .padding(vertical = 4.dp)
                )

                is MdBlock.Empty -> Spacer(modifier = Modifier.height(4.dp))
            }
            if (index != blocks.lastIndex) Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class ListItem(val text: String) : MdBlock()
    object Divider : MdBlock()
    object Empty : MdBlock()
}

private fun parseMarkdown(text: String): List<MdBlock> {
    val result = mutableListOf<MdBlock>()
    val lines = text.lines()
    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> result.add(MdBlock.Empty)
            trimmed.startsWith("### ") -> result.add(MdBlock.Heading(3, trimmed.removePrefix("### ").trim()))
            trimmed.startsWith("## ") -> result.add(MdBlock.Heading(2, trimmed.removePrefix("## ").trim()))
            trimmed.startsWith("# ") -> result.add(MdBlock.Heading(1, trimmed.removePrefix("# ").trim()))
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> result.add(MdBlock.Divider)
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> result.add(MdBlock.ListItem(trimmed.removePrefix("- ").removePrefix("* ").trim()))
            else -> result.add(MdBlock.Paragraph(trimmed))
        }
    }
    return result
}

private val inlineRegex = Regex("""\*\*(.+?)\*\*|`(.+?)`|\[([^\]]+)\]\(([^)]+)\)""")

private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var lastIndex = 0
    for (match in inlineRegex.findAll(text)) {
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }
        val bold = match.groupValues[1]
        val code = match.groupValues[2]
        val linkText = match.groupValues[3]
        val linkUrl = match.groupValues[4]
        when {
            bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            code.isNotEmpty() -> withStyle(
                SpanStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    background = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f)
                )
            ) { append(code) }

            linkText.isNotEmpty() -> withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(linkText) }
            else -> append(match.value)
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) append(text.substring(lastIndex))
}
