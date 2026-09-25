package com.par9uet.jm.clock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.clock.R
import com.par9uet.jm.clock.data.WatchSecureStore
import com.par9uet.jm.clock.data.WatchUiPreferences
import com.par9uet.jm.clock.ui.components.WatchActionRow
import com.par9uet.jm.clock.ui.components.WatchError
import com.par9uet.jm.clock.ui.components.WatchInfoCard
import com.par9uet.jm.clock.ui.components.WatchScreen

@Composable
fun WatchAppLockScreen(
    pinLength: Int,
    secureStore: WatchSecureStore,
    onUnlocked: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val horizontalInset = dimensionResource(R.dimen.watch_content_horizontal_padding)

    fun appendDigit(digit: Char) {
        if (input.length >= pinLength) return
        val next = input + digit
        input = next
        error = null
        if (next.length == pinLength) {
            if (secureStore.matchesLockPin(next)) {
                input = ""
                onUnlocked()
            } else {
                input = ""
                error = "PIN 不正确，请重试"
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = horizontalInset, end = horizontalInset, top = 14.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(25.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("JM 已锁定", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "输入 $pinLength 位 PIN 继续",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            WatchPinDots(length = pinLength, filled = input.length)
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            WatchPinPad(
                onDigit = ::appendDigit,
                onDelete = { input = input.dropLast(1); error = null },
            )
        }
    }
}

@Composable
fun AppLockSettingsScreen(
    preferences: WatchUiPreferences,
    hasLockPin: Boolean,
    onPreferencesChange: (WatchUiPreferences) -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
    onBack: () -> Unit,
) {
    var warning by rememberSaveable { mutableStateOf<String?>(null) }
    WatchScreen(title = "应用锁", onBack = onBack) {
        item(key = "intro") {
            WatchInfoCard(
                title = "离开后重新解锁",
                body = "应用进入后台后会立即锁定。PIN 使用系统 Keystore 加密保存，不保存图片或下载文件。",
            )
        }
        item(key = "toggle") {
            WatchLockToggleRow(
                checked = preferences.appLockEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !hasLockPin) {
                        warning = "请先设置 4 位 PIN"
                    } else {
                        warning = null
                        onPreferencesChange(preferences.copy(appLockEnabled = enabled))
                    }
                },
            )
        }
        warning?.let { message ->
            item(key = "warning") { WatchError(message) }
        }
        item(key = "pin") {
            WatchActionRow(
                title = if (hasLockPin) "修改 PIN" else "设置 PIN",
                subtitle = if (hasLockPin) "更换当前 4 位解锁 PIN" else "启用应用锁前需要先设置 PIN",
                icon = if (hasLockPin) Icons.Default.LockOpen else Icons.Default.Lock,
                onClick = onSetPin,
            )
        }
        if (hasLockPin) {
            item(key = "remove_pin") {
                OutlinedButton(
                    onClick = onRemovePin,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("移除 PIN 并关闭应用锁")
                }
            }
        }
    }
}

@Composable
fun AppLockPinSetupScreen(
    hasExistingPin: Boolean,
    secureStore: WatchSecureStore,
    onPinSaved: () -> Unit,
    onBack: () -> Unit,
) {
    var stage by remember {
        mutableStateOf(if (hasExistingPin) PinSetupStage.VerifyExisting else PinSetupStage.EnterNew)
    }
    var input by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun consume(next: String) {
        if (next.length < APP_LOCK_PIN_LENGTH) {
            input = next
            return
        }
        input = ""
        when (stage) {
            PinSetupStage.VerifyExisting -> {
                if (secureStore.matchesLockPin(next)) {
                    error = null
                    stage = PinSetupStage.EnterNew
                } else {
                    error = "当前 PIN 不正确"
                }
            }
            PinSetupStage.EnterNew -> {
                error = null
                newPin = next
                stage = PinSetupStage.ConfirmNew
            }
            PinSetupStage.ConfirmNew -> {
                if (next == newPin) {
                    secureStore.saveLockPin(next)
                    onPinSaved()
                } else {
                    error = "两次 PIN 不一致，请重新设置"
                    newPin = ""
                    stage = PinSetupStage.EnterNew
                }
            }
        }
    }

    val message = when (stage) {
        PinSetupStage.VerifyExisting -> "先输入当前 4 位 PIN"
        PinSetupStage.EnterNew -> "输入新的 4 位 PIN"
        PinSetupStage.ConfirmNew -> "再次输入新 PIN 以确认"
    }

    WatchScreen(title = "设置 PIN", onBack = onBack) {
        item(key = "intro") {
            WatchInfoCard("应用锁", message)
        }
        item(key = "dots") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WatchPinDots(length = APP_LOCK_PIN_LENGTH, filled = input.length)
            }
        }
        error?.let { message ->
            item(key = "error") { WatchError(message) }
        }
        item(key = "pad") {
            WatchPinPad(
                onDigit = { digit ->
                    if (input.length < APP_LOCK_PIN_LENGTH) consume(input + digit)
                },
                onDelete = { input = input.dropLast(1); error = null },
            )
        }
    }
}

@Composable
private fun WatchLockToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val minimumTouchTarget = LocalMinimumInteractiveComponentSize.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minimumTouchTarget)
            .clickable { onCheckedChange(!checked) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("启用应用锁", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (checked) "离开应用后需要 PIN 解锁" else "当前未启用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun WatchPinDots(length: Int, filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(length) { index ->
            Surface(
                modifier = Modifier.size(11.dp),
                shape = CircleShape,
                color = if (index < filled) MaterialTheme.colorScheme.primary else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {}
        }
    }
}

@Composable
private fun WatchPinPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "delete"),
        ).forEach { keys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                keys.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.weight(1f).aspectRatio(1.3f))
                        "delete" -> WatchPinKey(onClick = onDelete) {
                            Icon(Icons.Default.Backspace, contentDescription = "删除")
                        }
                        else -> WatchPinKey(onClick = { onDigit(key.single()) }) {
                            Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.WatchPinKey(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1.3f)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

private enum class PinSetupStage {
    VerifyExisting,
    EnterNew,
    ConfirmNew,
}

private const val APP_LOCK_PIN_LENGTH = 4
