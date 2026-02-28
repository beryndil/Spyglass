# Spyglass — ProGuard rules

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.spyglass.android.**$$serializer { *; }
-keepclassmembers class dev.spyglass.android.** {
    *** Companion;
}
-keepclasseswithmembers class dev.spyglass.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities (only @Entity-annotated classes)
-keep @androidx.room.Entity class dev.spyglass.android.data.db.entities.** { *; }
-keep @androidx.room.Dao class * { *; }

# Keep ViewModel constructors for reflection-based instantiation
-keep class dev.spyglass.android.** extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Kotlin
-dontwarn kotlin.**
