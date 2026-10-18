package com.par9uet.jm.retrofit.model

import com.par9uet.jm.data.models.User

data class LoginResponse(
    val uid: Int,
    val username: String,
    val email: String,
    val photo: String,
    val coin: String,
    val album_favorites: Int,
    val level_name: String,
    val level: Int,
    val nextLevelExp: Int,
    val exp: Int,
    val expPercent: Double,
    val album_favorites_max: Int,
) {
    fun toUser(password: String): User = User(
        id = uid,
        username = username,
        password = password,
        avatar = photo,
        level = level,
        levelName = level_name,
        currentLevelExp = exp,
        nextLevelExp = nextLevelExp,
        currentCollectCount = album_favorites,
        maxCollectCount = album_favorites_max,
        jCoin = coin.toInt(),
    )
}