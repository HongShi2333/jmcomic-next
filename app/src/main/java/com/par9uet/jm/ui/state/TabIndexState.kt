package com.par9uet.jm.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class TabIndexState(
    initial: Int = 0
) {
    var value by mutableIntStateOf(initial)
        internal set

    companion object {
        val Saver: Saver<TabIndexState, *> =
            Saver(save = { it.value }, restore = { TabIndexState(it) })
    }
}

@Composable
fun rememberTabIndexState(initial: Int = 0): TabIndexState {
    return rememberSaveable(saver = TabIndexState.Saver) {
        TabIndexState(initial)
    }
}