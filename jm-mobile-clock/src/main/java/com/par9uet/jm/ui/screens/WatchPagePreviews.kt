package com.par9uet.jm.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.components.*
import com.par9uet.jm.ui.theme.AppTypography
import com.par9uet.jm.ui.theme.lightScheme
import com.par9uet.jm.ui.theme.darkScheme

@Preview(name = "圆160", device = "spec:width=160dp,height=160dp,dpi=160,isRound=true")
@Preview(name = "圆192", device = "spec:width=192dp,height=192dp,dpi=160,isRound=true")
@Preview(name = "圆240", device = "spec:width=240dp,height=240dp,dpi=160,isRound=true")
@Preview(name = "方192", device = "spec:width=192dp,height=192dp,dpi=160,isRound=false")
@Preview(name = "圆240大字", device = "spec:width=240dp,height=240dp,dpi=160,isRound=true", fontScale = 2f)
@Preview(name = "圆240暗色", device = "spec:width=240dp,height=240dp,dpi=160,isRound=true", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
private annotation class WatchDialSizes

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme, typography = AppTypography, content = content)
}

// Every row below is a production component. Fixture covers contain no remote imagery.
@WatchDialSizes
@Composable
private fun WatchComicRowsPreview() = PreviewTheme {
    WatchScaffold("收藏", onNavigateBack = {}) {
        WatchLazyColumn(Modifier.fillMaxSize()) {
            item { WatchComicCard("很长的标题检查两行省略和触控", "示例作者", onClick = {}, cover = { Icon(Icons.Rounded.Book, null, Modifier.size(36.dp, 48.dp)) }) }
            item { WatchComicCard("批量选择中的漫画", "JM123456", editing = true, selected = true, onClick = {}, cover = { Icon(Icons.Rounded.Book, null, Modifier.size(36.dp, 48.dp)) }) }
        }
    }
}

@WatchDialSizes
@Composable
private fun WatchPreferencesPreview() = PreviewTheme {
    WatchScaffold("设置", onNavigateBack = {}) {
        WatchLazyColumn(Modifier.fillMaxSize()) {
            item { WatchPreferenceRow("阅读模式", "上下滚动、滑动翻页或点击翻页") {} }
            item { WatchPreferenceRow("漫画阅读记忆", "开启") {} }
            item { WatchPreferenceRow("数据源", "内置源") {} }
        }
    }
}

@WatchDialSizes
@Composable
private fun WatchReaderControlsPreview() = WatchLiveReaderPreview()

@WatchDialSizes
@Composable
private fun WatchFormDialogPreview() = PreviewTheme {
    Surface(Modifier.fillMaxSize().clip(if (watchIsRound()) CircleShape else RoundedCornerShape(0.dp))) {
        WatchDialogContent(title = { Text("编辑消息") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = "一条用于检查换行的示例消息", onValueChange = {}, label = { Text("消息内容") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
                Text("输入与确认均可滚动触达，系统字体不被压缩。", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { Button(onClick = {}, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("发送") } },
            dismissButton = { TextButton(onClick = {}, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保留草稿并返回") } })
    }
}

// Reuse the actual lock controls; no repository or fake phone keyboard.
@WatchDialSizes
@Preview(name = "PIN圆160两倍字号", device = "spec:width=160dp,height=160dp,dpi=160,isRound=true", fontScale = 2f)
@Composable
private fun WatchPinInputPreview() = PreviewTheme {
    WatchScaffold("", showTopBar = false) {
        WatchPinInput(title = "请输入密码", length = 4, entered = 2, isError = false,
            onDigit = {}, onDelete = {}, onCancel = {})
    }
}

@WatchDialSizes
@Composable
private fun WatchPatternInputPreview() = PreviewTheme {
    WatchScaffold("", showTopBar = false) {
        WatchPatternInputPage("请输入图案") {
            PatternLockInput(title = "", correctPassword = null, onUnlock = {}, modifier = Modifier.fillMaxSize())
        }
    }
}
