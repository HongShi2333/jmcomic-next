package com.par9uet.jm.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run on API 26+ (including Wear OS). Rendering the real floating ball used to
 * throw when painterResource tried to load the launcher's adaptive-icon XML.
 * No network or onboarding state is needed to exercise that crash path.
 */
@RunWith(AndroidJUnit4::class)
class WatchFloatingBallTest {
    @get:Rule
    val compose = createComposeRule()

    private var menuOpenCount = 0
    private var emergencyLockCount = 0

    private fun showFloatingBall(doubleTapEnabled: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                WatchFloatingBall(
                    doubleTapEnabled = doubleTapEnabled,
                    onOpenMenu = { menuOpenCount++ },
                    onEmergencyLock = { emergencyLockCount++ },
                )
            }
        }
    }

    @Test
    fun rendersFixedLogoAndAccessibilityClickOpensMenu() {
        showFloatingBall()
        compose.onNodeWithContentDescription("JMcomic 悬浮菜单")
            .assertIsDisplayed()
            .performClick()

        compose.runOnIdle {
            assertEquals(1, menuOpenCount)
            assertEquals(0, emergencyLockCount)
        }
    }

    @Test
    fun doubleTapLocksWithoutOpeningMenu() {
        showFloatingBall()
        compose.onNodeWithContentDescription("JMcomic 悬浮菜单")
            .assertIsDisplayed()
            .performTouchInput { doubleClick() }

        compose.runOnIdle {
            assertEquals(0, menuOpenCount)
            assertEquals(1, emergencyLockCount)
        }
    }

    @Test
    fun emergencyLockDisabledUsesImmediateSingleTap() {
        showFloatingBall(doubleTapEnabled = false)
        compose.onNodeWithContentDescription("JMcomic 悬浮菜单")
            .assertIsDisplayed()
            .performTouchInput { click() }

        compose.runOnIdle {
            assertEquals(1, menuOpenCount)
            assertEquals(0, emergencyLockCount)
        }
    }

    @Test
    fun idleFiveSecondsDocksWithoutBlockingTheNextClick() {
        compose.mainClock.autoAdvance = false
        showFloatingBall()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("watch-floating-ball").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "可拖动；闲置5秒贴边"),
        )
        compose.mainClock.advanceTimeBy(4_000)
        compose.onNodeWithTag("watch-floating-ball").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "可拖动；闲置5秒贴边"),
        )
        compose.mainClock.advanceTimeBy(2_000)
        compose.onNodeWithTag("watch-floating-ball")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "已贴边，轻点打开菜单"))
            .performClick()
        compose.runOnIdle {
            assertEquals(1, menuOpenCount)
            assertEquals(0, emergencyLockCount)
        }
    }

    @Test
    fun draggingDoesNotOpenMenuOrTriggerEmergencyLock() {
        showFloatingBall()
        compose.onNodeWithTag("watch-floating-ball").performTouchInput {
            swipe(center, center + Offset(-80f, 0f), durationMillis = 400)
        }
        compose.runOnIdle {
            assertEquals(0, menuOpenCount)
            assertEquals(0, emergencyLockCount)
        }
    }
}
