package com.par9uet.jm.data.models

enum class CollectComicOrderFilter(val value: String, val label: String) {
    COLLECT_TIME("mr", "收藏时间"),
    UPDATE_TIME("mp", "更新时间")
}

enum class TagFilterLogic(val label: String) {
    AND("同时包含"),
    OR("包含任意"),
    NOT("不包含")
}
