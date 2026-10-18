package com.par9uet.jm.ui.components

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

val LocalWatchMotionEnabled = staticCompositionLocalOf { true }

/** Also disables scroll-linked transforms, which do not use Compose's animation clock. */
@Composable
fun rememberWatchMotionEnabled(): Boolean {
    if (LocalInspectionMode.current) return false
    val resolver = LocalContext.current.contentResolver
    fun readEnabled() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var enabled by remember(resolver) { mutableStateOf(readEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enabled = readEnabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return enabled
}

internal fun watchMenuEnter(enabled: Boolean, origin: TransformOrigin = TransformOrigin.Center): EnterTransition =
    if (!enabled) EnterTransition.None else {
        fadeIn(tween(140)) + scaleIn(
            initialScale = .90f,
            transformOrigin = origin,
            animationSpec = spring(dampingRatio = 1f, stiffness = 700f),
        )
    }

internal fun watchMenuExit(enabled: Boolean, origin: TransformOrigin = TransformOrigin.Center): ExitTransition =
    if (!enabled) ExitTransition.None else {
        fadeOut(tween(110)) + scaleOut(targetScale = .94f, transformOrigin = origin, animationSpec = tween(160))
    }

/** Feedback begins at pointer down; the clickable Surface still owns semantics/ripple. */
@Composable
internal fun Modifier.watchPressFeedback(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val motion = LocalWatchMotionEnabled.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion) .94f else 1f,
        animationSpec = spring(dampingRatio = .86f, stiffness = 900f),
        label = "watch-press",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}
