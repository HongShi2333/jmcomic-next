package com.par9uet.jm.retrofit

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.Strictness
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// TODO 改为 koin 注入，改为类，并实现 koinComponent
private val g: Gson = Gson().newBuilder().setStrictness(Strictness.LENIENT).create()

fun parseHtml(htmlStr: String): List<String> {
    // 正则表达式匹配 result 对象
    val resultRegex = Regex("""const result\s*=\s*(\{[\s\S]*?\});""")
    val resultMatch = resultRegex.find(htmlStr)
    val originPicList = mutableListOf<String>()

    if (resultMatch != null) {
        try {
            val resultJson = resultMatch.groupValues[1]
            val o = g.fromJson(
                resultJson,
                JsonObject::class.java
            )
            val list = o.get("images").asJsonArray
            if (list != null) {
                for (i in 0 until list.size()) {
                    list.get(i).let {
                        if (it.isJsonPrimitive) {
                            originPicList.add(it.asString)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("api", "Error parsing result object: ${e.stackTraceToString()}")
        }
    }

    // 正则表达式匹配 config 对象
    val configRegex = Regex("""const config\s*=\s*(\{[\s\S]*?\});""")
    val configMatch = configRegex.find(htmlStr)
    var imgHost: String? = null
    var jmId: String? = null
    var cache: String? = null

    if (configMatch != null) {
        try {
            val resultJson = configMatch.groupValues[1]
            val o = g.fromJson(
                resultJson,
                JsonObject::class.java
            )
            imgHost = o.get("imghost").asString
            jmId = o.get("jmid").asString
            cache = o.get("cache").asString
        } catch (e: Exception) {
            Log.d("api", "Error parsing config object: ${e.stackTraceToString()}")
        }
    }

    if (originPicList.isEmpty() || imgHost == null || jmId == null || cache == null) {
        Log.d("api", "解析漫画 html 页失败")
        return listOf()
    }

    return originPicList.toList().map { item ->
        "$imgHost/media/photos/$jmId/$item$cache"
    }
}

fun parseRange(htmlStr: String): Pair<Int, Int> {
    var left = 0
    var right = 0
    val r1 = Regex("""var aid\s*=\s*(\d+);""")
    val rs1 = r1.find(htmlStr)
    if (rs1 != null) {
        try {
            val str = rs1.groupValues[1]
            left = str.toInt()
        } catch (e: Exception) {
            Log.d("parse", "Error parse range, result object: ${e.stackTraceToString()}")
        }
    }

    val r2 = Regex("""var scramble_id\s*=\s*(\d+);""")
    val rs2 = r2.find(htmlStr)
    if (rs2 != null) {
        try {
            val str = rs2.groupValues[1]
            right = str.toInt()
        } catch (e: Exception) {
            Log.d("parse", "Error parse range, result object: ${e.stackTraceToString()}")
        }
    }
    return left to right
}

fun parseSpeed(htmlStr: String): String {
    var speed = ""
    val r1 = Regex("""var speed\s*=\s*'(.*)';""")
    val rs1 = r1.find(htmlStr)
    if (rs1 != null) {
        try {
            speed = rs1.groupValues[1]
        } catch (e: Exception) {
            Log.d("parse", "Error parse speed, result object: ${e.stackTraceToString()}")
        }
    }
    return speed
}

fun decryptData(str: String): String {
    val decryptKey = ApiContext.getDataDecryptKey()
    val secretKey = SecretKeySpec(decryptKey.toByteArray(Charset.forName("UTF-8")), "AES")

    // 配置 Cipher
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)

    // 解密数据
    val encryptedBytes = android.util.Base64.decode(str, android.util.Base64.DEFAULT)
    val decryptedBytes = cipher.doFinal(encryptedBytes)

    return String(decryptedBytes, Charset.forName("UTF-8"))
}