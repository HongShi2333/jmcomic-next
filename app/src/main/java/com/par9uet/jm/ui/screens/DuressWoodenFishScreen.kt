package com.par9uet.jm.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.par9uet.jm.R
import kotlinx.coroutines.delay
import com.par9uet.jm.store.LocalSettingManager
import org.koin.compose.getKoin
import kotlin.math.min

/** 只存在于当前进程的胁迫页状态；应用进程重启后自动恢复为 false。 */
object DuressSession {
    var active by mutableStateOf(false)
}

/**
 * 胁迫凭据命中后的隔离页面。页面不提供返回入口，只有进程重启后才会重新进入应用锁。
 * 布局只保留木鱼和功德计数，避免引用页中的 Logo、作者和自动敲等无关组件。
 */
@Composable
fun DuressWoodenFishScreen(
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val context = LocalContext.current
    val localSetting by localSettingManager.localSettingState.collectAsState()
    var count by remember { mutableLongStateOf(localSetting.woodenFishCount.coerceAtLeast(0L)) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var striking by remember { mutableStateOf(false) }
    val fishScale by animateFloatAsState(
        targetValue = if (striking) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "wooden-fish-scale"
    )
    val soundPool = remember(context) {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    var soundId by remember { mutableIntStateOf(0) }

    DisposableEffect(soundPool) {
        soundId = soundPool.load(context, R.raw.wooden_fish_sound, 1)
        onDispose { soundPool.release() }
    }

    LaunchedEffect(localSetting.woodenFishCount) {
        count = localSetting.woodenFishCount.coerceAtLeast(0L)
    }

    LaunchedEffect(striking) {
        if (striking) {
            delay(120L)
            striking = false
        }
    }

    fun strike() {
        if (count < Long.MAX_VALUE) count += 1L
        localSettingManager.updateWoodenFishCount(count)
        striking = true
        if (localSetting.woodenFishSoundEnabled && soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
        if (localSetting.woodenFishVibrationEnabled) vibrateOnce(context)
    }

    // 胁迫页不能通过系统返回键回到主应用。
    BackHandler(enabled = true) {}

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val fishWidth = if (maxWidth >= 600.dp) {
                minOf(maxWidth * 0.48f, 420.dp)
            } else {
                minOf(maxWidth * 0.84f, 340.dp)
            }
            val fishHeight = fishWidth * 0.8f
            val fishBodyColor = MaterialTheme.colorScheme.onBackground
            val fishHoleColor = MaterialTheme.colorScheme.background

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "功德",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    Box(
                        modifier = Modifier
                            .size(fishWidth, fishHeight)
                            .scale(fishScale)
                            .clickable(onClick = ::strike),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawWoodenFish(
                                bodyColor = fishBodyColor,
                                holeColor = fishHoleColor
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "木鱼设置"
                    )
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("木鱼设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WoodenFishSettingRow(
                        title = "声音",
                        checked = localSetting.woodenFishSoundEnabled,
                        onCheckedChange = localSettingManager::updateWoodenFishSoundEnabled
                    )
                    WoodenFishSettingRow(
                        title = "震动",
                        checked = localSetting.woodenFishVibrationEnabled,
                        onCheckedChange = localSettingManager::updateWoodenFishVibrationEnabled
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("完成") }
            }
        )
    }
}

@Composable
private fun WoodenFishSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun vibrateOnce(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(32L, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(32L)
    }
}

private fun DrawScope.drawWoodenFish(bodyColor: Color, holeColor: Color) {
    val width = size.width
    val height = size.height
    val fish = Path().apply {
        moveTo(width * 0.43f, height * 0.06f)
        cubicTo(width * 0.63f, height * 0.01f, width * 0.86f, height * 0.12f, width * 0.94f, height * 0.37f)
        cubicTo(width * 0.97f, height * 0.48f, width * 0.96f, height * 0.58f, width * 0.94f, height * 0.66f)
        cubicTo(width * 0.78f, height * 0.69f, width * 0.70f, height * 0.69f, width * 0.61f, height * 0.70f)
        cubicTo(width * 0.58f, height * 0.84f, width * 0.47f, height * 0.91f, width * 0.34f, height * 0.88f)
        cubicTo(width * 0.20f, height * 0.86f, width * 0.08f, height * 0.78f, width * 0.04f, height * 0.67f)
        cubicTo(width * 0.00f, height * 0.54f, width * 0.06f, height * 0.46f, width * 0.13f, height * 0.37f)
        cubicTo(width * 0.18f, height * 0.30f, width * 0.16f, height * 0.18f, width * 0.24f, height * 0.12f)
        cubicTo(width * 0.29f, height * 0.08f, width * 0.36f, height * 0.07f, width * 0.43f, height * 0.06f)
        close()
    }
    drawPath(fish, color = bodyColor)

    val holeRadius = min(width, height) * 0.145f
    val holeCenter = Offset(width * 0.67f, height * 0.48f)
    drawCircle(color = holeColor, radius = holeRadius, center = holeCenter)
    drawCircle(
        color = bodyColor.copy(alpha = 0.55f),
        radius = holeRadius,
        center = holeCenter,
        style = Stroke(width = min(width, height) * 0.018f)
    )
}
