// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false

    // [관리] Google Maps API 키 보안 관리 플러그인
    // 주의: API 키는 절대 git에 포함되는 파일에 적지 말고 local.properties에서 관리할 것
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}