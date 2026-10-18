package com.par9uet.jm.ui

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BackgroundPrivacyCoverTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun coverSurvivesRapidBackgroundingAndRestoresOnResume() {
        lateinit var cover: BackgroundPrivacyCover
        compose.runOnUiThread {
            cover = BackgroundPrivacyCover(compose.activity)
            assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            cover.show()
            assertTrue(cover.isShowing)
            // A queued foreground frame must not remove a newer background cover.
            cover.revealAfterResume(compose.activity.lifecycle)
            cover.show()
        }
        compose.waitForIdle()
        compose.runOnUiThread {
            assertTrue(cover.isShowing)
            cover.revealAfterResume(compose.activity.lifecycle)
        }
        compose.waitForIdle()
        compose.runOnUiThread {
            assertFalse(cover.isShowing)
            cover.show()
            cover.dispose()
            assertFalse(cover.isShowing)
        }
    }
}
