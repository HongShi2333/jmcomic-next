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
-keep class com.par9uet.jm.database.model.** { *; }
-keep class com.par9uet.jm.ui.models.** { *; }
-keep class com.par9uet.jm.task.AppTaskInfo { *; }
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

# OkHttp cookies are persisted with Gson.
-keep class okhttp3.Cookie { *; }
-dontwarn okhttp3.**
