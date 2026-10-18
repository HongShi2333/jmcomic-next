package com.par9uet.jm.retrofit.converter

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class PrimitiveToRequestBodyConverterFactory(
    private val mediaType: MediaType = "text/plain".toMediaType()
) : Converter.Factory() {

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {

        if (type !is Class<*>) return null

        return when (type) {
            Int,
            Long,
            Float,
            Double,
            Boolean,
            String::class.java -> {
                Converter<Any, RequestBody> { value ->
                    value.toString().toRequestBody(mediaType)
                }
            }

            else -> null
        }
    }
}