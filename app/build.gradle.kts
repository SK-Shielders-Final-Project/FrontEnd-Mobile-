import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

// Load properties from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.mobility.hack"
    compileSdk = 36
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.mobility.hack"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }

        // [수정] local.properties에서 SERVER_URL 읽어오기 (없으면 기존 IP 사용)
        // 이 코드가 있어야 자바에서 BuildConfig.BASE_URL을 쓸 수 있습니다.
        val serverUrl = localProperties.getProperty("SERVER_URL") ?: ""
        buildConfigField("String", "BASE_URL", "\"$serverUrl\"")

        // Set the API key as a manifest placeholder
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY") ?: ""
        manifestPlaceholders["TOSS_CLIENT_KEY"] = localProperties.getProperty("toss.clientKey") ?: ""
    }

    // [수정] BuildConfig 클래스 생성을 활성화
    buildFeatures {
        buildConfig = true
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

    // JDK 21 사용에 따른 설정 변경
    compileOptions {
        // Enable core library desugaring
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
}

kapt {
    correctErrorTypes = true
    useBuildCache = false // Kapt 안정성을 위해 비활성화
    arguments {
        // Hilt 안정성 및 증분 빌드 오류(NPE) 방지 핵심 인자들
        arg("dagger.hilt.disableModulesHaveInstallInCheck", "true")
        arg("kapt.incremental.apt", "false")
        arg("kapt.use.worker.api", "false")
    }
}

dependencies {
    // Enable core library desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp.urlconnection) // ⭐️ JavaNetCookieJar를 위한 의존성 추가

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Security
    implementation(libs.security.crypto)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Google Maps SDK
    implementation("com.google.android.gms:play-services-maps:17.0.0")

    // Google Location SDK
    implementation("com.google.android.gms:play-services-location:17.0.0")

    // Barcode Scanner
    implementation(libs.zxing.embedded)

    implementation("org.jsoup:jsoup:1.17.2")
}