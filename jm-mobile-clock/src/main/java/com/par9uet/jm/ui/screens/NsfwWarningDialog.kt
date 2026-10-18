package com.par9uet.jm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.par9uet.jm.ui.components.WatchAlertDialog as AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * NSFW 内容警告弹窗。
 * 进入应用时（应用锁解锁之后）展示，提示用户选择合适的地点观看。
 * 勾选"不再显示此提示"并确认后，下次不再弹出。
 */
@Composable
fun NsfwWarningDialog(
    onAccept: (dontShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "\u5185\u5bb9\u8b66\u544a",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "\u672c\u8f6f\u4ef6\u542b\u6709NSFW\u5185\u5bb9\uff0c\u8bf7\u9009\u62e9\u5408\u9002\u7684\u5730\u70b9\u89c2\u770b\u3002",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it }
                    )
                    Text(
                        text = "\u4e0d\u518d\u663e\u793a\u6b64\u63d0\u793a",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAccept(dontShowAgain) }) {
                Text("\u786e\u5b9a")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("\u53d6\u6d88")
            }
        }
    )
}
