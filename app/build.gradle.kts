plugins {
    alias(libs.plugins.android.application)
    // alias(libs.plugins.hilt) // Hilt 플러그인 제거
    alias(libs.plugins.kotlin.android)
    // alias(libs.plugins.kotlin.kapt) // Kapt 플러그인 제거
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.mobility.hack" // 네임스페이스 변경
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mobility.hack"
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
    kotlinOptions { // 코틀린 옵션은 유지해도 괜찮습니다.
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Hilt 의존성 제거
    // implementation(libs.hilt.android)
    // kapt(libs.hilt.compiler)

    // Security
    implementation(libs.security.crypto)


    // Retrofit2 & OkHttp 추가
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // UI 및 기본 테마 지원
    implementation("com.google.android.material:material:1.9.0")

    // Google Maps SDK (지도 표시)
    implementation("com.google.android.gms:play-services-maps:17.0.0")

    // Google Location SDK (내 위치 가져오기)
    implementation("com.google.android.gms:play-services-location:17.0.0")
}

// Kapt 블록 제거
// kapt {
//     correctErrorTypes = true
// }
