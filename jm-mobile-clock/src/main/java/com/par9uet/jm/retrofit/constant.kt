package com.par9uet.jm.retrofit

import com.par9uet.jm.utils.md5

val API_TS = System.currentTimeMillis() / 1000
const val API_VERSION = "1.8.2"
const val API_TOKEN_SECRET = "185Hcomic3PAPP7R"
val API_TOKEN_HASH = md5("${API_TS}${API_TOKEN_SECRET}")