# Jellyspot ProGuard Rules

# Keep Jellyfin SDK
-keep class org.jellyfin.sdk.** { *; }
-keepclassmembers class org.jellyfin.sdk.** { *; }

# Keep NewPipe Extractor
-keep class org.schabi.newpipe.** { *; }
-keepclassmembers class org.schabi.newpipe.** { *; }

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

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coil
-keep class coil3.** { *; }
