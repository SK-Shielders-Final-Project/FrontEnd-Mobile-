pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // 플러그인 버전 해석 전략 추가
    resolutionStrategy {
        eachPlugin {
            // 코틀린 안드로이드 플러그인을 발견하면, 버전을 1.9.0으로 강제합니다.
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useVersion("1.9.0")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
        maven { url = uri("https://repo.tosspayments.com/repository/maven-public/") }
    }
}

rootProject.name = "mobile"
include(":app")
