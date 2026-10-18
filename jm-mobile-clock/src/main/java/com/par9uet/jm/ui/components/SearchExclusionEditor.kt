package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.BlockedTagTemplate
import com.par9uet.jm.utils.normalizeBlockedTagList

/** A summary on the search page; the tag list gets its own full-height editor. */
@Composable
fun SearchExclusionEditor(
    excludedTags: List<String>,
    templates: List<BlockedTagTemplate>,
    onChangeTags: (List<String>) -> Unit,
    onOpenTemplateSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        WatchActionRow("排除标签 · ${excludedTags.size}", excludedTags.take(3).joinToString("、").ifBlank { "添加标签或套用模板" }) { open = true }
    }
    if (open) WatchTagExclusionDialog(
        initialTags = excludedTags, templates = templates, onDismiss = { open = false },
        onConfirm = { onChangeTags(it); open = false },
        onOpenTemplateSettings = { open = false; onOpenTemplateSettings() },
    )
}

@Composable
internal fun WatchTagExclusionDialog(
    initialTags: List<String>,
    templates: List<BlockedTagTemplate>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    onOpenTemplateSettings: () -> Unit,
    title: String = "排除标签",
    showTemplates: Boolean = true,
) {
    var draft by rememberSaveable { mutableStateOf(initialTags) }
    var input by rememberSaveable { mutableStateOf("") }
    var panel by rememberSaveable { mutableStateOf("tags") }
    if (panel == "add") {
        WatchTextEditorDialog("添加标签", input, { input = it }, onDismiss = { panel = "tags" },
            onConfirm = { draft = normalizeBlockedTagList(draft + input); input = ""; panel = "tags" },
            confirmLabel = "添加", confirmEnabled = input.isNotBlank(), singleLine = true)
        return
    }
    key(panel) {
        WatchPanelDialog(if (panel == "templates") "套用模板" else title,
            onDismiss = { if (panel == "tags") onDismiss() else panel = "tags" }) {
            if (panel == "templates") {
                if (templates.isEmpty()) item { Text("暂无模板", style = MaterialTheme.typography.bodySmall) }
                items(templates) { template ->
                    WatchActionRow(template.name, "${template.tagList.size} 个标签") {
                        draft = normalizeBlockedTagList(draft + template.tagList); panel = "tags"
                    }
                }
                item { WatchActionRow("管理模板", onClick = onOpenTemplateSettings) }
            } else {
                item { Button(onClick = { onConfirm(normalizeBlockedTagList(draft)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("应用 · ${draft.size} 项") } }
                item {
                    if (showTemplates) WatchActionPair("添加", "模板", onFirst = { input = ""; panel = "add" }, onSecond = { panel = "templates" })
                    else WatchActionRow("添加标签") { input = ""; panel = "add" }
                }
                if (draft.isEmpty()) item { Text("暂无排除标签", style = MaterialTheme.typography.bodySmall) }
                else item { Text("点击已选项即可移除", style = MaterialTheme.typography.labelSmall) }
                items(draft, key = { it }) { tag ->
                    WatchActionRow(tag, selected = true) { draft = draft.filterNot { it == tag } }
                }
                if (draft.isNotEmpty()) item { TextButton(onClick = { draft = emptyList() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("清空草稿") } }
            }
        }
    }
}
