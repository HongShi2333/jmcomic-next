package com.par9uet.jm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.par9uet.jm.data.models.User
import com.par9uet.jm.store.RemoteSettingManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.WatchLazyColumn
import com.par9uet.jm.ui.viewModel.UserViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun WatchUserScreen(
    active: Boolean = true,
    userManager: UserManager = getKoin().get(),
    remoteSettingManager: RemoteSettingManager = getKoin().get(),
    userViewModel: UserViewModel = koinActivityViewModel(),
) {
    val mainNavController = LocalMainNavController.current
    val userState by userManager.userState.collectAsState()
    val isLogin by userManager.isLoginState.collectAsState(false)
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsState()

    WatchLazyColumn(
        modifier = Modifier.fillMaxSize(),
        rotaryEnabled = active,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
    ) {
        item {
            if (isLogin && userState.data != null) {
                WatchUserHeader(userState.data!!, remoteSetting.imgHost)
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { mainNavController.navigate("login") },
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("未登录", fontWeight = FontWeight.SemiBold)
                            Text(
                                "登录后同步收藏、历史和签到",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f),
                            )
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item {
            WatchUserSection(
                title = "我的内容",
                items = listOf(
                    Triple(Icons.Rounded.Bookmarks, "我的收藏", "userCollectComic"),
                    Triple(Icons.Rounded.History, "历史观看", "userHistoryComic"),
                    Triple(Icons.AutoMirrored.Rounded.Comment, "我的评论", "userHistoryComment"),
                    Triple(Icons.Rounded.CalendarMonth, "签到", "sign"),
                ),
                onNavigate = { route ->
                    if (isLogin) mainNavController.navigate(route) else mainNavController.navigate("login")
                },
            )
        }
        item {
            WatchUserSection(
                title = "应用",
                items = listOf(
                    Triple(Icons.Rounded.Download, "本地缓存", "download"),
                    Triple(Icons.Rounded.Settings, "设置", "appLocalSetting"),
                    Triple(Icons.Rounded.Tune, "标签排除", "blockedTags"),
                ),
                onNavigate = { mainNavController.navigate(it) },
            )
        }
        if (isLogin) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { userViewModel.logout() },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("退出登录", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchUserHeader(user: User, imgHost: String) {
    val progress = if (user.nextLevelExp > 0) {
        (user.currentLevelExp.toFloat() / user.nextLevelExp).coerceIn(0f, 1f)
    } else {
        0f
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val avatar = user.avatar.takeIf { it.startsWith("http", ignoreCase = true) }
                ?: user.avatar.takeIf { it.isNotBlank() }?.let { "$imgHost/media/users/$it" }
            AsyncImage(
                model = avatar,
                contentDescription = "头像",
                modifier = Modifier
                    .size(32.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    user.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Lv.${user.level} ${user.levelName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("收藏 ${user.currentCollectCount}", style = MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun WatchUserSection(
    title: String,
    items: List<Triple<ImageVector, String, String>>,
    onNavigate: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                items.forEach { (icon, label, route) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onNavigate(route) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
