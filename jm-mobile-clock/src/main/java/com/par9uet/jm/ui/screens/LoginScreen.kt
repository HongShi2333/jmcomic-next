package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.par9uet.jm.ui.components.watchVerticalScroll as verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.par9uet.jm.ui.components.WatchPageScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.par9uet.jm.ui.components.WatchTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.par9uet.jm.R
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.viewModel.UserViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    userManager: UserManager = getKoin().get(),
    userViewModel: UserViewModel = koinActivityViewModel(),
) {
    val focusManager = LocalFocusManager.current
    val mainNavController = LocalMainNavController.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val isLogin by userManager.isLoginState.collectAsState(false)
    val loginState by userViewModel.loginState.collectAsState()

    // 返回：若无可返回的页面则退回主页 tab
    fun goBack() {
        if (!mainNavController.popBackStack()) {
            mainNavController.navigate("tab/home") {
                popUpTo(mainNavController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(isLogin) {
        if (isLogin) {
            mainNavController.navigate("tab/user") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    fun sanitizeAscii(value: String): String = value.filter { it.code in 0..127 }

    fun toLogin() {
        if (username.isBlank() || password.isBlank()) return
        userViewModel.login(username, password)
    }

    com.par9uet.jm.ui.components.WatchScaffold(title = "登录", onNavigateBack = ::goBack) {
        com.par9uet.jm.ui.components.WatchLazyColumn(Modifier.fillMaxSize().imePadding()) {
            item { Text("登录账号后可使用收藏、评论和签到", style = MaterialTheme.typography.bodySmall) }
            item {
                OutlinedTextField(value = username, onValueChange = { username = sanitizeAscii(it) }, label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }))
            }
            item {
                OutlinedTextField(value = password, onValueChange = { password = sanitizeAscii(it) }, label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); toLogin() }))
            }
            if (loginState.isError && !loginState.errorMsg.isNullOrBlank()) item {
                Text(loginState.errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            item {
                Button(onClick = { focusManager.clearFocus(); toLogin() },
                    enabled = !loginState.isLoading && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    if (loginState.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(if (loginState.isLoading) "登录中" else "登录")
                }
            }
            item { TextButton(onClick = ::goBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("取消") } }
        }
    }
}
