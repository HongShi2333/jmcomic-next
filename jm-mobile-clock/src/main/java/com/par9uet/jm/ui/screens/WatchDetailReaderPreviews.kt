package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.data.models.ImageResultState
import com.par9uet.jm.ui.components.WatchScaffold
import com.par9uet.jm.ui.screens.readScreen.*
import com.par9uet.jm.ui.theme.AppTypography
import com.par9uet.jm.ui.theme.darkScheme

@Preview(name = "详情-圆192", device = "spec:width=192dp,height=192dp,dpi=160,isRound=true")
@Preview(name = "详情-圆240", device = "spec:width=240dp,height=240dp,dpi=160,isRound=true")
@Preview(name = "详情-方192大字", device = "spec:width=192dp,height=192dp,dpi=160,isRound=false", fontScale = 1.3f)
@Composable
private fun WatchFullDetailPreview() {
    val comic = Comic.create(42, "星空下的旅行 · 第一章", listOf("示例作者")).copy(
        tagList = listOf("全彩", "中文"), roleList = listOf("旅行者"), workList = listOf("星空之旅"),
        description = "此处使用离线示例检查简介、标签、作者与章节的信息层级。",
        likeCount = 136, readCount = 430, comicChapterList = listOf(ComicChapter(42, "出发")),
    )
    MaterialTheme(colorScheme = darkScheme, typography = AppTypography) {
        WatchScaffold("漫画详情", onNavigateBack = {}) {
            WatchComicDetailContent(comic, null, emptySet(), {}, {}, {}, {}, {}, {}, {}, {}, {},
                cover = { Surface(it, color = MaterialTheme.colorScheme.primaryContainer) { Text("封面", Modifier.padding(8.dp)) } })
        }
    }
}

@Preview(name = "实时阅读-圆160", device = "spec:width=160dp,height=160dp,dpi=160,isRound=true")
@Preview(name = "实时阅读-圆192", device = "spec:width=192dp,height=192dp,dpi=160,isRound=true")
@Preview(name = "实时阅读-圆240", device = "spec:width=240dp,height=240dp,dpi=160,isRound=true")
@Preview(name = "实时阅读-方192", device = "spec:width=192dp,height=192dp,dpi=160,isRound=false")
@Composable
internal fun WatchLiveReaderPreview() {
    val bitmap = remember {
        ImageBitmap(240, 480).also { bitmap ->
            val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
            for (row in 0..7) for (column in 0..3) {
                canvas.drawRect(Rect(column * 60f, row * 60f, (column + 1) * 60f, (row + 1) * 60f),
                    Paint().apply { color = if ((row + column) % 2 == 0) Color(0xFFD7E2F0) else Color(0xFF7192B5) })
            }
        }
    }
    val zoom = rememberReaderZoomState()
    val round = LocalConfiguration.current.isScreenRound
    SideEffect { zoom.updateRoundScreen(round) }
    MaterialTheme(colorScheme = darkScheme, typography = AppTypography) {
        Box(Modifier.fillMaxSize()) {
            ReaderImageViewport(ImageResultState.Success(bitmap, .5f), "preview", zoom, onTap = {})
            WatchReaderControlsOverlay(0, 11, zoom.scale, zoom.minimumScale, true, "离线测试网格",
                false, false, false, false, false, false, {}, {}, {}, {}, {},
                zoom::setScale, zoom::reset, zoom::fitPage, {}, {}, {}, {})
        }
    }
}
