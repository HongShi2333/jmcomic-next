package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.AiChatMessage
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.screens.readScreen.WatchReaderControlsOverlay
import com.par9uet.jm.ui.components.WatchLazyColumn
import com.par9uet.jm.ui.viewModel.CollectComicLocalFilter
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WatchInteractionFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun readerControlsTurnOnePageAndDisablePreviousAtStart() {
        var selected = -1
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(192.dp)) {
                    WatchReaderControlsOverlay(
                        0, 80, 1f, .5f, true, "", false, false, false, false, false, false,
                        {}, { selected = it }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("上一页").assertIsNotEnabled()
        compose.onNodeWithContentDescription("下一页").performClick()
        compose.runOnIdle { assertEquals(1, selected) }
    }

    @Test fun filterClearIsOnlyAppliedOnConfirm() {
        var applied: CollectComicLocalFilter? = null
        var commits = 0
        val original = CollectComicLocalFilter(searchText = "kept", selectedTags = setOf("A"))
        compose.setContent {
            MaterialTheme {
                WatchCollectFilterDialog(original, mapOf("A" to 5), emptyMap(), emptyMap(), emptyMap(),
                    loading = false, error = null, onReload = {}, onDismiss = {},
                    onApply = { applied = it; commits++ })
            }
        }
        compose.onNodeWithText("清空已选条件").performScrollTo().performClick()
        compose.runOnIdle { assertNull(applied); assertEquals(1, original.selectedTags.size) }
        compose.onNodeWithText("应用筛选 · 0").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, commits); assertEquals("kept", applied?.searchText); assertTrue(applied!!.selectedTags.isEmpty()) }
    }

    @Test fun assistantActionsAreHiddenUntilMoreIsTapped() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(192.dp)) {
                    WatchLazyColumn(Modifier.fillMaxSize()) {
                        item {
                            ChatMessageBubble(AiChatMessage(id = "one", role = "assistant", content = "示例回答"),
                                retryEnabled = true, onRetry = { _, _ -> }, onEditUserMessage = { _, _ -> },
                                onSwitchUserBranch = { _, _ -> }, toastManager = ToastManager())
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("重新回答").assertDoesNotExist()
        compose.onNodeWithContentDescription("回答操作").performClick()
        compose.onNodeWithText("复制全文").assertExists()
        compose.onNodeWithText("重新回答").assertExists()
    }
}
