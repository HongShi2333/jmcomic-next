package com.par9uet.jm

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import com.par9uet.jm.ui.BackgroundPrivacyCover

class MainActivity : FragmentActivity() {
    private lateinit var privacyCover: BackgroundPrivacyCover

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        privacyCover = BackgroundPrivacyCover(this)

        enableEdgeToEdge()
        requestNotificationPermission()
        setContent {
            AppTheme {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides Dp.Unspecified
                ) {
                    App()
                }
            }
        }
    }

    override fun onPause() {
        privacyCover.show()
        super.onPause()
    }

    override fun onPostResume() {
        super.onPostResume()
        privacyCover.revealAfterResume(lifecycle)
    }

    override fun onDestroy() {
        privacyCover.dispose()
        super.onDestroy()
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
