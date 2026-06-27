package com.par9uet.jm.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
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
import com.par9uet.jm.ui.components.CommonScaffold
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
    CommonScaffold(
        title = "每日签到"
    ) {
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize(),
            isRefreshing = signDataState.isLoading,
            onRefresh = {
                if (isLogin) {
                    userViewModel.getSignInData()
                } else {
                    mainNavController.navigate("login")
                }
            }
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val state = rememberCalendarState(
                                startMonth = startMonth,
                                endMonth = endMonth,
                                firstVisibleMonth = currentMonth,
                                firstDayOfWeek = daysOfWeek.first(),
                                outDateStyle = OutDateStyle.EndOfGrid,
                            )
                            val visibleMonth = rememberFirstVisibleMonthAfterScroll(state)
                            val title = visibleMonth.yearMonth.toString() + when {
                                signDataState.data != null -> "【${signDataState.data!!.eventName}】"
                                else -> ""
                            }
                            Text(
                                modifier = Modifier
                                    .weight(1f),
                                text = title,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        HorizontalCalendar(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            state = rememberCalendarState(),
                            calendarScrollPaged = true,
                            contentHeightMode = ContentHeightMode.Fill,
                            monthHeader = {
                                Row(
                                    Modifier
                                        .padding(bottom = 1.dp)
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(vertical = 10.dp),
                                ) {
                                    for (dayOfWeek in daysOfWeek) {
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            fontSize = 15.sp,
                                            text = weekTextMap[dayOfWeek.value]!!
                                        )
                                    }
                                }
                            },
                            dayContent = { day ->
                                if (day.position == DayPosition.MonthDate) {
                                    var isSign = false
                                    var hasExtraBonus = false
                                    if (signDataState.data != null) {
                                        val data =
                                            signDataState.data!!.dateMap[day.date.dayOfMonth]!!
                                        isSign = data.isSign
                                        hasExtraBonus = data.hasExtraBonus
                                    }
                                    Day(
                                        day = day,
                                        isToday = day.date == today,
                                        isSign = isSign,
                                        hasExtraBonus = hasExtraBonus,
                                    )
                                }
                            }
                        )
                        Text("已连续签到${signMaxDay}天")
                        Row(modifier = Modifier.padding(vertical = 10.dp)) {
                            for (i in 0 until 7) {
                                key(i) {
                                    Column(
                                        modifier = Modifier.width(60.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (i < signMaxDay) {
                                            Icon(
                                                modifier = Modifier.size(15.dp),
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .size(15.dp)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                            )
                                        }
                                        Text(
                                            text = "${i + 1}",
                                            modifier = Modifier.width(20.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    if (i < 6) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("连续签到三天额外获得${signDataState.data?.threeDaysCoin ?: 0}金币，${signDataState.data?.threeDaysExp ?: 0}经验")
                            Text("连续签到七天额外获得${signDataState.data?.sevenDaysCoin ?: 0}金币，${signDataState.data?.sevenDaysExp ?: 0}经验")
                        }
                        Button(
                            enabled = !signDataState.isLoading,
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .height(46.dp)
                                .fillMaxWidth(),
                            onClick = {
                                if (isLogin) {
                                    userViewModel.signIn()
                                } else {
                                    mainNavController.navigate("login")
                                }
                            }
                        ) {
                            if (signInState.isLoading) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = ButtonDefaults.buttonColors().disabledContainerColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("签到中")
                                }
                            } else {
                                Text("签到")
                            }
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
            .clip(RoundedCornerShape(8.dp))
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
                        // 创建路径
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(side1, 0f)
                            lineTo(0f, side1)
                            close()
                        }

                        // 绘制路径
                        drawPath(
                            path = path,
                            color = secondaryContainerColor
                        )
                        with(checkIcon) {
                            draw(
                                size = Size(side2, side2), // 图标大小
                                colorFilter = ColorFilter.tint(primaryColor) // 图标颜色
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
                            // 创建路径
                            val path = Path().apply {
                                moveTo(size.width, size.height)
                                lineTo(size.width - side1, size.height)
                                lineTo(size.width, size.height - side1)
                                close()
                            }

                            // 绘制路径
                            drawPath(
                                path = path,
                                color = secondaryContainerColor
                            )
                            with(starIcon) {
                                translate(
                                    left = size.width - side2,
                                    top = size.height - side2
                                ) {
                                    draw(
                                        size = Size(side2, side2), // 图标大小
                                        colorFilter = ColorFilter.tint(primaryColor) // 图标颜色
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
                fontSize = 15.sp,
                color = when {
                    day.position == DayPosition.OutDate -> MaterialTheme.colorScheme.secondary
                    else -> Color.Unspecified
                }
            )

        }
    }
}
