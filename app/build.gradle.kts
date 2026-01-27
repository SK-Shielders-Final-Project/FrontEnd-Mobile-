plugins {
    alias(libs.plugins.android.application)
    // id("org.jetbrains.kotlin.android") // 중복 오류의 원인이므로 다시 주석 처리합니다.
}

import java.util.Properties
import java.io.FileInputStream

val properties = Properties()
try {
    properties.load(FileInputStream(rootProject.file("local.properties")))
} catch (e: Exception) {
    // local.properties 파일이 없는 경우, CI/CD 환경 등을 고려하여 무시
}

android {
    namespace = "com.example.mobilityhack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mobilityhack"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }

        buildConfigField("String", "TOSS_CLIENT_KEY", "\"${properties.getProperty("toss.clientKey")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.zxing.embedded)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
