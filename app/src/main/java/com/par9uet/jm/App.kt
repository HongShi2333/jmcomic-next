package com.par9uet.jm

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.screens.AppLockScreen
import com.par9uet.jm.ui.screens.AppScreen
import com.par9uet.jm.ui.screens.DuressSession
import com.par9uet.jm.ui.screens.DuressWoodenFishScreen
import com.par9uet.jm.ui.screens.LoadingScreen
import com.par9uet.jm.ui.screens.NsfwWarningDialog
import com.par9uet.jm.ui.screens.WelcomeScreen
import com.par9uet.jm.ui.components.ComicCoverImage
import com.par9uet.jm.ui.viewModel.ComicViewModel
import com.par9uet.jm.ui.viewModel.GlobalViewModel
import com.par9uet.jm.ui.viewModel.UserViewModel
import kotlinx.coroutines.flow.first
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun App(
    globalViewModel: GlobalViewModel = koinActivityViewModel(),
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    userViewModel: UserViewModel = koinActivityViewModel(),
    toastManager: ToastManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get(),
    userManager: UserManager = getKoin().get(),
) {
    val localSetting by localSettingManager.localSettingState.collectAsState()

    // 锁定状态：初始为 true（启动即锁定），等待本地设置加载完成后根据 appLockEnabled 决定
    // 这样可以避免启动时主界面内容闪现后再显示锁屏
    var isLocked by remember { mutableStateOf(true) }
    // 冷启动尚未解锁时不创建业务页面；解锁过一次后保留页面组合，确保系统文件选择器回调不丢失。
    var hasUnlockedOnce by remember { mutableStateOf(false) }
    var settingsLoaded by remember { mutableStateOf(false) }
    // NSFW 警告本次会话是否已处理
    var sessionNsfwDismissed by remember { mutableStateOf(false) }
    // 首次启动引导
    var showOnboarding by remember { mutableStateOf(false) }
    // 应用锁通过后才启动开屏动画；首页首批数据到达后自动关闭。
    var splashStarted by remember { mutableStateOf(false) }
    var splashVisible by remember { mutableStateOf(false) }
    var splashSkipVisible by remember { mutableStateOf(false) }
    val homeComicState by comicViewModel.homeComicState.collectAsState()

    fun enterDuressMode() {
        DuressSession.active = true
        isLocked = true
        hasUnlockedOnce = false
    }

    // 应用锁配置只依赖本地存储：先读取并决定是否锁屏，再启动其余初始化任务。
    LaunchedEffect(Unit) {
        runCatching { localSettingManager.init() }
        settingsLoaded = true
        // 首次启动且未完成引导时显示欢迎页
        if (!localSettingManager.localSettingState.value.onboardingCompleted) {
            showOnboarding = true
        }
        // 仅当应用锁未开启时才解锁；若已开启，isLocked 保持 true，立即显示锁屏
        if (!localSettingManager.localSettingState.value.appLockEnabled) {
            isLocked = false
            hasUnlockedOnce = true
        }
        if (localSettingManager.localSettingState.value.nsfwWarningDismissed) {
            sessionNsfwDismissed = true
        }
        globalViewModel.init()
    }
    // 解锁后启动首页预加载过渡。超过一秒仍没有首页数据时允许用户跳过等待。
    LaunchedEffect(
        settingsLoaded,
        showOnboarding,
        hasUnlockedOnce,
        localSetting.splashLoadingEnabled,
        homeComicState.list.isNotEmpty()
    ) {
        if (!settingsLoaded || showOnboarding || !hasUnlockedOnce || splashStarted) {
            return@LaunchedEffect
        }
        splashStarted = true
        if (!localSetting.splashLoadingEnabled) return@LaunchedEffect
        splashVisible = true
        kotlinx.coroutines.delay(1000L)
        if (splashVisible && homeComicState.list.isEmpty()) {
            splashSkipVisible = true
        }
    }

    LaunchedEffect(homeComicState.list.isNotEmpty()) {
        if (homeComicState.list.isNotEmpty()) {
            splashVisible = false
            splashSkipVisible = false
        }
    }
    // 开启应用锁立即进入核验；关闭时立即解除。首次加载也由这里进行安全兜底。
    LaunchedEffect(settingsLoaded, localSetting.appLockEnabled) {
        if (settingsLoaded) {
            isLocked = localSetting.appLockEnabled
            if (!localSetting.appLockEnabled) hasUnlockedOnce = true
        }
    }

    // 自动签到：设置加载完成且自动签到开关开启时执行
    LaunchedEffect(settingsLoaded, isLocked) {
        if (!settingsLoaded || isLocked) return@LaunchedEffect
        val ls = localSettingManager.localSettingState.value
        if (!ls.autoSignInEnabled) return@LaunchedEffect
        if (!userManager.isLoginState.first()) return@LaunchedEffect
        kotlinx.coroutines.delay(2000L)
        userViewModel.getSignInData()
        val signData = kotlinx.coroutines.withTimeoutOrNull(10000L) {
            userViewModel.signDataState.first { state -> !state.isLoading }
        } ?: return@LaunchedEffect
        val todayDayOfMonth = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        val isSigned = signData.data?.dateMap?.get(todayDayOfMonth)?.isSign == true
        if (isSigned) return@LaunchedEffect
        userViewModel.signIn()
    }

    // ON_PAUSE 比 ON_STOP 更可靠：按 Home、切换应用、锁屏或打开外部系统界面时立即上锁。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, localSetting.appLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && localSetting.appLockEnabled) {
                isLocked = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 剪切板自动检测漫画编码（设置开关开启时）
    var clipboardDetectedComicId by remember { mutableStateOf<Int?>(null) }
    var clipboardDetectedComic by remember { mutableStateOf<com.par9uet.jm.data.models.Comic?>(null) }
    var clipboardDetectLoading by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var lastClipboardText by remember { mutableStateOf("") }
    var pendingNavComicId by remember { mutableStateOf(-1) }
    val mainNavController = rememberNavController()

    DisposableEffect(
        lifecycleOwner,
        localSetting.clipboardAutoDetectEnabled,
        settingsLoaded,
        isLocked,
        lastClipboardText,
    ) {
        if (!localSetting.clipboardAutoDetectEnabled || !settingsLoaded || isLocked) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val clipText = clipboardManager.getText()?.text ?: ""
                    if (clipText.isNotBlank() && clipText != lastClipboardText) {
                        lastClipboardText = clipText
                        extractClipboardComicId(clipText)?.let { comicId ->
                            // 新的候选到来时先清理旧详情，避免上一次弹窗残留。
                            clipboardDetectedComic = null
                            clipboardDetectLoading = true
                            clipboardDetectedComicId = comicId
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    // 剪切板检测后获取详情
    val comicRepository = remember { org.koin.core.context.GlobalContext.get().get<com.par9uet.jm.repository.ComicRepository>() }
    LaunchedEffect(clipboardDetectedComicId) {
        if (isLocked) return@LaunchedEffect
        val id = clipboardDetectedComicId ?: return@LaunchedEffect
        clipboardDetectLoading = true
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { comicRepository.getComicDetail(id) }.getOrNull()
        }
        when (result) {
            is com.par9uet.jm.retrofit.model.NetWorkResult.Success<*> -> {
                @Suppress("UNCHECKED_CAST")
                val comic = (result.data as com.par9uet.jm.retrofit.model.ComicDetailResponse).toComic()
                if (comic.id == id && comic.name.isNotBlank()) {
                    clipboardDetectedComic = comic
                } else {
                    clipboardDetectedComic = null
                    clipboardDetectedComicId = null
                }
            }
            else -> {
                // 剪切板内容可能是普通文本、失效编号或暂时解析失败。
                // 这些情况不应打断用户，也不应弹出没有详情的对话框。
                clipboardDetectedComic = null
                clipboardDetectedComicId = null
            }
        }
        clipboardDetectLoading = false
    }

    // 剪切板检测确认跳转
    LaunchedEffect(pendingNavComicId) {
        if (pendingNavComicId > 0) {
            mainNavController.navigate("comicDetail/$pendingNavComicId")
            pendingNavComicId = -1
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        toastManager.message.collect { text ->
            snackbarHostState.showSnackbar(
                message = text,
                actionLabel = null,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    // 应用锁优先级最高：锁定时不创建主界面、剪切板弹窗或加载页。
    val showAppLock = settingsLoaded && localSetting.appLockEnabled && isLocked
    if (DuressSession.active) {
        DuressWoodenFishScreen()
        return
    }
    if (showAppLock && !hasUnlockedOnce) {
        AppLockScreen(
            unlockMode = localSetting.appLockUnlockMode,
            correctPassword = localSetting.appLockPassword,
            correctPattern = localSetting.appLockPattern,
            passwordLength = localSetting.appLockPasswordLength,
            biometricEnabled = localSetting.appLockBiometricEnabled,
            fingerprintEnabled = localSetting.appLockFingerprintEnabled,
            faceEnabled = localSetting.appLockFaceEnabled,
            unlockRule = localSetting.appLockUnlockRule,
            requiredMethods = localSetting.appLockRequiredMethods,
            onUnlock = {
                isLocked = false
                hasUnlockedOnce = true
            },
            duressEnabled = localSetting.appLockDuressEnabled,
            duressPassword = localSetting.appLockDuressPassword,
            duressPattern = localSetting.appLockDuressPattern,
            onDuress = ::enterDuressMode,
        )
        return
    }

    // 设置尚未加载完成时保留初始化页；应用锁通过后的开屏动画以覆盖层显示，避免阻塞主页面。
    if (!settingsLoaded) {
        LoadingScreen()
        return
    }

    // 优先级：应用锁（上方已返回）> 欢迎引导 > NSFW 警告 > 主应用
    val showNsfwDialog = !showAppLock && !showOnboarding &&
            !sessionNsfwDismissed &&
            !localSetting.nsfwWarningDismissed
    // NSFW 弹窗显示时模糊背景（API 31+ 支持，低版本仅显示半透明遮罩）
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    if (showOnboarding) {
        WelcomeScreen(
            onComplete = {
                showOnboarding = false
                // 引导完成后若应用锁已启用且仍处于锁定状态，保持锁定
                // 否则解锁进入主应用
                if (!localSetting.appLockEnabled) {
                    isLocked = false
                }
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主应用内容 + Snackbar，当 NSFW 弹窗显示时模糊
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showNsfwDialog && canBlur) Modifier.blur(32.dp) else Modifier
                )
        ) {
            AppScreen(
                comicViewModel = comicViewModel,
                externalNavController = mainNavController
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
                .imePadding()
            )
        }
        if (!showAppLock && splashVisible) {
            LoadingScreen(
                showSkipButton = splashSkipVisible,
                onSkip = {
                    splashVisible = false
                    splashSkipVisible = false
                }
            )
        }
        if (showNsfwDialog) {
            NsfwWarningDialog(
                onAccept = { dontShowAgain ->
                    if (dontShowAgain) localSettingManager.dismissNsfwWarning()
                    sessionNsfwDismissed = true
                },
                onDismiss = {
                    // 本次会话关闭，下次启动再次提示
                    sessionNsfwDismissed = true
                }
            )
        }

        // 剪切板自动检测漫画编码弹窗（左侧封面小窗口 + 右侧信息）
        val detectedComic = clipboardDetectedComic
        if (detectedComic != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    clipboardDetectedComic = null
                    clipboardDetectedComicId = null
                },
                title = { androidx.compose.material3.Text("检测到漫画编码", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        // 左侧封面小窗口
                        ComicCoverImage(
                            comic = detectedComic,
                            modifier = Modifier
                                .width(96.dp)
                                .height(128.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        )
                        // 右侧信息
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text = "JM${detectedComic.id}",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            androidx.compose.material3.Text(
                                text = detectedComic.name,
                                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (detectedComic.authorList.isNotEmpty()) {
                                androidx.compose.material3.Text(
                                    text = "作者：${detectedComic.authorList.joinToString("、")}",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            if (detectedComic.tagList.isNotEmpty()) {
                                androidx.compose.material3.Text(
                                    text = "标签：${detectedComic.tagList.take(8).joinToString("、")}",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        val navId = detectedComic.id
                        clipboardDetectedComic = null
                        clipboardDetectedComicId = null
                        pendingNavComicId = navId
                    }) { androidx.compose.material3.Text("跳转详情") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        clipboardDetectedComic = null
                        clipboardDetectedComicId = null
                    }) { androidx.compose.material3.Text("取消") }
                }
            )
        }

        // 已进入过应用后，锁屏作为最顶层不透明覆盖层保留；底层页面继续存活以接收 SAF 文件回调。
        if (showAppLock) {
            AppLockScreen(
                unlockMode = localSetting.appLockUnlockMode,
                correctPassword = localSetting.appLockPassword,
                correctPattern = localSetting.appLockPattern,
                passwordLength = localSetting.appLockPasswordLength,
                biometricEnabled = localSetting.appLockBiometricEnabled,
                fingerprintEnabled = localSetting.appLockFingerprintEnabled,
                faceEnabled = localSetting.appLockFaceEnabled,
                unlockRule = localSetting.appLockUnlockRule,
                requiredMethods = localSetting.appLockRequiredMethods,
                onUnlock = {
                    isLocked = false
                    hasUnlockedOnce = true
                },
                duressEnabled = localSetting.appLockDuressEnabled,
                duressPassword = localSetting.appLockDuressPassword,
                duressPattern = localSetting.appLockDuressPattern,
                onDuress = ::enterDuressMode,
            )
        }
    }
}

private fun extractClipboardComicId(text: String): Int? {
    val trimmed = text.trim()
    val exact = Regex("(?i)^(?:jm\\s*)?(\\d{3,12})$")
        .matchEntire(trimmed)
        ?.groupValues
        ?.getOrNull(1)
    if (exact != null) return exact.toIntOrNull()

    val embedded = Regex(
        "(?i)(?:jm|album[\\s/=:]+|comic[\\s/=:]+|id[\\s:：=]+)(\\d{3,12})"
    ).find(text)?.groupValues?.getOrNull(1)
    return embedded?.toIntOrNull()
}
