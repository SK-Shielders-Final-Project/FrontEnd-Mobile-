plugins {
    alias(libs.plugins.android.application)
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
        
        // NDK 설정 추가
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // [4] 취약점: 난독화 미적용
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
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