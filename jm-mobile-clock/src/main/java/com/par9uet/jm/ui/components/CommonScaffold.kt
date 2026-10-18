package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.par9uet.jm.ui.screens.LocalMainNavController

/**
 * Compatibility wrapper for copied mobile screens.
 *
 * The screen implementations keep using CommonScaffold. The compact header
 * has its own dial inset; scroll rows own edge avoidance and end padding.
 * The entire page is deliberately NOT reduced to an inscribed rectangle.
 */
@Composable
fun CommonScaffold(
    title: String,
    titleContent: @Composable (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (() -> Unit)? = null,
) {
    val mainNavController = LocalMainNavController.current

    WatchScaffold(
        title = title,
        titleContent = titleContent,
        onNavigateBack = { onNavigateBack?.invoke() ?: mainNavController.popBackStack() },
        bottomBar = bottomBar,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            content?.invoke()
        }
    }
}
