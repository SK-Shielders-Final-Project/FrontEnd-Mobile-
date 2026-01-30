import java.util.Properties
import java.io.FileInputStream

        plugins {
            alias(libs.plugins.android.application)
            alias(libs.plugins.kotlin.android)
            alias(libs.plugins.hilt)
            alias(libs.plugins.kotlin.kapt)
        }

// Load MAPS_API_KEY from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.mobility.hack"
    compileSdk = 36

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

        // Set the API key as a manifest placeholder
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY") ?: ""
        manifestPlaceholders["TOSS_CLIENT_KEY"] = localProperties.getProperty("toss.clientKey") ?: ""
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

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
}

        dependencies {
            implementation(libs.appcompat)
            implementation(libs.material)
            implementation(libs.activity)
            implementation(libs.constraintlayout)
            implementation("androidx.recyclerview:recyclerview:1.4.0")

            // Retrofit & OkHttp
            implementation("com.squareup.retrofit2:retrofit:2.11.0")
            implementation("com.squareup.retrofit2:converter-gson:2.11.0")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

            // Glide: 이미지 로딩 라이브러리 (여기에 추가됨)
            implementation("com.github.bumptech.glide:glide:4.16.0")
            annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

            // Security
            implementation(libs.security.crypto)

            // Hilt
            implementation(libs.hilt.android)
            kapt(libs.hilt.compiler)

            // Google Maps & Location
            implementation("com.google.android.gms:play-services-maps:18.2.0")
            implementation("com.google.android.gms:play-services-location:21.1.0")

            // Barcode Scanner
            implementation(libs.zxing.embedded)

            testImplementation(libs.junit)
            androidTestImplementation(libs.ext.junit)
            androidTestImplementation(libs.espresso.core)
        }