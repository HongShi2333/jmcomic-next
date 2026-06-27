package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.par9uet.jm.ui.screens.AiChatScreen
import com.par9uet.jm.ui.screens.HomeScreen
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.screens.UserCollectComicScreen
import com.par9uet.jm.ui.screens.UserScreen
import com.par9uet.jm.store.UserManager
import org.koin.compose.getKoin

@Composable
fun TabScreen(
    tabName: String,
    userManager: UserManager = getKoin().get()
) {
    val tabNavController = rememberNavController()
    val mainNavController = LocalMainNavController.current
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isLogin by userManager.isLoginState.collectAsState(false)
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    CompositionLocalProvider(
        LocalTabNavController provides tabNavController,
    ) {
        BoxWithConstraints {
            val useNavigationRail = maxWidth >= 700.dp
            val hideBottomNavigation = currentRoute == "ai" && imeVisible
            Scaffold(
                bottomBar = {
                    if (!useNavigationRail && !hideBottomNavigation) {
                        BottomNavigationBarComponent()
                    }
                },
                topBar = {
                    TopBarComponent()
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    if (useNavigationRail) {
                        NavigationRailComponent()
                    }
                    NavHost(
                        modifier = Modifier.weight(1f),
                        navController = tabNavController,
                        startDestination = tabName
                    ) {
                        composable("home") {
                            HomeScreen()
                        }
                        composable("user") {
                            UserScreen()
                        }
                        composable("ai") {
                            AiChatScreen()
                        }
                        composable("collect") {
                            LaunchedEffect(isLogin) {
                                if (!isLogin) {
                                    mainNavController.navigate("login")
                                }
                            }
                            if (isLogin) {
                                UserCollectComicScreen(useScaffold = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

val LocalTabNavController = staticCompositionLocalOf<NavHostController> {
    error("none")
}
