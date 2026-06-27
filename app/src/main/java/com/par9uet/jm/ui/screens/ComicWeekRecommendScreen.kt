package com.par9uet.jm.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.data.models.WeekData
import com.par9uet.jm.ui.components.Comic
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.FilterItem
import com.par9uet.jm.ui.components.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.components.SelectDialog
import com.par9uet.jm.ui.components.SelectOption
import com.par9uet.jm.ui.components.adaptiveComicGridCells
import com.par9uet.jm.ui.models.CommonUIState
import com.par9uet.jm.ui.pagingSource.WeekFilter
import com.par9uet.jm.ui.viewModel.ComicViewModel
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
private fun ComicWeekCategorySelect(
    category: Pair<String, String>,
    weekDataState: CommonUIState<WeekData>,
    weekFilterState: WeekFilter,
    comicViewModel: ComicViewModel = koinActivityViewModel(),
) {
    var showSelectDialog by remember { mutableStateOf(false) }
    val weekCategoryOptionList by remember(weekDataState) {
        derivedStateOf {
            val list = weekDataState.data?.categoryList ?: listOf()
            list.map { SelectOption(label = it.second, value = it.first) }
        }
    }
    FilterItem(
        label = category.second,
        onClick = {
            showSelectDialog = true
        },
        active = true
    )
    if (showSelectDialog) {
        SelectDialog(
            title = "选择日期",
            value = weekFilterState.categoryId,
            selectOptionList = weekCategoryOptionList,
            onSelect = {
                comicViewModel.changeWeekCategoryFilter(it)
                showSelectDialog = false
            },
            onDismissRequest = {
                showSelectDialog = false
            }
        )
    }
}

@Composable
fun ComicWeekRecommendScreen(
    comicViewModel: ComicViewModel = koinActivityViewModel()
) {
    val weekDataState by comicViewModel.weekDataState.collectAsState()
    val weekFilterState by comicViewModel.weekFilterState.collectAsState()
    val weekRecommendComicPagingItems = comicViewModel.weekComicPager.collectAsLazyPagingItems()
    val weekCategoryFilter by remember(weekFilterState) {
        derivedStateOf {
            val categoryList = weekDataState.data?.categoryList ?: listOf()
            categoryList.find { it.first == weekFilterState.categoryId }
        }
    }
    LaunchedEffect(Unit) {
        if (weekDataState.data != null) {
            return@LaunchedEffect
        }
        comicViewModel.getWeekData()
    }
    CommonScaffold(
        title = "每周推荐"
    ) {
        Column {
            if (weekDataState.data != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(10.dp)
                ) {
                    val typeList = weekDataState.data!!.typeList
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        typeList.forEach { item ->
                            key(item.first) {
                                FilterItem(
                                    label = item.second,
                                    onClick = {
                                        comicViewModel.changeWeekTypeFilter(item.first)
                                    },
                                    active = weekFilterState.typeId == item.first
                                )
                            }
                        }
                    }
                    weekCategoryFilter?.let {
                        ComicWeekCategorySelect(
                            category = it,
                            weekDataState = weekDataState,
                            weekFilterState = weekFilterState
                        )
                    }
                }
                HorizontalDivider()
            }
            PullRefreshAndLoadMoreGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                lazyPagingItems = weekRecommendComicPagingItems,
                key = { it.id },
                columns = adaptiveComicGridCells(),
            ) {
                Comic(it)
            }
        }
    }
}
