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
#include <stdint.h>

#ifdef __cplusplus
#include <atomic>
#endif

// =============================================================
// [다층 방어] 메모리 조작 방지 시스템
// =============================================================

#ifdef __cplusplus
std::atomic<int> g_threat_score(0);
std::atomic<int> g_render_cache_size(0);
std::atomic<int> g_audio_buffer_count(0);
std::atomic<int> g_network_timeout_ms(0);

std::atomic<uint32_t> g_encoded_threat(0);
volatile uint32_t g_threat_encode_key = 0;

std::atomic<int> g_check_a(0);
std::atomic<int> g_check_b(0);
std::atomic<int> g_check_c(0);

struct ThreatSnapshot {
    int score;
    time_t timestamp;
    uint32_t checksum;
};
ThreatSnapshot g_timeline[50];
std::atomic<int> g_timeline_idx(0);

volatile uint32_t g_canary_before = 0xDEADBEEF;
volatile uint32_t g_canary_after  = 0xCAFEBABE;

std::atomic<bool> g_frida_detected(false);
std::atomic<bool> g_root_detected(false);
std::atomic<int>  g_silent_checks(0);
#else
_Atomic bool g_frida_detected = false;
_Atomic bool g_root_detected = false;
_Atomic int g_threat_score = 0;
_Atomic int g_render_cache_size = 0;
_Atomic int g_audio_buffer_count = 0;
#endif

#define TAG "SecurityEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)

#define ERR_CODE_ROOTED 0x47
#define THREAT_THRESHOLD 3

// =============================================================
// [Part 0] 유틸리티
// =============================================================

volatile uint32_t g_xor_key = 0;
volatile uint32_t g_masked_len = 0;
volatile uint32_t g_masked_hash_inline = 0;
volatile uint32_t g_masked_hash_rwx = 0;
volatile uint32_t g_masked_hash_exit = 0;

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

void my_force_exit(int status) {
    return;  // 공유용(테스트)
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
// [다층 방어] 위협 점수 관리 시스템
// =============================================================

inline uint32_t calc_score_checksum(int score) {
    uint32_t hash = (uint32_t)score;
    hash = ((hash >> 16) ^ hash) * 0x45d9f3b;
    hash = ((hash >> 16) ^ hash) * 0x45d9f3b;
    hash = (hash >> 16) ^ hash;
    return hash;
}

inline void silent_threat_increment() {
#ifdef __cplusplus
    if (g_canary_before != 0xDEADBEEF || g_canary_after != 0xCAFEBABE) {
        LOGE(">> [FATAL] Canary violated!");
        my_force_exit(0);
        return;
    }

    int new_score = g_threat_score.fetch_add(1) + 1;
    g_render_cache_size.fetch_add(1);
    g_audio_buffer_count.fetch_add(1);

    uint32_t decoded = g_encoded_threat.load() ^ g_threat_encode_key;
    g_encoded_threat.store((decoded + 1) ^ g_threat_encode_key);

    int r = rand() % 3;
    if (r == 0) g_check_a.fetch_add(1);
    else if (r == 1) g_check_b.fetch_add(1);
    else g_check_c.fetch_add(1);

    int idx = g_timeline_idx.fetch_add(1);
    if (idx < 50) {
        g_timeline[idx].score = new_score;
        g_timeline[idx].timestamp = time(NULL);
        g_timeline[idx].checksum = calc_score_checksum(new_score);
    }
#else
    g_threat_score++;
    g_render_cache_size++;
    g_audio_buffer_count++;
#endif
}

inline bool verify_score_integrity() {
#ifdef __cplusplus
    if (g_canary_before != 0xDEADBEEF || g_canary_after != 0xCAFEBABE) {
        LOGE(">> [FATAL] Canary violated during verify!");
        return true;
    }

    int s1 = g_threat_score.load();
    int s2 = g_render_cache_size.load();
    int s3 = g_audio_buffer_count.load();
    int s4 = (int)(g_encoded_threat.load() ^ g_threat_encode_key);
    int s5 = g_check_a.load() + g_check_b.load() + g_check_c.load();

    if (s1 != s2 || s1 != s3 || s1 != s4 || s1 != s5) {
        LOGE(">> [FATAL] Score mismatch! L1=%d L2=%d L3=%d L4=%d L5=%d", s1, s2, s3, s4, s5);
        return true;
    }

    int timeline_count = g_timeline_idx.load();
    if (timeline_count > 50) timeline_count = 50;

    for (int i = 1; i < timeline_count; i++) {
        if (g_timeline[i].score < g_timeline[i - 1].score) {
            LOGE(">> [FATAL] Score decreased! History tampered!");
            return true;
        }
        uint32_t expected = calc_score_checksum(g_timeline[i].score);
        if (g_timeline[i].checksum != expected) {
            LOGE(">> [FATAL] Timeline checksum mismatch!");
            return true;
        }
    }

    if (timeline_count > 0) {
        int last_recorded = g_timeline[timeline_count - 1].score;
        if (s1 < last_recorded) {
            LOGE(">> [FATAL] Current score < history!");
            return true;
        }
    }

    return s1 >= THREAT_THRESHOLD;
#else
    return g_threat_score >= THREAT_THRESHOLD;
#endif
}

inline void check_and_enforce() {
    return;  // 테스트용
    if (verify_score_integrity()) {
#ifdef __cplusplus
        g_frida_detected.store(true);
        g_root_detected.store(true);
#else
        g_frida_detected = true;
        g_root_detected = true;
#endif
        LOGE(">> [보안] 위협 탐지. 프로세스 종료.");
        my_force_exit(0);
    }
}

inline void mark_silent_check() {
#ifdef __cplusplus
    g_silent_checks.fetch_add(1);
#else
    g_silent_checks++;
#endif
}

inline bool verify_check_sequence() {
#ifdef __cplusplus
    return g_silent_checks.load() >= 2;
#else
    return g_silent_checks >= 2;
#endif
}

// =============================================================
// [Part 1] 탐지 로직
// =============================================================

bool check_zygisk_mounts() {
    mark_silent_check();
    FILE *fp = fopen("/proc/self/mountinfo", "r");
    if (!fp) {
        silent_threat_increment();
        check_and_enforce();
        return false;
    }

    char line[1024];
    bool suspicious_adb = false;
    bool suspicious_overlay = false;
    bool suspicious_tmpfs = false;

    while (fgets(line, sizeof(line), fp)) {
        char front[1024] = {0};
        char back[1024]  = {0};
        char* separator = strstr(line, " - ");

        if (separator) {
            size_t front_len = (size_t)(separator - line);
            if (front_len >= sizeof(front)) front_len = sizeof(front) - 1;
            strncpy(front, line, front_len);
            strncpy(back, separator + 3, sizeof(back) - 1);

            if (strstr(front, "magisk") || strstr(back, "magisk") ||
                strstr(front, "zygisk") || strstr(back, "zygisk") ||
                strstr(front, "/data/adb") || strstr(back, "/data/adb")) {
                suspicious_adb = true;
                silent_threat_increment();
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
            silent_threat_increment();
            fclose(fp);
            check_and_enforce();
            return true;
        }
        if (suspicious_tmpfs && suspicious_overlay) {
            LOGE(">> [탐지] Zygisk Confirmed (Tmpfs+Overlay)");
            silent_threat_increment();
            fclose(fp);
            check_and_enforce();
            return true;
        }
    }
    fclose(fp);
    check_and_enforce();
    return false;
}

bool check_anti_debug_fork() {
    mark_silent_check();
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
                LOGE(">> [탐지] Debugger Attached!");
                silent_threat_increment();
                silent_threat_increment();
                check_and_enforce();
                return true;
            }
        }
    }
    check_and_enforce();
    return false;
}

bool check_root_files_native() {
    const char* rootPaths[] = {
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su"
    };
    for (const char* path : rootPaths) {
        if (access(path, F_OK) == 0) {
            LOGE(">> [탐지] Rooting File Found: %s", path);
            silent_threat_increment();
            check_and_enforce();
            return true;
        }
    }
    check_and_enforce();
    return false;
}

bool check_frida_threads() {
    DIR *dir = opendir("/proc/self/task");
    if (!dir) {
        silent_threat_increment();
        check_and_enforce();
        return false;
    }
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
                    silent_threat_increment();
                }
            }
            close(fd);
        }
        if (detected) break;
    }
    closedir(dir);
    check_and_enforce();
    return detected;
}

bool check_frida_artifacts() {
    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) {
        silent_threat_increment();
        check_and_enforce();
        return false;
    }
    char line[512];
    bool detected = false;
    while (fgets(line, sizeof(line), fp)) {
        if (my_strstr(line, "frida") || my_strstr(line, "gum-js") || my_strstr(line, "gadget")) {
            LOGE(">> [탐지] Frida Artifact Found");
            detected = true;
            silent_threat_increment();
            break;
        }
    }
    fclose(fp);
    check_and_enforce();
    return detected;
}

bool check_tracer_pid() {
    mark_silent_check();
    FILE *fp = fopen("/proc/self/status", "r");
    if (!fp) {
        silent_threat_increment();
        check_and_enforce();
        return false;
    }
    char line[512];
    bool detected = false;
    while (fgets(line, sizeof(line), fp)) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            int pid = atoi(&line[10]);
            if (pid > 0) {
                LOGE(">> [탐지] Debugger(TracerPid) Found: %d", pid);
                detected = true;
                silent_threat_increment();
                silent_threat_increment();
            }
            break;
        }
    }
    fclose(fp);
    check_and_enforce();
    return detected;
}

bool check_frida_files() {
    const char* targets[] = {
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/data/local/tmp/frida"
    };
    for (int i = 0; i < 3; i++) {
        if (access(targets[i], F_OK) == 0) {
            LOGE(">> [탐지] Frida 파일: %s", targets[i]);
            silent_threat_increment();
            check_and_enforce();
            return true;
        }
    }
    check_and_enforce();
    return false;
}

bool check_frida_ports() {
    struct sockaddr_in sa;
    sa.sin_family = AF_INET;
    sa.sin_addr.s_addr = inet_addr("127.0.0.1");
    int ports[] = {27042, 27043};
    for (int i = 0; i < 2; i++) {
        int sock = socket(AF_INET, SOCK_STREAM, 0);
        if (sock < 0) continue;
        sa.sin_port = htons(ports[i]);
        if (connect(sock, (struct sockaddr*)&sa, sizeof(sa)) == 0) {
            LOGE(">> [탐지] Frida Port: %d", ports[i]);
            close(sock);
            silent_threat_increment();
            check_and_enforce();
            return true;
        }
        close(sock);
    }
    check_and_enforce();
    return false;
}

bool check_libc_inline_hook() {
    mark_silent_check();
    const char* targets[] = { "open", "read", "write", "close", "connect" };
    void* handle = dlopen("libc.so", RTLD_NOW);
    if (!handle) {
        silent_threat_increment();
        check_and_enforce();
        return false;
    }

    for (int i = 0; i < 5; i++) {
        void* func_ptr = dlsym(handle, targets[i]);
        if (!func_ptr) continue;
        unsigned long addr = (unsigned long)func_ptr;

#if defined(__aarch64__)
        uint32_t inst = *(uint32_t*)addr;
        if ((inst & 0xFF000000) == 0x58000000) {
            silent_threat_increment();
            dlclose(handle);
            check_and_enforce();
            return true;
        }
        if ((inst & 0xFC000000) == 0x14000000) {
            silent_threat_increment();
            dlclose(handle);
            check_and_enforce();
            return true;
        }
#endif
    }
    dlclose(handle);
    check_and_enforce();
    return false;
}

bool check_rwx_memory() {
    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) {
        silent_threat_increment();
        check_and_enforce();
        return false;
    }
    char line[512];
    bool detected = false;
    while (fgets(line, sizeof(line), fp)) {
        char perms[5] = {0};
        if (sscanf(line, "%*s %4s", perms) != 1) continue;

        if (perms[0] == 'r' && perms[1] == 'w' && perms[2] == 'x') {
            if (strstr(line, "[anon:dalvik-jit-code-cache]")) continue;
            if (strstr(line, "/dev/mali") || strstr(line, "vulkan")) continue;

            bool is_suspicious = false;
            if (strstr(line, "[anon:libc_malloc]")) is_suspicious = true;
            else if (strchr(line, '/') == NULL) is_suspicious = true;

            if (is_suspicious) {
                LOGE(">> [탐지] RWX Memory");
                detected = true;
                silent_threat_increment();
                break;
            }
        }
    }
    fclose(fp);
    check_and_enforce();
    return detected;
}

// =============================================================
// [Part 2] 정책 집행
// =============================================================

inline bool perform_full_check() {
    return false; // 테스트용(원래 로직 유지)

    if (!verify_check_sequence()) {
        LOGE(">> [경고] 검증 시퀀스 누락");
        silent_threat_increment();
    }
    check_and_enforce();

    bool suspicious = false;
    if (check_frida_threads()) suspicious = true;
    check_and_enforce();

    if (check_frida_artifacts()) suspicious = true;
    check_and_enforce();

    if (check_tracer_pid()) suspicious = true;
    check_and_enforce();

    if (check_frida_files()) suspicious = true;
    check_and_enforce();

    if (check_frida_ports()) suspicious = true;
    check_and_enforce();

    if (check_libc_inline_hook()) suspicious = true;
    check_and_enforce();

    if (check_rwx_memory()) suspicious = true;
    check_and_enforce();

    if (check_zygisk_mounts()) suspicious = true;
    check_and_enforce();

    if (check_root_files_native()) suspicious = true;
    check_and_enforce();

    if (suspicious) {
        if (check_anti_debug_fork()) return true;
        return true;
    }
    return false;
}

void enforce_policy(bool hard_kill) {
    check_and_enforce();

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
            LOGE(">> [보안] 위협 탐지. 종료.");
            my_force_exit(0);
        }
    }
}

// =============================================================
// [Part 3] 모니터링
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

        if (g_xor_key != 0 && g_masked_len != 0) {
            uint32_t real_len = g_masked_len ^ g_xor_key;
            uint32_t golden_inline = g_masked_hash_inline ^ g_xor_key;
            uint32_t golden_rwx    = g_masked_hash_rwx ^ g_xor_key;
            uint32_t golden_exit   = g_masked_hash_exit ^ g_xor_key;

            uint32_t cur_inline = get_function_checksum((void*)check_libc_inline_hook, real_len);
            uint32_t cur_rwx    = get_function_checksum((void*)check_rwx_memory, real_len);
            uint32_t cur_exit   = get_function_checksum((void*)my_force_exit, real_len);

            if (cur_inline != golden_inline || cur_rwx != golden_rwx || cur_exit != golden_exit) {
                LOGE(">> [FATAL] Code Tampering!");
                silent_threat_increment();
                silent_threat_increment();
                check_and_enforce();
                my_force_exit(0);
            }
        }

        check_and_enforce();
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
    return;  // 테스트용
    LOGE(">> [Native] Library Loaded");

    srand(time(NULL));

    g_xor_key = (uint32_t)rand() | 0x55555555;
    uint32_t real_len = 32 + (rand() % 65);
    g_masked_len = real_len ^ g_xor_key;

#ifdef __cplusplus
    g_threat_encode_key = (uint32_t)rand() | 0xABCDABCD;
    g_encoded_threat.store(0 ^ g_threat_encode_key);
#endif

    uint32_t real_hash_inline = get_function_checksum((void*)check_libc_inline_hook, real_len);
    uint32_t real_hash_rwx    = get_function_checksum((void*)check_rwx_memory, real_len);
    uint32_t real_hash_exit   = get_function_checksum((void*)my_force_exit, real_len);

    g_masked_hash_inline = real_hash_inline ^ g_xor_key;
    g_masked_hash_rwx    = real_hash_rwx ^ g_xor_key;
    g_masked_hash_exit   = real_hash_exit ^ g_xor_key;

    LOGE(">> [Init] Multi-layer Protection Armed");
    enforce_policy(true);
    start_monitor_thread(2, 100);
}

// =============================================================
// [Part 4] JNI 인터페이스 (+ SSL Pinning 추가)
// =============================================================

static inline bool clearIfException(JNIEnv* env, const char* where) {
    if (env->ExceptionCheck()) {
        LOGE("JNI Exception at %s", where);
        env->ExceptionClear();
        return true;
    }
    return false;
}

static inline int constantTimeEq32(const unsigned char* a, const unsigned char* b) {
    unsigned char diff = 0;
    for (int i = 0; i < 32; i++) diff |= (unsigned char)(a[i] ^ b[i]);
    return diff == 0;
}

// ---- (SSL Pinning) pinned hash (SHA-256 of leaf cert DER) ----
// 예시(네가 전에 적어둔 것):
// 0b:a9:48:a3:f9:2d:cf:e9:62:24:e9:71:0d:e3:19:84:cf:6d:97:56:62:77:45:26:60:38:d3:49:33:1b:6a:39
// 아래는 단순 난독화( XOR ) 저장.
static const unsigned char PIN_XOR_KEY = 0xA7;
static const unsigned char PIN_HASH_OBF[32] = {
        0xac, 0x0e, 0xef, 0x04, 0x5e, 0x8a, 0x68, 0x4e,
        0xc5, 0x83, 0x4e, 0xd6, 0xaa, 0x44, 0xbe, 0x23,
        0x68, 0xca, 0x30, 0xf1, 0xc5, 0xd0, 0xe2, 0x81,
        0xc7, 0x9f, 0x74, 0xee, 0x94, 0xbc, 0xcd, 0x9e
};

static inline void decodePinHash(unsigned char out[32]) {
    for (int i = 0; i < 32; i++) out[i] = (unsigned char)(PIN_HASH_OBF[i] ^ PIN_XOR_KEY);
}

// Java MessageDigest(SHA-256)로 digest 계산
static bool sha256_via_java(JNIEnv* env, jbyteArray input, unsigned char out32[32]) {
    jclass mdCls = env->FindClass("java/security/MessageDigest");
    if (mdCls == nullptr) return false;

    jmethodID getInstance = env->GetStaticMethodID(mdCls, "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    if (getInstance == nullptr) return false;

    jstring algo = env->NewStringUTF("SHA-256");
    jobject mdObj = env->CallStaticObjectMethod(mdCls, getInstance, algo);
    env->DeleteLocalRef(algo);
    if (clearIfException(env, "MessageDigest.getInstance")) return false;
    if (mdObj == nullptr) return false;

    jmethodID digest = env->GetMethodID(mdCls, "digest", "([B)[B");
    if (digest == nullptr) return false;

    jbyteArray hashArr = (jbyteArray)env->CallObjectMethod(mdObj, digest, input);
    if (clearIfException(env, "MessageDigest.digest")) return false;
    if (hashArr == nullptr) return false;

    jsize hlen = env->GetArrayLength(hashArr);
    if (hlen != 32) return false;

    jbyte* hbytes = env->GetByteArrayElements(hashArr, nullptr);
    if (!hbytes) return false;

    memcpy(out32, hbytes, 32);
    env->ReleaseByteArrayElements(hashArr, hbytes, JNI_ABORT);

    env->DeleteLocalRef(hashArr);
    env->DeleteLocalRef(mdObj);
    return true;
}

#ifdef __cplusplus
static std::atomic<bool> g_pinning_enabled(true);
#else
static _Atomic bool g_pinning_enabled = true;
#endif

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityBridge_getSecureApiKey(JNIEnv* env, jobject) {
    check_and_enforce();
    const char part1[] = {'H', 'A', 'C', 'K', '\0'};
    const char part2[] = {'I', 'N', 'G', '_', 'L', '\0'};
    const char part3[] = {'A', 'B', '_', '2', '0', '\0'};
    const char part4[] = {'2', '4', '_', 'O', 'K', '\0'};
    const char* parts[] = {part1, part2, part3, part4};
    std::string result;
    for (int i = 0; i < 4; i++) result.append(parts[i]);
    check_and_enforce();
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jint JNICALL
Java_com_mobility_hack_security_SecurityBridge_detectRooting(JNIEnv* env, jobject thiz, jobject context) {
    (void)thiz;
    check_and_enforce();

#ifdef __cplusplus
    if (g_root_detected.load()) return ERR_CODE_ROOTED;
#else
    if (g_root_detected) return ERR_CODE_ROOTED;
#endif

    if (check_root_files_native()) return ERR_CODE_ROOTED;

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
        env->DeleteLocalRef(pkgString);

        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        } else if (pkgInfo != NULL) {
            silent_threat_increment();
            check_and_enforce();
            return ERR_CODE_ROOTED;
        }
    }

    check_and_enforce();
    return 0;
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_initAntiDebug(JNIEnv* env, jobject thiz) {
    (void)env; (void)thiz;
    check_and_enforce();
    return;
}

JNIEXPORT jstring JNICALL
Java_com_mobility_hack_util_Keys_getApiKey(JNIEnv *env, jobject thiz) {
    (void)thiz;
    check_and_enforce();
    return env->NewStringUTF("DUMMY_KEY");
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_startFridaMonitoring(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    check_and_enforce();
    start_monitor_thread(0, 3000);
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_checkFridaOnce(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    check_and_enforce();
    enforce_policy(true);
}

JNIEXPORT jboolean JNICALL
Java_com_mobility_hack_security_SecurityEngine_wasFridaDetectedEarly(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;
    check_and_enforce();
#ifdef __cplusplus
    return (jboolean)(g_frida_detected.load() ? JNI_TRUE : JNI_FALSE);
#else
    return (jboolean)(g_frida_detected ? JNI_TRUE : JNI_FALSE);
#endif
}

// =============================================================
// ✅ SSL PINNING JNI (SslGuard)
//  - Java: com.mobility.hack.security.SslGuard
// =============================================================

// public static native void setSslPinningEnabled(boolean enabled);
JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SslGuard_setSslPinningEnabled(JNIEnv* /*env*/, jclass /*clazz*/, jboolean enabled) {
#ifdef __cplusplus
    g_pinning_enabled.store(enabled == JNI_TRUE);
#else
    g_pinning_enabled = (enabled == JNI_TRUE);
#endif
    LOGI("Pinning toggle set to %s", (enabled == JNI_TRUE) ? "ON" : "OFF");
}

// public native boolean verifyCert(byte[] certEncoded, boolean checkEnabled);
JNIEXPORT jboolean JNICALL
Java_com_mobility_hack_security_SslGuard_verifyCert(
        JNIEnv *env,
        jobject /*thiz*/,
        jbyteArray certEncoded,
        jboolean checkEnabled) {

    if (certEncoded == nullptr) return JNI_FALSE;
    jsize inLen = env->GetArrayLength(certEncoded);
    if (inLen <= 0) return JNI_FALSE;

    // Java에서 checkEnabled=false 보내면 통과(너가 말한 테스트 스위치)
    if (checkEnabled == JNI_FALSE) {
        LOGW("[Pinning] checkEnabled=false -> BYPASS");
        return JNI_TRUE;
    }

    // native 토글 OFF면 통과(선택 기능)
#ifdef __cplusplus
    if (!g_pinning_enabled.load()) {
#else
        if (!g_pinning_enabled) {
#endif
        LOGW("[Pinning] g_pinning_enabled=OFF -> BYPASS");
        return JNI_TRUE;
    }

    unsigned char expected[32];
    decodePinHash(expected);

    unsigned char actual[32];
    memset(actual, 0, sizeof(actual));

    if (!sha256_via_java(env, certEncoded, actual)) {
        LOGE("[Pinning] SHA-256 calc failed");
        return JNI_FALSE;
    }

    if (constantTimeEq32(actual, expected)) {
        LOGI("[Pinning] OK (leaf cert matched)");
        return JNI_TRUE;
    }

    // mismatch
    LOGE("[Pinning] FAIL (leaf cert mismatch)");
    return JNI_FALSE;
}

} // extern "C"
