package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.par9uet.jm.ui.screens.downloadScreen.DownloadScreen
import com.par9uet.jm.ui.screens.downloadScreen.DownloadComicDetailScreen
import com.par9uet.jm.ui.screens.readScreen.ComicReadScreen
import com.par9uet.jm.ui.screens.tabScreen.TabScreen
import com.par9uet.jm.ui.viewModel.ComicViewModel
import com.par9uet.jm.utils.EXTRA_NAVIGATE_ROUTE
import com.par9uet.jm.utils.deserializeExcludedTags
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun AppScreen(
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    externalNavController: NavHostController? = null,
) {
    val mainNavController = externalNavController ?: rememberNavController()
    val context = LocalContext.current
    // 处理从通知点击进入时的目标路由（例如更新下载完成通知）
    LaunchedEffect(Unit) {
        val activity = context.findActivity()
        activity?.intent?.getStringExtra(EXTRA_NAVIGATE_ROUTE)?.let { route ->
            activity.intent.removeExtra(EXTRA_NAVIGATE_ROUTE)
            runCatching { mainNavController.navigate(route) }
        }
    }
    CompositionLocalProvider(
        LocalMainNavController provides mainNavController,
    ) {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = mainNavController,
//            startDestination = "comicQuickSearch/百合"
//            startDestination = "appLocalSetting"
            startDestination = "tab/home",
//            startDestination = "comicRead/1044155",
//             startDestination = "comicDetail/1044155"
//            startDestination = "comicSearch"
//            startDestination = "sign",
//            startDestination = "download"
        ) {
            composable(
                route = "tab/{tabName}?",
                arguments = listOf(
                    navArgument(name = "tabName") {
                        type = NavType.StringType; defaultValue = null; nullable = true
                    }
                ),
//                enterTransition = { slideInHorizontally(initialOffsetX = { width -> -width }) },
//                exitTransition = { slideOutHorizontally(targetOffsetX = { width -> -width }) }
            ) { backStackEntry ->
                val tabName = backStackEntry.arguments?.getString("tabName") ?: "home"
                TabScreen(tabName = tabName)
            }
            composable("login") { LoginScreen() }
            composable(route = "userCollectComic") { UserCollectComicScreen() }
            composable(route = "userHistoryComic") { UserHistoryComicScreen() }
            composable(route = "userHistoryComment") { UserHistoryCommentScreen() }
            composable(route = "appLocalSetting") { LocalSettingScreen() }
            composable(route = "dohSetting") { DohSettingScreen() }
            composable(route = "appLockSetting") { AppLockSettingScreen() }
            composable(route = "colorPalette") { ColorPaletteScreen() }
            composable(route = "blockedTags") { BlockedTagsScreen() }
            composable(route = "about") { AboutScreen() }
            composable(route = "checkUpdate") { CheckUpdateScreen() }
            composable(route = "logViewer") { LogViewerScreen() }
            composable(route = "cacheCleanup") { CacheCleanupScreen() }
            composable(route = "backupRestore") { BackupRestoreScreen() }
            composable(route = "personaManager") { PersonaManagerScreen() }
            composable(
                route = "comicDetail/{id}",
                arguments = listOf(
                    navArgument(name = "id") { type = NavType.IntType; defaultValue = -1 }
                ),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                ComicDetailScreen(id = id)
            }
            composable(
                route = "comicChapter/{id}?currentChapterId={currentChapterId}",
                arguments = listOf(
                    navArgument(name = "id") { type = NavType.IntType; defaultValue = -1 },
                    navArgument(name = "currentChapterId") { type = NavType.IntType; defaultValue = -1 }
                ),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                val currentChapterId = backStackEntry.arguments?.getInt("currentChapterId") ?: -1
                ComicChapterScreen(currentChapterId = currentChapterId)
            }
            composable(
                route = "comicRelate/{id}",
                arguments = listOf(
                    navArgument(name = "id") { type = NavType.IntType; defaultValue = -1 }
                ),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                ComicRelateListScreen(/*comicId = id*/)
            }
            composable(
                route = "comicRead/{id}",
                arguments = listOf(
                    navArgument(name = "id") { type = NavType.IntType; defaultValue = -1 }
                ),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                ComicReadScreen(comicId = id)
            }
            composable(
                route = "comicSearch?searchContent={searchContent}&excludedTags={excludedTags}&mainTag={mainTag}",
                arguments = listOf(
                    navArgument(name = "searchContent") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(name = "excludedTags") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(name = "mainTag") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                ),
            ) { backStackEntry ->
                val searchContent = backStackEntry.arguments?.getString("searchContent") ?: ""
                val excludedTags = deserializeExcludedTags(
                    backStackEntry.arguments?.getString("excludedTags")
                )
                val mainTag = backStackEntry.arguments?.getInt("mainTag") ?: 0
                ComicSearchScreen(
                    initialSearchContent = searchContent,
                    initialExcludedTags = excludedTags,
                    initialMainTag = mainTag,
                )
            }
            composable(
                route = "comicSearchResult/{searchContent}?excludedTags={excludedTags}&mainTag={mainTag}",
                arguments = listOf(
                    navArgument(name = "searchContent") { type = NavType.StringType },
                    navArgument(name = "excludedTags") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(name = "mainTag") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { backStackEntry ->
                val searchContent = backStackEntry.arguments?.getString("searchContent") ?: ""
                val excludedTagsValue = backStackEntry.arguments?.getString("excludedTags") ?: ""
                val excludedTags = deserializeExcludedTags(excludedTagsValue)
                val mainTag = backStackEntry.arguments?.getInt("mainTag") ?: 0
                LaunchedEffect(searchContent, excludedTagsValue, mainTag) {
                    comicViewModel.changeSearchComicContent(searchContent, excludedTags, mainTag)
                }
                ComicSearchResultScreen()
            }
            composable(
                route = "comicRanking/{targetType}/{targetValue}?period={period}",
                arguments = listOf(
                    navArgument(name = "targetType") { type = NavType.StringType },
                    navArgument(name = "targetValue") { type = NavType.StringType },
                    navArgument(name = "period") {
                        type = NavType.StringType
                        defaultValue = "week"
                    },
                ),
            ) { backStackEntry ->
                ComicRankingScreen(
                    targetType = backStackEntry.arguments?.getString("targetType").orEmpty(),
                    targetValue = backStackEntry.arguments?.getString("targetValue").orEmpty(),
                    periodValue = backStackEntry.arguments?.getString("period") ?: "week",
                )
            }
            composable(route = "comicRecommend") { ComicWeekRecommendScreen() }
            composable(
                route = "comment/{comicId}",
                arguments = listOf(
                    navArgument(name = "comicId") { type = NavType.IntType }
                ),
            ) { backStackEntry ->
                val comicId = backStackEntry.arguments?.getInt("comicId") ?: -1
                ComicCommentScreen(comicId = comicId)
            }
            composable(route = "sign") { SignInScreen() }
            composable(route = "download") { DownloadScreen() }
            composable(
                route = "downloadComicDetail/{id}",
                arguments = listOf(
                    navArgument(name = "id") { type = NavType.IntType; defaultValue = -1 }
                ),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                DownloadComicDetailScreen(id = id)
            }
            composable(
                route = "localComicRead/{id}",
                arguments = listOf(
                    navArgument(name = "id") { type = NavType.IntType; defaultValue = -1 }
                ),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                ComicReadScreen(comicId = id, localOnly = true)
            }
        }
    }
}

val LocalMainNavController = staticCompositionLocalOf<NavHostController> {
    error("none")
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? {
    return when (this) {
        is android.app.Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
