import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

// [핵심] local.properties 파일 로드
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

        // -------------------------------------------------------------------
        // [수정 포인트] local.properties의 "SERVER_URL"을 읽어서 자바 코드(BuildConfig)로 넘김
        // -------------------------------------------------------------------
        val serverUrl = localProperties.getProperty("SERVER_URL") ?: ""

        // BuildConfig.BASE_URL 변수 생성
        buildConfigField("String", "BASE_URL", "\"$serverUrl\"")

        // AndroidManifest.xml에서 쓸 변수들 (${MAPS_API_KEY} 등으로 사용)
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY") ?: ""
        manifestPlaceholders["TOSS_CLIENT_KEY"] = localProperties.getProperty("toss.clientKey") ?: ""
    }

    // [필수] BuildConfig 클래스 생성을 활성화해야 위 설정이 먹힙니다.
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

    // JDK 21 설정
    compileOptions {
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
    useBuildCache = false
    arguments {
        arg("dagger.hilt.disableModulesHaveInstallInCheck", "true")
        arg("kapt.incremental.apt", "false")
        arg("kapt.use.worker.api", "false")
    }
}

dependencies {
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
    implementation(libs.okhttp.urlconnection)

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
    implementation("com.google.android.gms:play-services-location:17.0.0")

    // Barcode Scanner
    implementation(libs.zxing.embedded)

    implementation("org.jsoup:jsoup:1.17.2")
}