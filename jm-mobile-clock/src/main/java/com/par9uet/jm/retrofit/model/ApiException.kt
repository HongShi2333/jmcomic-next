package com.par9uet.jm.retrofit.model

class ApiException(
    val errorCode: Int,
    override val message: String
): Exception(message)