#include <jni.h>
#include <string>
#include <unistd.h>
#include <vector>
#include <sys/stat.h>
#include <fstream>
#include <sys/system_properties.h>
#include <android/log.h>

#define LOG_TAG "SecurityEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define ERR_CODE_ROOTED 0x47

// ------------------------------------------------------------------------
// [Helper] 탐지 로직 함수들 (반드시 extern "C" 위에 있어야 함!)
// ------------------------------------------------------------------------

// 1. 파일 존재 여부 확인
bool fileExists(const char* path) {
    struct stat buffer;
    return (stat(path, &buffer) == 0);
}

// 2. 위험한 파일/디렉토리 체크
bool checkRootFiles() {
    const char* paths[] = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99SuperSUDaemon",
            "/dev/com.koushikdutta.superuser.daemon/",
            "/system/app/SuperSU.apk",
            "/sbin/.magisk",
            "/sbin/.core",
            "/cache/magisk.log",
            "/data/adb/magisk",
            "/data/adb/magisk.db",
            "/data/adb/ksu",
            "/data/adb/kernelsu",
            "/data/data/me.weishu.kernelsu",
            //"/system/bin/sh", // [테스트용] 필요할 때만 주석 해제
            nullptr
    };

    for (int i = 0; paths[i] != nullptr; i++) {
        if (fileExists(paths[i])) {
            LOGE("[탐지] 파일 발견됨: %s", paths[i]);
            return true;
        }
    }
    return false;
}

// 3. 빌드 태그 확인
bool checkBuildTags(JNIEnv* env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID tagsField = env->GetStaticFieldID(buildClass, "TAGS", "Ljava/lang/String;");
    jstring tags = (jstring)env->GetStaticObjectField(buildClass, tagsField);

    const char* tagsStr = env->GetStringUTFChars(tags, nullptr);
    bool isTestKeys = (strstr(tagsStr, "test-keys") != nullptr);

    if (isTestKeys) {
        LOGE("[탐지] test-keys 빌드 태그 발견");
    }

    env->ReleaseStringUTFChars(tags, tagsStr);
    return isTestKeys;
}

// 4. 시스템 속성(Props) 확인 (이 함수가 없어서 에러가 났었습니다!)
bool checkSystemProperties() {
    char property[PROP_VALUE_MAX];

    /*
    // ro.debuggable = 1 (디버깅 가능)
    if (__system_property_get("ro.debuggable", property) > 0) {
        if (strcmp(property, "1") == 0) {
            LOGE("[탐지] ro.debuggable = 1");
            return true;
        }
    }

     ro.secure = 0 (보안 해제)
    if (__system_property_get("ro.secure", property) > 0) {
        if (strcmp(property, "0") == 0) {
            LOGE("[탐지] ro.secure = 0");
            return true;
        }
    }
    */
    return false;
}

extern "C" {

// ========================================================================
// [통합] SecurityEngine.java와 연결된 함수들
// ========================================================================

// 1. API Key 가져오기
JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityEngine_getSecureApiKey(JNIEnv* env, jobject thiz) {
    const char part1[] = {'H', 'A', 'C', 'K', '\0'};
    const char part2[] = {'I', 'N', 'G', '_', 'L', '\0'};
    const char part3[] = {'A', 'B', '_', '2', '0', '\0'};
    const char part4[] = {'2', '4', '_', 'O', 'K', '\0'};

    std::string result = "";
    result.append(part1).append(part2).append(part3).append(part4);
    return env->NewStringUTF(result.c_str());
}

// 2. Anti-Debug 초기화
JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_initAntiDebug(JNIEnv* env, jobject thiz) {
    LOGD("Anti-Debug Initialized");
}

// 3. 시스템 검사 시작 (기만 전술 포함)
JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_startSystemCheck(JNIEnv *env, jobject thiz, jobject activity) {

    LOGD("--- 통합 보안 엔진 검사 시작 ---");
    bool isRooted = false;

    // 여기서 checkSystemProperties()를 호출하는데, 위쪽에 정의가 없으면 에러가 납니다.
    // 이제 위쪽(4번 함수)에 정의해두었으므로 정상 작동합니다.
    if (checkRootFiles()) isRooted = true;
    else if (checkBuildTags(env)) isRooted = true;
    else if (checkSystemProperties()) isRooted = true;
    /*else if (fileExists("/sys/fs/selinux/enforce")) {
        std::ifstream selinux("/sys/fs/selinux/enforce");
        int enforcing;
        selinux >> enforcing;
        if (enforcing == 0) {
            LOGE("[탐지] SELinux Permissive Mode");
            isRooted = true;
        }
    } */

    // Java 메서드 찾기 (SplashActivity의 메서드를 찾음)
    jclass activityClass = env->GetObjectClass(activity);
    jmethodID methodFakeError = env->GetMethodID(activityClass, "onNetworkError", "(I)V");
    jmethodID methodSuccess = env->GetMethodID(activityClass, "onSystemStable", "()V");

    if (methodFakeError == nullptr || methodSuccess == nullptr) {
        LOGE("Java 콜백 메서드를 찾을 수 없습니다.");
        return;
    }

    if (isRooted) {
        LOGE(">> 위협 감지! 기만 전술 실행 (0x47)");
        env->CallVoidMethod(activity, methodFakeError, 0x47);
    } else {
        LOGD(">> 시스템 안전. 로그인 진행");
        env->CallVoidMethod(activity, methodSuccess);
    }
}

} // extern "C" 끝