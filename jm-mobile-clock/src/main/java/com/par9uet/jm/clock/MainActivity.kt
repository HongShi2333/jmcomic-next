package com.par9uet.jm.clock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.par9uet.jm.clock.ui.JmClockApp

class MainActivity : ComponentActivity() {
    private var lockEpoch by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            JmClockApp(lockEpoch = lockEpoch)
        }
    }

    override fun onPause() {
        lockEpoch += 1
        super.onPause()
    }
}
