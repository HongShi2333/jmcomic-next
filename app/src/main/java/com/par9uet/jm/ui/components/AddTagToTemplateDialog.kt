package com.par9uet.jm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.BlockedTagTemplate
import com.par9uet.jm.utils.normalizeBlockedTagList

@Composable
fun AddTagToTemplateDialog(
    tag: String,
    templates: List<BlockedTagTemplate>,
    onAddToTemplate: (Int) -> Unit,
    onCreateTemplate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var templateName by rememberSaveable(tag) { mutableStateOf(tag) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加到排除模板") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("将「$tag」添加到已有模板，或新建模板。保存后按现有过滤规则生效。")
                if (templates.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(templates) { index, template ->
                            val included = normalizeBlockedTagList(template.tagList + tag).size == template.tagList.size
                            ListItem(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable(
                                        enabled = !included,
                                        onClickLabel = "添加到${template.name}",
                                    ) { onAddToTemplate(index) },
                                headlineContent = {
                                    Text(template.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text(if (included) "已包含该标签" else "${template.tagList.size} 个标签")
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = if (included) Icons.Rounded.Check else Icons.Rounded.Add,
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("新模板名称") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = templateName.isNotBlank(),
                onClick = { onCreateTemplate(templateName.trim()) },
            ) { Text("新建并添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
