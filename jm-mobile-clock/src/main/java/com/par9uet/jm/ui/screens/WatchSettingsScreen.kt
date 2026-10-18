package com.par9uet.jm.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.COMIC_API_SOURCE_BUILTIN
import com.par9uet.jm.data.models.COMIC_API_SOURCE_MIXED
import com.par9uet.jm.data.models.COMIC_API_SOURCE_NETWORK
import com.par9uet.jm.data.models.LauncherDisguise
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.WatchLazyColumn
import com.par9uet.jm.ui.components.WatchScaffold
import com.par9uet.jm.ui.components.LocalWatchListActive
import com.par9uet.jm.ui.components.LocalWatchMotionEnabled
import com.par9uet.jm.ui.components.watchPressFeedback
import org.koin.compose.getKoin

private data class WatchSettingsSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private const val SETTINGS_READING = "reading"
private const val SETTINGS_NETWORK = "network"
private const val SETTINGS_APPEARANCE = "appearance"
private const val SETTINGS_SECURITY = "security"
private const val SETTINGS_TOOLS = "tools"

@Composable
fun WatchSettingsScreen(localSettingManager: LocalSettingManager = getKoin().get()) {
    val nav = LocalMainNavController.current
    val settings by localSettingManager.localSettingState.collectAsState()
    var sectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var select by remember { mutableStateOf<SettingType?>(null) }
    var excludedTagsOpen by remember { mutableStateOf(false) }

    val sections = listOf(
        WatchSettingsSection(
            SETTINGS_READING,
            "阅读与图片",
            "阅读模式、缩放、预加载与内存占用",
            Icons.AutoMirrored.Rounded.MenuBook,
        ),
        WatchSettingsSection(
            SETTINGS_NETWORK,
            "首页与网络",
            "数据源、线路、推荐与排除标签",
            Icons.Rounded.Dns,
        ),
        WatchSettingsSection(
            SETTINGS_APPEARANCE,
            "外观与交互",
            "主题、入口、浮球与手表操作方式",
            Icons.Rounded.Palette,
        ),
        WatchSettingsSection(
            SETTINGS_SECURITY,
            "安全与隐私",
            "应用锁、通知和胁迫木鱼",
            Icons.Rounded.Lock,
        ),
        WatchSettingsSection(
            SETTINGS_TOOLS,
            "账户与工具",
            "签到、标签模板、日志、更新与关于",
            Icons.Rounded.Build,
        ),
    )
    val savedPages = rememberSaveableStateHolder()
    val motion = LocalWatchMotionEnabled.current
    val parentActive = LocalWatchListActive.current
    BackHandler(enabled = sectionId != null && select == null && !excludedTagsOpen) { sectionId = null }

    AnimatedContent(
        targetState = sectionId,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (!motion) EnterTransition.None togetherWith ExitTransition.None else {
                val direction = if (targetState == null) -1 else 1
                (fadeIn(tween(140)) + slideInHorizontally(tween(200)) { it / 10 * direction }) togetherWith
                    (fadeOut(tween(100)) + slideOutHorizontally(tween(160)) { -it / 10 * direction })
            }
        },
        label = "settings-drill-down",
    ) { page ->
    val selectedSection = sections.firstOrNull { it.id == page }
    savedPages.SaveableStateProvider(page ?: "settings-root") {
    CompositionLocalProvider(LocalWatchListActive provides (parentActive && page == sectionId && select == null && !excludedTagsOpen)) {
    WatchScaffold(
        title = selectedSection?.title ?: "设置",
        scrollHeader = true,
        onNavigateBack = {
            if (selectedSection != null) sectionId = null else nav.popBackStack()
        },
    ) {
        if (selectedSection == null) {
            WatchLazyColumn(
                modifier = Modifier.fillMaxSize(),
                edgeScalingEnabled = true,
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sections.size, key = { sections[it].id }) { index ->
                    WatchSettingsSectionCard(sections[index]) { sectionId = sections[index].id }
                }
            }
        } else {
            WatchLazyColumn(
                modifier = Modifier.fillMaxSize(),
                edgeScalingEnabled = true,
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when (selectedSection.id) {
                    SETTINGS_READING -> readingSettings(this, settings, localSettingManager, select = { select = it })
                    SETTINGS_NETWORK -> networkSettings(this, settings, localSettingManager, select = { select = it }, onOpenExcludedTags = { excludedTagsOpen = true }, nav = nav)
                    SETTINGS_APPEARANCE -> appearanceSettings(this, settings, localSettingManager, select = { select = it }, nav = nav)
                    SETTINGS_SECURITY -> securitySettings(this, settings, localSettingManager, select = { select = it }, nav = nav)
                    SETTINGS_TOOLS -> toolsSettings(this, settings, localSettingManager, nav = nav)
                }
            }
        }
    }
    }
    }
    }

    select?.let { settingType ->
        SettingSelectDialogContent(settingType, settings, localSettingManager) { select = null }
    }
    if (excludedTagsOpen) {
        HomeExcludedTagsDialog(
            settings.homeExcludedTags,
            settings.blockedTagTemplateList,
            onConfirm = { localSettingManager.updateHomeExcludedTags(it); excludedTagsOpen = false },
            onDismiss = { excludedTagsOpen = false },
            onOpenTemplateSettings = { excludedTagsOpen = false; nav.navigate("blockedTags") },
        )
    }
}

private fun readingSettings(
    scope: LazyListScope,
    settings: com.par9uet.jm.data.models.LocalSetting,
    manager: LocalSettingManager,
    select: (SettingType) -> Unit,
) = with(scope) {
    item { WatchSettingsHint("阅读页只保留适合手表的单列、低内存选项。双击图片可平滑放大，再次双击还原。") }
    item { WatchPreferenceRow("阅读模式", when (settings.readMode) { "scroll" -> "上下滚动"; "tap" -> "点击两侧翻页"; else -> "左右滑动翻页" }) { select(SettingType.ReadMode) } }
    item { WatchPreferenceRow("点击翻页区域", if (settings.readTapMode == "side") "左右两侧" else "默认区域") { select(SettingType.ReadTapMode) } }
    item { WatchPreferenceRow("图片预加载", "${settings.prefetchCount.coerceIn(0, 1)} 张，仅内存（手表最多1张）") { select(SettingType.PrefetchCount) } }
    item { WatchPreferenceRow("漫画编号副标题", if (settings.showComicIdAsSubtitle) "显示 JM 编号" else "显示作者") { manager.updateShowComicIdAsSubtitle(!settings.showComicIdAsSubtitle) } }
    item { WatchTogglePreferenceRow("漫画阅读记忆", settings.comicReadingMemoryEnabled, manager::updateComicReadingMemoryEnabled) }
    item { WatchTogglePreferenceRow("章节已读标记", settings.chapterReadingMemoryEnabled, manager::updateChapterReadingMemoryEnabled) }
    item { GravityReadingSettings(settings, manager) }
    item { WatchTogglePreferenceRow("图片内存优化", settings.readMemoryOptEnabled, manager::updateReadMemoryOptEnabled) }
    item { WatchPreferenceRow("并发解码数", "${settings.readDecodeConcurrency.coerceIn(1, 2)} 路（手表最多2路）") { select(SettingType.ReadDecodeConcurrency) } }
}

@Composable
private fun GravityReadingSettings(
    settings: com.par9uet.jm.data.models.LocalSetting,
    manager: LocalSettingManager,
    gravity: com.par9uet.jm.store.GravityReadingManager = getKoin().get(),
) {
    var sensitivity by remember(settings.gravityPageSensitivity) { mutableStateOf(settings.gravityPageSensitivity) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WatchTogglePreferenceRow("重力翻页", settings.gravityPageTurningEnabled, manager::updateGravityPageTurningEnabled,
            enabled = gravity.available)
        if (!gravity.available) WatchSettingsHint("此设备没有可用的重力传感器。")
        if (settings.gravityPageTurningEnabled && gravity.available) {
            WatchSettingsHint("先在悬浮菜单中“校准中心”。前后倾斜调节翻页方向和速度，回正暂停。")
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(10.dp)) {
                    Text("灵敏度 · ${kotlin.math.round(sensitivity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    androidx.compose.material3.Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        onValueChangeFinished = { manager.updateGravityPageSensitivity(sensitivity) },
                        valueRange = .5f..2.5f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("低灵敏度需要更大倾角", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun networkSettings(
    scope: LazyListScope,
    settings: com.par9uet.jm.data.models.LocalSetting,
    manager: LocalSettingManager,
    select: (SettingType) -> Unit,
    onOpenExcludedTags: () -> Unit,
    nav: androidx.navigation.NavHostController,
) = with(scope) {
    item { WatchPreferenceRow("数据源", when (settings.comicApiSource) { COMIC_API_SOURCE_NETWORK -> "网络 API"; COMIC_API_SOURCE_MIXED -> "混合 API"; else -> "内置 API" }) { select(SettingType.ComicApiSource) } }
    if (settings.comicApiSource == COMIC_API_SOURCE_NETWORK || settings.comicApiSource == COMIC_API_SOURCE_MIXED) {
        item { WatchPreferenceRow("API 地址", settings.api) { select(SettingType.Api) } }
        item { WatchPreferenceRow("图片线路", "线路 ${settings.shunt}") { select(SettingType.Shunt) } }
    }
    item { WatchPreferenceRow("网络解析", "DNS over HTTPS") { nav.navigate("dohSetting") } }
    if (settings.comicApiSource == COMIC_API_SOURCE_BUILTIN || settings.comicApiSource == COMIC_API_SOURCE_MIXED) {
        item { WatchTogglePreferenceRow("偏好推荐", settings.preferenceRecommendEnabled, manager::updatePreferenceRecommendEnabled) }
        if (settings.preferenceRecommendEnabled) item { WatchPreferenceRow("推荐源", if (settings.recommendSource == "network") "网络 API 推荐" else "内置 API 推荐") { select(SettingType.RecommendSource) } }
        item { WatchPreferenceRow("首页标签排除", if (settings.homeExcludedTags.isEmpty()) "未设置" else "${settings.homeExcludedTags.size} 个标签") { onOpenExcludedTags() } }
    }
}

private fun appearanceSettings(
    scope: LazyListScope,
    settings: com.par9uet.jm.data.models.LocalSetting,
    manager: LocalSettingManager,
    select: (SettingType) -> Unit,
    nav: androidx.navigation.NavHostController,
) = with(scope) {
    item { WatchSettingsHint("拖动 JM 浮球可换位置；闲置 5 秒贴边淡出。轻点打开菜单，双击可紧急锁定。") }
    item { WatchPreferenceRow("主题", when (settings.theme) { "light" -> "日间模式"; "dark" -> "夜间模式"; else -> "跟随系统" }) { select(SettingType.Theme) } }
    item { WatchPreferenceRow("配色", if (settings.colorPalettePreset == "custom") "自定义" else if (settings.colorPalettePreset == "monet") "莫奈取色" else "预设方案") { nav.navigate("colorPalette") } }
    item { WatchPreferenceRow("图标伪装", LauncherDisguise.fromId(settings.launcherDisguise).label) { select(SettingType.LauncherDisguise) } }
    item { WatchTogglePreferenceRow("显示 AI 入口", settings.showAiEntry, manager::updateShowAiEntry) }
    item { WatchTogglePreferenceRow("开屏加载动画", settings.splashLoadingEnabled, manager::updateSplashLoadingEnabled) }
    item { WatchTogglePreferenceRow("双击浮球紧急锁定", settings.emergencyDoubleTapEnabled, manager::updateEmergencyDoubleTapEnabled) }
}

private fun securitySettings(
    scope: LazyListScope,
    settings: com.par9uet.jm.data.models.LocalSetting,
    manager: LocalSettingManager,
    select: (SettingType) -> Unit,
    nav: androidx.navigation.NavHostController,
) = with(scope) {
    item { WatchSettingsHint("离开手表或切到后台时会重新触发应用锁；胁迫密码和紧急锁定会进入木鱼隔离页。") }
    item { WatchPreferenceRow("应用锁与胁迫密码", if (settings.appLockEnabled) "已启用" else "未启用") { nav.navigate("appLockSetting") } }
    item { WatchPreferenceRow("通知管理", when { !settings.showComicCacheNotification -> "关闭"; settings.showComicCacheNotificationName -> "显示漫画名"; else -> "不显示漫画名" }) { select(SettingType.NotificationManagement) } }
}

private fun toolsSettings(
    scope: LazyListScope,
    settings: com.par9uet.jm.data.models.LocalSetting,
    manager: LocalSettingManager,
    nav: androidx.navigation.NavHostController,
) = with(scope) {
    item { WatchPreferenceRow("本地缓存", "离线漫画、缓存进度与删除") { nav.navigate("download") } }
    item { WatchTogglePreferenceRow("缓存进度通知", settings.showComicCacheNotification, manager::updateShowComicCacheNotification) }
    item { WatchTogglePreferenceRow("通知显示漫画名", settings.showComicCacheNotificationName, manager::updateShowComicCacheNotificationName) }
    item { WatchTogglePreferenceRow("自动签到", settings.autoSignInEnabled, manager::updateAutoSignInEnabled) }
    item { WatchPreferenceRow("标签排除模板", "管理搜索排除模板") { nav.navigate("blockedTags") } }
    item { WatchPreferenceRow("AI 人格面具", "创建与管理") { nav.navigate("personaManager") } }
    item { WatchPreferenceRow("查看日志", "网络与错误信息") { nav.navigate("logViewer") } }
    item { WatchPreferenceRow("检查更新", "查看应用更新") { nav.navigate("checkUpdate") } }
    item { WatchPreferenceRow("关于", "版本与项目说明") { nav.navigate("about") } }
}

@Composable
private fun WatchSettingsHint(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WatchSettingsSectionCard(section: WatchSettingsSection, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).watchPressFeedback(source),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(section.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(section.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(section.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, modifier = Modifier.size(14.dp), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun WatchPreferenceRow(title: String, subtitle: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).watchPressFeedback(source),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WatchTogglePreferenceRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        interactionSource = source,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).watchPressFeedback(source),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}
