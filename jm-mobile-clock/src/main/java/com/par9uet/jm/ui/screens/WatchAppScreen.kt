package com.par9uet.jm.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.ui.components.LocalWatchListActive
import com.par9uet.jm.ui.components.LocalWatchMotionEnabled
import com.par9uet.jm.ui.components.watchMenuEnter
import com.par9uet.jm.ui.components.watchMenuExit
import com.par9uet.jm.ui.screens.readScreen.ComicReadScreen
import com.par9uet.jm.ui.screens.tabScreen.WatchTabScreen
import com.par9uet.jm.ui.viewModel.ComicViewModel
import com.par9uet.jm.utils.EXTRA_NAVIGATE_ROUTE
import com.par9uet.jm.utils.deserializeExcludedTags
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun WatchAppScreen(
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    externalNavController: NavHostController? = null,
    onEmergencyLock: () -> Unit = {},
    localSettingManager: LocalSettingManager = getKoin().get(),
    downloadManager: DownloadManager = getKoin().get(),
    active: Boolean = true,
) {
    val mainNavController = externalNavController ?: rememberNavController()
    val context = LocalContext.current
    val localSetting by localSettingManager.localSettingState.collectAsState()
    var appsMenuOpen by rememberSaveable { mutableStateOf(false) }
    var calibrationOpen by remember { mutableStateOf(false) }
    var readerControlsVisible by remember { mutableStateOf(false) }
    var openHomeMenuRequest by rememberSaveable { mutableIntStateOf(0) }
    var floatingAnchor by remember { mutableStateOf(Offset(.88f, .60f)) }
    val menuTransition = remember { MutableTransitionState(false) }
    menuTransition.targetState = appsMenuOpen && active
    val menuVisible = menuTransition.currentState || menuTransition.targetState || calibrationOpen
    val motion = LocalWatchMotionEnabled.current
    val entry by mainNavController.currentBackStackEntryAsState()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val menuOrigin = TransformOrigin(floatingAnchor.x, floatingAnchor.y)

    LaunchedEffect(active) { if (!active) { appsMenuOpen = false; calibrationOpen = false } }

    LaunchedEffect(Unit) {
        // Repair cache jobs created by older builds that were constrained by
        // WorkManager's unreliable Wear OS CONNECTED state.
        downloadManager.requeuePendingDownloads()
        val activity = context.findActivity()
        activity?.intent?.getStringExtra(EXTRA_NAVIGATE_ROUTE)?.let { route ->
            activity.intent.removeExtra(EXTRA_NAVIGATE_ROUTE)
            runCatching { mainNavController.navigate(route) }
        }
    }

    CompositionLocalProvider(LocalMainNavController provides mainNavController) {
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalWatchListActive provides (active && !menuVisible)) {
            NavHost(
            modifier = Modifier.fillMaxSize().then(
                if (menuVisible || !active) Modifier.clearAndSetSemantics {} else Modifier,
            ),
            navController = mainNavController,
            startDestination = "tab/home",
            enterTransition = { if (motion) fadeIn(tween(140)) + slideInHorizontally(tween(200)) { it / 12 } else EnterTransition.None },
            exitTransition = { if (motion) fadeOut(tween(110)) else ExitTransition.None },
            popEnterTransition = { if (motion) fadeIn(tween(140)) + slideInHorizontally(tween(200)) { -it / 12 } else EnterTransition.None },
            popExitTransition = { if (motion) fadeOut(tween(110)) + slideOutHorizontally(tween(160)) { it / 12 } else ExitTransition.None },
        ) {
            composable(
                route = "tab/{tabName}?",
                arguments = listOf(
                    navArgument("tabName") {
                        type = NavType.StringType
                        defaultValue = "home"
                        nullable = true
                    },
                ),
            ) { entry ->
                WatchTabScreen(
                    tabName = entry.arguments?.getString("tabName") ?: "home",
                    openMenuRequest = openHomeMenuRequest,
                    active = active && !menuVisible,
                )
            }
            composable("login") { LoginScreen() }
            composable("userCollectComic") { UserCollectComicScreen() }
            composable("userHistoryComic") { UserHistoryComicScreen() }
            composable("userHistoryComment") { UserHistoryCommentScreen() }
            composable("appLocalSetting") { WatchSettingsScreen() }
            composable("download") { WatchCacheScreen() }
            composable("downloadComicDetail/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                WatchCacheDetailScreen(it.arguments?.getInt("id") ?: -1)
            }
            composable("localComicRead/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                ComicReadScreen(it.arguments?.getInt("id") ?: -1, localOnly = true,
                    onControlsVisibilityChanged = { readerControlsVisible = it })
            }
            composable("dohSetting") { DohSettingScreen() }
            composable("appLockSetting") { AppLockSettingScreen() }
            composable("colorPalette") { ColorPaletteScreen() }
            composable("blockedTags") { BlockedTagsScreen() }
            composable("about") { AboutScreen() }
            composable("checkUpdate") { CheckUpdateScreen() }
            composable("logViewer") { LogViewerScreen() }
            composable("personaManager") { PersonaManagerScreen() }
            composable(
                route = "comicDetail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                ComicDetailScreen(entry.arguments?.getInt("id") ?: -1)
            }
            composable(
                route = "comicChapter/{id}?currentChapterId={currentChapterId}",
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("currentChapterId") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                ),
            ) { entry ->
                ComicChapterScreen(
                    comicId = entry.arguments?.getInt("id") ?: -1,
                    currentChapterId = entry.arguments?.getInt("currentChapterId") ?: -1,
                )
            }
            composable(
                route = "comicRelate/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                ComicRelateListScreen(comicId = entry.arguments?.getInt("id") ?: -1)
            }
            composable(
                route = "comicRead/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                ComicReadScreen(entry.arguments?.getInt("id") ?: -1,
                    onControlsVisibilityChanged = { readerControlsVisible = it })
            }
            composable(
                route = "comicSearch?searchContent={searchContent}&excludedTags={excludedTags}&mainTag={mainTag}",
                arguments = listOf(
                    navArgument("searchContent") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("excludedTags") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("mainTag") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                ComicSearchScreen(
                    initialSearchContent = entry.arguments?.getString("searchContent").orEmpty(),
                    initialExcludedTags = deserializeExcludedTags(
                        entry.arguments?.getString("excludedTags"),
                    ),
                    initialMainTag = entry.arguments?.getInt("mainTag") ?: 0,
                )
            }
            composable(
                route = "comicSearchResult/{searchContent}?excludedTags={excludedTags}&mainTag={mainTag}",
                arguments = listOf(
                    navArgument("searchContent") { type = NavType.StringType },
                    navArgument("excludedTags") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("mainTag") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val searchContent = entry.arguments?.getString("searchContent").orEmpty()
                val excludedTagsValue = entry.arguments?.getString("excludedTags").orEmpty()
                LaunchedEffect(searchContent, excludedTagsValue) {
                    comicViewModel.changeSearchComicContent(
                        searchContent,
                        deserializeExcludedTags(excludedTagsValue),
                        entry.arguments?.getInt("mainTag") ?: 0,
                    )
                }
                ComicSearchResultScreen()
            }
            composable(
                route = "comicRanking/{targetType}/{targetValue}?period={period}",
                arguments = listOf(
                    navArgument("targetType") { type = NavType.StringType },
                    navArgument("targetValue") { type = NavType.StringType },
                    navArgument("period") {
                        type = NavType.StringType
                        defaultValue = "week"
                    },
                ),
            ) { entry ->
                ComicRankingScreen(
                    targetType = entry.arguments?.getString("targetType").orEmpty(),
                    targetValue = entry.arguments?.getString("targetValue").orEmpty(),
                    periodValue = entry.arguments?.getString("period") ?: "week",
                )
            }
            composable("comicRecommend") { ComicWeekRecommendScreen() }
            composable(
                route = "comment/{comicId}",
                arguments = listOf(navArgument("comicId") { type = NavType.IntType }),
            ) {
                ComicCommentScreen(it.arguments?.getInt("comicId") ?: -1)
            }
            composable("sign") { SignInScreen() }
            }
            }
            // Keep the transition in composition during exit; don't remove the menu on tap.
            AnimatedVisibility(
                visibleState = menuTransition,
                enter = watchMenuEnter(motion, menuOrigin),
                exit = watchMenuExit(motion, menuOrigin),
            ) {
                CompositionLocalProvider(LocalWatchListActive provides (appsMenuOpen && active)) {
                WatchAppsMenu(
                    onDismiss = { appsMenuOpen = false },
                    onEmergencyLock = {
                        appsMenuOpen = false
                        onEmergencyLock()
                    },
                    onOpenSettings = {
                        appsMenuOpen = false
                        mainNavController.navigate("appLocalSetting") { launchSingleTop = true }
                    },
                    onOpenSearch = {
                        appsMenuOpen = false
                        mainNavController.navigate("comicSearch")
                    },
                    onOpenHomeMenu = {
                        appsMenuOpen = false
                        openHomeMenuRequest += 1
                        mainNavController.navigate("tab/home") { launchSingleTop = true }
                    },
                    onNavigate = { route ->
                        appsMenuOpen = false
                        mainNavController.navigate(route) { launchSingleTop = true }
                    },
                    showAi = localSetting.showAiEntry,
                    active = appsMenuOpen && active,
                    gravityEnabled = localSetting.gravityPageTurningEnabled,
                    onCalibrateGravity = { appsMenuOpen = false; calibrationOpen = true },
                )
                }
            }
            if (calibrationOpen) GravityCalibrationDialog(onDismiss = { calibrationOpen = false })
                WatchFloatingBall(
                    doubleTapEnabled = localSetting.emergencyDoubleTapEnabled,
                    onOpenMenu = { appsMenuOpen = true },
                    onEmergencyLock = onEmergencyLock,
                    visible = active && !menuVisible && !keyboardVisible && !readerControlsVisible &&
                        entry?.arguments?.getString("tabName") != "ai",
                    interactionKey = entry?.destination?.route,
                    onAnchorChanged = { floatingAnchor = it },
                )
        }
    }
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? =
    when (this) {
        is android.app.Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }
