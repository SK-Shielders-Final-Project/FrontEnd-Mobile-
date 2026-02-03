#include <jni.h>
#include <pthread.h>
#include <unistd.h>
#include <dirent.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <android/log.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>

// [추가] C++ 원자적 연산을 위한 헤더 (멀티스레드 탐지 상태 공유)
#ifdef __cplusplus
#include <atomic>
std::atomic<bool> g_frida_detected(false);
#else
_Atomic bool g_frida_detected = false;
#endif

#define TAG "SecurityEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// =============================================================
// [1] 도구 함수 (분석을 어렵게 하기 위해 직접 구현)
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

// =============================================================
// [2] 개별 탐지 로직
// =============================================================

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

// [신규 추가] D. 특정 경로의 Frida 서버 파일 존재 여부 검사
bool check_frida_files() {
    const char* targets[] = {
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/data/local/tmp/frida"
    };

    for (int i = 0; i < 3; i++) {
        // access() 함수를 통해 파일 존재 여부(F_OK) 확인
        if (access(targets[i], F_OK) == 0) {
            LOGE(">> [탐지] 위험 경로에 Frida 관련 파일 존재: %s", targets[i]);
            return true;
        }
    }
    return false;
}

// =============================================================
// [3] 보안 코어 및 정책 실행
// =============================================================

inline bool perform_full_check() {
    if (check_frida_threads()) return true;
    if (check_frida_artifacts()) return true;
    if (check_tracer_pid()) return true;
    if (check_frida_files()) return true;
    return false;
}

void enforce_policy(bool hard_kill) {
    //안티프리다 쓸 때
/*    if (perform_full_check()) {
        // 탐지 플래그 설정 (Java에서 확인 가능)
#ifdef __cplusplus
        g_frida_detected.store(true);
#else
        g_frida_detected = true;
#endif

        static bool killed_once = false;

        if (hard_kill && !killed_once) {
            killed_once = true;
            kill(getpid(), SIGKILL);
        }
    }*/

    // 안티프리다 안쓸 때
    return;
}

// =============================================================
// [4] 감시 매니저 (스레드)
// =============================================================

typedef struct {
    int total_duration_sec;
    int interval_ms;
} MonitorConfig;

void* monitor_loop(void* args) {
    MonitorConfig* config = (MonitorConfig*)args;
    int elapsed_ms = 0;

    // 백그라운드 감시는 탐지 즉시 hard_kill 수행
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

// =============================================================
// [5] JNI 진입점
// =============================================================

__attribute__((constructor))
void native_init() {
    // 앱 로드 즉시 1회 검사 (Hard Kill)
    enforce_policy(true);
    // Early Bird: 2초간 0.1초 간격 감시 (Hard Kill)
    start_monitor_thread(2, 100);
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_startFridaMonitoring(JNIEnv *env, jobject thiz) {
    // 장기 감시: 무한(0), 3초(3000ms) 간격
    start_monitor_thread(0, 3000);
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_checkFridaOnce(JNIEnv *env, jobject thiz) {
    // 중요 로직 전 수동 검사
    enforce_policy(true);
}

JNIEXPORT jboolean JNICALL
Java_com_mobility_hack_security_SecurityEngine_wasFridaDetectedEarly(JNIEnv *env, jobject thiz) {
    // 탐지 플래그 반환
#ifdef __cplusplus
    return (jboolean)g_frida_detected.load();
#else
    return (jboolean)g_frida_detected;
#endif
}

}