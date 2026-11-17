plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.androidvip.ddgui"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.androidvip.ddgui"
        minSdk = 24
        targetSdk = 37
        versionCode = 6
        versionName = "2.0.0"
        androidResources {
            localeFilters.addAll(listOf("en", "de", "pt-rBR"))
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-kt.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/**",
                "/androidsupportmultidexversion.txt",
                "/org/bouncycastle/**",
                "/kotlin/**",
                "/kotlinx/**",
                "/okhttp3/**"
            )
        }
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_11
        sourceCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.material)
    implementation(libs.koin.android)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.kotlinx.collections.immutable)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}
