package com.par9uet.jm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.SelectDialog
import com.par9uet.jm.ui.components.SelectOption
import org.koin.compose.getKoin

private sealed class SettingType {
    object Api : SettingType()
    object Theme : SettingType()
    object LauncherDisguise : SettingType()
    object Shunt : SettingType()
    object PrefetchCount : SettingType()
    object ReadMode : SettingType()
}

private val themeTextMap = mapOf(
    "auto" to "跟随系统",
    "light" to "日间模式",
    "dark" to "夜间模式",
)

private val launcherDisguiseTextMap = mapOf(
    "default" to "默认（JMcomic）",
    "launcher" to "系统工具",
    "launcher_round" to "相册",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSettingScreen(
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val localSetting by localSettingManager.localSettingState.collectAsState()
    var settingType by remember { mutableStateOf<SettingType>(SettingType.Api) }
    var isOpenSettingSelectDialog by remember { mutableStateOf(false) }

    fun openSetting(type: SettingType) {
        settingType = type
        isOpenSettingSelectDialog = true
    }

    CommonScaffold(title = "设置") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "外观") {
                    SettingsRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "主题",
                        value = themeTextMap[localSetting.theme].orEmpty(),
                        onClick = { openSetting(SettingType.Theme) }
                    )
                    SettingsRow(
                        icon = Icons.Rounded.VisibilityOff,
                        title = "图标伪装",
                        value = launcherDisguiseTextMap[localSetting.launcherDisguise].orEmpty(),
                        onClick = { openSetting(SettingType.LauncherDisguise) }
                    )
                }
            }
            item {
                SettingsSection(title = "连接") {
                    SettingsRow(
                        icon = Icons.Rounded.Api,
                        title = "API 接口",
                        value = localSetting.api,
                        onClick = { openSetting(SettingType.Api) }
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Image,
                        title = "图片线路",
                        value = "线路${localSetting.shunt}",
                        onClick = { openSetting(SettingType.Shunt) }
                    )
                }
            }
            item {
                SettingsSection(title = "阅读") {
                    SettingsRow(
                        icon = Icons.Rounded.Tune,
                        title = "图片预载数量",
                        value = prefetchText(localSetting.prefetchCount),
                        onClick = { openSetting(SettingType.PrefetchCount) }
                    )
                    SettingsRow(
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        title = "阅读模式",
                        value = if (localSetting.readMode == "scroll") "滚动模式" else "翻页模式",
                        onClick = { openSetting(SettingType.ReadMode) }
                    )
                }
            }
            item {
                SettingsSection(title = "其他") {
                    SettingsRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "检查更新",
                        value = "查看 GitHub Release 最新版本",
                        onClick = { mainNavController.navigate("checkUpdate") }
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "关于",
                        value = "应用版本与 GitHub 仓库",
                        onClick = { mainNavController.navigate("about") }
                    )
                }
            }
        }

        if (isOpenSettingSelectDialog) {
            val apiSelectOptionList by remember(localSetting.apiList) {
                derivedStateOf {
                    localSetting.apiList.map {
                        SelectOption(it.removePrefix("https://"), it)
                    }
                }
            }
            val themeSelectOptionList by remember(localSetting.themeList) {
                derivedStateOf {
                    localSetting.themeList.map {
                        SelectOption(themeTextMap[it].orEmpty(), it)
                    }
                }
            }
            val launcherDisguiseOptionList by remember(localSetting.launcherDisguiseList) {
                derivedStateOf {
                    localSetting.launcherDisguiseList.map {
                        SelectOption(launcherDisguiseTextMap[it].orEmpty(), it)
                    }
                }
            }
            val shuntOptionList by remember(localSetting.shuntList) {
                derivedStateOf {
                    localSetting.shuntList.map {
                        SelectOption("线路$it", it)
                    }
                }
            }
            val prefetchCountOptionList by remember {
                derivedStateOf {
                    listOf(
                        SelectOption("关闭", "0"),
                        SelectOption("预载一张", "1"),
                        SelectOption("预载两张", "2"),
                        SelectOption("预载三张", "3")
                    )
                }
            }
            val readModeOptionList by remember {
                derivedStateOf {
                    listOf(
                        SelectOption("滚动模式", "scroll"),
                        SelectOption("翻页模式", "page")
                    )
                }
            }
            SelectDialog(
                title = settingTitle(settingType),
                value = settingValue(settingType, localSetting),
                selectOptionList = when (settingType) {
                    is SettingType.Api -> apiSelectOptionList
                    is SettingType.Theme -> themeSelectOptionList
                    is SettingType.LauncherDisguise -> launcherDisguiseOptionList
                    is SettingType.Shunt -> shuntOptionList
                    is SettingType.PrefetchCount -> prefetchCountOptionList
                    is SettingType.ReadMode -> readModeOptionList
                },
                onSelect = {
                    when (settingType) {
                        is SettingType.Api -> localSettingManager.updateApi(it)
                        is SettingType.Theme -> localSettingManager.updateTheme(it)
                        is SettingType.LauncherDisguise -> localSettingManager.updateLauncherDisguise(it)
                        is SettingType.Shunt -> localSettingManager.updateShunt(it)
                        is SettingType.PrefetchCount -> localSettingManager.updatePrefetchCount(it)
                        is SettingType.ReadMode -> localSettingManager.updateReadMode(it)
                    }
                    isOpenSettingSelectDialog = false
                },
                onDismissRequest = {
                    isOpenSettingSelectDialog = false
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = icon,
                        contentDescription = null
                    )
                }
            }
        },
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

private fun prefetchText(value: Int): String {
    return when (value) {
        0 -> "关闭"
        1 -> "预载一张"
        2 -> "预载两张"
        3 -> "预载三张"
        else -> "$value 张"
    }
}

private fun settingTitle(type: SettingType): String {
    return when (type) {
        is SettingType.Api -> "切换接口"
        is SettingType.Theme -> "切换主题"
        is SettingType.LauncherDisguise -> "图标伪装"
        is SettingType.Shunt -> "线路选择"
        is SettingType.PrefetchCount -> "图片预载数量"
        is SettingType.ReadMode -> "阅读模式"
    }
}

private fun settingValue(type: SettingType, localSetting: com.par9uet.jm.data.models.LocalSetting): String {
    return when (type) {
        is SettingType.Api -> localSetting.api
        is SettingType.Theme -> localSetting.theme
        is SettingType.LauncherDisguise -> localSetting.launcherDisguise
        is SettingType.Shunt -> localSetting.shunt
        is SettingType.PrefetchCount -> "${localSetting.prefetchCount}"
        is SettingType.ReadMode -> localSetting.readMode
    }
}