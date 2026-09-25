package com.par9uet.jm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun ScrollToTopButton(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val visible by remember(gridState) {
        derivedStateOf {
            val visibleItemCount = gridState.layoutInfo.visibleItemsInfo.size
            visibleItemCount > 0 && gridState.firstVisibleItemIndex >= visibleItemCount
        }
    }
    val coroutineScope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        SmallFloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    gridState.scrollToItem(0)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "回到顶部",
            )
        }
    }
}
