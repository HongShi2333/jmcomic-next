package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ImageResultState
import com.par9uet.jm.ui.screens.readScreen.ReaderImageViewport
import com.par9uet.jm.ui.screens.readScreen.ReaderZoomState
import com.par9uet.jm.ui.screens.readScreen.WatchReaderControlsOverlay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WatchDetailReaderFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun zoomChangesRealImageTransformWithoutClosingControlsOrResizingImage() {
        val zoom = ReaderZoomState()
        var current = 0
        val bitmap = ImageBitmap(240, 480).also {
            androidx.compose.ui.graphics.Canvas(it).drawRect(Rect(0f, 0f, 240f, 480f), Paint().apply { color = Color.Green })
        }
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(192.dp)) {
                    ReaderImageViewport(ImageResultState.Success(bitmap, .5f), "fixture", zoom, onTap = {})
                    WatchReaderControlsOverlay(
                        currentIndex = current, pageCount = 11, scale = zoom.scale, minimumScale = zoom.minimumScale, imageReady = true,
                        chapterTitle = "", previousChapterEnabled = false, nextChapterEnabled = false, chapterListEnabled = false,
                        collectEnabled = false, collected = false, commentsEnabled = false,
                        onResume = {}, onPageSelected = { current = it }, onPreviousChapter = {}, onNextChapter = {}, onOpenChapters = {},
                        onScaleChange = zoom::setScale, onFitWidth = zoom::reset, onFitPage = zoom::fitPage,
                        onToggleCollect = {}, onComments = {}, onSettings = {}, onExit = {},
                    )
                }
            }
        }
        val before = compose.onNodeWithTag("reader-image-viewport").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("切换到缩放").performClick()
        compose.onNodeWithContentDescription("放大漫画").performClick()
        compose.runOnIdle { assertEquals(1.25f, zoom.scale, .001f) }
        compose.onNodeWithTag("reader-live-controls").assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag("reader-image-viewport").fetchSemanticsNode().boundsInRoot)
        val pixels = compose.onNodeWithTag("reader-image-viewport").captureToImage().toPixelMap()
        // The center remains the un-dimmed comic, not a dialog surface or preview thumbnail.
        val center = pixels[pixels.width / 2, pixels.height / 2]
        assertEquals(1f, center.green, .02f)
        assertEquals(0f, center.red, .02f)
        compose.onNodeWithContentDescription("缩小漫画").performClick()
        compose.runOnIdle { assertEquals(1f, zoom.scale, .001f) }
    }

    @Test fun allDetailMetadataIsDiscoverableAndTagOpensItsExistingAction() {
        var selectedTag = ""
        val comic = Comic.create(42, "一个需要完整展示而不能省略的漫画标题", listOf("作者甲")).copy(
            tagList = listOf("全彩", "中文"), roleList = listOf("角色甲"), workList = listOf("作品甲"),
            description = "完整简介", readCount = 12345, likeCount = 678, commentCount = 9,
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(192.dp)) {
                    WatchComicDetailContent(comic, null, emptySet(), {}, {}, {}, { selectedTag = it }, {}, {}, {}, {}, {},
                        cover = { Text("封面", it) })
                }
            }
        }
        val list = compose.onNodeWithTag("watch-comic-detail")
        for (text in listOf("作者甲", "全彩", "角色甲", "作品甲", "完整简介")) {
            list.performScrollToNode(hasText(text))
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        list.performScrollToNode(hasText("全彩"))
        compose.onNodeWithText("全彩").performClick()
        compose.runOnIdle { assertEquals("全彩", selectedTag) }
    }
}
