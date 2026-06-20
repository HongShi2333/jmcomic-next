package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.par9uet.jm.ui.screens.AiChatScreen
import com.par9uet.jm.ui.screens.HomeScreen
import com.par9uet.jm.ui.screens.UserScreen

@Composable
fun TabScreen(tabName: String) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    CompositionLocalProvider(
        LocalTabNavController provides tabNavController,
    ) {
        Scaffold(
            bottomBar = {
                if (!(currentRoute == "ai" && imeVisible)) {
                    BottomNavigationBarComponent()
                }
            },
            topBar = {
                TopBarComponent()
            }
        ) { innerPadding ->
            NavHost(
                modifier = Modifier.padding(innerPadding),
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
            }
        }
    }
}

val LocalTabNavController = staticCompositionLocalOf<NavHostController> {
    error("none")
}
