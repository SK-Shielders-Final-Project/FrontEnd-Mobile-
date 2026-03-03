import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

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

        val serverUrl = localProperties.getProperty("SERVER_URL") ?: ""
        buildConfigField("String", "BASE_URL", "\"$serverUrl\"")

        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY") ?: ""
        manifestPlaceholders["TOSS_CLIENT_KEY"] = localProperties.getProperty("toss.clientKey") ?: ""
    }

    sourceSets {
        getByName("main") {
            jni.srcDirs("src/main/cpp")
        }
    }

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
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Security
    implementation(libs.security.crypto)

    // Hilt (버전 업데이트: 2.54는 Kotlin 2.1 메타데이터를 지원합니다)
    implementation("com.google.dagger:hilt-android:2.54")
    kapt("com.google.dagger:hilt-android-compiler:2.54")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.gms:play-services-maps:17.0.0")
    implementation("com.google.android.gms:play-services-location:17.0.0")
    implementation(libs.zxing.embedded)
    implementation("org.jsoup:jsoup:1.17.2")
}
