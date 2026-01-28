plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mobility.hack" // 네임스페이스 변경
    compileSdk = 35 // 최신 안정 버전 권장

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

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // UI 및 기본 테마 지원
    implementation("com.google.android.material:material:1.9.0")

    // Google Maps SDK (지도 표시)
    implementation("com.google.android.gms:play-services-maps:17.0.0")

    // Google Location SDK (내 위치 가져오기)
    implementation("com.google.android.gms:play-services-location:17.0.0")

    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:5.3.2")

    // Barcode Scanner
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}

// Kapt 블록 제거
// kapt {
//     correctErrorTypes = true
// }
