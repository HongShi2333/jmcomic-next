package com.par9uet.jm.retrofit.model

// 20260323
// 似乎每个车牌的 aid 和 scramble_id 都是可以不同的，所以解析出来给后面程序使用而不是写死
data class ComicPicListResponse(
    val list: List<String>,
    val __aId: Int,
    val __scrambleId: Int,
    val __speed: String,
)