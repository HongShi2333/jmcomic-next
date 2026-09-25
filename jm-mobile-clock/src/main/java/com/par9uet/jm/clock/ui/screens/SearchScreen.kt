package com.par9uet.jm.clock.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.par9uet.jm.clock.data.LoadState
import com.par9uet.jm.clock.data.WatchComic
import com.par9uet.jm.clock.data.WatchRepository
import com.par9uet.jm.clock.ui.components.WatchComicCard
import com.par9uet.jm.clock.ui.components.WatchError
import com.par9uet.jm.clock.ui.components.WatchInfoCard
import com.par9uet.jm.clock.ui.components.WatchLoading
import com.par9uet.jm.clock.ui.components.WatchScreen

@Composable
fun SearchScreen(
    repository: WatchRepository,
    onBack: () -> Unit,
    onComic: (Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var submitKey by rememberSaveable { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<LoadState<List<WatchComic>>>(LoadState.Idle) }

    fun submit() {
        if (query.isNotBlank()) submitKey += 1
    }

    LaunchedEffect(submitKey) {
        if (submitKey == 0) return@LaunchedEffect
        state = LoadState.Loading
        val result = repository.search(query)
        state = result.fold(
            onSuccess = { LoadState.Content(it) },
            onFailure = { LoadState.Error(it.message ?: "搜索失败") },
        )
    }

    WatchScreen(title = "搜索", onBack = onBack) {
        item(key = "query") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("名称或 JM 编号") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
            )
        }
        item(key = "submit") {
            Button(
                onClick = ::submit,
                enabled = query.isNotBlank() && state !is LoadState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("搜索")
            }
        }
        when (val current = state) {
            LoadState.Idle -> item(key = "hint") {
                WatchInfoCard("手表输入", "可输入作品名、作者或 JM 编号；可在设置中开启剪切板自动检测。")
            }
            LoadState.Loading -> item(key = "loading") { WatchLoading("正在搜索…") }
            is LoadState.Error -> item(key = "error") { WatchError(current.message, onRetry = ::submit) }
            is LoadState.Content -> items(current.value, key = WatchComic::id) { comic ->
                WatchComicCard(
                    comic = comic,
                    repository = repository,
                    onClick = { onComic(comic.id) },
                )
            }
        }
    }
}
