package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.*
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.*
import org.koin.compose.getKoin

@Composable
fun WelcomeScreen(onComplete: () -> Unit, localSettingManager: LocalSettingManager = getKoin().get()) {
    val setting by localSettingManager.localSettingState.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var saveError by remember { mutableStateOf(false) }
    var chooseSource by rememberSaveable { mutableStateOf(false) }
    fun update(block: () -> Unit) {
        runCatching(block).onSuccess { saveError = false }.onFailure { saveError = true }
    }
    fun finish() { update { localSettingManager.updateOnboardingCompleted(true); onComplete() } }
    key(step) {
        WatchScaffold("设置 ${step + 1}/4") {
            WatchLazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), rotaryEnabled = !chooseSource) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(when(step) { 0 -> "开始手表阅读"; 1 -> "选择内容来源"; 2 -> "AI 对话"; else -> "自动签到" },
                            style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                        if (step == 0) Text("在线阅读，也可缓存后离线看。", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                when(step) {
                    1 -> item {
                        WatchActionRow("数据源", when(setting.comicApiSource) { COMIC_API_SOURCE_NETWORK -> "网络源"; COMIC_API_SOURCE_MIXED -> "混合源"; else -> "内置源（推荐）" }) { chooseSource = true }
                    }
                    2 -> item { WelcomeToggle("显示 AI 入口", setting.showAiEntry) { update { localSettingManager.updateShowAiEntry(it) } } }
                    3 -> item { WelcomeToggle("登录后自动签到", setting.autoSignInEnabled) { update { localSettingManager.updateAutoSignInEnabled(it) } } }
                }
                if (saveError) item { Text("设置未保存，请重试", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                item {
                    Button(onClick = { if (step == 3) finish() else { step++; saveError = false } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (step == 3) "完成设置" else "下一步")
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        if (step > 0) TextButton(onClick = { step--; saveError = false }, modifier = Modifier.weight(1f).heightIn(min = 40.dp)) { Text("上一步", style = MaterialTheme.typography.labelMedium) }
                        TextButton(onClick = ::finish, modifier = Modifier.weight(1f).heightIn(min = 40.dp)) { Text("跳过", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }
    if (chooseSource) SelectDialog("数据源", setting.comicApiSource,
        listOf(SelectOption("内置源", COMIC_API_SOURCE_BUILTIN), SelectOption("网络源", COMIC_API_SOURCE_NETWORK), SelectOption("混合源", COMIC_API_SOURCE_MIXED)),
        onSelect = { source -> update { localSettingManager.updateComicApiSource(source) }; chooseSource = false },
        onDismissRequest = { chooseSource = false })
}

@Composable
private fun WelcomeToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Surface(onClick = { onChange(!value) }, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Switch(value, onCheckedChange = null)
        }
    }
}
