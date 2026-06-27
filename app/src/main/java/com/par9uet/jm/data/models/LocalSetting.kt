package com.par9uet.jm.data.models

data class LocalSetting(
    val apiList: List<String> = listOf(
        "https://www.cdnhth.club",
        "https://www.cdnmhwscc.vip",
        "https://www.jmapiproxyxxx.vip",
        "https://www.cdnxxx-proxy.xyz",
        "https://www.jmeadpoolcdn.life"
    ),
    val api: String = apiList[0],
    val themeList: List<String> = listOf(
        "auto",
        "light",
        "dark",
    ),
    val theme: String = "auto",
    val shunt: String = "1",
    val shuntList: List<String> = listOf(
        "1",
        "2",
        "3",
        "4",
    ),
    // 阅读页预先加载的图片张数
    val prefetchCount: Int = 3,
    // scroll || page || tap
    val readMode: String = "scroll",
    // default || side
    val readTapMode: String = "default",
    val launcherDisguise: String = "default",
    val showComicScrollReadTip: Boolean = true,
    val showComicPageReadTip: Boolean = true,
    val showComicCacheNotification: Boolean = true,
    val showComicCacheNotificationName: Boolean = true,
    val blockedTagList: List<String> = listOf(),
)
