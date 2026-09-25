package com.par9uet.jm.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            // 平板模式下限制内容宽度，避免键盘区域过宽导致需要滚动
            val maxContentWidth = if (maxWidth >= 600.dp) 420.dp else maxWidth
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (unlockRule == APP_LOCK_RULE_REQUIRED) {
                        "需通过 ${methodsToPass.size} 项验证（已完成 ${completedMethods.size} 项）"
                    } else "通过任一已启用方式即可解锁",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (APP_LOCK_METHOD_BIOMETRIC in methodsToPass && APP_LOCK_METHOD_BIOMETRIC !in completedMethods) {
                    FilledTonalButton(
                        onClick = {
                            authenticateWithBiometrics(
                                activity = activity!!,
                                title = "解锁 JM",
                                subtitle = "由系统验证已录入的${when {
                                    fingerprintEnabled && faceEnabled -> "指纹或面容"
                                    faceEnabled -> "面容"
                                    else -> "指纹"
                                }}",
                        onSuccess = { completeMethod(APP_LOCK_METHOD_BIOMETRIC) },
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(when {
                            fingerprintEnabled && faceEnabled -> "使用指纹或面容"
                            faceEnabled -> "使用面容"
                            else -> "使用指纹"
                        })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val inputMethods = methodsToPass.filter {
                    it != APP_LOCK_METHOD_BIOMETRIC && it !in completedMethods
                }
                if (inputMethods.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        inputMethods.forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(if (method == APP_LOCK_TYPE_PASSWORD) "密码" else "图形") },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                val activeInputMethod = selectedMethod.takeIf { it in inputMethods }
                    ?: inputMethods.firstOrNull()

                if (activeInputMethod == APP_LOCK_TYPE_PATTERN) {
                    PatternLockInput(
                        title = "请绘制图案",
                        correctPassword = correctPattern,
                        additionalCorrectPasswords = activeDuressPattern?.let { setOf(it) } ?: emptySet(),
                        onUnlock = {},
                        onVerified = { entered ->
                            completeMethod(
                                APP_LOCK_TYPE_PATTERN,
                                isDuress = entered == activeDuressPattern
                            )
                        }
                    )
                } else if (activeInputMethod == APP_LOCK_TYPE_PASSWORD) {
                    PasswordLockInput(
                        title = "请输入密码",
                        correctPassword = correctPassword,
                        passwordLength = passwordLength,
                        additionalCorrectPasswords = activeDuressPassword?.let { setOf(it) } ?: emptySet(),
                        onUnlock = {},
                        onVerified = { entered ->
                            completeMethod(
                                APP_LOCK_TYPE_PASSWORD,
                                isDuress = entered == activeDuressPassword
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 密码锁输入：[passwordLength] 位数字 PIN，带数字键盘与圆点进度，错误时抖动。
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
    onVerified: ((String) -> Unit)? = null
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.offset(x = shakeOffset.value.dp)
        ) {
            repeat(len) { index ->
                val filled = index < digits.size
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isError) MaterialTheme.colorScheme.error
                            else if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        NumericKeypad(
            onDigit = ::pushDigit,
            onDelete = ::deleteDigit
        )
    }
}

@Composable
private fun NumericKeypad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val keyColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            KeyButton(1, keyColor, contentColor, onDigit)
            KeyButton(2, keyColor, contentColor, onDigit)
            KeyButton(3, keyColor, contentColor, onDigit)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            KeyButton(4, keyColor, contentColor, onDigit)
            KeyButton(5, keyColor, contentColor, onDigit)
            KeyButton(6, keyColor, contentColor, onDigit)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            KeyButton(7, keyColor, contentColor, onDigit)
            KeyButton(8, keyColor, contentColor, onDigit)
            KeyButton(9, keyColor, contentColor, onDigit)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(modifier = Modifier.size(68.dp))
            KeyButton(0, keyColor, contentColor, onDigit)
            DeleteButton(keyColor, contentColor, onDelete)
        }
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
            .size(68.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onDigit(digit) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            fontSize = 26.sp,
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
            .size(68.dp)
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
    val hitRadiusPx = with(density) { 28.dp.toPx() }
    // 图案锁区域固定为 280dp，提前计算像素尺寸以避免依赖 PointerInputScope.size 类型不一致
    val gridSizePx = with(density) { 280.dp.toPx() }
    val gridSize = androidx.compose.ui.geometry.Size(gridSizePx, gridSizePx)
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
        val dotCenters = computeDotCenters(gridSize)
        for (index in 0 until 9) {
            if (index in selectedDots) continue
            val center = dotCenters[index]
            if (hypot(position.x - center.x, position.y - center.y) <= hitRadiusPx) {
                selectedDots.add(index)
                break
            }
        }
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
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = shakeOffset.value.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isError = false
                            selectedDots.clear()
                            currentTouch = offset
                            tryAddDot(offset)
                        },
                        onDrag = { change, _ ->
                            currentTouch = change.position
                            tryAddDot(change.position)
                            change.consume()
                        },
                        onDragEnd = { complete() },
                        onDragCancel = {
                            currentTouch = null
                            selectedDots.clear()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPattern(
                    selectedDots = selectedDots.toList(),
                    currentTouch = currentTouch,
                    isError = isError
                )
            }
        }
    }
}

private fun computeDotCenters(size: androidx.compose.ui.geometry.Size): List<Offset> {
    val w = size.width
    val h = size.height
    val xs = listOf(w * 0.2f, w * 0.5f, w * 0.8f)
    val ys = listOf(h * 0.2f, h * 0.5f, h * 0.8f)
    val centers = ArrayList<Offset>(9)
    for (row in 0..2) {
        for (col in 0..2) {
            centers.add(Offset(xs[col], ys[row]))
        }
    }
    return centers
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPattern(
    selectedDots: List<Int>,
    currentTouch: Offset?,
    isError: Boolean
) {
    val centers = computeDotCenters(size)
    val accent = Color(0xFF6750A4)
    val lineColor = if (isError) Color.Red else accent
    val dotFillColor = if (isError) Color.Red else accent
    val dotEmptyColor = Color(0xFF9E9E9E)

    // 连接线
    if (selectedDots.isNotEmpty()) {
        for (i in 0 until selectedDots.size - 1) {
            val start = centers[selectedDots[i]]
            val end = centers[selectedDots[i + 1]]
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 8f
            )
        }
        // 从最后一个点到当前触摸位置的引导线
        currentTouch?.let { touch ->
            val last = centers[selectedDots.last()]
            drawLine(
                color = lineColor.copy(alpha = 0.5f),
                start = last,
                end = touch,
                strokeWidth = 8f
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
                radius = 18f,
                center = center
            )
            drawCircle(
                color = dotFillColor,
                radius = 9f,
                center = center,
                style = Stroke(width = 6f)
            )
        } else {
            drawCircle(
                color = dotEmptyColor,
                radius = 9f,
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

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (lockType == APP_LOCK_TYPE_PATTERN) {
                    PatternLockInput(
                        title = "",
                        correctPassword = null,
                        onUnlock = {},
                        onInputComplete = { pattern ->
                            if (firstInput == null) {
                                firstInput = pattern
                                error = null
                            } else {
                                secondInput = pattern
                                if (secondInput == firstInput) {
                                    val validationError = validate(pattern)
                                    if (validationError == null) {
                                        onConfirm(pattern)
                                    } else {
                                        error = validationError
                                        firstInput = null
                                        secondInput = null
                                    }
                                } else {
                                    error = "两次输入不一致，请重新设置"
                                    firstInput = null
                                    secondInput = null
                                }
                            }
                        }
                    )
                } else {
                    PasswordLockInput(
                        title = "",
                        correctPassword = null,
                        onUnlock = {},
                        passwordLength = passwordLength,
                        onInputComplete = { pwd ->
                            if (firstInput == null) {
                                firstInput = pwd
                                error = null
                            } else {
                                secondInput = pwd
                                if (secondInput == firstInput) {
                                    val validationError = validate(pwd)
                                    if (validationError == null) {
                                        onConfirm(pwd)
                                    } else {
                                        error = validationError
                                        firstInput = null
                                        secondInput = null
                                    }
                                } else {
                                    error = "两次输入不一致，请重新设置"
                                    firstInput = null
                                    secondInput = null
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    }
}
