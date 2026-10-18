package com.par9uet.jm.ui.screens.tabScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.par9uet.jm.R
import com.par9uet.jm.store.LocalSettingManager
import org.koin.compose.getKoin

@Composable
fun BottomNavigationBarComponent(
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val tabNavController = LocalTabNavController.current
    val backStackEntryState by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntryState?.destination?.route
    val localSetting by localSettingManager.localSettingState.collectAsState()

    fun navigate(name: String) {
        if (name == currentRoute) return
        tabNavController.navigate(name)
    }

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    AnimatedVisibility(visible = currentRoute != "login") {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            NavigationBarItem(
                colors = itemColors,
                icon = {
                    Icon(
                        painterResource(R.drawable.home_icon),
                        contentDescription = "Home"
                    )
                },
                selected = currentRoute == "home",
                onClick = { navigate("home") }
            )
            if (localSetting.showAiEntry) {
                NavigationBarItem(
                    colors = itemColors,
                    icon = {
                        Icon(
                            Icons.Rounded.Psychology,
                            contentDescription = "AI"
                        )
                    },
                    selected = currentRoute == "ai",
                    onClick = { navigate("ai") }
                )
            }
            NavigationBarItem(
                colors = itemColors,
                icon = {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = "Collect"
                    )
                },
                selected = currentRoute == "collect",
                onClick = { navigate("collect") }
            )
            NavigationBarItem(
                colors = itemColors,
                icon = {
                    Icon(
                        painterResource(R.drawable.person_icon),
                        contentDescription = "User"
                    )
                },
                selected = currentRoute == "user",
                onClick = { navigate("user") }
            )
        }
    }
}

@Composable
fun NavigationRailComponent(
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val tabNavController = LocalTabNavController.current
    val backStackEntryState by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntryState?.destination?.route
    val localSetting by localSettingManager.localSettingState.collectAsState()

    fun navigate(name: String) {
        if (name == currentRoute) return
        tabNavController.navigate(name)
    }

    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        NavigationRailItem(
            colors = itemColors,
            icon = {
                Icon(
                    painterResource(R.drawable.home_icon),
                    contentDescription = "Home"
                )
            },
            selected = currentRoute == "home",
            onClick = { navigate("home") }
        )
        if (localSetting.showAiEntry) {
            NavigationRailItem(
                colors = itemColors,
                icon = {
                    Icon(
                        Icons.Rounded.Psychology,
                        contentDescription = "AI"
                    )
                },
                selected = currentRoute == "ai",
                onClick = { navigate("ai") }
            )
        }
        NavigationRailItem(
            colors = itemColors,
            icon = {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Collect"
                )
            },
            selected = currentRoute == "collect",
            onClick = { navigate("collect") }
        )
        NavigationRailItem(
            colors = itemColors,
            icon = {
                Icon(
                    painterResource(R.drawable.person_icon),
                    contentDescription = "User"
                )
            },
            selected = currentRoute == "user",
            onClick = { navigate("user") }
        )
    }
}
