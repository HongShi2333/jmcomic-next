package com.par9uet.jm.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.par9uet.jm.R
import com.par9uet.jm.ui.components.LocalWatchMotionEnabled
import com.par9uet.jm.ui.components.WATCH_FLOATING_IDLE_ALPHA
import com.par9uet.jm.ui.components.WATCH_FLOATING_IDLE_MILLIS
import com.par9uet.jm.ui.components.WatchPoint
import com.par9uet.jm.ui.components.clampWatchFloatingPoint
import com.par9uet.jm.ui.components.projectWatchFloatingPoint
import com.par9uet.jm.ui.components.watchFloatingDockPoint
import com.par9uet.jm.ui.components.watchIsRound
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A 48dp input target with a smaller visual. Idle docking never moves the input
 * target outside the dial. Only the artwork retracts toward the rim.
 */
@Composable
internal fun WatchFloatingBall(
    doubleTapEnabled: Boolean,
    onOpenMenu: () -> Unit,
    onEmergencyLock: () -> Unit,
    visible: Boolean = true,
    interactionKey: Any? = null,
    onAnchorChanged: (Offset) -> Unit = {},
) {
    var xFraction by rememberSaveable { mutableFloatStateOf(.88f) }
    var yFraction by rememberSaveable { mutableFloatStateOf(.60f) }
    var dormant by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    val openMenu by rememberUpdatedState(onOpenMenu)
    val emergencyLock by rememberUpdatedState(onEmergencyLock)
    val anchorChanged by rememberUpdatedState(onAnchorChanged)
    val scope = rememberCoroutineScope()
    val motion = LocalWatchMotionEnabled.current
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val round = watchIsRound()
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val diameter = with(density) { 48.dp.toPx() }
        // Geometry uses the same units throughout; a small margin in physical pixels is sufficient.
        val initial = remember(w, h, round) {
            clampWatchFloatingPoint(WatchPoint(w * xFraction, h * yFraction), w, h, diameter, round)
        }
        val x = remember(w, h, round) { Animatable(initial.x) }
        val y = remember(w, h, round) { Animatable(initial.y) }
        var settleJob by remember { mutableStateOf<Job?>(null) }
        val tracker = remember { VelocityTracker() }
        var dragPoint by remember { mutableStateOf(initial) }
        val alpha by animateFloatAsState(
            if (dormant) WATCH_FLOATING_IDLE_ALPHA else 1f,
            if (motion) spring(dampingRatio = 1f, stiffness = 500f) else snap(), label = "floating-idle-alpha",
        )
        val visualScale by animateFloatAsState(
            if (pressed && motion) .92f else if (dormant) .84f else 1f,
            if (motion) spring(dampingRatio = 1f, stiffness = 800f) else snap(), label = "floating-press",
        )
        val rimShift by animateFloatAsState(
            if (dormant) with(density) { 8.dp.toPx() } * if (x.value < w / 2f) -1f else 1f else 0f,
            if (motion) spring(dampingRatio = 1f, stiffness = 700f) else snap(), label = "floating-rim-retract",
        )

        suspend fun settle(target: WatchPoint, velocity: WatchPoint = WatchPoint(0f, 0f)) = coroutineScope {
            if (motion) {
                launch { x.animateTo(target.x, spring(1f, 650f), initialVelocity = velocity.x.coerceIn(-2_000f, 2_000f)) }
                launch { y.animateTo(target.y, spring(1f, 650f), initialVelocity = velocity.y.coerceIn(-2_000f, 2_000f)) }
            } else {
                x.snapTo(target.x); y.snapTo(target.y)
            }
        }

        fun wake() {
            interaction++
            dormant = false
            settleJob?.cancel()
        }

        fun showMenu() {
            wake()
            // Report once on activation, not every animation frame to the entire NavHost.
            anchorChanged(Offset((x.value / w).coerceIn(0f, 1f), (y.value / h).coerceIn(0f, 1f)))
            openMenu()
        }

        LaunchedEffect(x, y) {
            snapshotFlow { Offset(x.value, y.value) }.collect { point ->
                xFraction = (point.x / w).coerceIn(0f, 1f)
                yFraction = (point.y / h).coerceIn(0f, 1f)
            }
        }
        LaunchedEffect(visible, interactionKey, windowFocused) {
            settleJob?.cancel()
            if (visible && windowFocused) wake() else { pressed = false; dragging = false }
        }
        LaunchedEffect(visible, windowFocused, interaction, pressed, dragging, w, h) {
            if (!visible || !windowFocused || pressed || dragging) return@LaunchedEffect
            delay(WATCH_FLOATING_IDLE_MILLIS)
            dormant = true
            val target = watchFloatingDockPoint(WatchPoint(x.value, y.value), w, h, diameter, round)
            settleJob?.cancel()
            settleJob = scope.launch { settle(target) }
        }

        if (visible) Box(
            modifier = Modifier
                .offset {
                    val point = clampWatchFloatingPoint(
                        if (dragging) dragPoint else WatchPoint(x.value, y.value), w, h, diameter, round,
                    )
                    IntOffset((point.x - diameter / 2f).roundToInt(), (point.y - diameter / 2f).roundToInt())
                }
                .size(48.dp)
                .testTag("watch-floating-ball")
                .semantics(mergeDescendants = true) {
                    contentDescription = "JMcomic 悬浮菜单"
                    stateDescription = if (dormant) "已贴边，轻点打开菜单" else "可拖动；闲置5秒贴边"
                    role = Role.Button
                    onClick(label = "打开 Apps 菜单") { showMenu(); true }
                    customActions = listOf(CustomAccessibilityAction("紧急锁定") { emergencyLock(); true })
                }
                .pointerInput(doubleTapEnabled, w, h) {
                    detectTapGestures(
                        onPress = {
                            wake()
                            pressed = true
                            try { tryAwaitRelease() } finally { pressed = false }
                        },
                        onTap = { showMenu() },
                        onDoubleTap = if (doubleTapEnabled) ({ emergencyLock() }) else null,
                    )
                }
                .pointerInput(w, h, round, motion) {
                    detectDragGestures(
                        onDragStart = {
                            wake()
                            dragging = true
                            dragPoint = clampWatchFloatingPoint(WatchPoint(x.value, y.value), w, h, diameter, round)
                            tracker.resetTracking()
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragPoint = clampWatchFloatingPoint(
                                WatchPoint(dragPoint.x + amount.x, dragPoint.y + amount.y), w, h, diameter, round,
                            )
                            // Track global coordinates: the ball itself moves under the pointer.
                            tracker.addPosition(change.uptimeMillis, Offset(dragPoint.x, dragPoint.y))
                        },
                        onDragEnd = {
                            interaction++
                            val velocity = tracker.calculateVelocity().let { WatchPoint(it.x, it.y) }
                            val released = dragPoint
                            val target = clampWatchFloatingPoint(
                                projectWatchFloatingPoint(released, velocity), w, h, diameter, round,
                            )
                            settleJob = scope.launch {
                                x.snapTo(released.x); y.snapTo(released.y)
                                dragging = false
                                settle(target, velocity)
                            }
                        },
                        onDragCancel = {
                            pressed = false; interaction++
                            val stopped = dragPoint
                            settleJob = scope.launch {
                                x.snapTo(stopped.x); y.snapTo(stopped.y); dragging = false
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(36.dp).graphicsLayer {
                    this.alpha = alpha; scaleX = visualScale; scaleY = visualScale; translationX = rimShift
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (dormant) 0.dp else 2.dp,
            ) {
                // Isolated bitmap, never the adaptive launcher mipmap (API26+ crash).
                Image(
                    painterResource(R.drawable.jm_floating_logo), contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape),
                )
            }
        }
    }
}
