import java.util.Calendar

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// CalVer: versionCode = YYYYMMDD, versionName = "YYYY.MMDD"
val calVerDate: Calendar = Calendar.getInstance()
val calVerCode = calVerDate.get(Calendar.YEAR) * 10000 +
    (calVerDate.get(Calendar.MONTH) + 1) * 100 +
    calVerDate.get(Calendar.DAY_OF_MONTH)
val calVerName = "%d.%02d%02d".format(
    calVerDate.get(Calendar.YEAR),
    calVerDate.get(Calendar.MONTH) + 1,
    calVerDate.get(Calendar.DAY_OF_MONTH),
)

android {
    namespace = "dev.spyglass.android"
    compileSdk = 35

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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}
