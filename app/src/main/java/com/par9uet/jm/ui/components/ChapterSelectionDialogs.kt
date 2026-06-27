package com.par9uet.jm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.par9uet.jm.data.models.ComicChapter

@Composable
fun ChapterMultiSelectDialog(
    title: String,
    chapters: List<ComicChapter>,
    selectedChapterIds: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    secondaryConfirmText: String? = null,
    onSecondaryConfirm: (() -> Unit)? = null,
) {
    val allChapterIds = remember(chapters) { chapters.map { it.id }.toSet() }
    val allSelected = chapters.isNotEmpty() && selectedChapterIds.containsAll(allChapterIds)

    ChapterDialogLayout(
        title = title,
        topActionText = if (chapters.isNotEmpty()) {
            if (allSelected) "取消全选" else "全选"
        } else {
            null
        },
        onTopActionClick = {
            onSelectedChange(if (allSelected) emptySet() else allChapterIds)
        },
        onDismiss = onDismiss,
        footer = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
            if (secondaryConfirmText != null && onSecondaryConfirm != null) {
                TextButton(
                    onClick = onSecondaryConfirm,
                    enabled = selectedChapterIds.isNotEmpty()
                ) {
                    Text(secondaryConfirmText)
                }
            }
            Button(
                onClick = onConfirm,
                enabled = selectedChapterIds.isNotEmpty()
            ) {
                Text(confirmText)
            }
        }
    ) {
        if (chapters.isEmpty()) {
            Text(
                text = "暂无可选章节",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                    val selected = chapter.id in selectedChapterIds
                    SelectableChapterCard(
                        title = chapter.name.ifBlank { "第 ${index + 1} 章" },
                        selected = selected,
                        onClick = {
                            onSelectedChange(
                                if (selected) {
                                    selectedChapterIds - chapter.id
                                } else {
                                    selectedChapterIds + chapter.id
                                }
                            )
                        },
                        trailing = {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { checked ->
                                    onSelectedChange(
                                        if (checked) {
                                            selectedChapterIds + chapter.id
                                        } else {
                                            selectedChapterIds - chapter.id
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterSingleSelectDialog(
    title: String,
    chapters: List<ComicChapter>,
    currentChapterId: Int?,
    onDismiss: () -> Unit,
    onSelect: (ComicChapter) -> Unit,
) {
    ChapterDialogLayout(
        title = title,
        onDismiss = onDismiss,
        footer = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        if (chapters.isEmpty()) {
            Text(
                text = "暂无可选章节",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                    SelectableChapterCard(
                        title = chapter.name.ifBlank { "第 ${index + 1} 章" },
                        selected = chapter.id == currentChapterId,
                        onClick = { onSelect(chapter) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterDialogLayout(
    title: String,
    onDismiss: () -> Unit,
    footer: @Composable RowScope.() -> Unit,
    topActionText: String? = null,
    onTopActionClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (topActionText != null && onTopActionClick != null) {
                        TextButton(onClick = onTopActionClick) {
                            Text(topActionText)
                        }
                    }
                }
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    footer()
                }
            }
        }
    }
}

@Composable
private fun SelectableChapterCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            trailing?.invoke()
        }
    }
}
