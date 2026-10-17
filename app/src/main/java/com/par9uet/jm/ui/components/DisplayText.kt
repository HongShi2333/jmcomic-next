package com.par9uet.jm.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.core.os.ConfigurationCompat
import com.par9uet.jm.utils.ChineseScript
import com.par9uet.jm.utils.ChineseTextConverter
import java.util.Locale

@Composable
private fun currentChineseScript(): ChineseScript {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT
    return ChineseScript.forLocale(locale)
}

/** 仅转换显示文本，保留请求和存储中的原始值。 */
@Composable
fun String.toDisplayText(): String {
    val script = currentChineseScript()
    return remember(this, script) { ChineseTextConverter.convert(this, script) }
}

@Composable
fun AnnotatedString.toDisplayText(): AnnotatedString {
    val script = currentChineseScript()
    return remember(this, script) { ChineseTextConverter.convert(this, script) }
}
