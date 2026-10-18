package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class WatchCacheProgressFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun progressUpdatesOnTheSameScreenAndHidesSpeedWhenPaused() {
        val progress = mutableStateOf(.1f)
        val status = mutableStateOf("downloading")
        val stage = mutableStateOf("下载第 2/20 张图片")
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(160.dp)) {
                    WatchCacheProgress(progress.value, status.value, stage.value, 1024f)
                }
            }
        }
        compose.onNodeWithText("缓存中 · 10%").assertIsDisplayed()
        compose.onNodeWithText("下载第 2/20 张图片").assertIsDisplayed()
        compose.runOnIdle { progress.value = .5f; stage.value = "下载第 10/20 张图片" }
        compose.onNodeWithText("缓存中 · 50%").assertIsDisplayed()
        compose.onNodeWithText("下载第 10/20 张图片").assertIsDisplayed()
        compose.runOnIdle { status.value = "paused" }
        compose.onNodeWithText("已暂停 · 50%").assertIsDisplayed()
        compose.onNodeWithText("保存 1.0 KB/s").assertDoesNotExist()
    }
}
