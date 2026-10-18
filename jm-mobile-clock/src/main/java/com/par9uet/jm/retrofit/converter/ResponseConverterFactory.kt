package com.par9uet.jm.retrofit.converter

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.par9uet.jm.retrofit.decryptData
import com.par9uet.jm.retrofit.model.ResponseWrapper
import com.par9uet.jm.store.ToastManager
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class ResponseConverterFactory(
    private val toastManager: ToastManager,
    private val gson: Gson = Gson()
) : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val elementAdapter = Gson().getAdapter(TypeToken.get(type))
        return Converter<ResponseBody, Any> { responseBody ->
            val body = responseBody.string()
            val json = gson.fromJson(body, JsonObject::class.java)
            val code = json["code"].asInt
            if (code == 200) {
                // code=200 时，即使 data 为 null 也视为成功（如点赞接口不返回 data）
                if (json["data"] != null && !json["data"].isJsonNull) {
                    val encryptedData = json["data"].asString
                    val decryptedData = decryptData(encryptedData)
                    val data = gson.fromJson(decryptedData, JsonElement::class.java)
                    json.add("data", data)
                }
                val result = elementAdapter.fromJsonTree(json)
                return@Converter result
            }
            val msg = json["errorMsg"]?.asString ?: "接口未返回错误"
            toastManager.showAsync("接口请求错误：${msg}")
            return@Converter ResponseWrapper.Error(
                code = code,
                errorMsg = msg
            )
        }
    }
}