package com.par9uet.jm.ui.screens.readScreen

import android.os.SystemClock
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.par9uet.jm.store.GravityReadingManager
import com.par9uet.jm.utils.gravityReadingSpeed
import com.par9uet.jm.utils.gravityTiltDegrees
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import kotlin.math.abs
import kotlin.math.sign

@Composable
internal fun GravityReaderEffect(
    enabled: Boolean,
    sensitivity: Float,
    scrollMode: Boolean,
    listState: LazyListState,
    onTurnPage: (Int) -> Unit,
    manager: GravityReadingManager = getKoin().get(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val neutral by manager.neutral.collectAsState()
    val calibrating by manager.calibrating.collectAsState()
    val focused = LocalWindowInfo.current.isWindowFocused
    val density = LocalDensity.current.density
    val view = LocalView.current
    val turnPage by rememberUpdatedState(onTurnPage)
    LaunchedEffect(enabled, focused, calibrating, neutral, sensitivity, scrollMode, lifecycle) {
        val center = neutral ?: return@LaunchedEffect
        if (!enabled || !focused || calibrating || !manager.available) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            coroutineScope {
                var speed = 0f
                var lastSample = 0L
                launch {
                    manager.samples.collect { sample ->
                        val target = gravityReadingSpeed(
                            gravityTiltDegrees(sample.vector, center, view.display?.rotation ?: 0), sensitivity,
                        )
                        speed = if (target == 0f || sign(target) != sign(speed)) target else speed * .7f + target * .3f
                        lastSample = SystemClock.elapsedRealtime()
                    }
                }
                var accumulated = 0f
                var direction = 0f
                var previous = SystemClock.elapsedRealtime()
                while (true) {
                    delay(50L)
                    val now = SystemClock.elapsedRealtime()
                    val dt = ((now - previous) / 1_000f).coerceIn(0f, .1f)
                    previous = now
                    if (now - lastSample > 400 || now < manager.pauseUntilMillis) {
                        accumulated = 0f
                        continue
                    }
                    if (speed == 0f || direction != sign(speed)) accumulated = 0f
                    direction = sign(speed)
                    if (scrollMode) {
                        if (speed != 0f && !listState.isScrollInProgress) listState.scrollBy(speed * 140f * density * dt)
                    } else {
                        // At maximum tilt: at most 1.5 pages/s. No burst after pause or direction change.
                        accumulated += abs(speed) * 1.5f * dt
                        if (accumulated >= 1f) { turnPage(direction.toInt()); accumulated = 0f }
                    }
                }
            }
        }
    }
}
