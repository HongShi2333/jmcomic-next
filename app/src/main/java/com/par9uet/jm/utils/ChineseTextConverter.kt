package com.par9uet.jm.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import com.github.houbb.opencc4j.util.ZhConverterUtil
import java.util.Locale

enum class ChineseScript {
    ORIGINAL, SIMPLIFIED, TRADITIONAL;

    companion object {
        fun forLocale(locale: Locale): ChineseScript {
            if (locale.language != "zh") return ORIGINAL
            return when (locale.script) {
                "Hans" -> SIMPLIFIED
                "Hant" -> TRADITIONAL
                else -> if (locale.country in setOf("TW", "HK", "MO")) TRADITIONAL else SIMPLIFIED
            }
        }
    }
}

object ChineseTextConverter {
    private val cache = object : LinkedHashMap<Pair<ChineseScript, String>, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<ChineseScript, String>, String>?): Boolean =
            size > 2048
    }

    fun convert(text: String, script: ChineseScript): String {
        if (script == ChineseScript.ORIGINAL || text.isBlank()) return text
        val key = script to text
        synchronized(cache) { cache[key]?.let { return it } }
        val converted = when (script) {
            ChineseScript.SIMPLIFIED -> ZhConverterUtil.toSimple(text)
            ChineseScript.TRADITIONAL -> ZhConverterUtil.toTraditional(text)
            ChineseScript.ORIGINAL -> text
        }
        // 共享缓存仅保留短文本。
        if (text.length <= 1024) synchronized(cache) { cache[key] = converted }
        return converted
    }

    fun convert(text: AnnotatedString, script: ChineseScript): AnnotatedString {
        if (script == ChineseScript.ORIGINAL || text.isEmpty()) return text
        val links = text.getLinkAnnotations(0, text.length)
        val strings = text.getStringAnnotations(0, text.length)
        // 保留样式、注解及原始链接。
        val boundaries = buildSet {
            add(0)
            add(text.length)
            (text.spanStyles + text.paragraphStyles + links + strings).forEach {
                add(it.start)
                add(it.end)
            }
        }.sorted()
        val offsets = mutableMapOf(0 to 0)
        return buildAnnotatedString {
            boundaries.zipWithNext { start, end ->
                append(convert(text.text.substring(start, end), script))
                offsets[end] = length
            }
            text.spanStyles.forEach { addStyle(it.item, offsets.getValue(it.start), offsets.getValue(it.end)) }
            text.paragraphStyles.forEach { addStyle(it.item, offsets.getValue(it.start), offsets.getValue(it.end)) }
            links.forEach {
                val start = offsets.getValue(it.start)
                val end = offsets.getValue(it.end)
                when (val link = it.item) {
                    is LinkAnnotation.Url -> addLink(link, start, end)
                    is LinkAnnotation.Clickable -> addLink(link, start, end)
                }
            }
            strings.forEach { addStringAnnotation(it.tag, it.item, offsets.getValue(it.start), offsets.getValue(it.end)) }
        }
    }
}

fun chineseTextKey(value: String): String =
    ChineseTextConverter.convert(value.trim(), ChineseScript.SIMPLIFIED).lowercase(Locale.ROOT)
