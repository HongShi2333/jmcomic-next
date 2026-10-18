package com.par9uet.jm.retrofit.model

import com.par9uet.jm.data.models.SignInData

data class SignInDataResponse(
    val daily_id: Int,
    val three_days_coin: String,
    val three_days_exp: String,
    val seven_days_coin: String,
    val seven_days_exp: String,
    val event_name: String,
    val background_pc: String,
    val background_phone: String,
    val currentProgress: String,
    val record: List<List<RecordItem>>
) {
    data class RecordItem(
        val date: String,
        val signed: Boolean,
        val bonus: Boolean,
    )

    fun toSignData() = SignInData(
        dailyId = daily_id,
        threeDaysCoin = three_days_coin.toIntOrNull() ?: 0,
        threeDaysExp = three_days_exp.toIntOrNull() ?: 0,
        sevenDaysCoin = seven_days_coin.toIntOrNull() ?: 0,
        sevenDaysExp = seven_days_exp.toIntOrNull() ?: 0,
        eventName = event_name,
        currentProgress = currentProgress,
        dateMap = record.flatten().map {
            SignInData.SignInDataDateMapValue(
                isSign = it.signed,
                hasExtraBonus = it.bonus,
            )
        }.foldIndexed(mutableMapOf()) { index, acc, item ->
            acc[index + 1] = item
            acc
        }
    )
}