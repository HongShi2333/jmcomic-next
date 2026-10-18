package com.par9uet.jm.ui.screens.tabScreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.WatchCompactHeader
import com.par9uet.jm.ui.components.WatchLazyColumn
import com.par9uet.jm.ui.components.WatchMenuButton
import com.par9uet.jm.ui.components.WatchScaffold
import com.par9uet.jm.ui.components.WatchSearchButton
import com.par9uet.jm.ui.components.WatchTopBar
import com.par9uet.jm.ui.components.watchHomeUsesScrollableHeader
import com.par9uet.jm.ui.components.LocalWatchMotionEnabled
import com.par9uet.jm.ui.components.watchMenuEnter
import com.par9uet.jm.ui.components.watchMenuExit
import com.par9uet.jm.ui.components.watchPressFeedback
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.screens.UserCollectComicScreen
import com.par9uet.jm.ui.screens.WatchHomeScreen
import com.par9uet.jm.ui.screens.WatchUserScreen
import com.par9uet.jm.ui.viewModel.ComicViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun WatchTabScreen(
    tabName: String,
    userManager: UserManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get(),
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    openMenuRequest: Int = 0,
    active: Boolean = true,
) {
    val tabNavController = rememberNavController()
    val mainNavController = LocalMainNavController.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val scrollHomeHeader = watchHomeUsesScrollableHeader(
        configuration.screenWidthDp.toFloat(),
        configuration.screenHeightDp.toFloat(),
        configuration.isScreenRound,
        configuration.fontScale,
    )
    val entry by tabNavController.currentBackStackEntryAsState()
    val isLogin by userManager.isLoginState.collectAsState(false)
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val homeState by comicViewModel.homeComicState.collectAsState()
    // A stable home root prevents tab/user (e.g. after login) from making
    // Back alternate forever between user and home.
    val initialRoute = "home"
    val currentRoute = entry?.destination?.route ?: initialRoute
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val menuTransition = remember { MutableTransitionState(false) }
    menuTransition.targetState = menuOpen
    val menuVisible = menuTransition.currentState || menuTransition.targetState
    val motion = LocalWatchMotionEnabled.current
    var categoryMenu by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, homeState.list.lastIndex.coerceAtLeast(0))
    val categoryTitle = homeState.list.getOrNull(safeIndex)?.title ?: "发现漫画"

    fun navigateTab(route: String) {
        tabNavController.navigate(route) {
            popUpTo(tabNavController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun closeOrReturn() {
        when {
            menuOpen && categoryMenu -> categoryMenu = false
            menuOpen -> menuOpen = false
            currentRoute != "home" -> navigateTab("home")
        }
    }

    fun openMenu() {
        keyboard?.hide()
        focusManager.clearFocus()
        categoryMenu = false
        menuOpen = true
    }

    LaunchedEffect(openMenuRequest) {
        if (openMenuRequest > 0) openMenu()
    }

    val searchHeader: @Composable () -> Unit = {
        com.par9uet.jm.ui.components.WatchHomeActions(onSearch = { mainNavController.navigate("comicSearch") }, onMenu = ::openMenu)
    }

    LaunchedEffect(tabName) {
        val candidate = tabName.takeIf { it in setOf("home", "collect", "user", "ai") } ?: "home"
        val requested = if (candidate == "ai" && !localSetting.showAiEntry) "home" else candidate
        if (currentRoute != requested) navigateTab(requested)
    }
    LaunchedEffect(localSetting.showAiEntry, currentRoute) {
        if (!localSetting.showAiEntry && currentRoute == "ai") navigateTab("home")
    }
    BackHandler(enabled = menuOpen || currentRoute != "home") { closeOrReturn() }

    val destinations = buildList {
        add(WatchDestination("home", Icons.Default.Home, "发现", true))
        if (localSetting.showAiEntry) add(WatchDestination("ai", Icons.Rounded.Psychology, "AI", true))
        add(WatchDestination("collect", Icons.Default.Bookmark, "收藏", true))
        add(WatchDestination("user", Icons.Default.Person, "我的", true))
        add(WatchDestination("comicRecommend", Icons.Rounded.Star, "每周推荐"))
        add(WatchDestination("sign", Icons.Rounded.CalendarMonth, "签到"))
        add(WatchDestination("appLocalSetting", Icons.Rounded.Settings, "设置"))
        add(WatchDestination("download", Icons.Rounded.Menu, "本地缓存"))
        add(WatchDestination("refresh", Icons.Rounded.Refresh, "刷新首页"))
    }
    val pageTitle = destinations.firstOrNull { it.route == currentRoute }?.label ?: "发现"

    CompositionLocalProvider(LocalTabNavController provides tabNavController) {
        WatchScaffold(
            title = pageTitle,
            showTopBar = !menuVisible && currentRoute != "ai" && (currentRoute != "home" || !scrollHomeHeader),
            titleContent = if (!menuOpen && currentRoute == "home") {
                { WatchSearchButton { mainNavController.navigate("comicSearch") } }
            } else null,
            onNavigateBack = if (!menuOpen) ({ closeOrReturn() }) else null,
            // Apps/category access stays in the floating menu and home actions;
            // a second 48dp menu row would recreate the large watch forehead.
            actions = null,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Keep tab state alive while the full-screen menu is open.
                CompositionLocalProvider(com.par9uet.jm.ui.components.LocalWatchListActive provides (active && !menuVisible)) {
                NavHost(
                    modifier = Modifier.fillMaxSize().then(
                        if (menuVisible) Modifier.clearAndSetSemantics { } else Modifier,
                    ),
                    navController = tabNavController,
                    startDestination = initialRoute,
                    // No gratuitous home-feed motion; the explicit menu owns its transition.
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {
                    composable("home") {
                        WatchHomeScreen(
                            selectedIndex = safeIndex,
                            active = active && !menuVisible,
                            header = if (scrollHomeHeader) searchHeader else null,
                        )
                    }
                    composable("user") { WatchUserScreen(active = active && !menuVisible) }
                    composable("ai") { com.par9uet.jm.ui.screens.AiChatScreen() }
                    composable("collect") {
                        if (isLogin) {
                            UserCollectComicScreen(useScaffold = false)
                        } else {
                            LaunchedEffect(Unit) { mainNavController.navigate("login") }
                            Text("登录后查看收藏", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                }
                AnimatedVisibility(
                    visibleState = menuTransition,
                    enter = watchMenuEnter(motion, TransformOrigin(.67f, .14f)),
                    exit = watchMenuExit(motion, TransformOrigin(.67f, .14f)),
                ) {
                    CompositionLocalProvider(com.par9uet.jm.ui.components.LocalWatchListActive provides (active && menuOpen)) {
                    WatchScaffold(
                        title = if (categoryMenu) "分类" else "菜单",
                        scrollHeader = true,
                        onNavigateBack = { if (categoryMenu) categoryMenu = false else menuOpen = false },
                    ) {
                        WatchLazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            rotaryEnabled = active && menuOpen,
                            userScrollEnabled = menuOpen,
                            edgeScalingEnabled = true,
                            edgeStartPadding = false,
                            contentPadding = PaddingValues(bottom = 8.dp),
                        ) {
                            if (categoryMenu) {
                                itemsIndexed(homeState.list, key = { index, category -> "$index:${category.id}" }) { index, category ->
                                    WatchMenuRow(
                                        icon = Icons.Rounded.FilterList,
                                        label = category.title,
                                        selected = index == safeIndex,
                                        onClick = {
                                            selectedIndex = index
                                            menuOpen = false
                                            navigateTab("home")
                                        },
                                    )
                                }
                                if (homeState.list.isEmpty()) {
                                    item { Text("暂无分类，请刷新首页", style = MaterialTheme.typography.bodySmall) }
                                    item {
                                        WatchMenuRow(Icons.Rounded.Refresh, "刷新首页") {
                                            comicViewModel.getHomeComic()
                                            menuOpen = false
                                            navigateTab("home")
                                        }
                                    }
                                }
                            } else {
                                item {
                                    WatchMenuRow(Icons.Rounded.FilterList, "分类 · $categoryTitle") {
                                        categoryMenu = true
                                    }
                                }
                                items(destinations, key = { it.route }) { destination ->
                                    WatchMenuRow(
                                        icon = destination.icon,
                                        label = destination.label,
                                        selected = destination.isTab && currentRoute == destination.route,
                                        onClick = {
                                            menuOpen = false
                                            when {
                                                destination.route == "refresh" -> {
                                                    comicViewModel.getHomeComic()
                                                    navigateTab("home")
                                                }
                                                !isLogin && destination.route in setOf("collect", "sign") -> {
                                                    mainNavController.navigate("login")
                                                }
                                                destination.isTab -> navigateTab(destination.route)
                                                else -> mainNavController.navigate(destination.route)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WatchMenuRow(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).watchPressFeedback(source),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (selected) Icon(Icons.Rounded.Check, contentDescription = "已选中", modifier = Modifier.size(18.dp))
        }
    }
}

private data class WatchDestination(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val isTab: Boolean = false,
)
