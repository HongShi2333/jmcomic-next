package com.par9uet.jm.clock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.par9uet.jm.clock.data.WatchPreferences
import com.par9uet.jm.clock.data.WatchRepository
import com.par9uet.jm.clock.data.WatchSecureStore
import com.par9uet.jm.clock.ui.screens.AccountScreen
import com.par9uet.jm.clock.ui.screens.AppLockPinSetupScreen
import com.par9uet.jm.clock.ui.screens.AppLockSettingsScreen
import com.par9uet.jm.clock.ui.screens.DetailScreen
import com.par9uet.jm.clock.ui.screens.FavoritesScreen
import com.par9uet.jm.clock.ui.screens.HomeScreen
import com.par9uet.jm.clock.ui.screens.LoginScreen
import com.par9uet.jm.clock.ui.screens.ReaderScreen
import com.par9uet.jm.clock.ui.screens.SearchScreen
import com.par9uet.jm.clock.ui.screens.SettingsScreen
import com.par9uet.jm.clock.ui.screens.WatchAppLockScreen
import com.par9uet.jm.clock.ui.components.WatchLoading
import com.par9uet.jm.clock.ui.theme.JmClockTheme

@Composable
fun JmClockApp(lockEpoch: Int) {
    val context = LocalContext.current.applicationContext
    val preferenceStore = remember(context) { WatchPreferences(context) }
    val secureStore = remember(context) { WatchSecureStore(context) }
    val repository = remember { WatchRepository() }
    var preferences by remember { mutableStateOf(preferenceStore.load()) }
    var account by remember { mutableStateOf(secureStore.account()) }
    var lockPinSet by remember { mutableStateOf(secureStore.hasLockPin()) }
    var locked by remember { mutableStateOf(preferences.appLockEnabled && lockPinSet) }
    var sessionReady by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var lastClipboardText by remember { mutableStateOf("") }
    var clipboardDetectedComicId by remember { mutableStateOf<Int?>(null) }
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity.density, baseDensity.fontScale, preferences.uiScale) {
        Density(
            density = baseDensity.density * preferences.uiScale,
            fontScale = baseDensity.fontScale * preferences.uiScale,
        )
    }
    val minimumTouchTarget = remember(preferences.uiScale) {
        (48f / preferences.uiScale).dp
    }

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalMinimumInteractiveComponentSize provides minimumTouchTarget,
        LocalWatchUiScale provides preferences.uiScale,
    ) {
        JmClockTheme(mode = preferences.themeMode) {
            androidx.compose.runtime.LaunchedEffect(lockEpoch) {
                if (lockEpoch > 0 && preferences.appLockEnabled && lockPinSet) {
                    locked = true
                }
            }
            androidx.compose.runtime.LaunchedEffect(
                lockEpoch,
                locked,
                sessionReady,
                preferences.clipboardAutoDetectEnabled,
            ) {
                if (locked || !sessionReady || !preferences.clipboardAutoDetectEnabled) {
                    if (locked) clipboardDetectedComicId = null
                    return@LaunchedEffect
                }
                val clipboardText = clipboardManager.getText()?.text?.toString().orEmpty()
                if (clipboardText.isNotBlank() && clipboardText != lastClipboardText) {
                    lastClipboardText = clipboardText
                    clipboardDetectedComicId = extractClipboardComicId(clipboardText)
                }
            }
            if (locked) {
                WatchAppLockScreen(
                    pinLength = preferences.appLockPinLength,
                    secureStore = secureStore,
                    onUnlocked = { locked = false },
                )
            } else {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val credentials = secureStore.credentials()
                    if (credentials != null) {
                        repository.login(credentials.username, credentials.password).onSuccess { restored ->
                            account = restored
                            secureStore.saveSession(restored, credentials.password)
                        }
                    }
                    sessionReady = true
                }
                if (!sessionReady) {
                    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                        WatchLoading("正在恢复账号…")
                    }
                } else {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = ROUTE_HOME) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                repository = repository,
                                account = account,
                                onSearch = { navController.navigate(ROUTE_SEARCH) },
                                onAccount = { navController.navigate(ROUTE_ACCOUNT) },
                                onComic = { id -> navController.navigate("detail/$id") },
                            )
                        }
                        composable(ROUTE_SEARCH) {
                            SearchScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onComic = { id -> navController.navigate("detail/$id") },
                            )
                        }
                        composable(ROUTE_ACCOUNT) {
                            AccountScreen(
                                account = account,
                                onBack = { navController.popBackStack() },
                                onLogin = { navController.navigate(ROUTE_LOGIN) },
                                onFavorites = { navController.navigate(ROUTE_FAVORITES) },
                                onSettings = { navController.navigate(ROUTE_SETTINGS) },
                                onLogout = {
                                    repository.clearSession()
                                    secureStore.clearSession()
                                    account = null
                                    navController.navigate(ROUTE_HOME) {
                                        popUpTo(ROUTE_HOME) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                        composable(ROUTE_LOGIN) {
                            LoginScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onLoggedIn = { loggedIn, password ->
                                    secureStore.saveSession(loggedIn, password)
                                    account = loggedIn
                                    navController.popBackStack()
                                },
                            )
                        }
                        composable(ROUTE_FAVORITES) {
                            FavoritesScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onComic = { id -> navController.navigate("detail/$id") },
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                preferences = preferences,
                                onPreferencesChange = { next ->
                                    preferences = next
                                    preferenceStore.save(next)
                                },
                                onAppLockSettings = { navController.navigate(ROUTE_APP_LOCK_SETTINGS) },
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(ROUTE_APP_LOCK_SETTINGS) {
                            AppLockSettingsScreen(
                                preferences = preferences,
                                hasLockPin = lockPinSet,
                                onPreferencesChange = { next ->
                                    preferences = next
                                    preferenceStore.save(next)
                                },
                                onSetPin = { navController.navigate(ROUTE_APP_LOCK_PIN) },
                                onRemovePin = {
                                    secureStore.clearLockPin()
                                    lockPinSet = false
                                    val next = preferences.copy(appLockEnabled = false)
                                    preferences = next
                                    preferenceStore.save(next)
                                },
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(ROUTE_APP_LOCK_PIN) {
                            AppLockPinSetupScreen(
                                hasExistingPin = lockPinSet,
                                secureStore = secureStore,
                                onPinSaved = {
                                    lockPinSet = true
                                    val next = preferences.copy(appLockEnabled = true)
                                    preferences = next
                                    preferenceStore.save(next)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(
                            route = "detail/{comicId}",
                            arguments = listOf(navArgument("comicId") { type = NavType.IntType }),
                        ) { entry ->
                            DetailScreen(
                                comicId = entry.arguments?.getInt("comicId") ?: -1,
                                repository = repository,
                                isLoggedIn = account != null,
                                onLoginRequired = { navController.navigate(ROUTE_LOGIN) },
                                onBack = { navController.popBackStack() },
                                onRead = { chapterId -> navController.navigate("reader/$chapterId") },
                            )
                        }
                        composable(
                            route = "reader/{chapterId}",
                            arguments = listOf(navArgument("chapterId") { type = NavType.IntType }),
                        ) { entry ->
                            ReaderScreen(
                                chapterId = entry.arguments?.getInt("chapterId") ?: -1,
                                repository = repository,
                                preferences = preferenceStore,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                    clipboardDetectedComicId?.let { comicId ->
                        AlertDialog(
                            onDismissRequest = { clipboardDetectedComicId = null },
                            title = { Text("检测到 JM$comicId") },
                            text = { Text("是否打开该漫画详情？") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        clipboardDetectedComicId = null
                                        navController.navigate("detail/$comicId")
                                    },
                                ) { Text("打开") }
                            },
                            dismissButton = {
                                TextButton(onClick = { clipboardDetectedComicId = null }) { Text("忽略") }
                            },
                        )
                    }
                }
            }
        }
    }
}

private const val ROUTE_HOME = "home"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_ACCOUNT = "account"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_FAVORITES = "favorites"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_APP_LOCK_SETTINGS = "app_lock_settings"
private const val ROUTE_APP_LOCK_PIN = "app_lock_pin"

val LocalWatchUiScale = staticCompositionLocalOf { 1f }

private fun extractClipboardComicId(text: String): Int? {
    val trimmed = text.trim()
    Regex("(?i)^(?:jm\\s*)?(\\d{3,12})$")
        .matchEntire(trimmed)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.let { return it }
    return Regex("(?i)(?:jm|album[\\s/=:]+|comic[\\s/=:]+|id[\\s:：=]+)(\\d{3,12})")
        .find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
}
