package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.*
import org.koin.compose.getKoin

@Composable
fun BlockedTagsScreen(localSettingManager: LocalSettingManager = getKoin().get()) {
    val setting by localSettingManager.localSettingState.collectAsState()
    val templates = setting.blockedTagTemplateList
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var index by rememberSaveable { mutableStateOf<Int?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val parentActive = LocalWatchListActive.current

    CompositionLocalProvider(LocalWatchListActive provides (parentActive && panel == null)) {
        CommonScaffold("排除模板") {
            WatchLazyColumn(Modifier.fillMaxSize()) {
                item { Button(onClick = {
                    index = null; name = "排除模板 ${templates.size + 1}"; tags = emptyList(); panel = "edit"
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("新建模板") } }
                if (templates.isEmpty()) item { Text("保存常用标签组合，筛选时直接套用。", style = MaterialTheme.typography.bodySmall) }
                itemsIndexed(templates) { position, template ->
                    WatchActionRow(template.name, "${template.tagList.size} 个标签 · ${template.tagList.take(3).joinToString("、")}") {
                        index = position; name = template.name; tags = template.tagList; panel = "edit"
                    }
                }
            }
        }
    }
    when (panel) {
        "edit" -> WatchPanelDialog(if (index == null) "新建模板" else "编辑模板", onDismiss = { panel = null }) {
            item { Button(onClick = { localSettingManager.saveBlockedTagTemplate(index, name, tags); panel = null },
                enabled = tags.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保存模板") } }
            item { WatchActionRow("模板名称", name) { nameInput = name; panel = "name" } }
            item { WatchActionRow("标签 · ${tags.size}", tags.take(3).joinToString("、").ifBlank { "添加标签" }) { panel = "tags" } }
            if (index != null) item { TextButton(onClick = { panel = "delete" }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("删除模板") } }
        }
        "name" -> WatchTextEditorDialog("模板名称", nameInput, { nameInput = it }, onDismiss = { panel = "edit" },
            onConfirm = { name = nameInput.trim(); panel = "edit" }, singleLine = true, confirmEnabled = nameInput.isNotBlank())
        "tags" -> WatchTagExclusionDialog(tags, emptyList(), title = "模板标签", showTemplates = false,
            onDismiss = { panel = "edit" }, onConfirm = { tags = it; panel = "edit" }, onOpenTemplateSettings = {})
        "delete" -> WatchPanelDialog("删除模板？", onDismiss = { panel = "edit" }) {
            item { Text(name, style = MaterialTheme.typography.bodySmall) }
            item { WatchActionPair("取消", "删除", onFirst = { panel = "edit" }, onSecond = {
                index?.let(localSettingManager::removeBlockedTagTemplate); index = null; panel = null
            }) }
        }
    }
}
