package com.par9uet.jm.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.par9uet.jm.ui.components.WatchLazyColumn as LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.par9uet.jm.ui.components.WatchPageScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.par9uet.jm.ui.components.WatchTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.BlockedTagTemplate
import com.par9uet.jm.store.HistorySearchManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.ComicSearchHistoryTag
import com.par9uet.jm.ui.components.SearchExclusionEditor
import com.par9uet.jm.ui.viewModel.ComicViewModel
import com.par9uet.jm.utils.normalizeSearchExcludedTags
import com.par9uet.jm.utils.parseSearchSyntax
import com.par9uet.jm.utils.searchContentWithoutExcludedTags
import com.par9uet.jm.utils.serializeExcludedTags
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComicSearchScreen(
    initialSearchContent: String = "",
    initialExcludedTags: List<String> = emptyList(),
    initialMainTag: Int = 0,
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    historySearchManager: HistorySearchManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val mainNavController = LocalMainNavController.current
    val focusRequester = remember { FocusRequester() }
    val searchComicFilterState by comicViewModel.searchComicFilterState.collectAsState()
    // 从搜索结果页返回时，ViewModel 持有最新搜索参数；首次进入时 ViewModel 为空，回退到 URL 参数
    val effectiveSearchContent = searchComicFilterState.searchContent.ifBlank { initialSearchContent }
    val effectiveExcludedTags = searchComicFilterState.excludedTags.ifEmpty { initialExcludedTags }
    val editableInitialContent = remember(effectiveSearchContent) {
        searchContentWithoutExcludedTags(effectiveSearchContent)
    }
    val textFieldState = rememberTextFieldState(initialText = editableInitialContent)
    var excludedTags by remember(effectiveSearchContent, effectiveExcludedTags) {
        mutableStateOf(
            normalizeSearchExcludedTags(
                parseSearchSyntax(effectiveSearchContent).excludes + effectiveExcludedTags
            )
        )
    }
    val historySearchState by historySearchManager.historySearchState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    fun addExcludedTag(tag: String) {
        excludedTags = normalizeSearchExcludedTags(excludedTags + tag)
    }

    fun applyTemplate(template: BlockedTagTemplate) {
        excludedTags = normalizeSearchExcludedTags(excludedTags + template.tagList)
    }

    fun search(text: String) {
        val visibleSearchContent = searchContentWithoutExcludedTags(text).trim()
        val inlineExcludedTags = parseSearchSyntax(text).excludes
        val finalExcludedTags = normalizeSearchExcludedTags(excludedTags + inlineExcludedTags)
        if (visibleSearchContent.isBlank()) return

        historySearchManager.addItem(visibleSearchContent)
        val encodedSearchContent = Uri.encode(visibleSearchContent)
        val encodedExcludedTags = Uri.encode(serializeExcludedTags(finalExcludedTags))
        val resultRoute =
            "comicSearchResult/$encodedSearchContent?excludedTags=$encodedExcludedTags&mainTag=$initialMainTag"
        // Replace the current result instead of stacking another result
        // destination on the watch back stack. A previous result can still
        // hold its visible image painters while its entry is kept for back
        // navigation, which is a needless memory spike on Wear OS.
        val currentDestinationId = mainNavController.currentDestination?.id
        mainNavController.navigate(resultRoute) {
            currentDestinationId?.let { destinationId ->
                popUpTo(destinationId) { saveState = false }
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    LaunchedEffect(editableInitialContent) {
        if (textFieldState.text.toString() != editableInitialContent) {
            textFieldState.edit { replace(0, length, editableInitialContent) }
        }
        // Do not open the keyboard automatically when merely returning to this page.
    }

    com.par9uet.jm.ui.components.WatchScaffold(title = "搜索", onNavigateBack = { mainNavController.popBackStack() }) {
        LazyColumn(Modifier.fillMaxSize().imePadding()) {
            item { SearchInputCard(textFieldState, focusRequester) { search(textFieldState.text.toString()) } }
            item {
                androidx.compose.material3.Button(onClick = { search(textFieldState.text.toString()) },
                    enabled = textFieldState.text.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("搜索") }
            }
            item {
                SearchExclusionEditor(excludedTags = excludedTags, templates = localSetting.blockedTagTemplateList,
                    onChangeTags = { excludedTags = normalizeSearchExcludedTags(it) },
                    onOpenTemplateSettings = { mainNavController.navigate("blockedTags") })
            }
            item { Text("搜索历史", style = MaterialTheme.typography.titleSmall) }
            if (historySearchState.isNotEmpty()) item {
                TextButton(onClick = { historySearchManager.clear() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("清空历史") }
            }
            if (historySearchState.isEmpty()) item { Text("暂无搜索历史", style = MaterialTheme.typography.bodySmall) }
            else items(historySearchState.size) { index ->
                androidx.compose.material3.Surface(onClick = { search(historySearchState[index]) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer) {
                    Text(historySearchState[index], modifier = Modifier.padding(10.dp), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputCard(
    textFieldState: androidx.compose.foundation.text.input.TextFieldState,
    focusRequester: FocusRequester,
    onSearch: () -> Unit
) {
    androidx.compose.material3.OutlinedTextField(
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        state = textFieldState,
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text("名称 / 作者 / 标签") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { onSearch() },
    )
}
