package com.par9uet.jm.ui.screens.readScreen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicReadScreen(
    comicId: Int,
    localOnly: Boolean = false,
    comicReadViewModel: ComicReadViewModel = koinViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val context = LocalContext.current
    val isShowToolbar by comicReadViewModel.isShowToolBar
    val size = comicReadViewModel.size
    var currentIndexState by comicReadViewModel.currentIndexState
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val comicPicState by comicReadViewModel.comicPicState.collectAsState()
    val loading = comicPicState.isLoading
    val lazyListState = rememberLazyListState()
    val pagerState = rememberPagerState(initialPage = 0) { size }
    var targetIndex by remember { mutableIntStateOf(0) }

    fun updateIndexFromReader(value: Float) {
        val target = value.toInt().coerceIn(0, maxOf(0, size - 1))
        if (target != currentIndexState) {
            currentIndexState = target
        }
    }

    fun jumpToIndex(index: Int) {
        if (size <= 0) return

        val target = index.coerceIn(0, size - 1)
        currentIndexState = target
        targetIndex = target
        comicReadViewModel.decodeIndex(target, context)
        comicReadViewModel.showToolBar()
    }

    LaunchedEffect(comicId) {
        val onSuccess = {
            currentIndexState = 0
            targetIndex = 0
            comicReadViewModel.decodeIndex(0, context)
        }
        if (localOnly) {
            comicReadViewModel.getLocalComicPicList(comicId, context, onSuccess)
        } else {
            comicReadViewModel.getComicPicList(
                comicId,
                localSettingManager.localSettingState.value.shunt,
                onSuccess
            )
        }
    }

    val view = LocalView.current
    val controller = remember(view) {
        val window = (context as? Activity)?.window
        if (window == null) {
            null
        } else {
            WindowInsetsControllerCompat(window, view).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
    LaunchedEffect(isShowToolbar) {
        if (isShowToolbar) {
            controller?.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller?.hide(WindowInsetsCompat.Type.statusBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            if (localSetting.readMode == "scroll") {
                ComicScrollRead(
                    lazyListState = lazyListState,
                    pagerState = pagerState,
                    targetIndex = targetIndex,
                    onUpdateSliderValue = { updateIndexFromReader(it) }
                )
            } else {
                ComicPageRead(
                    lazyListState = lazyListState,
                    pagerState = pagerState,
                    targetIndex = targetIndex,
                    onUpdateSliderValue = { updateIndexFromReader(it) }
                )
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = isShowToolbar,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut()
            ) {
                ToolsBar(
                    currentIndex = currentIndexState,
                    pageCount = size,
                    onPageSelected = { jumpToIndex(it) }
                )
            }
            if (localSetting.showComicPageReadTip && localSetting.readMode == "page" || localSetting.showComicScrollReadTip && localSetting.readMode == "scroll") {
                Tip(readMode = localSetting.readMode)
                TipCloseButton(
                    modifier = Modifier.align(
                        if (localSetting.readMode == "scroll") Alignment.CenterEnd else Alignment.BottomCenter
                    ).let {
                        if (localSetting.readMode == "scroll") {
                            it.padding(end = 40.dp)
                        } else {
                            it.padding(bottom = 40.dp)
                        }
                    },
                    onClick = {
                        if (localSetting.readMode == "scroll") {
                            localSettingManager.closeShowComicScrollReadTip()
                        } else {
                            localSettingManager.closeShowComicPageReadTip()
                        }
                    }
                )
            }
        }
    }
}
