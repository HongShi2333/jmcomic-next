# Keep the Java API model metadata used by jmcomic's reflective adapters.
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep class io.github.jukomu.jmcomic.api.model.** { *; }

# Keep the Android image processor and its SPI implementation for release builds.
# Without these rules ServiceLoader can fall back to the unavailable AWT processor.
-keep class io.github.jukomu.jmcomic.android.support.** { *; }
-keep class io.github.jukomu.jmcomic.core.image.spi.** { *; }
-keepnames class io.github.jukomu.jmcomic.core.image.spi.** { *; }
-dontwarn io.github.jukomu.jmcomic.core.image.AwtImageProcessor

-dontwarn org.conscrypt.**
