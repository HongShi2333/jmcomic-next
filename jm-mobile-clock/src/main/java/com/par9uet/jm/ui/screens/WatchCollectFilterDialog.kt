package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.TagFilterLogic
import com.par9uet.jm.ui.components.*
import com.par9uet.jm.ui.viewModel.CollectComicLocalFilter

internal data class WatchFilterOption(val label: String, val count: Int)

internal fun watchFilterOptions(counts: Map<String, Int>, chosen: Set<String>, query: String, selectedOnly: Boolean): List<WatchFilterOption> =
    (counts.keys + chosen).asSequence()
        .filter { (!selectedOnly || it in chosen) && it.contains(query.trim(), ignoreCase = true) }
        .map { WatchFilterOption(it, counts[it] ?: 0) }
        .sortedWith(compareByDescending<WatchFilterOption> { it.count }.thenBy { it.label })
        .toList()

internal fun CollectComicLocalFilter.selectionCount(): Int =
    selectedTags.size + selectedRoles.size + selectedAuthors.size + selectedTypes.size

internal fun CollectComicLocalFilter.withoutTagSelections(): CollectComicLocalFilter =
    copy(selectedTags = emptySet(), selectedRoles = emptySet(), selectedAuthors = emptySet(), selectedTypes = emptySet())

private val CollectFilterSaver = listSaver<CollectComicLocalFilter, Any>(
    save = { listOf(it.searchText, it.selectedTags.toList(), it.selectedRoles.toList(),
        it.selectedAuthors.toList(), it.selectedTypes.toList(), it.tagLogic.name) },
    restore = {
        fun values(index: Int) = (it[index] as List<*>).filterIsInstance<String>().toSet()
        CollectComicLocalFilter(it[0] as String, values(1), values(2), values(3), values(4), TagFilterLogic.valueOf(it[5] as String))
    },
)

@Composable
internal fun WatchCollectFilterDialog(
    initial: CollectComicLocalFilter,
    tagCounts: Map<String, Int>,
    roleCounts: Map<String, Int>,
    authorCounts: Map<String, Int>,
    typeCounts: Map<String, Int>,
    loading: Boolean,
    error: String?,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
    onApply: (CollectComicLocalFilter) -> Unit,
) {
    var draft by rememberSaveable(stateSaver = CollectFilterSaver) { mutableStateOf(initial) }
    var category by rememberSaveable { mutableIntStateOf(-1) }
    var ruleOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var queryDraft by rememberSaveable { mutableStateOf("") }
    var queryOpen by rememberSaveable { mutableStateOf(false) }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }
    val labels = listOf("标签", "角色", "作者", "类型")
    val selections = listOf(draft.selectedTags, draft.selectedRoles, draft.selectedAuthors, draft.selectedTypes)
    val total = draft.selectionCount()
    if (queryOpen) {
        WatchTextEditorDialog(
            "查找${labels[category]}", queryDraft, { queryDraft = it }, onDismiss = { queryOpen = false },
            onConfirm = { query = queryDraft.trim(); queryOpen = false }, singleLine = true,
        )
        return
    }
    val categoryCounts = listOf(tagCounts, roleCounts, authorCounts, typeCounts)
    val choices = if (category in labels.indices) watchFilterOptions(categoryCounts[category], selections[category], query, selectedOnly) else emptyList()
    fun toggle(value: String) {
        val current = selections[category]
        val next = if (value in current) current - value else current + value
        draft = when (category) {
            0 -> draft.copy(selectedTags = next)
            1 -> draft.copy(selectedRoles = next)
            2 -> draft.copy(selectedAuthors = next)
            else -> draft.copy(selectedTypes = next)
        }
    }
    // One dialog at a time; Back in a category returns to the overview, retaining the draft.
    key(category, ruleOpen, query, selectedOnly) {
        WatchPanelDialog(
            title = if (ruleOpen) "匹配规则" else if (category >= 0) labels[category] else "筛选收藏",
            onDismiss = { when { ruleOpen -> ruleOpen = false; category >= 0 -> { category = -1; query = ""; selectedOnly = false }; else -> onDismiss() } },
        ) {
            item {
                Button(onClick = { onApply(draft) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("应用筛选 · $total")
                }
            }
            when {
                ruleOpen -> {
                    items(TagFilterLogic.entries, key = { it.name }) { logic ->
                        WatchActionRow(logic.label, when (logic) {
                            TagFilterLogic.AND -> "需符合所有已选条件"
                            TagFilterLogic.OR -> "符合任一已选条件即可"
                            TagFilterLogic.NOT -> "排除命中任一条件的漫画"
                        }, selected = draft.tagLogic == logic) { draft = draft.copy(tagLogic = logic); ruleOpen = false }
                    }
                }
                category < 0 -> {
                    item { WatchActionRow("匹配规则", draft.tagLogic.label) { ruleOpen = true } }
                    items(labels.size) { index ->
                        WatchActionRow(labels[index], if (selections[index].isEmpty()) "不限" else "已选 ${selections[index].size} 项") {
                            category = index; query = ""; selectedOnly = false
                        }
                    }
                    item {
                        TextButton(onClick = { draft = draft.withoutTagSelections() }, enabled = total > 0,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("清空已选条件") }
                    }
                    item { Text("应用后生效；返回列表不改动原筛选。", style = MaterialTheme.typography.labelSmall) }
                }
                else -> {
                    item { WatchActionRow("查找${labels[category]}", query.ifBlank { "输入关键词" }) { queryDraft = query; queryOpen = true } }
                    item { WatchActionPair(
                        "全部", "已选 ${selections[category].size}",
                        firstEnabled = selectedOnly || query.isNotBlank(), secondEnabled = !selectedOnly,
                        onFirst = { selectedOnly = false; query = "" }, onSecond = { selectedOnly = true; query = "" },
                    ) }
                    if (loading) item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("正在补充选项，可先选择", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (error != null) item { WatchActionRow("重新加载选项", error, onClick = onReload) }
                    if (choices.isEmpty() && !loading) item {
                        Text(if (selectedOnly) "此类别未选择条件" else if (query.isNotBlank()) "没有匹配项，请更换关键词" else "暂无选项",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    items(choices, key = { it.label }) { option ->
                        WatchActionRow(option.label, if (option.count > 0) "${option.count} 部" else "已选条件",
                            selected = option.label in selections[category]) { toggle(option.label) }
                    }
                    item { Button(onClick = { category = -1; query = ""; selectedOnly = false },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("完成选择") } }
                }
            }
        }
    }
}
