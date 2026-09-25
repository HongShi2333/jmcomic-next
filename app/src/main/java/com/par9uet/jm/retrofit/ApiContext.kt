package com.par9uet.jm.retrofit

import com.par9uet.jm.utils.md5

// 内置 API 模式常量（来自 JMComic-Api-Java 库）
const val BUILTIN_TOKEN_SECRET = "18comicAPP"
const val BUILTIN_APP_VERSION = "2.0.20"
// 数据解密密钥（两种模式共用）
const val APP_DATA_SECRET = "185Hcomic3PAPP7R"

/**
 * 管理 API 请求的上下文信息。
 *
 * 官方 API 模式使用固定时间戳（API_TS），内置 API 模式使用每请求时间戳。
 * 由于 OkHttp 拦截器链和 Retrofit 转换器在同一线程上同步执行，
 * ThreadLocal 可以安全地在拦截器和转换器之间传递每请求时间戳。
 */
object ApiContext {
    private val perRequestTimestamp = ThreadLocal<Long>()

    fun setTimestamp(ts: Long) {
        perRequestTimestamp.set(ts)
    }

    fun getTimestamp(): Long {
        return perRequestTimestamp.get() ?: API_TS
    }

    /**
     * 获取数据解密密钥。
     * 两种模式的数据解密密钥均为 md5(timestamp + APP_DATA_SECRET)。
     */
    fun getDataDecryptKey(): String {
        return md5("${getTimestamp()}$APP_DATA_SECRET")
    }
}
