package com.par9uet.jm.network

/**
 * Normalizes the API's `image` field into loadable cover URLs.
 *
 * JMComic's API commonly returns a relative path such as
 * `/media/albums/<id>_3x4.jpg`; the path is not itself a URL. The official
 * client keeps image domains separately in its constants, so every caller must
 * resolve both pieces before requesting the image. These values mirror the
 * upstream list but are local to avoid initializing the Java 9 collection APIs
 * used by the upstream class on Android 6.
 */
object ComicCoverUrlResolver {
    private val upstreamImageDomains = listOf(
        "cdn-msp.jmapiproxy1.cc",
        "cdn-msp.jmapiproxy2.cc",
        "cdn-msp2.jmapiproxy2.cc",
        "cdn-msp3.jmapiproxy2.cc",
        "cdn-msp.jmapinodeudzn.net",
        "cdn-msp3.jmapinodeudzn.net",
    )

    fun resolve(
        comicId: Int,
        apiImage: String,
        configuredImageHost: String,
    ): List<String> {
        val source = apiImage.trim()
        val imageHosts = buildList {
            normalizeHost(configuredImageHost)?.let(::add)
            upstreamImageDomains
                .mapNotNull(::normalizeHost)
                .forEach(::add)
        }.distinct()
        val relativeSource = source.takeIf { it.isNotBlank() && !isAbsoluteUrl(it) }
            ?.let(::normalizeRelativePath)

        return buildList {
            when {
                isAbsoluteUrl(source) -> add(source)
                source.startsWith("//") -> add("https:$source")
            }
            if (relativeSource != null) {
                imageHosts.forEach { host -> add(host + relativeSource) }
            }
            // The album endpoint does not always include `image`; retain the
            // canonical cover convention as a final fallback for that case.
            if (comicId > 0) {
                imageHosts.forEach { host -> add("$host/media/albums/${comicId}_3x4.jpg") }
            }
        }.distinct()
    }

    private fun normalizeHost(value: String): String? {
        val trimmed = value.trim().trimEnd('/')
        if (trimmed.isBlank()) return null
        return if (isAbsoluteUrl(trimmed)) trimmed else "https://$trimmed"
    }

    private fun normalizeRelativePath(value: String): String =
        if (value.startsWith('/')) value else "/$value"

    private fun isAbsoluteUrl(value: String): Boolean =
        value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true)
}
