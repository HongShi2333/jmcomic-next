package com.par9uet.jm.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.par9uet.jm.R
import com.par9uet.jm.ui.components.WatchCompactHeader
import com.par9uet.jm.ui.components.WatchLazyColumn
import com.par9uet.jm.ui.components.WatchScaffold
import com.par9uet.jm.ui.components.watchIsRound
import com.par9uet.jm.ui.components.watchSafePadding
import com.par9uet.jm.ui.components.watchAppsColumns
import com.par9uet.jm.ui.components.watchPressFeedback

@Composable
internal fun WatchAppsMenu(
    onDismiss: () -> Unit,
    onEmergencyLock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHomeMenu: () -> Unit,
    onNavigate: (String) -> Unit,
    showAi: Boolean,
    active: Boolean = true,
    gravityEnabled: Boolean = false,
    onCalibrateGravity: () -> Unit = {},
) {
    BackHandler(enabled = active, onBack = onDismiss)
    val configuration = LocalConfiguration.current
    val columns = watchAppsColumns(configuration.screenWidthDp.toFloat(), configuration.fontScale)
    val entries = buildList {
        add(WatchAppMenuEntry("紧急锁定", Icons.Rounded.Lock, onEmergencyLock, isDanger = true))
        if (gravityEnabled) add(WatchAppMenuEntry("校准中心", Icons.Rounded.FilterList, onCalibrateGravity))
        add(WatchAppMenuEntry("设置", Icons.Rounded.Settings, onOpenSettings))
        add(WatchAppMenuEntry("搜索", Icons.Rounded.Search, onOpenSearch))
        add(WatchAppMenuEntry("菜单", Icons.Rounded.Menu, onOpenHomeMenu))
        add(WatchAppMenuEntry("发现", Icons.Default.Home, { onNavigate("tab/home") }))
        add(WatchAppMenuEntry("收藏", Icons.Default.Bookmark, { onNavigate("tab/collect") }))
        add(WatchAppMenuEntry("我的", Icons.Default.Person, { onNavigate("tab/user") }))
        add(WatchAppMenuEntry("缓存", Icons.Rounded.Download, { onNavigate("download") }))
        add(WatchAppMenuEntry("签到", Icons.Rounded.CalendarMonth, { onNavigate("sign") }))
        if (showAi) add(WatchAppMenuEntry("AI", Icons.Rounded.Psychology, { onNavigate("tab/ai") }))
        add(WatchAppMenuEntry("分类", Icons.Rounded.FilterList, onOpenHomeMenu))
    }

    WatchScaffold(title = "Apps", onNavigateBack = onDismiss, scrollHeader = true) {
        WatchLazyColumn(
            modifier = Modifier.fillMaxSize().testTag("watch-apps-list"),
            rotaryEnabled = active,
            userScrollEnabled = active,
            edgeScalingEnabled = true,
            edgeStartPadding = false,
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(entries.chunked(columns), key = { row -> row.joinToString("|") { it.label } }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEach { entry ->
                        WatchAppMenuItem(entry, Modifier.weight(1f), active)
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private data class WatchAppMenuEntry(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isDanger: Boolean = false,
)

@Composable
private fun WatchAppMenuItem(entry: WatchAppMenuEntry, modifier: Modifier = Modifier, active: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Surface(
            onClick = entry.onClick,
            enabled = active,
            interactionSource = source,
            modifier = Modifier.size(48.dp).watchPressFeedback(source),
            shape = CircleShape,
            color = if (entry.isDanger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (entry.isDanger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(entry.icon, contentDescription = entry.label, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            text = entry.label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
