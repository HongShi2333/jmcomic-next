package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.components.WatchLazyColumn
import com.par9uet.jm.ui.components.WatchPinInput
import com.par9uet.jm.ui.components.WatchScaffold
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WatchControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun keypadShowsAllDigitsOn160DpAndSendsDirectInput() {
        var digits = ""
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(160.dp)) {
                    WatchPinInput("输入密码", 4, 0, false, { digits += it }, {})
                }
            }
        }
        for (digit in 0..9) {
            compose.onNodeWithContentDescription("数字 $digit").assertIsDisplayed().performClick()
        }
        compose.runOnIdle { assertEquals("0123456789", digits) }
    }

    @Test fun capsuleScrollsAwayWithTheList() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(192.dp)) {
                    WatchScaffold("设置", onNavigateBack = {}) {
                        WatchLazyColumn(Modifier.fillMaxSize().testTag("scrollable-controls")) {
                            items(30) { Text("选项 $it", Modifier.fillMaxWidth().height(48.dp)) }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("设置").assertIsDisplayed()
        compose.onNodeWithTag("scrollable-controls").performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithText("设置").assertIsNotDisplayed()
    }
}
