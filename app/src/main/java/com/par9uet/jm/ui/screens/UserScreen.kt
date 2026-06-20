package com.par9uet.jm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.par9uet.jm.R
import com.par9uet.jm.data.models.User
import com.par9uet.jm.store.RemoteSettingManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.viewModel.UserViewModel
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {},
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = MaterialTheme.colorScheme.onSurface,
            leadingIconColor = tint,
            trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        leadingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = containerColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp),
                        tint = tint
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.chevron_right_icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

@Composable
private fun DataItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UserHeader(
    user: User,
    imgHost: String
) {
    val progress = if (user.nextLevelExp > 0) {
        (user.currentLevelExp.toFloat() / user.nextLevelExp.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = "$imgHost/media/users/${user.avatar}",
                    contentDescription = "${user.username}的头像",
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Lv.${user.level} ${user.levelName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "经验",
                    value = "${user.currentLevelExp}/${user.nextLevelExp}",
                    modifier = Modifier.weight(1f)
                )
                DataItem(
                    icon = Icons.Default.Leaderboard,
                    label = "等级",
                    value = "Lv.${user.level}",
                    modifier = Modifier.weight(1f)
                )
                DataItem(
                    icon = Icons.Default.Savings,
                    label = "金币",
                    value = "${user.jCoin}",
                    modifier = Modifier.weight(1f)
                )
                DataItem(
                    icon = Icons.Default.Bookmark,
                    label = "收藏",
                    value = "${user.currentCollectCount}/${user.maxCollectCount}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LoginHeader(onLogin: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = "登录后同步收藏、历史和签到",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = onLogin) {
                Text("点击登录")
            }
        }
    }
}

@Composable
private fun MenuGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun UserScreen(
    userManager: UserManager = getKoin().get(),
    remoteSettingManager: RemoteSettingManager = getKoin().get(),
    userViewModel: UserViewModel = koinActivityViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val userState by userManager.userState.collectAsState()
    val isLogin by userManager.isLoginState.collectAsState(false)
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsState()
    val mainNavController = LocalMainNavController.current

    fun checkLoginThenDo(onDo: () -> Unit) {
        if (!isLogin) {
            mainNavController.navigate("login")
            return
        }
        onDo()
    }

    PullToRefreshBox(
        isRefreshing = userState.isLoading,
        state = rememberPullToRefreshState(),
        onRefresh = {
            val user = userState.data
            if (isLogin && user != null) {
                coroutineScope.launch {
                    userManager.autoLogin(user.username, user.password)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val user = userState.data
            if (isLogin && user != null) {
                UserHeader(
                    user = user,
                    imgHost = remoteSetting.imgHost
                )
            } else {
                LoginHeader {
                    mainNavController.navigate("login")
                }
            }

            MenuGroup {
                MenuItem(
                    icon = Icons.Default.Download,
                    label = "下载中心",
                    onClick = { mainNavController.navigate("download") }
                )
                MenuDivider()
                MenuItem(
                    icon = Icons.Default.Bookmarks,
                    label = "我的收藏",
                    onClick = {
                        checkLoginThenDo { mainNavController.navigate("userCollectComic") }
                    }
                )
                MenuDivider()
                MenuItem(
                    icon = Icons.Default.History,
                    label = "历史观看",
                    onClick = {
                        checkLoginThenDo { mainNavController.navigate("userHistoryComic") }
                    }
                )
                MenuDivider()
                MenuItem(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    label = "我的评论",
                    onClick = {
                        checkLoginThenDo { mainNavController.navigate("userHistoryComment") }
                    }
                )
                MenuDivider()
                MenuItem(
                    icon = Icons.Default.CalendarMonth,
                    label = "签到",
                    onClick = {
                        checkLoginThenDo { mainNavController.navigate("sign") }
                    }
                )
            }

            MenuGroup {
                MenuItem(
                    icon = Icons.Default.Settings,
                    label = "设置",
                    onClick = { mainNavController.navigate("appLocalSetting") }
                )
                if (isLogin) {
                    MenuDivider()
                    MenuItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        label = "退出登录",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = {
                            userViewModel.logout()
                        }
                    )
                }
            }
        }
    }
}
