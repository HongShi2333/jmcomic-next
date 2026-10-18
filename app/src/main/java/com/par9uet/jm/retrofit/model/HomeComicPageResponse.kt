package com.par9uet.jm.retrofit.model

import com.google.gson.annotations.SerializedName

data class HomeComicPageResponse(
    @SerializedName(value = "list", alternate = ["content"])
    val list: List<HomeSwiperComicListItemResponse.ListItem>? = null,
)
