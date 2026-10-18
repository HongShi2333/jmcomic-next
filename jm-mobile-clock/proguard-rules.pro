# Project ProGuard / R8 rules.

# Keep metadata needed by Retrofit, Gson, Koin, Room, and suspend functions.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Retrofit interfaces and annotations must stay visible to runtime parsing.
-keep class retrofit2.** { *; }
-keep interface * extends retrofit2.Call
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# Network response models are deserialized through Gson/Retrofit.
-keep class com.par9uet.jm.retrofit.model.** { *; }
-keepclassmembers class com.par9uet.jm.retrofit.model.** {
    *;
}

# Gson is also used for persisted local data. Keep field names so existing
# installed data and API JSON remain compatible after R8 obfuscation.
-keep class com.par9uet.jm.data.models.** { *; }
-keep class com.par9uet.jm.cache.DownloadComicCacheConfig { *; }
-keep class com.par9uet.jm.cache.DownloadComicCacheChapter { *; }
-keep class com.par9uet.jm.database.model.** { *; }
-keep class com.par9uet.jm.ui.models.** { *; }
-keep class com.par9uet.jm.task.AppTaskInfo { *; }
-keep class com.par9uet.jm.utils.** { *; }
-keep class com.par9uet.jm.utils.DownloadSpeedTracker { *; }
-keepclassmembers class com.par9uet.jm.utils.DownloadSpeedTracker {
    public static ** INSTANCE;
    <methods>;
}
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room and WorkManager rely on generated/runtime-discovered classes in release.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.par9uet.jm.database.** { *; }
-keep class com.par9uet.jm.worker.** { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-dontwarn androidx.room.**
-dontwarn androidx.work.**

# Koin resolves definitions and the WorkManager factory at runtime.
-keep class org.koin.** { *; }
-keep class com.par9uet.jm.di.** { *; }
-keep class com.par9uet.jm.store.** { *; }
-keep class com.par9uet.jm.repository.** { *; }
-keep class com.par9uet.jm.storage.** { *; }
-keep class com.par9uet.jm.JmApplication { *; }
-dontwarn org.koin.**


# JMComic-Api-Java: keep AndroidImageProcessor and SPI service files so
# ServiceLoader can discover the Android-compatible ImageProcessor at runtime
# instead of falling back to AwtImageProcessor (which uses java.awt unavailable on Android).
-keep class io.github.jukomu.jmcomic.android.support.** { *; }
-keep class io.github.jukomu.jmcomic.core.image.spi.** { *; }
-dontwarn io.github.jukomu.jmcomic.core.image.AwtImageProcessor
-keepnames class io.github.jukomu.jmcomic.core.image.spi.** { *; }

# Transitive dependency com.luciad.imageio.webp references javax.imageio.* which
# is not available on Android. Android natively supports WebP via BitmapFactory,
# so these classes are never used at runtime.
-dontnote com.luciad.imageio.webp.**
-dontwarn com.luciad.imageio.webp.**
-dontwarn javax.imageio.ImageReader
-dontwarn javax.imageio.spi.ImageReaderSpi
-dontwarn javax.imageio.spi.ImageWriterSpi
-dontwarn javax.imageio.stream.ImageInputStream
-dontwarn javax.imageio.stream.ImageOutputStream

# OkHttp cookies are persisted with Gson.
-keep class okhttp3.Cookie { *; }
-dontwarn okhttp3.**

# Core library desugaring: keep desugared java.time classes (j$.* package)
# Prevents R8 from stripping desugared java.time APIs in release builds on Android 6/7
-keep class j$.** { *; }
-dontwarn j$.**
-keep class java.time.** { *; }
-dontwarn java.time.**

