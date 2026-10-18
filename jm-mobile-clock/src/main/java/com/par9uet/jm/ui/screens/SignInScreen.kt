package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.par9uet.jm.ui.components.WatchLazyColumn as LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.par9uet.jm.ui.components.WatchPageScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.par9uet.jm.ui.components.WatchTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.ContentHeightMode
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.yearMonth
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.viewModel.UserViewModel
import org.koin.compose.getKoin
import kotlinx.coroutines.flow.filter
import org.koin.compose.viewmodel.koinActivityViewModel
import java.time.LocalDate
import kotlin.math.max

private val weekTextMap = mapOf(
    1 to "一",
    2 to "二",
    3 to "三",
    4 to "四",
    5 to "五",
    6 to "六",
    7 to "七",
)

@Composable
fun rememberFirstVisibleMonthAfterScroll(state: CalendarState): CalendarMonth {
    val visibleMonth = remember(state) { mutableStateOf(state.firstVisibleMonth) }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { scrolling -> !scrolling }
            .collect { visibleMonth.value = state.firstVisibleMonth }
    }
    return visibleMonth.value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    userViewModel: UserViewModel = koinActivityViewModel(),
    userManager: UserManager = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val isLogin by userManager.isLoginState.collectAsState(false)
    val today = remember { LocalDate.now() }
    val daysOfWeek = remember { daysOfWeek() }
    val currentMonth = remember(today) { today.yearMonth }
    val startMonth = remember { currentMonth.minusMonths(500) }
    val endMonth = remember { currentMonth.plusMonths(500) }
    val signDataState by userViewModel.signDataState.collectAsState()
    val signInState by userViewModel.signInState.collectAsState()
    val signMaxDay by remember {
        derivedStateOf {
            signDataState.data?.dateMap?.entries?.fold(mutableListOf(0, 0)) { acc, item ->
                if (item.value.isSign) {
                    acc[1] += 1
                    acc[0] = max(acc[0], acc[1])
                } else {
                    acc[1] = 0
                }
                acc
            }?.get(0) ?: 0
        }
    }
    LaunchedEffect(isLogin) {
        if (isLogin) {
            userViewModel.getSignInData()
        } else {
            mainNavController.navigate("login")
        }
    }

    com.par9uet.jm.ui.components.WatchScaffold(title = "每日签到", onNavigateBack = { mainNavController.popBackStack() }) {
        PullToRefreshBox(modifier = Modifier.fillMaxSize(), isRefreshing = signDataState.isLoading, onRefresh = userViewModel::getSignInData) {
            LazyColumn(Modifier.fillMaxSize()) {
                item { Text("${today.year}年${today.monthValue}月 · ${signDataState.data?.eventName.orEmpty()}", style = MaterialTheme.typography.titleSmall) }
                item {
                    val signed = signDataState.data?.dateMap?.get(today.dayOfMonth)?.isSign == true
                    Button(onClick = { if (isLogin) userViewModel.signIn() else mainNavController.navigate("login") },
                        enabled = !signDataState.isLoading && !signInState.isLoading && !signed,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(when { signed -> "今日已签到"; signInState.isLoading -> "签到中"; else -> "签到" })
                    }
                }
                if (signDataState.isLoading) item { CircularProgressIndicator(Modifier.size(24.dp)) }
                if (signDataState.isError) item {
                    Text(signDataState.errorMsg ?: "加载签到信息失败", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = userViewModel::getSignInData, modifier = Modifier.fillMaxWidth()) { Text("重试") }
                }
                if (signInState.isError) item { Text(signInState.errorMsg ?: "签到失败，请重试", color = MaterialTheme.colorScheme.error) }
                item { Text("最长连续签到 $signMaxDay 天", style = MaterialTheme.typography.bodyMedium) }
                item { Text("连续3天：${signDataState.data?.threeDaysCoin ?: 0} 金币 + ${signDataState.data?.threeDaysExp ?: 0} 经验", style = MaterialTheme.typography.bodySmall) }
                item { Text("连续7天：${signDataState.data?.sevenDaysCoin ?: 0} 金币 + ${signDataState.data?.sevenDaysExp ?: 0} 经验", style = MaterialTheme.typography.bodySmall) }
                item { Text("本月记录", style = MaterialTheme.typography.titleSmall) }
                items(today.lengthOfMonth()) { index ->
                    val day = index + 1
                    val record = signDataState.data?.dateMap?.get(day)
                    Surface(Modifier.fillMaxWidth(), color = if (day == today.dayOfMonth) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large) {
                        Column(Modifier.padding(8.dp)) {
                            Text("${today.monthValue}月${day}日${if (day == today.dayOfMonth) " · 今天" else ""}")
                            Text(if (record?.isSign == true) "已签到${if (record.hasExtraBonus) " · 额外奖励" else ""}" else if (day > today.dayOfMonth) "尚未到来" else "未签到", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    isToday: Boolean = false,
    isSign: Boolean = false,
    hasExtraBonus: Boolean = false,
) {
    val checkIcon = rememberVectorPainter(Icons.Default.Check)
    val starIcon = rememberVectorPainter(Icons.Default.Star)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                color = when {
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
            ).let { modifier ->
                if (isSign) {
                    modifier.drawBehind {
                        val side1 = size.minDimension * 0.5f
                        val side2 = size.minDimension * 0.3f
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(side1, 0f)
                            lineTo(0f, side1)
                            close()
                        }
                        drawPath(path = path, color = secondaryContainerColor)
                        with(checkIcon) {
                            draw(
                                size = Size(side2, side2),
                                colorFilter = ColorFilter.tint(primaryColor)
                            )
                        }
                    }
                } else {
                    modifier
                }.let { modifier ->
                    if (hasExtraBonus) {
                        modifier.drawBehind {
                            val side1 = size.minDimension * 0.5f
                            val side2 = size.minDimension * 0.3f
                            val path = Path().apply {
                                moveTo(size.width, size.height)
                                lineTo(size.width - side1, size.height)
                                lineTo(size.width, size.height - side1)
                                close()
                            }
                            drawPath(path = path, color = secondaryContainerColor)
                            with(starIcon) {
                                translate(
                                    left = size.width - side2,
                                    top = size.height - side2
                                ) {
                                    draw(
                                        size = Size(side2, side2),
                                        colorFilter = ColorFilter.tint(primaryColor)
                                    )
                                }
                            }
                        }
                    } else {
                        modifier
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    day.position == DayPosition.OutDate -> MaterialTheme.colorScheme.onSurfaceVariant
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> Color.Unspecified
                }
            )
        }
    }
}
