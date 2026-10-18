package com.par9uet.jm.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
        mainNavController.navigate(
            "comicSearchResult/$encodedSearchContent?excludedTags=$encodedExcludedTags&mainTag=$initialMainTag"
        )
    }

    LaunchedEffect(editableInitialContent) {
        if (textFieldState.text.toString() != editableInitialContent) {
            textFieldState.edit { replace(0, length, editableInitialContent) }
        }
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier.fillMaxWidth(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                navigationIcon = {
                    IconButton(onClick = { mainNavController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("搜索") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SearchInputCard(
                    textFieldState = textFieldState,
                    focusRequester = focusRequester,
                    onSearch = { search(textFieldState.text.toString()) }
                )
            }
            item {
                SearchExclusionEditor(
                    excludedTags = excludedTags,
                    templates = localSetting.blockedTagTemplateList,
                    onAddTag = { addExcludedTag(it) },
                    onRemoveTag = { tag ->
                        excludedTags = excludedTags.filterNot { it.equals(tag, ignoreCase = true) }
                    },
                    onClearTags = {
                        excludedTags = emptyList()
                    },
                    onApplyTemplate = { applyTemplate(it) },
                    onOpenTemplateSettings = {
                        mainNavController.navigate("blockedTags")
                    },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "搜索历史",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (historySearchState.isNotEmpty()) {
                        TextButton(onClick = { historySearchManager.clear() }) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text("清空")
                        }
                    }
                }
            }
            if (historySearchState.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            text = "暂无搜索历史",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        historySearchState.forEach { tag ->
                            ComicSearchHistoryTag(
                                label = tag,
                                onClick = { search(tag) }
                            )
                        }
                    }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp)
            )
            androidx.compose.material3.TextField(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                state = textFieldState,
                lineLimits = TextFieldLineLimits.SingleLine,
                placeholder = {
                    Text(
                        "搜索漫画名 / 作者 / +标签",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    errorContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    errorIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = { onSearch() }
            )
            if (textFieldState.text.toString().isNotEmpty()) {
                IconButton(onClick = {
                    textFieldState.edit { replace(0, length, "") }
                }) {
                    Icon(Icons.Rounded.Cancel, contentDescription = "清空")
                }
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "搜索")
            }
        }
    }
}
