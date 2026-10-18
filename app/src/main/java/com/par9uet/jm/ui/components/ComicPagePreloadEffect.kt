package com.par9uet.jm.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.ImageLoader
import com.par9uet.jm.cache.preloadComicCovers
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.RemoteSettingManager
import com.par9uet.jm.ui.pagingSource.ComicPageController
import kotlinx.coroutines.delay
import org.koin.compose.getKoin

@Composable
fun ComicPagePreloadEffect(controller: ComicPageController, ready: Boolean) {
    val state by controller.state.collectAsState()
    ComicPreloadEffect(
        key = controller to state,
        enabled = ready && !state.isLoading && state.enabled && state.hasNext,
        loadComics = { controller.preloadNextPage() },
    )
}

/** Home lists are already in memory; only their next page's covers need warming. */
@Composable
fun ComicCoverPreloadEffect(comics: List<Comic>, enabled: Boolean = true) {
    ComicPreloadEffect(key = comics, enabled = enabled && comics.isNotEmpty(), loadComics = { comics })
}

@Composable
private fun ComicPreloadEffect(
    key: Any,
    enabled: Boolean,
    loadComics: suspend () -> List<Comic>,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageLoader: ImageLoader = getKoin().get()
    val localSettingManager: LocalSettingManager = getKoin().get()
    val remoteSettingManager: RemoteSettingManager = getKoin().get()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsState()
    val latestLoadComics by rememberUpdatedState(loadComics)
    val duration = localSetting.coverCacheDurationHours
    val imageHost = remoteSetting.imgHost

    LaunchedEffect(key, enabled, lifecycleOwner, duration, imageHost) {
        if (!enabled) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Give the visible page's list and image requests a head start.
            delay(500)
            val comics = latestLoadComics()
            preloadComicCovers(context, imageLoader, comics, duration, imageHost)
        }
    }
}
