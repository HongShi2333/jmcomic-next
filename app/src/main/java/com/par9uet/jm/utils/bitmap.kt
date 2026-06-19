package com.par9uet.jm.utils

import android.graphics.Bitmap
import android.os.Build
import java.io.OutputStream

fun Bitmap.compressWebpCompat(quality: Int, stream: OutputStream): Boolean {
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        @Suppress("DEPRECATION")
        Bitmap.CompressFormat.WEBP
    }
    return compress(format, quality, stream)
}
