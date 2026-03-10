import java.util.Calendar
import java.util.Properties
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    // Firebase plugins — only applied when google-services.json exists
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.detekt)
    alias(libs.plugins.play.publisher)
}

// Conditionally apply Firebase plugins when google-services.json exists
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
    apply(plugin = libs.plugins.firebase.perf.get().pluginId)
}

// CalVer: versionCode = YYMMDDHHmm (fits Int), versionName = "ZodiacName.MMDD.HHmm-a"
// Chinese Zodiac Element+Animal names map to years (cutover Jan 1, not Lunar New Year).
// CI passes BUILD_TIMESTAMP env var (e.g. "FireHorse.0307.0806") so the APK version matches
// the release tag exactly. Local builds fall back to Calendar.getInstance().
val ZODIAC_NAMES = mapOf(
    2024 to "WoodDragon", 2025 to "WoodSnake",
    2026 to "FireHorse", 2027 to "FireGoat",
    2028 to "EarthMonkey", 2029 to "EarthRooster",
    2030 to "MetalDog", 2031 to "MetalPig",
    2032 to "WaterRat", 2033 to "WaterOx",
    2034 to "WoodTiger", 2035 to "WoodRabbit",
)
val ZODIAC_YEARS = ZODIAC_NAMES.entries.associate { (k, v) -> v to k }

fun computeCalVer(): Pair<Int, String> {
    val ts = System.getenv("BUILD_TIMESTAMP") // "FireHorse.0307.0806" from CI
    if (ts != null) {
        val parts = ts.split(".", limit = 3)
        val yyyy = ZODIAC_YEARS[parts[0]] ?: error("Unknown zodiac: ${parts[0]}")
        val mo = parts[1].substring(0, 2).toInt()
        val dd = parts[1].substring(2, 4).toInt()
        val hh = parts[2].substring(0, 2).toInt()
        val mi = parts[2].substring(2, 4).toInt()
        return ((yyyy - 2000) * 10_000_000 + mo * 1_000_000 + dd * 10_000 + hh * 100 + mi) to "$ts-a"
    }
    val d = Calendar.getInstance(TimeZone.getTimeZone("America/Chicago"))
    val year = d.get(Calendar.YEAR)
    val zodiac = ZODIAC_NAMES[year] ?: error("No zodiac name for year $year")
    val code = (year - 2000) * 10_000_000 +
        (d.get(Calendar.MONTH) + 1) * 1_000_000 +
        d.get(Calendar.DAY_OF_MONTH) * 10_000 +
        d.get(Calendar.HOUR_OF_DAY) * 100 +
        d.get(Calendar.MINUTE)
    val name = "%s.%02d%02d.%02d%02d-a".format(
        zodiac, d.get(Calendar.MONTH) + 1,
        d.get(Calendar.DAY_OF_MONTH), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE),
    )
    return code to name
}
val (calVerCode, calVerName) = computeCalVer()

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "dev.spyglass.android"
    compileSdk = 35

    signingConfigs {
        create("release") {
            val ks = localProps.getProperty("RELEASE_STORE_FILE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    defaultConfig {
        applicationId = "dev.spyglass.android"
        minSdk = 26
        targetSdk = 35
        versionCode = calVerCode
        versionName = calVerName

        // AdMob — defaults to test IDs; override in local.properties for production
        val testAppId = "ca-app-pub-3940256099942544~3347511713"
        val testBannerId = "ca-app-pub-3940256099942544/9214589741"
        val admobAppId = localProps.getProperty("ADMOB_APP_ID")
            .let { if (it.isNullOrBlank()) testAppId else it }
        val admobBannerId = localProps.getProperty("ADMOB_BANNER_ID")
            .let { if (it.isNullOrBlank()) testBannerId else it }
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

baselineProfile {
    automaticGenerationDuringBuild = false
}

play {
    val saPath = localProps.getProperty("PLAY_SERVICE_ACCOUNT_JSON", "")
    if (saPath.isNotBlank()) {
        serviceAccountCredentials.set(file(saPath))
    }
    track.set("internal")
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.DRAFT)
    defaultToAppBundles.set(true)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    // Debug tools
    implementation(libs.timber)
    debugImplementation(libs.leakcanary)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Firebase — disabled at runtime by default; enabled only with user consent
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)

    // In-App Review
    implementation(libs.play.review.ktx)

    // Ads — AdMob + mediation adapters
    implementation(libs.play.services.ads)
    implementation(libs.admob.mediation.meta)
    implementation(libs.admob.mediation.unity)
    implementation(libs.admob.mediation.applovin)
    implementation(libs.admob.mediation.ironsource)
    implementation(libs.unity.ads)

    // Window Manager
    implementation(libs.window)

    // WorkManager & Paging
    implementation(libs.work.runtime.ktx)
    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)

    // Spyglass Connect — QR scanning + CameraX
    implementation(libs.zxing.android.embedded)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Biometric
    implementation(libs.biometric)
    implementation(libs.appcompat)

    // Baseline Profiles
    implementation(libs.profileinstaller)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
