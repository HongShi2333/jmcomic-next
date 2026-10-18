package com.par9uet.jm.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.par9uet.jm.ui.components.watchPatternCenters
import com.par9uet.jm.ui.components.watchPatternHit
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import com.par9uet.jm.ui.components.watchVerticalScroll as verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Fingerprint
import com.par9uet.jm.ui.components.WatchBasicAlertDialog as BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.par9uet.jm.data.models.APP_LOCK_TYPE_PASSWORD
import com.par9uet.jm.data.models.APP_LOCK_TYPE_PATTERN
import com.par9uet.jm.data.models.APP_LOCK_METHOD_BIOMETRIC
import com.par9uet.jm.data.models.APP_LOCK_RULE_REQUIRED
import com.par9uet.jm.utils.authenticateWithBiometrics
import com.par9uet.jm.utils.canUseBiometricAuth
import com.par9uet.jm.utils.findFragmentActivity
import kotlinx.coroutines.launch
import kotlin.math.hypot

// 解锁模式常量
const val APP_LOCK_UNLOCK_MODE_PASSWORD = "password"
const val APP_LOCK_UNLOCK_MODE_PATTERN = "pattern"
const val APP_LOCK_UNLOCK_MODE_BOTH = "both"

/**
 * 应用锁全屏遮罩。当应用锁开启且处于锁定状态时，覆盖整个应用界面。
 *
 * 支持 [unlockMode]：
 * - [APP_LOCK_UNLOCK_MODE_PASSWORD]：仅密码解锁
 * - [APP_LOCK_UNLOCK_MODE_PATTERN]：仅图案解锁
 * - [APP_LOCK_UNLOCK_MODE_BOTH]：先密码后图案，两步都成功才解锁
 */
@Composable
fun AppLockScreen(
    unlockMode: String,
    correctPassword: String,
    correctPattern: String,
    passwordLength: Int,
    biometricEnabled: Boolean,
    fingerprintEnabled: Boolean,
    faceEnabled: Boolean,
    unlockRule: String,
    requiredMethods: List<String>,
    onUnlock: () -> Unit,
    duressEnabled: Boolean = false,
    duressPassword: String = "",
    duressPattern: String = "",
    onDuress: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val canUseBiometric = (biometricEnabled || fingerprintEnabled || faceEnabled) &&
        activity != null && canUseBiometricAuth(context)
    val enabledMethods = remember(correctPassword, correctPattern, canUseBiometric) {
        buildList {
            if (correctPassword.isNotBlank()) add(APP_LOCK_TYPE_PASSWORD)
            if (correctPattern.isNotBlank()) add(APP_LOCK_TYPE_PATTERN)
            if (canUseBiometric) add(APP_LOCK_METHOD_BIOMETRIC)
        }
    }
    val methodsToPass = remember(unlockRule, requiredMethods, enabledMethods, unlockMode) {
        if (unlockRule == APP_LOCK_RULE_REQUIRED) {
            requiredMethods.filter { it in enabledMethods }.ifEmpty {
                if (unlockMode == APP_LOCK_UNLOCK_MODE_BOTH) {
                    listOf(APP_LOCK_TYPE_PASSWORD, APP_LOCK_TYPE_PATTERN).filter { it in enabledMethods }
                } else enabledMethods
            }
        } else enabledMethods
    }
    val activeDuressPassword = duressPassword.takeIf { duressEnabled && it.isNotBlank() }
    val activeDuressPattern = duressPattern.takeIf { duressEnabled && it.isNotBlank() }
    val completedMethods = remember(methodsToPass) { mutableStateListOf<String>() }
    var duressDetected by remember(methodsToPass) { mutableStateOf(false) }
    var selectedMethod by remember(methodsToPass) {
        mutableStateOf(methodsToPass.firstOrNull { it != APP_LOCK_METHOD_BIOMETRIC } ?: APP_LOCK_METHOD_BIOMETRIC)
    }

    fun completeMethod(method: String, isDuress: Boolean = false) {
        val shouldEnterDuress = duressDetected || isDuress
        if (isDuress) duressDetected = true
        if (unlockRule != APP_LOCK_RULE_REQUIRED) {
            if (shouldEnterDuress) onDuress() else onUnlock()
            return
        }
        if (method !in completedMethods) completedMethods += method
        val remaining = methodsToPass.filter { it !in completedMethods }
        if (remaining.isEmpty()) {
            if (shouldEnterDuress) onDuress() else onUnlock()
        } else {
            selectedMethod = remaining.first()
        }
    }

    var optionsOpen by remember { mutableStateOf(false) }
    val inputMethods = methodsToPass.filter { it != APP_LOCK_METHOD_BIOMETRIC && it !in completedMethods }
    val activeInputMethod = selectedMethod.takeIf { it in inputMethods } ?: inputMethods.firstOrNull()
    val canChoose = inputMethods.size > 1 || APP_LOCK_METHOD_BIOMETRIC in methodsToPass
    androidx.compose.runtime.CompositionLocalProvider(com.par9uet.jm.ui.components.LocalWatchListActive provides !optionsOpen) {
        when (activeInputMethod) {
            APP_LOCK_TYPE_PASSWORD -> PasswordLockInput(title = "请输入密码", correctPassword = correctPassword, passwordLength = passwordLength,
                additionalCorrectPasswords = activeDuressPassword?.let { setOf(it) } ?: emptySet(), onUnlock = {},
                onMoreOptions = if (canChoose) ({ optionsOpen = true }) else null,
                onVerified = { entered -> completeMethod(APP_LOCK_TYPE_PASSWORD, isDuress = entered == activeDuressPassword) })
            APP_LOCK_TYPE_PATTERN -> com.par9uet.jm.ui.components.WatchPatternInputPage("图案${if (canChoose) " · 切换方式" else ""}", if (canChoose) ({ optionsOpen = true }) else null) {
                PatternLockInput(title = "", correctPassword = correctPattern, modifier = Modifier.fillMaxSize(),
                    additionalCorrectPasswords = activeDuressPattern?.let { setOf(it) } ?: emptySet(), onUnlock = {},
                    onVerified = { entered -> completeMethod(APP_LOCK_TYPE_PATTERN, isDuress = entered == activeDuressPattern) })
            }
            else -> com.par9uet.jm.ui.components.WatchScaffold("应用锁") {
                com.par9uet.jm.ui.components.WatchLazyColumn(Modifier.fillMaxSize()) {
                    item { Text("请使用已设置的系统验证方式") }
                    item { TextButton(onClick = { optionsOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("选择验证方式") } }
                }
            }
        }
    }
    if (optionsOpen) com.par9uet.jm.ui.components.WatchAlertDialog(onDismissRequest = { optionsOpen = false }, title = { Text("验证方式") }, text = {
        Column {
            Text(if (unlockRule == APP_LOCK_RULE_REQUIRED) "已通过 ${completedMethods.size} / ${methodsToPass.size} 项" else "任选一种已设置的方式")
            inputMethods.forEach { method -> TextButton(onClick = { selectedMethod = method; optionsOpen = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (method == APP_LOCK_TYPE_PASSWORD) "密码" else "图案") } }
            if (canUseBiometric && APP_LOCK_METHOD_BIOMETRIC in methodsToPass && APP_LOCK_METHOD_BIOMETRIC !in completedMethods) {
                TextButton(onClick = { authenticateWithBiometrics(activity = activity!!, title = "解锁 JM", subtitle = "验证已录入的指纹或面容", onSuccess = { optionsOpen = false; completeMethod(APP_LOCK_METHOD_BIOMETRIC) }) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("系统指纹 / 面容验证") }
            }
        }
    }, confirmButton = { TextButton(onClick = { optionsOpen = false }) { Text("返回输入") } })

}

/**
 * 密码锁输入：[passwordLength] 位数字 PIN，滑动/表冠选数、确认输入，保留验证及进度。
 */
@Composable
fun PasswordLockInput(
    title: String,
    correctPassword: String?,
    onUnlock: () -> Unit,
    onInputComplete: ((String) -> Unit)? = null,
    passwordLength: Int = 4,
    modifier: Modifier = Modifier,
    additionalCorrectPasswords: Set<String> = emptySet(),
    onVerified: ((String) -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onMoreOptions: (() -> Unit)? = null,
) {
    val len = passwordLength.coerceIn(4, 8)
    val scope = rememberCoroutineScope()
    val digits = remember { mutableStateListOf<Int>() }
    var isError by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        scope.launch {
            shakeOffset.animateTo(0f)
            val values = listOf(-10f, 10f, -8f, 8f, -5f, 5f, 0f)
            for (v in values) {
                shakeOffset.snapTo(v)
                kotlinx.coroutines.delay(40)
            }
        }
    }

    fun pushDigit(d: Int) {
        if (isError) {
            isError = false
            digits.clear()
        }
        if (digits.size >= len) return
        digits.add(d)
        if (digits.size == len) {
            val pwd = digits.joinToString("")
            if (onInputComplete != null) {
                onInputComplete(pwd)
                digits.clear()
            } else if (pwd == correctPassword || pwd in additionalCorrectPasswords) {
                if (onVerified != null) onVerified(pwd) else onUnlock()
            } else {
                isError = true
                triggerShake()
            }
        }
    }

    fun deleteDigit() {
        if (isError) {
            isError = false
            digits.clear()
            return
        }
        if (digits.isNotEmpty()) digits.removeAt(digits.size - 1)
    }

    com.par9uet.jm.ui.components.WatchPinInput(title, len, digits.size, isError, ::pushDigit, ::deleteDigit,
        modifier = modifier, onCancel = onCancel, onOptions = onMoreOptions)

}

@Composable
private fun NumericKeypad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val keyColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface

    val columns = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 200) 2 else 3
    val keys = (1..9).toList() + 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        keys.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { row.forEach { KeyButton(it, keyColor, contentColor, onDigit) } }
        }
        DeleteButton(keyColor, contentColor, onDelete)
    }
}

@Composable
private fun KeyButton(
    digit: Int,
    bgColor: Color,
    contentColor: Color,
    onDigit: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onDigit(digit) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun DeleteButton(
    bgColor: Color,
    contentColor: Color,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onDelete() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Backspace,
            contentDescription = "删除",
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * 图案锁输入：3x3 九宫格手势锁。
 * 图案为点序号(0-8)按连接顺序拼接的字符串，例如 "01246"。
 */
@Composable
fun PatternLockInput(
    title: String,
    correctPassword: String?,
    onUnlock: () -> Unit,
    onInputComplete: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    additionalCorrectPasswords: Set<String> = emptySet(),
    onVerified: ((String) -> Unit)? = null
) {
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentTouch by remember { mutableStateOf<Offset?>(null) }
    var isError by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val dial = androidx.compose.ui.platform.LocalConfiguration.current
    var gridSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val accent = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        scope.launch {
            shakeOffset.animateTo(0f)
            val values = listOf(-10f, 10f, -8f, 8f, -5f, 5f, 0f)
            for (v in values) {
                shakeOffset.snapTo(v)
                kotlinx.coroutines.delay(40)
            }
        }
    }

    fun tryAddDot(position: Offset) {
        val index = watchPatternHit(position.x, position.y, gridSize.width, gridSize.height)
        if (index != null && index !in selectedDots) selectedDots.add(index)
    }

    fun complete() {
        val pattern = selectedDots.joinToString("")
        if (pattern.isBlank()) return
        currentTouch = null
        if (onInputComplete != null) {
            onInputComplete(pattern)
            selectedDots.clear()
        } else if (pattern == correctPassword || pattern in additionalCorrectPasswords) {
            if (onVerified != null) onVerified(pattern) else onUnlock()
        } else {
            isError = true
            triggerShake()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (title.isNotBlank()) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .testTag("watch-pattern-grid")
                .onSizeChanged { gridSize = androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat()) }
                .offset(x = shakeOffset.value.dp)
                .pointerInput(gridSize) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        isError = false
                        selectedDots.clear()
                        currentTouch = down.position
                        tryAddDot(down.position)
                        down.consume()
                        var released = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } > 1) break
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.isConsumed) break
                                currentTouch = change.position
                                tryAddDot(change.position)
                                change.consume()
                                if (!change.pressed) { released = true; break }
                            }
                        } finally {
                            if (released) complete() else { currentTouch = null; selectedDots.clear() }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPattern(
                    selectedDots = selectedDots.toList(),
                    currentTouch = currentTouch,
                    isError = isError, accent = accent, errorColor = errorColor, emptyColor = emptyColor,
                )
            }
        }
    }
}

private fun computeDotCenters(size: androidx.compose.ui.geometry.Size): List<Offset> {
    return watchPatternCenters(size.width, size.height).map { Offset(it.x, it.y) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPattern(
    selectedDots: List<Int>,
    currentTouch: Offset?,
    isError: Boolean,
    accent: Color,
    errorColor: Color,
    emptyColor: Color,
) {
    val centers = computeDotCenters(size)
    val lineColor = if (isError) errorColor else accent
    val dotFillColor = if (isError) errorColor else accent
    val dotEmptyColor = emptyColor

    // 连接线
    if (selectedDots.isNotEmpty()) {
        for (i in 0 until selectedDots.size - 1) {
            val start = centers[selectedDots[i]]
            val end = centers[selectedDots[i + 1]]
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        // 从最后一个点到当前触摸位置的引导线
        currentTouch?.let { touch ->
            val last = centers[selectedDots.last()]
            drawLine(
                color = lineColor.copy(alpha = 0.5f),
                start = last,
                end = touch,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
            )
        }
    }

    // 绘制 9 个点
    for (index in 0..8) {
        val center = centers[index]
        val selected = index in selectedDots
        if (selected) {
            drawCircle(
                color = dotFillColor,
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = dotFillColor,
                radius = 9.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        } else {
            drawCircle(
                color = dotEmptyColor,
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}

/**
 * 设置应用锁密码弹窗：要求用户输入两次相同密码/图案后确认。
 * 密码长度通过 [passwordLength] 指定（仅对密码类型生效）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetAppLockPasswordDialog(
    lockType: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    passwordLength: Int = 4,
    validate: (String) -> String? = { null }
) {
    var firstInput by remember { mutableStateOf<String?>(null) }
    var secondInput by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val title = if (firstInput == null) {
        "请设置${if (lockType == APP_LOCK_TYPE_PATTERN) "图案" else "密码"}"
    } else {
        "请再次输入以确认"
    }

    fun accept(value: String) {
        if (firstInput == null) { firstInput = value; error = null }
        else if (value == firstInput) {
            val validationError = validate(value)
            if (validationError == null) onConfirm(value)
            else { error = validationError; firstInput = null; secondInput = null }
        } else { error = "两次输入不一致，请重新设置"; firstInput = null; secondInput = null }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        if (lockType == APP_LOCK_TYPE_PATTERN) com.par9uet.jm.ui.components.WatchPatternInputPage("取消 · ${error ?: title}", onDismiss) {
            PatternLockInput(title = "", correctPassword = null, onUnlock = {}, onInputComplete = ::accept, modifier = Modifier.fillMaxSize())
        } else PasswordLockInput(title = error ?: title, correctPassword = null, onUnlock = {}, passwordLength = passwordLength, onInputComplete = ::accept, onCancel = onDismiss)
    }
}
