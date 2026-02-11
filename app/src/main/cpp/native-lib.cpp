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
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/mman.h>
#include <dlfcn.h>
#include <elf.h>
#include <time.h>
#include <sys/wait.h>
#include <sys/ptrace.h>
#include <errno.h>

#ifdef __cplusplus
#include <atomic>
std::atomic<bool> g_frida_detected(false);
std::atomic<bool> g_root_detected(false);
#else
_Atomic bool g_frida_detected = false;
_Atomic bool g_root_detected = false;
#endif

#define TAG "SecurityEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define ERR_CODE_ROOTED 0x47

// =============================================================
// [Part 0] 전역 변수 및 유틸리티 (Helpers)
// =============================================================

// 1. 보안 키
volatile uint32_t g_xor_key = 0;
volatile uint32_t g_masked_len = 0;

// 2. 골든 해시
volatile uint32_t g_masked_hash_inline = 0;
volatile uint32_t g_masked_hash_rwx = 0;
volatile uint32_t g_masked_hash_exit = 0;

// CRC32
uint32_t calc_crc32(const unsigned char *s, size_t n) {
    uint32_t crc = 0xFFFFFFFF;
    for (size_t i = 0; i < n; i++) {
        char ch = s[i];
        for (size_t j = 0; j < 8; j++) {
            uint32_t b = (ch ^ crc) & 1;
            crc >>= 1;
            if (b) crc = crc ^ 0xEDB88320;
            ch >>= 1;
        }
    }
    return ~crc;
}

uint32_t get_function_checksum(void* func_ptr, size_t len) {
    if (!func_ptr) return 0;
    return calc_crc32((const unsigned char*)func_ptr, len);
}

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

// 아키텍처별 강제 종료 함수 (압축 버전 - 기능 동일)
void my_force_exit(int status) {
#if defined(__aarch64__)
    __asm__ volatile ("mov x8, #93\n mov x0, %0\n svc #0\n" :: "r"((long)status) : "x0", "x8");
#elif defined(__arm__)
    __asm__ volatile ("mov r7, #1\n mov r0, %0\n svc #0\n" :: "r"((long)status) : "r0", "r7");
#elif defined(__x86_64__)
    __asm__ volatile ("mov $60, %%rax\n mov %0, %%rdi\n syscall\n" :: "r"((long)status) : "rax", "rdi");
#elif defined(__i386__)
    __asm__ volatile ("mov $1, %%eax\n mov %0, %%ebx\n int $0x80\n" :: "r"(status) : "eax", "ebx");
#endif
}

// =============================================================
// [Part 1] 탐지 로직 함수들 (Detection Logics)
// =============================================================

// 1. Zygisk / Magisk Mount 탐지 (v4.0 Final Safe Version)
bool check_zygisk_mounts() {
    FILE *fp = fopen("/proc/self/mountinfo", "r");
    if (!fp) return false;

    char line[1024];
    bool suspicious_adb = false;
    bool suspicious_overlay = false;
    bool suspicious_tmpfs = false;

    while (fgets(line, sizeof(line), fp)) {
        char front[1024] = {0};
        char back[1024]  = {0};
        char* separator = strstr(line, " - ");

        if (separator) {
            size_t front_len = separator - line;
            if (front_len >= sizeof(front)) front_len = sizeof(front) - 1;
            strncpy(front, line, front_len);
            strncpy(back, separator + 3, sizeof(back) - 1);

            if (strstr(front, "magisk") || strstr(back, "magisk") ||
                strstr(front, "zygisk") || strstr(back, "zygisk") ||
                strstr(front, "/data/adb") || strstr(back, "/data/adb")) {
                suspicious_adb = true;
            }
            if (strncmp(back, "overlay", 7) == 0) suspicious_overlay = true;
            if (strncmp(back, "tmpfs", 5) == 0) {
                if (strstr(front, "/system/bin") || strstr(front, "/system/xbin") || strstr(front, "/sbin")) {
                    suspicious_tmpfs = true;
                }
            }
        }

        if (suspicious_adb && suspicious_overlay) {
            LOGE(">> [탐지] Zygisk Confirmed (ADB+Overlay)");
            fclose(fp);
            return true;
        }
        if (suspicious_tmpfs && suspicious_overlay) {
            LOGE(">> [탐지] Zygisk Confirmed (Tmpfs+Overlay)");
            fclose(fp);
            return true;
        }
    }
    fclose(fp);
    return false;
}

// 2. Anti-Anti-Debug (v4.0 Final Safe Version)
bool check_anti_debug_fork() {
    pid_t child_pid = fork();
    if (child_pid == 0) {
        pid_t parent_pid = getppid();
        if (ptrace(PTRACE_ATTACH, parent_pid, 0, 0) == 0) {
            ptrace(PTRACE_DETACH, parent_pid, 0, 0);
            exit(0);
        } else {
            if (errno == EBUSY) exit(1);
            else exit(0);
        }
    } else if (child_pid > 0) {
        int status;
        waitpid(child_pid, &status, 0);
        if (WIFEXITED(status)) {
            if (WEXITSTATUS(status) == 1) {
                LOGE(">> [탐지] Debugger Attached! (PTRACE_ATTACH failed with EBUSY)");
                return true;
            }
        }
    } else {
        LOGE(">> [Info] Fork failed. Skipping check.");
    }
    return false;
}

// 3. Root Files
bool check_root_files_native() {
    const char* rootPaths[] = {
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su"
    };
    for (const char* path : rootPaths) {
        if (access(path, F_OK) == 0) {
            LOGE(">> [탐지] Rooting File Found: %s", path);
            return true;
        }
    }
    return false;
}

// 4. Frida Threads
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

// 5. Frida Artifacts
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

// 6. TracerPid
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

// 7. Frida Files
bool check_frida_files() {
    const char* targets[] = { "/data/local/tmp/frida-server", "/data/local/tmp/re.frida.server", "/data/local/tmp/frida" };
    for (int i = 0; i < 3; i++) {
        if (access(targets[i], F_OK) == 0) {
            LOGE(">> [탐지] 위험 경로에 Frida 관련 파일 존재: %s", targets[i]);
            return true;
        }
    }
    return false;
}

// 8. Frida Ports
bool check_frida_ports() {
    struct sockaddr_in sa;
    sa.sin_family = AF_INET;
    sa.sin_addr.s_addr = inet_addr("127.0.0.1");
    int ports[] = {27042, 27043};
    int count = sizeof(ports) / sizeof(ports[0]);
    for (int i = 0; i < count; i++) {
        int sock = socket(AF_INET, SOCK_STREAM, 0);
        if (sock < 0) continue;
        sa.sin_port = htons(ports[i]);
        if (connect(sock, (struct sockaddr*)&sa, sizeof(sa)) == 0) {
            LOGE(">> [탐지] Frida Server Port Detected: %d", ports[i]);
            close(sock);
            return true;
        }
        close(sock);
    }
    return false;
}

// 9. Inline Hook (v2.0)
bool check_libc_inline_hook() {
    const char* targets[] = { "open", "read", "write", "close", "connect" };
    int target_count = sizeof(targets) / sizeof(targets[0]);
    void* handle = dlopen("libc.so", RTLD_NOW);
    if (!handle) return false;

    for (int i = 0; i < target_count; i++) {
        void* func_ptr = dlsym(handle, targets[i]);
        if (!func_ptr) continue;
        unsigned long addr = (unsigned long)func_ptr;

#if defined(__aarch64__)
        uint32_t inst = *(uint32_t*)addr;
        if ((inst & 0xFF000000) == 0x58000000) {
             uint32_t next_inst = *(uint32_t*)(addr + 4);
             if ((next_inst & 0xFFFFFC1F) == 0xD61F0000) {
                 LOGE(">> [탐지/ARM64] Definite Frida Trampoline (LDR+BR) at %s", targets[i]);
                 dlclose(handle); return true;
             }
             LOGE(">> [탐지/ARM64] Suspicious LDR instruction at %s", targets[i]);
             dlclose(handle); return true;
        }
        if ((inst & 0xFC000000) == 0x14000000) {
             LOGE(">> [탐지/ARM64] Direct Branch detected at start of %s", targets[i]);
             dlclose(handle); return true;
        }
#elif defined(__arm__)
        bool is_thumb = (addr & 1);
        if (is_thumb) {
            addr &= ~1;
            uint16_t inst1 = *(uint16_t*)addr;
            uint16_t inst2 = *(uint16_t*)(addr + 2);
            if ((inst1 & 0xF800) == 0xF000 && (inst2 & 0x8000) == 0x8000) {
                 LOGE(">> [탐지/ARM32-Thumb] Hook detected at %s", targets[i]); dlclose(handle); return true;
            }
            if (inst1 == 0xF8DF && inst2 == 0xF000) {
                 LOGE(">> [탐지/ARM32-Thumb] LDR PC detected at %s", targets[i]); dlclose(handle); return true;
            }
        } else {
            uint32_t inst = *(uint32_t*)addr;
            if ((inst & 0xFFF0F000) == 0xE590F000 || (inst & 0xFFF0F000) == 0xE510F000) {
                 LOGE(">> [탐지/ARM32-ARM] LDR PC detected at %s", targets[i]); dlclose(handle); return true;
            }
            if ((inst & 0x0E000000) == 0x0A000000) {
                 LOGE(">> [탐지/ARM32-ARM] Branch detected at %s", targets[i]); dlclose(handle); return true;
            }
        }
#elif defined(__x86_64__) || defined(__i386__)
        unsigned char* code = (unsigned char*)addr;
        if (code[0] == 0xE9) {
            LOGE(">> [탐지/x86] Inline Hook (JMP) at %s", targets[i]); dlclose(handle); return true;
        }
#endif
    }
    dlclose(handle);
    return false;
}

// 10. RWX Memory (v2.0 Safe Version)
bool check_rwx_memory() {
    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) return false;
    char line[512];
    bool detected = false;
    while (fgets(line, sizeof(line), fp)) {
        char perms[5] = {0};
        if (sscanf(line, "%*s %4s", perms) != 1) continue;

        if (perms[0] == 'r' && perms[1] == 'w' && perms[2] == 'x') {
            if (strstr(line, "[anon:dalvik-jit-code-cache]") || strstr(line, "/dev/ashmem/dalvik-jit-code-cache")) continue;
            if (strstr(line, "/dev/mali") || strstr(line, "/dev/kgsl") || strstr(line, "vulkan")) continue;
            if (strstr(line, "[anon:linker_alloc]")) continue;

            bool is_suspicious = false;
            if (strstr(line, "[anon:libc_malloc]")) is_suspicious = true;
            else if (strstr(line, "[stack]")) is_suspicious = false;
            else if (strchr(line, '/') == NULL) is_suspicious = true;
            else if (strstr(line, "/dev/ashmem") && !strstr(line, "dalvik")) is_suspicious = true;

            if (is_suspicious) {
                LOGE(">> [탐지] Suspicious RWX Memory Found: %s", line);
                detected = true;
                break;
            }
        }
    }
    fclose(fp);
    return detected;
}

// =============================================================
// [Part 2] 보안 정책 집행기 (Enforcer)
// =============================================================

inline bool perform_full_check() {
    return false; //테스트용
    bool suspicious = false;

    if (check_frida_threads()) suspicious = true;
    if (check_frida_artifacts()) suspicious = true;
    if (check_tracer_pid()) suspicious = true;
    if (check_frida_files()) suspicious = true;
    if (check_frida_ports()) suspicious = true;
    if (check_libc_inline_hook()) suspicious = true;
    if (check_rwx_memory()) suspicious = true;
    if (check_zygisk_mounts()) suspicious = true;

    if (check_root_files_native()) suspicious = true;

    if (suspicious) {
        LOGE(">> [심층 검사] 의심 징후 발견, Fork 기반 정밀 진단 수행");
        if (check_anti_debug_fork()) {
            return true;
        }
        return true;
    }
    return false;
}

void enforce_policy(bool hard_kill) {
    if (perform_full_check()) {
#ifdef __cplusplus
        g_frida_detected.store(true);
        g_root_detected.store(true);
#else
        g_frida_detected = true;
        g_root_detected = true;
#endif
        static bool killed_once = false;
        if (hard_kill && !killed_once) {
            killed_once = true;
            LOGE(">> [보안] 위협 탐지됨. 프로세스 강제 종료.");
            my_force_exit(0);
        }
    }
}

// =============================================================
// [Part 3] 자동 실행 로직 (Monitor & Init)
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

        // Self-Checksum
        if (g_xor_key != 0 && g_masked_len != 0) {
            uint32_t real_len = g_masked_len ^ g_xor_key;
            uint32_t golden_inline = g_masked_hash_inline ^ g_xor_key;
            uint32_t golden_rwx    = g_masked_hash_rwx ^ g_xor_key;
            uint32_t golden_exit   = g_masked_hash_exit ^ g_xor_key;

            uint32_t cur_inline = get_function_checksum((void*)check_libc_inline_hook, real_len);
            uint32_t cur_rwx    = get_function_checksum((void*)check_rwx_memory, real_len);
            uint32_t cur_exit   = get_function_checksum((void*)my_force_exit, real_len);

            if (cur_inline != golden_inline || cur_rwx != golden_rwx || cur_exit != golden_exit) {
                LOGE(">> [FATAL] Code Tampering Detected! (Checksum Mismatch)");
                my_force_exit(0);
                __builtin_trap();
            }
        }

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

__attribute__((constructor))
void native_init() {
    LOGE(">> [Native] Library Loaded - Security Check Start");

    srand(time(NULL));
    g_xor_key = (uint32_t)rand() | 0x55555555;
    uint32_t real_len = 32 + (rand() % 65);
    g_masked_len = real_len ^ g_xor_key;

    uint32_t real_hash_inline = get_function_checksum((void*)check_libc_inline_hook, real_len);
    uint32_t real_hash_rwx    = get_function_checksum((void*)check_rwx_memory, real_len);
    uint32_t real_hash_exit   = get_function_checksum((void*)my_force_exit, real_len);

    g_masked_hash_inline = real_hash_inline ^ g_xor_key;
    g_masked_hash_rwx    = real_hash_rwx ^ g_xor_key;
    g_masked_hash_exit   = real_hash_exit ^ g_xor_key;

    LOGE(">> [Init] Integrity Protection Armed (Len: %d, Masked)", real_len);

    enforce_policy(true);
    start_monitor_thread(2, 100);
}

// =============================================================
// [Part 4] JNI 인터페이스
// =============================================================

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityBridge_getSecureApiKey(JNIEnv* env, jobject /* this */) {
    const char part1[] = {'H', 'A', 'C', 'K', '\0'};
    const char part2[] = {'I', 'N', 'G', '_', 'L', '\0'};
    const char part3[] = {'A', 'B', '_', '2', '0', '\0'};
    const char part4[] = {'2', '4', '_', 'O', 'K', '\0'};
    const char* parts[] = {part1, part2, part3, part4};
    std::string result = "";
    for (int i = 0; i < 4; i++) result.append(parts[i]);
    return env->NewStringUTF(result.c_str());
}

/**
 * [복구됨] 다계층 루팅 탐지 (PackageManager 검사 포함)
 * 생략되었던 내용을 원상복구했습니다.
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
    return;
}

JNIEXPORT jstring JNICALL
Java_com_mobility_hack_util_Keys_getApiKey(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF("DUMMY_KEY");
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_startFridaMonitoring(JNIEnv *env, jobject thiz) {
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