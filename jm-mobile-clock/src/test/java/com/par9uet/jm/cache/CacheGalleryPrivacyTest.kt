package com.par9uet.jm.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheGalleryPrivacyTest {
    @Test
    fun buildsRelativePathInsideSelectedFolder() {
        assertEquals("Download/JmCache/", relativePathFromExternalStorageDocumentId("primary:Download/JmCache"))
        assertEquals("Comics/", relativePathFromExternalStorageDocumentId("ABCD-1234:Comics"))
        assertEquals("Foo/Bar/", relativePathFromExternalStorageDocumentId("primary:Foo/Bar/"))
    }

    @Test
    fun ignoresVolumeRootSoGalleryCleanupCannotMatchEntireStorage() {
        assertNull(relativePathFromExternalStorageDocumentId("primary:"))
        assertNull(relativePathFromExternalStorageDocumentId("ABCD-1234:"))
        assertNull(relativePathFromExternalStorageDocumentId("primary"))
    }

    @Test
    fun recognizesCacheImageNamesOnly() {
        assertTrue(isCacheImageDisplayName("cover.webp"))
        assertTrue(isCacheImageDisplayName("0.webp"))
        assertTrue(isCacheImageDisplayName("12.jpg"))
        assertTrue(isCacheImageDisplayName("3.PNG"))
        assertFalse(isCacheImageDisplayName("photo.png"))
        assertFalse(isCacheImageDisplayName("config.json"))
        assertFalse(isCacheImageDisplayName(NOMEDIA_FILE_NAME))
        assertFalse(isCacheImageDisplayName("1.webp.bak"))
    }
}
