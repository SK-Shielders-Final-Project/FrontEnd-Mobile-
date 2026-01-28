#include <jni.h>
#include <string>
#include <unistd.h>
#include <vector>

// [app047 기믹] 탐지 시 반환할 가짜 에러 코드
#define ERR_CODE_ROOTED 0x47

/**
 * [보안 기믹 1] API Key 조각화 및 루프 기반 복원
 * 정적 분석 시 'strings' 명령어로 전체 키가 노출되는 것을 방지
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityBridge_getSecureApiKey(JNIEnv* env, jobject /* this */) {

    // 키 조각화 (4개 이상의 배열)
    const char part1[] = {'H', 'A', 'C', 'K', '\0'};
    const char part2[] = {'I', 'N', 'G', '_', 'L', '\0'};
    const char part3[] = {'A', 'B', '_', '2', '0', '\0'};
    const char part4[] = {'2', '4', '_', 'O', 'K', '\0'};

    const char* parts[] = {part1, part2, part3, part4};
    std::string result = "";

    // [app047 기믹] 단순 strcat 대신 루프를 사용해 동적으로 결합
    for (int i = 0; i < 4; i++) {
        result.append(parts[i]);
    }

    return env->NewStringUTF(result.c_str());
}

/**
 * [보안 기믹 2] 다계층 루팅 탐지
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_mobility_hack_security_SecurityBridge_detectRooting(JNIEnv* env, jobject thiz, jobject context) {

    // 1. 경로 검사 (access 함수 사용)
    const char* rootPaths[] = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su"
    };

    for (const char* path : rootPaths) {
        if (access(path, F_OK) == 0) {
            return ERR_CODE_ROOTED; // 탐지 시 0x47 반환
        }
    }

    // 2. 패키지 검사 (JNI를 통해 PackageManager 호출)
    // 분석 방해를 위해 패키지명도 조각내어 검사할 수 있으나 여기선 평문 예시
    const char* rootPackages[] = {
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.topjohnwu.magisk"
    };

    jclass contextClass = env->GetObjectClass(context);
    jmethodID getPM = env->GetMethodID(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jobject packageManager = env->CallObjectMethod(context, getPM);
    jclass pmClass = env->GetObjectClass(packageManager);
    jmethodID getPackageInfo = env->GetMethodID(pmClass, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");

    for (const char* pkg : rootPackages) {
        jstring pkgString = env->NewStringUTF(pkg);
        jobject pkgInfo = env->CallObjectMethod(packageManager, getPackageInfo, pkgString, 0);
        if (env->ExceptionCheck()) {
            env->ExceptionClear(); // 패키지 없으면 예외 발생하므로 클리어
        } else if (pkgInfo != NULL) {
            return ERR_CODE_ROOTED;
        }
    }


    return 0; // 정상
}

extern "C" JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_initAntiDebug(JNIEnv* env, jobject thiz) {
    // 안티 디버깅 로직 (나중에 구현하거나 일단 비워둠)
    return;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_mobility_hack_util_Keys_getApiKey(JNIEnv *env, jobject thiz) {
    // TODO: implement getApiKey()
}