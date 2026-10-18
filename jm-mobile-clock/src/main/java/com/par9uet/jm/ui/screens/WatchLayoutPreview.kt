package com.par9uet.jm.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.ui.components.WatchLazyColumn
import com.par9uet.jm.ui.components.WatchMenuButton
import com.par9uet.jm.ui.components.WatchScaffold
import com.par9uet.jm.ui.components.WatchSearchButton
import com.par9uet.jm.ui.components.WatchTopBar
import com.par9uet.jm.ui.components.watchHomeUsesScrollableHeader
import com.par9uet.jm.ui.screens.tabScreen.WatchMenuRow
import com.par9uet.jm.ui.theme.darkScheme
import com.par9uet.jm.ui.theme.lightScheme

// These previews use the production scaffolds, list and menu rows. Only data
// and cover content are fixtures; no Koin, network, disk caches or APK is needed.
@Preview(name = "圆屏160", device = "spec:width=160dp,height=160dp,dpi=160,isRound=true")
@Preview(name = "圆屏192", device = "spec:width=192dp,height=192dp,dpi=160,isRound=true")
@Preview(name = "圆屏240", device = "spec:width=240dp,height=240dp,dpi=160,isRound=true")
@Preview(name = "圆屏240大字", device = "spec:width=240dp,height=240dp,dpi=160,isRound=true", fontScale = 1.3f)
@Preview(name = "方屏192", device = "spec:width=192dp,height=192dp,dpi=160,isRound=false")
@Preview(name = "圆屏240暗色", device = "spec:width=240dp,height=240dp,dpi=160,isRound=true",
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WatchHomeLayoutPreview() {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme, typography = com.par9uet.jm.ui.theme.AppTypography) {
        val configuration = LocalConfiguration.current
        val scrollingHeader = watchHomeUsesScrollableHeader(
            configuration.screenWidthDp.toFloat(),
            configuration.screenHeightDp.toFloat(),
            configuration.isScreenRound,
            configuration.fontScale,
        )
        val header: @Composable () -> Unit = {
            com.par9uet.jm.ui.components.WatchHomeActions(onSearch = {}, onMenu = {})
        }
        WatchScaffold(
            title = "发现",
            showTopBar = !scrollingHeader,
            titleContent = { WatchSearchButton {} },
            actions = { WatchMenuButton {} },
        ) {
            WatchHomeList(
                comics = listOf(
                    Comic.create(1, "远行的故事", listOf("示例作者")),
                    Comic.create(2, "星空里的小屋", listOf("另一位作者")),
                    Comic.create(3, "很长的漫画标题用于检查双行换行与省略", listOf("作者名字也很长")),
                ),
                selectedTitle = "最新上架",
                isLoading = false,
                header = if (scrollingHeader) header else null,
                onRefresh = {},
                onComicClick = {},
                coverContent = { _, modifier ->
                    Box(
                        modifier = modifier.clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Book, contentDescription = "预览封面占位",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                },
            )
        }
    }
}

@Preview(name = "圆屏菜单192", device = "spec:width=192dp,height=192dp,dpi=160,isRound=true")
@Preview(name = "圆屏菜单240大字", device = "spec:width=240dp,height=240dp,dpi=160,isRound=true", fontScale = 1.3f)
@Preview(name = "方屏菜单192", device = "spec:width=192dp,height=192dp,dpi=160,isRound=false")
@Composable
private fun WatchMenuLayoutPreview() {
    MaterialTheme(colorScheme = lightScheme, typography = com.par9uet.jm.ui.theme.AppTypography) {
        WatchScaffold(title = "菜单", onNavigateBack = {}) {
            WatchLazyColumn(modifier = Modifier.fillMaxSize()) {
                item { WatchMenuRow(Icons.Rounded.FilterList, "分类 · 最新上架") {} }
                item { WatchMenuRow(Icons.Rounded.Home, "发现", selected = true) {} }
                item { WatchMenuRow(Icons.Rounded.Star, "每周推荐") {} }
            }
        }
    }
}
