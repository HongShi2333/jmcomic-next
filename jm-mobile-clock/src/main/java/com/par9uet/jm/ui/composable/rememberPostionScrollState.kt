package com.par9uet.jm.ui.composable

import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable

private val SaveMap = mutableMapOf<String, State>()

private data class State(
    val scrollValue: Int,
)

@Composable
fun rememberPositionScrollState(
    key: String,
    initial: Int = 0
): ScrollState {
    val scrollState = rememberSaveable(saver = ScrollState.Saver) {
        val state = SaveMap[key]
        val value = state?.scrollValue ?: initial
        Log.d("JM-MOBILE", "value = $value")
        ScrollState(
            state?.scrollValue ?: initial
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            val value = scrollState.value
            SaveMap[key] = State(scrollValue = value)
            Log.d("JM-MOBILE", "save value = $value")
        }
    }
    return scrollState
}
