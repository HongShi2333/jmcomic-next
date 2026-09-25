package com.par9uet.jm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.APP_LOCK_TYPE_PASSWORD
import com.par9uet.jm.data.models.APP_LOCK_TYPE_PATTERN
import com.par9uet.jm.data.models.APP_LOCK_METHOD_BIOMETRIC
import com.par9uet.jm.data.models.APP_LOCK_RULE_ANY
import com.par9uet.jm.data.models.APP_LOCK_RULE_REQUIRED
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.SelectDialog
import com.par9uet.jm.ui.components.SelectOption
import com.par9uet.jm.utils.biometricCapabilities
import org.koin.compose.getKoin

@Composable
fun AppLockSettingScreen(
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val context = LocalContext.current
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val biometricCaps = remember(context) { biometricCapabilities(context) }
    val fingerprintAvailable = biometricCaps.canAuthenticate && biometricCaps.hasFingerprintHardware
    val faceAvailable = biometricCaps.canAuthenticate && biometricCaps.hasFaceHardware

    // 密码/图案是否已设置
    val hasPassword by remember(localSetting) {
        derivedStateOf { localSetting.appLockPassword.isNotEmpty() }
    }
    val hasPattern by remember(localSetting) {
        derivedStateOf { localSetting.appLockPattern.isNotEmpty() }
    }
    val hasDuressPassword = localSetting.appLockDuressPassword.isNotEmpty()
    val hasDuressPattern = localSetting.appLockDuressPattern.isNotEmpty()
    val hasDuressMethod = hasDuressPassword || hasDuressPattern
    val hasBiometric = localSetting.appLockFingerprintEnabled || localSetting.appLockFaceEnabled
    val hasAnyMethod by remember(hasPassword, hasPattern, hasBiometric) {
        derivedStateOf { hasPassword || hasPattern || hasBiometric }
    }
    val enabledMethods = buildList {
        if (hasPassword) add(APP_LOCK_TYPE_PASSWORD)
        if (hasPattern) add(APP_LOCK_TYPE_PATTERN)
        if (hasBiometric) add(APP_LOCK_METHOD_BIOMETRIC)
    }

    fun setMethodRequired(method: String, required: Boolean) {
        val next = if (required) {
            (localSetting.appLockRequiredMethods + method).distinct()
        } else {
            localSetting.appLockRequiredMethods - method
        }
        if (next.isNotEmpty()) localSettingManager.updateAppLockRequiredMethods(next)
    }

    // 弹窗状态
    var showPasswordLengthDialog by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showSetPatternDialog by remember { mutableStateOf(false) }
    var showSetDuressPasswordDialog by remember { mutableStateOf(false) }
    var showSetDuressPatternDialog by remember { mutableStateOf(false) }
    // 设置密码时的临时长度（仅在选择完长度后弹出输入框时使用）
    var pendingPasswordLength by remember { mutableStateOf(localSetting.appLockPasswordLength) }

    // 关闭密码时需要清理相关状态
    fun disablePassword() {
        localSettingManager.updateAppLockPassword("")
        // 调整解锁模式
        val newMode = when {
            hasPattern -> APP_LOCK_UNLOCK_MODE_PATTERN
            else -> APP_LOCK_UNLOCK_MODE_PASSWORD
        }
        localSettingManager.updateAppLockUnlockMode(newMode)
        // 若应用锁仍开启且无任何解锁方式，则关闭应用锁
        if (!hasPattern && !hasBiometric && localSetting.appLockEnabled) {
            localSettingManager.updateAppLockEnabled(false)
        }
    }

    // 关闭图案时需要清理相关状态
    fun disablePattern() {
        localSettingManager.updateAppLockPattern("")
        val newMode = when {
            hasPassword -> APP_LOCK_UNLOCK_MODE_PASSWORD
            else -> APP_LOCK_UNLOCK_MODE_PATTERN
        }
        localSettingManager.updateAppLockUnlockMode(newMode)
        if (!hasPassword && !hasBiometric && localSetting.appLockEnabled) {
            localSettingManager.updateAppLockEnabled(false)
        }
    }

    CommonScaffold(title = "应用锁") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: 设置解锁方式
            item {
                SettingsSection(title = "设置解锁方式") {
                    // 密码开关
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Key,
                        title = "密码",
                        value = hasPassword,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // 先选择密码长度，再弹出输入框
                                pendingPasswordLength = localSetting.appLockPasswordLength
                                showPasswordLengthDialog = true
                            } else {
                                disablePassword()
                            }
                        }
                    )
                    // 密码已设置时显示长度修改入口
                    if (hasPassword) {
                        SettingsRow(
                            icon = Icons.Rounded.Key,
                            title = "密码长度",
                            value = "${localSetting.appLockPasswordLength} 位"
                        ) {
                            pendingPasswordLength = localSetting.appLockPasswordLength
                            showPasswordLengthDialog = true
                        }
                    }
                    // 图案锁开关
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Gesture,
                        title = "图案锁",
                        value = hasPattern,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showSetPatternDialog = true
                            } else {
                                disablePattern()
                            }
                        }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Fingerprint,
                        title = "指纹",
                        value = localSetting.appLockFingerprintEnabled,
                        enabled = fingerprintAvailable,
                        valueText = when {
                            !fingerprintAvailable -> "系统未录入或不支持指纹"
                            localSetting.appLockFingerprintEnabled -> "已启用"
                            else -> "未启用"
                        },
                        onCheckedChange = { enabled ->
                            localSettingManager.updateAppLockFingerprintEnabled(enabled)
                            if (!enabled && !hasPassword && !hasPattern && !localSetting.appLockFaceEnabled) {
                                localSettingManager.updateAppLockEnabled(false)
                            }
                        },
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Face,
                        title = "面容",
                        value = localSetting.appLockFaceEnabled,
                        enabled = faceAvailable,
                        valueText = when {
                            !faceAvailable -> "系统未录入或不支持面容"
                            localSetting.appLockFaceEnabled -> "已启用"
                            else -> "未启用"
                        },
                        onCheckedChange = { enabled ->
                            localSettingManager.updateAppLockFaceEnabled(enabled)
                            if (!enabled && !hasPassword && !hasPattern && !localSetting.appLockFingerprintEnabled) {
                                localSettingManager.updateAppLockEnabled(false)
                            }
                        },
                    )
                }
            }

            if (hasAnyMethod) {
                item {
                    SettingsSection(title = "通过规则") {
                        listOf(
                            APP_LOCK_RULE_ANY to "任一已启用方式通过即可",
                            APP_LOCK_RULE_REQUIRED to "必须通过指定的全部方式",
                        ).forEach { (rule, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = localSetting.appLockUnlockRule == rule,
                                        onClick = {
                                            localSettingManager.updateAppLockUnlockRule(rule)
                                            if (rule == APP_LOCK_RULE_REQUIRED && localSetting.appLockRequiredMethods.isEmpty()) {
                                                localSettingManager.updateAppLockRequiredMethods(enabledMethods)
                                            }
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = localSetting.appLockUnlockRule == rule,
                                    onClick = {
                                        localSettingManager.updateAppLockUnlockRule(rule)
                                        if (rule == APP_LOCK_RULE_REQUIRED && localSetting.appLockRequiredMethods.isEmpty()) {
                                            localSettingManager.updateAppLockRequiredMethods(enabledMethods)
                                        }
                                    }
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                        if (localSetting.appLockUnlockRule == APP_LOCK_RULE_REQUIRED) {
                            if (hasPassword) SettingsSwitchRow(
                                icon = Icons.Rounded.Key,
                                title = "强制通过密码",
                                value = APP_LOCK_TYPE_PASSWORD in localSetting.appLockRequiredMethods,
                                onCheckedChange = { setMethodRequired(APP_LOCK_TYPE_PASSWORD, it) },
                            )
                            if (hasPattern) SettingsSwitchRow(
                                icon = Icons.Rounded.Gesture,
                                title = "强制通过图形",
                                value = APP_LOCK_TYPE_PATTERN in localSetting.appLockRequiredMethods,
                                onCheckedChange = { setMethodRequired(APP_LOCK_TYPE_PATTERN, it) },
                            )
                            if (hasBiometric) SettingsSwitchRow(
                                icon = Icons.Rounded.Fingerprint,
                                title = "强制通过系统生物识别",
                                value = APP_LOCK_METHOD_BIOMETRIC in localSetting.appLockRequiredMethods,
                                valueText = "Android 系统统一验证已启用的指纹/面容",
                                onCheckedChange = { setMethodRequired(APP_LOCK_METHOD_BIOMETRIC, it) },
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "胁迫密码") {
                    Text(
                        text = "输入胁迫凭据后进入电子木鱼页面，当前进程内不会返回主应用。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Key,
                        title = "胁迫数字密码",
                        value = hasDuressPassword,
                        enabled = hasPassword,
                        valueText = when {
                            !hasPassword -> "请先设置应用锁密码"
                            hasDuressPassword -> "已设置，与应用锁密码同为 ${localSetting.appLockPasswordLength} 位"
                            else -> "未设置"
                        },
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showSetDuressPasswordDialog = true
                            } else {
                                localSettingManager.updateAppLockDuressPassword("")
                            }
                        },
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Gesture,
                        title = "胁迫图案",
                        value = hasDuressPattern,
                        enabled = hasPattern,
                        valueText = when {
                            !hasPattern -> "请先设置应用锁图案"
                            hasDuressPattern -> "已设置"
                            else -> "未设置"
                        },
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showSetDuressPatternDialog = true
                            } else {
                                localSettingManager.updateAppLockDuressPattern("")
                            }
                        },
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Lock,
                        title = "启用胁迫密码",
                        value = localSetting.appLockDuressEnabled,
                        enabled = hasDuressMethod,
                        valueText = when {
                            !hasDuressMethod -> "请先设置胁迫密码或胁迫图案"
                            localSetting.appLockDuressEnabled -> "已启用"
                            else -> "未启用"
                        },
                        onCheckedChange = { enabled ->
                            localSettingManager.updateAppLockDuressEnabled(enabled)
                        },
                    )
                }
            }

            // Section 3: 启用应用锁
            item {
                SettingsSection(title = "启用") {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Lock,
                        title = "启用应用锁",
                        value = localSetting.appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (hasAnyMethod) {
                                    localSettingManager.updateAppLockEnabled(true)
                                }
                                // 没有任何解锁方式时不允许开启（弹窗提示由下方 hint 处理）
                            } else {
                                localSettingManager.updateAppLockEnabled(false)
                            }
                        }
                    )
                    if (!hasAnyMethod) {
                        Text(
                            text = "请先设置至少一种解锁方式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 密码长度选择对话框
        if (showPasswordLengthDialog) {
            val lengthOptions = remember {
                (4..8).map { SelectOption("$it 位", it.toString()) }
            }
            SelectDialog(
                title = "密码长度",
                value = pendingPasswordLength.toString(),
                selectOptionList = lengthOptions,
                onSelect = { value ->
                    pendingPasswordLength = value.toIntOrNull()?.toInt() ?: 4
                    showPasswordLengthDialog = false
                    showSetPasswordDialog = true
                },
                onDismissRequest = { showPasswordLengthDialog = false }
            )
        }

        // 设置密码弹窗
        if (showSetPasswordDialog) {
            SetAppLockPasswordDialog(
                lockType = APP_LOCK_TYPE_PASSWORD,
                passwordLength = pendingPasswordLength,
                onConfirm = { pwd ->
                    localSettingManager.updateAppLockPasswordLength(pendingPasswordLength)
                    localSettingManager.updateAppLockPassword(pwd)
                    showSetPasswordDialog = false
                },
                onDismiss = {
                    showSetPasswordDialog = false
                }
            )
        }

        // 设置图案弹窗
        if (showSetPatternDialog) {
            SetAppLockPasswordDialog(
                lockType = APP_LOCK_TYPE_PATTERN,
                onConfirm = { pattern ->
                    localSettingManager.updateAppLockPattern(pattern)
                    showSetPatternDialog = false
                },
                onDismiss = {
                    showSetPatternDialog = false
                }
            )
        }

        if (showSetDuressPasswordDialog) {
            SetAppLockPasswordDialog(
                lockType = APP_LOCK_TYPE_PASSWORD,
                passwordLength = localSetting.appLockPasswordLength,
                validate = { password ->
                    if (password == localSetting.appLockPassword) {
                        "胁迫密码不能与应用锁密码相同"
                    } else if (password.length != localSetting.appLockPasswordLength) {
                        "胁迫密码必须为 ${localSetting.appLockPasswordLength} 位"
                    } else {
                        null
                    }
                },
                onConfirm = { password ->
                    localSettingManager.updateAppLockDuressPassword(password)
                    showSetDuressPasswordDialog = false
                },
                onDismiss = {
                    showSetDuressPasswordDialog = false
                }
            )
        }

        if (showSetDuressPatternDialog) {
            SetAppLockPasswordDialog(
                lockType = APP_LOCK_TYPE_PATTERN,
                validate = { pattern ->
                    if (pattern == localSetting.appLockPattern) {
                        "胁迫图案不能与应用锁图案相同"
                    } else {
                        null
                    }
                },
                onConfirm = { pattern ->
                    localSettingManager.updateAppLockDuressPattern(pattern)
                    showSetDuressPatternDialog = false
                },
                onDismiss = {
                    showSetDuressPatternDialog = false
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    SettingsBaseRow(
        icon = icon,
        title = title,
        value = value,
        onClick = onClick,
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    value: Boolean,
    enabled: Boolean = true,
    valueText: String = if (value) "已设置" else "未设置",
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsBaseRow(
        icon = icon,
        title = title,
        value = valueText,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!value) },
        trailingContent = {
            Switch(
                checked = value,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun SettingsBaseRow(
    icon: ImageVector,
    title: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        leadingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = icon,
                        contentDescription = null
                    )
                }
            }
        },
        headlineContent = { Text(text = title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    )
}
