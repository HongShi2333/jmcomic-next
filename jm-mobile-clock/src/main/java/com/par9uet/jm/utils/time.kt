package com.par9uet.jm.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

fun translateCommentTime(time: String): String {
    return try {
        val inputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE)
        val date = inputFormat.parse(time)
        if (date != null) {
            outputFormat.format(date)
        } else {
            ""
        }
    } catch (e: Exception) {
        Log.d("comment", "评论时间解析错误，原时间：$time ")
        "" // 或者处理异常情况
    }
}