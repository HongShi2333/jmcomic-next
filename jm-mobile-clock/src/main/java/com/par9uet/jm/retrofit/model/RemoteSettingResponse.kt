package com.par9uet.jm.retrofit.model

import com.par9uet.jm.data.models.RemoteSetting

data class RemoteSettingResponse(
//    val logo_path: String,
//    val main_web_host: String,
    val img_host: String,
//    val base_url: String,
//    val is_cn: Int,
//    val cn_base_url: String,
//    val version: String,
//    val test_version: String,
//    val store_link: String,
//    val ios_version: String,
//    val ios_test_version: String,
//    val ios_store_link: String,
//    val ad_cache_version: Int,
//    val bundle_url: String,
//    val is_hot_update: String,
//    val api_banner_path: String,
//    val version_info: String,
    val app_shunts: List<Shunt>,
//    val download_url: String,
//    val app_landing_page: String,
//    val float_ad: Boolean,
//    val newYearEvent: Boolean,
//    val foolsDayEvent: Boolean,
) {
    class Shunt(val title: String, val key: String)

    fun toRemoteSetting(): RemoteSetting = RemoteSetting(
        imgHost = img_host
    )
}