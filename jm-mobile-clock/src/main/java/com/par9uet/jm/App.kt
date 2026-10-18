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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.par9uet.jm.ui.screens.DuressSession
import com.par9uet.jm.ui.screens.DuressWoodenFishScreen
import com.par9uet.jm.ui.screens.LoadingScreen
import com.par9uet.jm.ui.screens.NsfwWarningDialog
import com.par9uet.jm.ui.screens.WatchAppScreen
import com.par9uet.jm.ui.screens.WelcomeScreen
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
    val settingsLoaded by localSettingManager.initializedState.collectAsState()
    // NSFW 警告本次会话是否已处理
    var sessionNsfwDismissed by remember { mutableStateOf(false) }
    // 首次启动引导
    var showOnboarding by remember { mutableStateOf(false) }
    val globalInitialized by globalViewModel.initialized.collectAsState()
    // 应用锁通过后才启动开屏动画；首页首批数据到达后自动关闭。
    var splashStarted by rememberSaveable { mutableStateOf(false) }
    var splashVisible by rememberSaveable { mutableStateOf(false) }
    var splashSkipVisible by rememberSaveable { mutableStateOf(false) }
    val homeComicState by comicViewModel.homeComicState.collectAsState()

    fun enterDuressMode() {
        // 进入木鱼页时不再同时叠加应用锁；离开应用后由 ON_PAUSE 清除胁迫会话并重新锁定。
        DuressSession.active = true
        isLocked = false
        hasUnlockedOnce = true
    }

    // 应用锁配置只依赖本地存储：先读取并决定是否锁屏，再启动其余初始化任务。
    LaunchedEffect(Unit) {
        runCatching { localSettingManager.init() }
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
        // 在引导页期间提前完成全局初始化，避免点击“开始/跳过”后首次创建主页时
        // 网络拦截器、用户状态和远端配置仍处于半初始化状态。
        globalViewModel.init()
    }
    val onboardingVisible = showOnboarding || (settingsLoaded && !localSetting.onboardingCompleted)
    // 解锁后启动首页预加载过渡。超过一秒仍没有首页数据时允许用户跳过等待。
    LaunchedEffect(
        settingsLoaded,
        onboardingVisible,
        hasUnlockedOnce,
        globalInitialized,
        localSetting.splashLoadingEnabled,
        splashVisible,
        splashSkipVisible,
    ) {
        if (!settingsLoaded || onboardingVisible || !hasUnlockedOnce || !globalInitialized) {
            return@LaunchedEffect
        }
        if (!localSetting.splashLoadingEnabled) {
            splashStarted = true
            splashVisible = false
            splashSkipVisible = false
            return@LaunchedEffect
        }
        if (!splashStarted) {
            splashStarted = true
            splashVisible = true
        }
        if (splashVisible && !splashSkipVisible) {
            kotlinx.coroutines.delay(1000L)
            if (splashVisible) {
                splashSkipVisible = true
            }
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
            if (event != Lifecycle.Event.ON_PAUSE) return@LifecycleEventObserver
            if (DuressSession.active) {
                // 木鱼页不能靠返回键退出；切到后台后清掉胁迫会话，下一次进入先验证应用锁。
                DuressSession.active = false
                isLocked = localSetting.appLockEnabled
                hasUnlockedOnce = !localSetting.appLockEnabled
            } else if (localSetting.appLockEnabled) {
                isLocked = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val mainNavController = rememberNavController()

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
    if (DuressSession.active) {
        DuressWoodenFishScreen()
        return
    }

    // 设置尚未加载完成时保留初始化页；应用锁通过后的开屏动画以覆盖层显示，避免阻塞主页面。
    if (!settingsLoaded) {
        LoadingScreen()
        return
    }

    // 优先级：应用锁（上方已返回）> 欢迎引导 > NSFW 警告 > 主应用
    if (onboardingVisible) {
        WelcomeScreen(
            onComplete = {
                showOnboarding = false
                if (!localSetting.appLockEnabled) {
                    isLocked = false
                }
            }
        )
        return
    }
    if (!globalInitialized) {
        LoadingScreen()
        return
    }

    val showNsfwDialog = !showAppLock && !onboardingVisible &&
            !sessionNsfwDismissed &&
            !localSetting.nsfwWarningDismissed
    // NSFW 弹窗显示时模糊背景（API 31+ 支持，低版本仅显示半透明遮罩）
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = Modifier.fillMaxSize()) {
        // 主应用内容 + Snackbar，当 NSFW 弹窗显示时模糊
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showNsfwDialog && canBlur) Modifier.blur(32.dp) else Modifier
                )
        ) {
            WatchAppScreen(
                comicViewModel = comicViewModel,
                externalNavController = mainNavController,
                onEmergencyLock = ::enterDuressMode,
                localSettingManager = localSettingManager,
                active = !showAppLock && !showNsfwDialog && !splashVisible,
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
