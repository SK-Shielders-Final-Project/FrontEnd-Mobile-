#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dirent.h>
#include <signal.h> // kill() 사용을 위해
#include <android/log.h>
#include <stdlib.h>
#include <stdio.h>

#define TAG "SecurityEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// =============================================================
// [1] 탐지 로직 (Helper Functions)
// =============================================================

// 문자열 검색 도구
char* my_strstr(const char *haystack, const char *needle) {
    if (!*needle) return (char *)haystack;
    for (; *haystack; haystack++) {
        const char *h = haystack, *n = needle;
        while (*h && *n && *h == *n) { h++; n++; }
        if (!*n) return (char *)haystack;
    }
    return NULL;
}

// 1. Frida 스레드 검사
bool check_frida_threads() {
    DIR *dir = opendir("/proc/self/task");
    if (!dir) return false;
    struct dirent *entry;
    char path[256], buf[256];
    bool detected = false;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] == '.') continue;
        snprintf(path, sizeof(path), "/proc/self/task/%s/comm", entry->d_name);
        int fd = open(path, O_RDONLY);
        if (fd >= 0) {
            int len = read(fd, buf, sizeof(buf) - 1);
            if (len > 0) {
                buf[len] = 0;
                // 프리다 특유의 스레드 이름들
                if (my_strstr(buf, "gmain") || my_strstr(buf, "gum-js-loop") ||
                    my_strstr(buf, "pool-frida") || my_strstr(buf, "linjector")) {
                    detected = true;
                }
            }
            close(fd);
        }
        if (detected) break;
    }
    closedir(dir);
    return detected;
}

// 2. 루팅/프리다 파일 검사 (통합)
bool check_files() {
    const char* paths[] = {
            "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/tmp/su",
            "/data/local/tmp/frida-server", "/data/local/tmp/re.frida.server",
            "/sbin/.magisk", "/data/adb/magisk.db", "/data/adb/ksu", nullptr
    };
    struct stat buffer;
    for (int i = 0; paths[i] != nullptr; i++) {
        if (stat(paths[i], &buffer) == 0) return true;
    }
    return false;
}

// 3. 빌드 태그 확인 (JNI 필요)
bool checkBuildTags(JNIEnv* env) {
    jclass buildClass = env->FindClass("android/os/Build");
    jfieldID tagsField = env->GetStaticFieldID(buildClass, "TAGS", "Ljava/lang/String;");
    jstring tags = (jstring)env->GetStaticObjectField(buildClass, tagsField);
    const char* tagsStr = env->GetStringUTFChars(tags, nullptr);
    bool isTestKeys = (my_strstr(tagsStr, "test-keys") != NULL);
    env->ReleaseStringUTFChars(tags, tagsStr);
    return isTestKeys;
}

// =============================================================
// [2] 초기화 시점 방어 (Java 호출 없이 자동 실행)
// =============================================================

// ① [가장 빠름] 라이브러리 로드 시 생성자 단계에서 실행
// JNIEnv가 없으므로 리눅스 시스템 콜 기반 검사만 수행 (파일, 스레드)
__attribute__((constructor))
void native_init() {
    bool compromised = false;

    if (check_files()) compromised = true;
    if (check_frida_threads()) compromised = true;

// [수정 전]
/*
    if (compromised) {
        LOGE(">> [CRITICAL] 초기화 단계에서 위협 감지됨. 프로세스 제거.");
        // 조용하고 확실하게 죽임 (Java로 돌아가지 않음)
        kill(getpid(), SIGKILL);
    }
*/

// [수정 후: 테스트용]
    if (compromised) {
        LOGE(">> [테스트 모드] 위협이 감지되었으나 종료하지 않음.");
        // kill(getpid(), SIGKILL);  // <--- ★ 이 줄을 주석 처리하세요
    }
}

// ② [두 번째] JNI 로드 단계 (Java 연동 직전)
// 여기서 JNIEnv를 쓸 수 있으므로 빌드 태그 등을 추가 검사
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // 1차: 이미 생성자에서 걸러졌겠지만 한번 더 확인
    if (check_frida_threads()) {
        // kill(getpid(), SIGKILL); // <--- ★ 주석 처리
        // return JNI_ERR;          // <--- ★ 주석 처리 (에러 리턴 막기)
        LOGE(">> [테스트 모드] Frida 스레드 감지됨 (무시)");
    }

    // 2차: JNI가 필요한 검사 수행
    if (checkBuildTags(env)) {
        LOGE(">> [CRITICAL] Test-Keys 빌드 감지됨.");
        // JNI_ERR를 리턴하면 Java에서 UnsatisfiedLinkError가 발생하며 앱이 뻗음
        // return JNI_ERR;          // <--- ★ 주석 처리 (에러 리턴 막기)
        LOGE(">> [테스트 모드] 빌드 태그 감지됨 (무시)");
    }

    return JNI_VERSION_1_6; // 정상 로드
}

// =============================================================
// [3] Java 연동 함수 (기존 유지하되 startSystemCheck는 제거)
// =============================================================
extern "C" {

// API Key 가져오기 (탐지 시 엉뚱한 값 반환 로직 추가 가능)
JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityEngine_getSecureApiKey(JNIEnv* env, jobject thiz) {
    // 만약 여기까지 살아서 왔다면 정상일 확률이 높음
    return env->NewStringUTF("REAL_SECRET_KEY_HACKING_LAB_2024");
}

// Anti-Debug (빈 껍데기만 남김 - 이미 native_init에서 수행함)
JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_initAntiDebug(JNIEnv* env, jobject thiz) {
    // Do nothing or additional runtime check
}

// Frida 모니터링 (백그라운드 감시 스레드 시작용)
JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_startFridaMonitoring(JNIEnv *env, jobject thiz) {
    // (필요하다면 여기에 스레드 생성 코드 추가 - 이전 코드의 start_monitor_thread 활용)
}

// ★ startSystemCheck 삭제됨! ★
// (Java에서 호출할 필요가 없어졌으므로 제거합니다.)

} // extern "C"