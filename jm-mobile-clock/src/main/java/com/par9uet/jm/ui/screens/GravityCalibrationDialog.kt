package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.par9uet.jm.store.GravityReadingManager
import com.par9uet.jm.ui.components.WatchLazyColumn
import com.par9uet.jm.ui.components.WatchScaffold
import com.par9uet.jm.utils.GravityStabilityTracker
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.getKoin
import kotlin.math.ceil

@Composable
internal fun GravityCalibrationDialog(onDismiss: () -> Unit, manager: GravityReadingManager = getKoin().get()) {
    var stableMillis by remember { mutableLongStateOf(0L) }
    var complete by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(manager) {
        manager.setCalibrating(true)
        onDispose { manager.setCalibrating(false) }
    }
    LaunchedEffect(attempt, lifecycle) {
        if (!manager.available) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (complete) return@repeatOnLifecycle
            stableMillis = 0
            failed = false
            val tracker = GravityStabilityTracker()
            val sample = withTimeoutOrNull(20_000L) {
                manager.samples.firstOrNull { event ->
                    val neutral = tracker.sample(event.vector, event.timestampNanos)
                    stableMillis = tracker.stableMillis
                    neutral != null
                }
            }
            if (sample == null) failed = true
            else { manager.calibrate(sample.vector); complete = true }
        }
    }
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        WatchScaffold("校准中心", onNavigateBack = onDismiss) {
            WatchLazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text(
                        when {
                            !manager.available -> "此设备没有可用的重力传感器"
                            complete -> "中心已校准"
                            failed -> "未检测到持续静止，请重试"
                            else -> "保持表盘三秒不倾斜，当前姿势将作为悬停位置。移动后重新计时。"
                        },
                        Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    )
                }
                if (manager.available && !complete && !failed) item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ceil((3_000L - stableMillis).coerceAtLeast(0) / 1000f).toInt().toString(),
                            style = MaterialTheme.typography.headlineMedium)
                        LinearProgressIndicator(progress = { (stableMillis / 3_000f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (complete) item { Text("保持此姿势暂停；前后倾斜控制方向，倾斜越大翻页越快。",
                    style = MaterialTheme.typography.bodySmall) }
                item {
                    Button(
                        onClick = { if (failed) attempt++ else onDismiss() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(if (failed) "重新校准" else if (complete) "完成" else "取消") }
                }
            }
        }
    }
}
