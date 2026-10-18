package com.par9uet.jm.data.models

data class User(
    val id: Int,
    val username: String,
    // 该字段为重启后自动登录使用。
    val password: String,
    val avatar: String ,
    val level: Int,
    val levelName: String,
    val currentLevelExp: Int,
    val nextLevelExp: Int,
    val currentCollectCount: Int,
    val maxCollectCount: Int,
    val jCoin: Int,
) {
    companion object {
        fun create(): User {
            return User(
                id = 0,
                username = "",
                password = "",
                avatar = "",
                level = 0,
                levelName = "",
                currentLevelExp = 0,
                nextLevelExp = 0,
                currentCollectCount = 0,
                maxCollectCount = 0,
                jCoin = 0,
            )
        }
    }
}