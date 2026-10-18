package com.par9uet.jm.utils

import java.security.MessageDigest

fun md5(str: String): String {
    return MessageDigest.getInstance("MD5").digest(str.toByteArray())
        .joinToString("") { "%02x".format(it) }.lowercase()
}