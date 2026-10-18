package com.par9uet.jm.storage

import com.google.gson.reflect.TypeToken
import com.par9uet.jm.repository.CachedComicTags
import com.par9uet.jm.repository.ComicTagCache

class ComicTagCacheStorage(private val storage: SecureStorage) : ComicTagCache {
    override suspend fun read(): Map<String, CachedComicTags> =
        storage.get<Map<String, CachedComicTags>>(
            "catalogTagCacheV1",
            object : TypeToken<Map<String, CachedComicTags>>() {}.type,
        ).orEmpty()

    override suspend fun write(entries: Map<String, CachedComicTags>) {
        storage.set("catalogTagCacheV1", entries)
    }
}
