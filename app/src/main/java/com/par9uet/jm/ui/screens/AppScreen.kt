package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun AppScreen(
    comicViewModel: ComicViewModel = koinActivityViewModel()
) {
    val mainNavController = rememberNavController()
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
                route = "comicChapter/{id}",
                arguments = listOf(
                    navArgument(name = "id") { type = NavType.IntType; defaultValue = -1 }
                ),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                ComicChapterScreen(/*comicId = id*/)
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
            composable(route = "comicSearch",) { ComicSearchScreen() }
            composable(
                route = "comicSearchResult/{searchContent}",
                arguments = listOf(
                    navArgument(name = "searchContent") { type = NavType.StringType }
                ),
            ) { backStackEntry ->
                val searchContent = backStackEntry.arguments!!.getString("searchContent")!!
                comicViewModel.changeSearchComicContent(searchContent)
                ComicSearchResultScreen()
            }
            composable(route = "comicSearch") { ComicSearchScreen() }
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
