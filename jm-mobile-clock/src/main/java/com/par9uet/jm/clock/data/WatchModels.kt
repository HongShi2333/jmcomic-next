package com.par9uet.jm.clock.data

data class WatchComic(
    val id: Int,
    val title: String,
    val author: String,
    val coverPath: String,
    val description: String,
)

data class WatchChapter(
    val id: Int,
    val title: String,
)

data class WatchComicDetail(
    val id: Int,
    val title: String,
    val description: String,
    val authors: List<String>,
    val tags: List<String>,
    val coverPath: String,
    val chapters: List<WatchChapter>,
    val isFavorite: Boolean,
) {
    fun asComic(): WatchComic = WatchComic(
        id = id,
        title = title,
        author = authors.joinToString("、"),
        coverPath = coverPath,
        description = description,
    )
}

data class WatchChapterPages(
    val chapterId: Int,
    val scrambleId: Int,
    val urls: List<String>,
)

data class WatchFavoritePage(
    val comics: List<WatchComic>,
    val page: Int,
    val total: Int,
) {
    val hasMore: Boolean get() = comics.size < total
}

data class WatchAccount(
    val id: Int,
    val username: String,
    val avatarUrl: String,
    val level: Int,
    val levelName: String,
    val favoriteCount: Int,
    val favoriteLimit: Int,
    val coins: Int,
)

data class WatchCredentials(
    val username: String,
    val password: String,
)

sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Content<T>(val value: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

enum class WatchThemeMode(val storageValue: String, val label: String) {
    System("system", "跟随系统"),
    Dark("dark", "深色"),
    Light("light", "浅色");

    companion object {
        fun fromStorage(value: String?): WatchThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: System
    }
}

data class WatchUiPreferences(
    val themeMode: WatchThemeMode = WatchThemeMode.System,
    val uiScale: Float = 1f,
    val appLockEnabled: Boolean = false,
    val appLockPinLength: Int = 4,
    val clipboardAutoDetectEnabled: Boolean = false,
)
