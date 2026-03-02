import java.util.Calendar
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.baselineprofile)
}

// CalVer: versionCode = YYYYMMDD, versionName = "YYYY.MMDD-alpha"
val calVerDate: Calendar = Calendar.getInstance()
val calVerCode = calVerDate.get(Calendar.YEAR) * 10000 +
    (calVerDate.get(Calendar.MONTH) + 1) * 100 +
    calVerDate.get(Calendar.DAY_OF_MONTH)
val calVerName = "%d.%02d%02d-alpha".format(
    calVerDate.get(Calendar.YEAR),
    calVerDate.get(Calendar.MONTH) + 1,
    calVerDate.get(Calendar.DAY_OF_MONTH),
)

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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

baselineProfile {
    automaticGenerationDuringBuild = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    // Debug tools
    implementation(libs.timber)
    debugImplementation(libs.leakcanary)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Firebase (uncomment after adding google-services.json)
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.crashlytics)
    // implementation(libs.firebase.analytics)
    // implementation(libs.firebase.perf)

    // Window Manager
    implementation(libs.window)

    // WorkManager & Paging
    implementation(libs.work.runtime.ktx)
    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)

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
