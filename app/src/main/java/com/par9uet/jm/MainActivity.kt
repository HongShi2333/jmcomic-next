package com.par9uet.jm

import android.Manifest
import android.graphics.Color
import android.view.KeyEvent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.content.pm.PackageManager
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import com.par9uet.jm.ui.theme.AppTheme
import com.par9uet.jm.ui.theme.LocalAppMotion
import com.par9uet.jm.ui.theme.rememberAppMotionPolicy
import com.par9uet.jm.utils.VolumeKeyPageTurnDispatcher

class MainActivity : FragmentActivity() {
    private var privacyCover: View? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeKeyPageTurnDispatcher.handle(event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeKeyPageTurnDispatcher.handle(event)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep task previews and screenshots from exposing the last visible page.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        requestNotificationPermission()
        setContent {
            AppTheme {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                    LocalAppMotion provides rememberAppMotionPolicy(),
                ) {
                    App()
                }
            }
        }
    }

    override fun onPause() {
        coverAppWhenLeavingForeground()
        super.onPause()
    }

    override fun onUserLeaveHint() {
        coverAppWhenLeavingForeground()
        super.onUserLeaveHint()
    }

    override fun onStop() {
        coverAppWhenLeavingForeground()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        removeAppPrivacyCover()
    }

    override fun onDestroy() {
        removeAppPrivacyCover()
        super.onDestroy()
    }

    private fun coverAppWhenLeavingForeground() {
        val decorView = window.decorView as? ViewGroup ?: return
        if (privacyCover?.parent === decorView) return

        val cover = View(this).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        decorView.addView(
            cover,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        cover.bringToFront()
        decorView.invalidate()
        privacyCover = cover
    }

    private fun removeAppPrivacyCover() {
        privacyCover?.let { cover ->
            (cover.parent as? ViewGroup)?.removeView(cover)
        }
        privacyCover = null
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1001
        )
    }
}
