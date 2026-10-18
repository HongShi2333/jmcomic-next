package com.par9uet.jm.retrofit.annotation

// 被该注解标记的 service 方法会被通行，其他则会阻塞到初始化结束后再放行
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GInit