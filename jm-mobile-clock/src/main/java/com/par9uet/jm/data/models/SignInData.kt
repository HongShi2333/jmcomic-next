package com.par9uet.jm.data.models

data class SignInData(
    val dailyId: Int,
    val threeDaysCoin: Int,
    val threeDaysExp: Int,
    val sevenDaysCoin: Int,
    val sevenDaysExp: Int,
    val eventName: String,
    val currentProgress: String,
    val dateMap: Map<Int, SignInDataDateMapValue>
) {
    data class SignInDataDateMapValue(
        val isSign: Boolean,
        val hasExtraBonus: Boolean,
    )
}