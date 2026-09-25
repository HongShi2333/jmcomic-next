package com.par9uet.jm.clock.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.par9uet.jm.clock.data.WatchAccount
import com.par9uet.jm.clock.data.WatchRepository
import com.par9uet.jm.clock.ui.components.WatchError
import com.par9uet.jm.clock.ui.components.WatchInfoCard
import com.par9uet.jm.clock.ui.components.WatchScreen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repository: WatchRepository,
    onBack: () -> Unit,
    onLoggedIn: (WatchAccount, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val minimumTouchTarget = LocalMinimumInteractiveComponentSize.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun submit() {
        if (isSubmitting || username.isBlank() || password.isBlank()) return
        focusManager.clearFocus()
        scope.launch {
            isSubmitting = true
            error = null
            repository.login(username, password).fold(
                onSuccess = { account -> onLoggedIn(account, password) },
                onFailure = { failure -> error = failure.message ?: "登录失败，请稍后重试" },
            )
            isSubmitting = false
        }
    }

    WatchScreen(title = "登录账号", onBack = onBack) {
        item(key = "login_intro") {
            WatchInfoCard(
                title = "同步主项目账号",
                body = "登录后可查看和管理线上收藏；凭据使用系统 Keystore 加密保存，不保存漫画图片。",
            )
        }
        item(key = "username") {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.filter { char -> char.code in 0..127 } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("用户名") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
        item(key = "password") {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.filter { char -> char.code in 0..127 } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        }
        error?.let { message ->
            item(key = "error") { WatchError(message) }
        }
        item(key = "submit") {
            Button(
                onClick = ::submit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minimumTouchTarget),
                enabled = !isSubmitting && username.isNotBlank() && password.isNotBlank(),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp), strokeWidth = 2.dp)
                    Text("正在登录…")
                } else {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Text("登录")
                }
            }
        }
    }
}
