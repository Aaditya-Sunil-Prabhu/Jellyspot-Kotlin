# Jellyspot ProGuard Rules

# Keep Jellyfin SDK
-keep class org.jellyfin.sdk.** { *; }
-keepclassmembers class org.jellyfin.sdk.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.jellyspot.**$$serializer { *; }
-keepclassmembers class com.jellyspot.** {
    *** Companion;
}
-keepclasseswithmembers class com.jellyspot.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor - dontwarn for JVM classes not available on Android
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.util.debug.**
-dontwarn java.lang.management.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coil3
-keep class coil3.** { *; }
-dontwarn coil3.PlatformContext
-dontwarn coil3.network.**

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# General Android
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
