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

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**

# Tink / AndroidX Security Crypto
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# AdMob mediation adapters
-keep class com.google.ads.mediation.** { *; }
-dontwarn com.google.ads.mediation.**

# Meta Audience Network
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# Unity Ads
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.ads.**
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.services.**

# AppLovin
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**

# ironSource
-keep class com.ironsource.** { *; }
-dontwarn com.ironsource.**
-keep class com.unity3d.mediation.** { *; }
-dontwarn com.unity3d.mediation.**
