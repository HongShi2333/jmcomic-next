package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class SelectOption(val label: String, val value: String)

@Composable
fun SelectDialog(title: String, value: String?, selectOptionList: List<SelectOption> = listOf(), onSelect: (String) -> Unit = {}, onDismissRequest: () -> Unit = {}) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        WatchScaffold(title, onNavigateBack = onDismissRequest) {
            WatchLazyColumn(Modifier.fillMaxSize()) {
                items(selectOptionList.size, key = { selectOptionList[it].value }) { index ->
                    val option = selectOptionList[index]
                    Surface(onClick = { onSelect(option.value) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.large, color = if (value == option.value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(option.label, modifier = Modifier.weight(1f))
                            RadioButton(selected = value == option.value, onClick = null)
                        }
                    }
                }
                item { TextButton(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("取消") } }
            }
        }
    }
}
