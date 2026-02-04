#include <jni.h>
#include <string>
#include <unistd.h>
#include <vector>
#include <pthread.h>
#include <dirent.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <android/log.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#ifdef __cplusplus
#include <atomic>
std::atomic<bool> g_frida_detected(false);
std::atomic<bool> g_root_detected(false); // 루팅 상태 공유용 추가
#else
_Atomic bool g_frida_detected = false;
_Atomic bool g_root_detected = false;
#endif

#define TAG "SecurityEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define ERR_CODE_ROOTED 0x47

// =============================================================
// [Part 1] 공용 유틸리티 및 탐지 로직 (Helper Functions)
// =============================================================

char* my_strstr(const char *haystack, const char *needle) {
    if (!*needle) return (char *)haystack;
    for (; *haystack; haystack++) {
        const char *h = haystack;
        const char *n = needle;
        while (*h && *n && *h == *n) { h++; n++; }
        if (!*n) return (char *)haystack;
    }
    return NULL;
}

// [루트탐지] 파일 기반 탐지 로직 (Context 불필요, OnLoad 시 실행 가능)
bool check_root_files_native() {
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
            LOGE(">> [탐지] Rooting File Found: %s", path);
            return true;
        }
    }
    return false;
}

// [프리다] 스레드 검사
bool check_frida_threads() {
    DIR *dir = opendir("/proc/self/task");
    if (!dir) return false;

    struct dirent *entry;
    char path[256];
    char buf[256];
    bool detected = false;

    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] == '.') continue;
        snprintf(path, sizeof(path), "/proc/self/task/%s/comm", entry->d_name);
        int fd = open(path, O_RDONLY);
        if (fd >= 0) {
            int len = read(fd, buf, sizeof(buf) - 1);
            if (len > 0) {
                buf[len] = 0;
                if (my_strstr(buf, "gmain") || my_strstr(buf, "gum-js-loop") ||
                    my_strstr(buf, "pool-frida") || my_strstr(buf, "linjector")) {
                    LOGE(">> [탐지] Frida Thread: %s", buf);
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

// [프리다] 아티팩트(메모리 맵) 검사
bool check_frida_artifacts() {
    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) return false;

    char line[512];
    bool detected = false;
    while (fgets(line, sizeof(line), fp)) {
        if (my_strstr(line, "frida") || my_strstr(line, "gum-js") || my_strstr(line, "gadget")) {
            LOGE(">> [탐지] Frida Artifact Found");
            detected = true;
            break;
        }
    }
    fclose(fp);
    return detected;
}

// [프리다] 디버거(TracerPid) 검사
bool check_tracer_pid() {
    FILE *fp = fopen("/proc/self/status", "r");
    if (!fp) return false;
    char line[512];
    bool detected = false;
    while (fgets(line, sizeof(line), fp)) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            int pid = atoi(&line[10]);
            if (pid > 0) {
                LOGE(">> [탐지] Debugger(TracerPid) Found: %d", pid);
                detected = true;
            }
            break;
        }
    }
    fclose(fp);
    return detected;
}

// [프리다] 서버 파일 검사
bool check_frida_files() {
    const char* targets[] = {
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/data/local/tmp/frida"
    };

    for (int i = 0; i < 3; i++) {
        if (access(targets[i], F_OK) == 0) {
            LOGE(">> [탐지] 위험 경로에 Frida 관련 파일 존재: %s", targets[i]);
            return true;
        }
    }
    return false;
}

// [프리다] 포트 스캔 (Default Port: 27042)
bool check_frida_ports() {
    struct sockaddr_in sa;
    sa.sin_family = AF_INET;
    sa.sin_addr.s_addr = inet_addr("127.0.0.1");

    // 검사할 포트 목록 (기본 27042, 가끔 27043도 사용됨)
    int ports[] = {27042, 27043};
    int count = sizeof(ports) / sizeof(ports[0]);

    for (int i = 0; i < count; i++) {
        int sock = socket(AF_INET, SOCK_STREAM, 0);
        if (sock < 0) continue;

        sa.sin_port = htons(ports[i]);

        // connect 시도 (성공하면 누군가 리스닝 중이라는 뜻)
        if (connect(sock, (struct sockaddr*)&sa, sizeof(sa)) == 0) {
            LOGE(">> [탐지] Frida Server Port Detected: %d", ports[i]);
            close(sock);
            return true;
        }

        close(sock);
    }
    return false;
}

// 아키텍처별 분기 처리
void my_force_exit(int status) {

    // [1] ARM64 (현재 안드로이드 폰의 95% 이상)
#if defined(__aarch64__)
    // Syscall Number 93 = exit
        // Register x8 = Syscall Num, x0 = status
        __asm__ volatile (
            "mov x8, #93\n"
            "mov x0, %0\n"
            "svc #0\n"
            :
            : "r"((long)status)
            : "x0", "x8"
        );

    // [2] ARM32 (구형 안드로이드 폰)
#elif defined(__arm__)
    // Syscall Number 1 = exit
        // Register r7 = Syscall Num, r0 = status
        __asm__ volatile (
            "mov r7, #1\n"
            "mov r0, %0\n"
            "svc #0\n"
            :
            : "r"((long)status)
            : "r0", "r7"
        );

    // [3] x86_64 (최신 에뮬레이터 - 지니모션, 블루스택 64비트)
#elif defined(__x86_64__)
    // Syscall Number 60 = exit
        // Register rax = Syscall Num, rdi = status
        __asm__ volatile (
            "mov $60, %%rax\n"
            "mov %0, %%rdi\n"
            "syscall\n"
            :
            : "r"((long)status)
            : "rax", "rdi"
        );

    // [4] x86 (구형 에뮬레이터)
#elif defined(__i386__)
    // Syscall Number 1 = exit
    // Register eax = Syscall Num, ebx = status
    __asm__ volatile (
            "mov $1, %%eax\n"
            "mov %0, %%ebx\n"
            "int $0x80\n"
            :
            : "r"(status)
            : "eax", "ebx"
            );
#endif
}

// =============================================================
// [Part 2] 보안 정책 집행기 (Enforcer)
// =============================================================

inline bool perform_full_check() {

    return false; //테스트용

    // 1. 프리다 체크
    if (check_frida_threads()) return true;
    if (check_frida_artifacts()) return true;
    if (check_tracer_pid()) return true;
    if (check_frida_files()) return true;
    if (check_frida_ports()) return true;

    // 2. 루팅(파일) 체크 추가
    if (check_root_files_native()) return true;

    return false;
}

void enforce_policy(bool hard_kill) {
    if (perform_full_check()) {
        // 탐지 플래그 설정
#ifdef __cplusplus
        g_frida_detected.store(true);
        g_root_detected.store(true);
#else
        g_frida_detected = true;
        g_root_detected = true;
#endif

        static bool killed_once = false;

        // [보안 강제] 로드 시점이나 감시 스레드에서 발견 시 즉시 종료
        if (hard_kill && !killed_once) {
            killed_once = true;
            LOGE(">> [보안] 위협 탐지됨. 프로세스 강제 종료.");
            // kill(getpid(), SIGKILL); // 실제 배포 시 주석 해제하여 사용
            //exit(0); // 또는 exit 사용
            my_force_exit(0);
        }
    }
}

// =============================================================
// [Part 3] 자동 실행 로직 (Constructor & Monitor)
// =============================================================

typedef struct {
    int total_duration_sec;
    int interval_ms;
} MonitorConfig;

void* monitor_loop(void* args) {
    MonitorConfig* config = (MonitorConfig*)args;
    int elapsed_ms = 0;

    while (config->total_duration_sec == 0 || (elapsed_ms < config->total_duration_sec * 1000)) {
        enforce_policy(true);
        usleep(config->interval_ms * 1000);
        elapsed_ms += config->interval_ms;
    }

    free(config);
    return NULL;
}

void start_monitor_thread(int duration, int interval) {
    pthread_t t;
    MonitorConfig* config = (MonitorConfig*)malloc(sizeof(MonitorConfig));
    if (!config) return;

    config->total_duration_sec = duration;
    config->interval_ms = interval;

    if (pthread_create(&t, NULL, monitor_loop, config) == 0) {
        pthread_detach(t);
    } else {
        free(config);
    }
}

// ★★★ [핵심] 라이브러리 로드(onload) 시 자동 실행 ★★★
__attribute__((constructor))
void native_init() {
    LOGE(">> [Native] Library Loaded - Security Check Start");

    // 1. 앱 로드 즉시 1회 강력 검사 (Rooting + Frida)
    enforce_policy(true);

    // 2. Early Bird: 초기 2초간 0.1초 간격 집중 감시
    start_monitor_thread(2, 100);
}

// =============================================================
// [Part 4] JNI 인터페이스 (Java와 통신)
// =============================================================

extern "C" {

/**
 * [보안 기믹 1] API Key 조각화 및 루프 기반 복원
 */
JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityBridge_getSecureApiKey(JNIEnv* env, jobject /* this */) {
    const char part1[] = {'H', 'A', 'C', 'K', '\0'};
    const char part2[] = {'I', 'N', 'G', '_', 'L', '\0'};
    const char part3[] = {'A', 'B', '_', '2', '0', '\0'};
    const char part4[] = {'2', '4', '_', 'O', 'K', '\0'};

    const char* parts[] = {part1, part2, part3, part4};
    std::string result = "";

    for (int i = 0; i < 4; i++) {
        result.append(parts[i]);
    }
    return env->NewStringUTF(result.c_str());
}

/**
 * [보안 기믹 2] 다계층 루팅 탐지 (수동 호출용)
 * - 자동 탐지(native_init)에서 이미 파일 검사를 수행했지만,
 * 여기서는 PackageManager를 이용한 정밀 검사를 추가로 수행합니다.
 */
JNIEXPORT jint JNICALL
Java_com_mobility_hack_security_SecurityBridge_detectRooting(JNIEnv* env, jobject thiz, jobject context) {

    // 1. 이미 native_init에서 탐지되었는지 확인
    if (g_root_detected.load()) {
        return ERR_CODE_ROOTED;
    }

    // 2. 경로 검사 (재확인)
    if (check_root_files_native()) {
        return ERR_CODE_ROOTED;
    }

    // 3. 패키지 검사 (Context 필요하므로 여기서만 수행 가능)
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
            env->ExceptionClear();
        } else if (pkgInfo != NULL) {
            return ERR_CODE_ROOTED;
        }
    }

    return 0; // 정상
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_initAntiDebug(JNIEnv* env, jobject thiz) {
    // 필요 시 여기서도 enforce_policy(true) 호출 가능
    // 안티 디버깅 로직 (나중에 구현하거나 일단 비워둠)
    return;
}

JNIEXPORT jstring JNICALL
Java_com_mobility_hack_util_Keys_getApiKey(JNIEnv *env, jobject thiz) {
    // TODO: implement getApiKey()
    return env->NewStringUTF("DUMMY_KEY");
}

// --- Frida 관련 JNI ---

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_startFridaMonitoring(JNIEnv *env, jobject thiz) {
    // 장기 감시: 무한(0), 3초(3000ms) 간격
    start_monitor_thread(0, 3000);
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_checkFridaOnce(JNIEnv *env, jobject thiz) {
    enforce_policy(true);
}

JNIEXPORT jboolean JNICALL
Java_com_mobility_hack_security_SecurityEngine_wasFridaDetectedEarly(JNIEnv *env, jobject thiz) {
#ifdef __cplusplus
    return (jboolean)g_frida_detected.load();
#else
    return (jboolean)g_frida_detected;
#endif
}

} // extern "C"