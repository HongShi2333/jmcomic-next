package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.compose.currentBackStackEntryAsState
import com.par9uet.jm.ui.screens.LocalMainNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBarComponent() {
    val mainNavController = LocalMainNavController.current
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(
                "禁漫天堂",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            IconButton(onClick = {
                mainNavController.navigate("comicRecommend")
            }) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "每周推荐",
                )
            }
            IconButton(onClick = {
                mainNavController.navigate("comicSearch")
            }) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "搜索",
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserTopBarComponent() {
    val mainNavController = LocalMainNavController.current
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(
                "个人中心",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            IconButton(onClick = {
                mainNavController.navigate("appLocalSetting")
            }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "设置",
                )
            }
            IconButton(onClick = {
                mainNavController.navigate("download")
            }) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "下载",
                )
            }
        }
    )
}

@Composable
fun TopBarComponent() {
    val tabNavController = LocalTabNavController.current
    val backStackEntryState by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntryState?.destination?.route
    when (currentRoute) {
        "home" -> HomeTopBarComponent()
        "user" -> UserTopBarComponent()
    }
}
