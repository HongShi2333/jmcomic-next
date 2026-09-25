package com.par9uet.jm.clock.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.par9uet.jm.clock.data.WatchAccount
import com.par9uet.jm.clock.ui.components.WatchActionRow
import com.par9uet.jm.clock.ui.components.WatchInfoCard
import com.par9uet.jm.clock.ui.components.WatchScreen

@Composable
fun AccountScreen(
    account: WatchAccount?,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    WatchScreen(title = "我的", onBack = onBack) {
        if (account == null) {
            item(key = "guest") {
                WatchInfoCard(
                    title = "还未登录",
                    body = "登录主项目账号后可同步收藏，并在详情页直接管理收藏状态。",
                )
            }
            item(key = "login") {
                WatchActionRow(
                    title = "登录账号",
                    subtitle = "登录后同步线上收藏",
                    icon = Icons.Default.Login,
                    onClick = onLogin,
                )
            }
        } else {
            item(key = "profile") {
                WatchInfoCard(
                    title = account.username,
                    body = buildString {
                        append("Lv.${account.level}")
                        if (account.levelName.isNotBlank()) append(" · ${account.levelName}")
                        append("\n收藏 ${account.favoriteCount}")
                        if (account.favoriteLimit > 0) append(" / ${account.favoriteLimit}")
                        append(" · J 币 ${account.coins}")
                    },
                )
            }
            item(key = "favorites") {
                WatchActionRow(
                    title = "我的收藏",
                    subtitle = "查看并打开账号收藏的作品",
                    icon = Icons.Default.Favorite,
                    onClick = onFavorites,
                )
            }
            item(key = "logout") {
                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.Icon(Icons.Default.Logout, contentDescription = null)
                    Text("退出登录")
                }
            }
        }
        item(key = "settings") {
            WatchActionRow(
                title = "手表设置",
                subtitle = "主题、界面缩放和应用锁",
                icon = Icons.Default.Settings,
                onClick = onSettings,
            )
        }
    }
}
