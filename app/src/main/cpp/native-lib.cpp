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
#include <sys/system_properties.h>

#ifdef __cplusplus
#include <atomic>

// =============================================================
// [다층 방어] 메모리 조작 방지 시스템
// =============================================================

// Layer 1: 공개 점수 (미끼)
std::atomic<int> g_threat_score(0);

// Layer 2: 위장 변수들 (실제 위협 점수를 다른 이름으로)
std::atomic<int> g_render_cache_size(0);     // 렌더링 캐시로 위장
std::atomic<int> g_audio_buffer_count(0);    // 오디오 버퍼로 위장
std::atomic<int> g_network_timeout_ms(0);    // 네트워크 타임아웃으로 위장

// Layer 3: XOR 인코딩 점수
std::atomic<uint32_t> g_encoded_threat(0);
volatile uint32_t g_threat_encode_key = 0;

// Layer 4: 분산 저장
std::atomic<int> g_check_a(0);
std::atomic<int> g_check_b(0);
std::atomic<int> g_check_c(0);

// Layer 5: 타임라인 (점수 감소 탐지용)
struct ThreatSnapshot {
    int score;
    time_t timestamp;
    uint32_t checksum;
};
ThreatSnapshot g_timeline[50];
std::atomic<int> g_timeline_idx(0);

// 카나리 (메모리 오염 탐지)
volatile uint32_t g_canary_before = 0xDEADBEEF;
volatile uint32_t g_canary_after = 0xCAFEBABE;

std::atomic<bool> g_frida_detected(false);
std::atomic<bool> g_root_detected(false);
std::atomic<int> g_silent_checks(0);

// 초기화 완료 플래그 추가
std::atomic<bool> g_init_completed(false);

#else
// C 버전 (간소화)
_Atomic bool g_frida_detected = false;
_Atomic bool g_root_detected = false;
_Atomic int g_threat_score = 0;
_Atomic int g_render_cache_size = 0;
_Atomic int g_audio_buffer_count = 0;
_Atomic bool g_init_completed = false;
#endif

#define TAG "SecurityEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
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
#if defined(__aarch64__)
    register long x8 __asm__("x8") = 94; // exit_group
    register long x0 __asm__("x0") = (long)status;
    __asm__ volatile (
        "svc #0"
        :
        : "r"(x0), "r"(x8)
        : "memory"
    );

#elif defined(__arm__)
    __asm__ volatile (
        "push {r7} \n"      // R7 백업
        "mov r0, %0 \n"     // status를 r0에 복사
        "mov r7, %1 \n"     // syscall 번호(248)를 r7에 복사
        "svc #0 \n"         // 시스템 콜 실행
        "pop {r7}"          // R7 복구
        :
        : "r"((long)status), "i"(248) // 'i'는 immediate value(상수)를 의미
        : "r0", "memory"              // r0가 수정됨을 컴파일러에 알림
    );

#elif defined(__x86_64__)
    __asm__ volatile (
        "mov $231, %%rax\n"
        "mov %0, %%rdi\n"
        "syscall\n"
        :: "r"((long)status)
        : "rax", "rdi", "memory"  // ← memory 추가
    );

#elif defined(__i386__)
    __asm__ volatile (
            "mov $252, %%eax\n"
            "mov %0, %%ebx\n"
            "int $0x80\n"
            :: "r"(status)
            : "eax", "ebx", "memory"  // ← memory 추가
            );
#endif

    // [Layer 2] Fallback
    LOGE(">> [WARNING] Direct syscall failed or skipped. Using fallback.");
    _exit(status);
}

// =============================================================
// [다층 방어] 위협 점수 관리 시스템
// =============================================================

// 체크섬 계산
inline uint32_t calc_score_checksum(int score) {
    uint32_t hash = score;
    hash = ((hash >> 16) ^ hash) * 0x45d9f3b;
    hash = ((hash >> 16) ^ hash) * 0x45d9f3b;
    hash = (hash >> 16) ^ hash;
    return hash;
}

// 5개 레이어에 동시 증가
inline void silent_threat_increment() {
#ifdef __cplusplus
    // 카나리 체크
    if (g_canary_before != 0xDEADBEEF || g_canary_after != 0xCAFEBABE) {
        LOGE(">> [FATAL] Canary violated!");
        my_force_exit(0);
        return;
    }

    // Layer 1-4: 모든 점수 증가
    int new_score = g_threat_score.fetch_add(1) + 1;
    g_render_cache_size.fetch_add(1);
    g_audio_buffer_count.fetch_add(1);

    // Layer 3: XOR 인코딩
    uint32_t decoded = g_encoded_threat.load() ^ g_threat_encode_key;
    g_encoded_threat.store((decoded + 1) ^ g_threat_encode_key);

    // Layer 4: 분산 (랜덤 배분)
    int r = rand() % 3;
    if (r == 0) g_check_a.fetch_add(1);
    else if (r == 1) g_check_b.fetch_add(1);
    else g_check_c.fetch_add(1);

    // Layer 5: 타임라인 기록
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

// 교차 검증: 5개 레이어가 일치하는지 확인
inline bool verify_score_integrity() {
#ifdef __cplusplus
    // 카나리 체크
    if (g_canary_before != 0xDEADBEEF || g_canary_after != 0xCAFEBABE) {
        LOGE(">> [FATAL] Canary violated during verify!");
        return true;  // 즉시 종료
    }

    int s1 = g_threat_score.load();
    int s2 = g_render_cache_size.load();
    int s3 = g_audio_buffer_count.load();
    int s4 = g_encoded_threat.load() ^ g_threat_encode_key;
    int s5 = g_check_a.load() + g_check_b.load() + g_check_c.load();

    // 불일치 탐지
    if (s1 != s2 || s1 != s3 || s1 != s4 || s1 != s5) {
        LOGE(">> [FATAL] Score mismatch! L1=%d L2=%d L3=%d L4=%d L5=%d",
             s1, s2, s3, s4, s5);
        return true;  // 메모리 조작 탐지!
    }

    // 타임라인 검증 (점수 감소 탐지)
    int timeline_count = g_timeline_idx.load();
    if (timeline_count > 50) timeline_count = 50;

    for (int i = 1; i < timeline_count; i++) {
        // 점수가 감소했는지 확인
        if (g_timeline[i].score < g_timeline[i-1].score) {
            LOGE(">> [FATAL] Score decreased! History tampered!");
            return true;
        }

        // 체크섬 확인
        uint32_t expected = calc_score_checksum(g_timeline[i].score);
        if (g_timeline[i].checksum != expected) {
            LOGE(">> [FATAL] Timeline checksum mismatch!");
            return true;
        }
    }

    // 현재 점수 vs 마지막 기록 비교
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

// 검증 후 조치
inline void check_and_enforce() {
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

// 시퀀스 검증 로직 수정
inline bool verify_check_sequence() {
    // 초기화 중일 때는 검증 패스 (True 반환) -> 점수 증가 방지
#ifdef __cplusplus
    if (!g_init_completed.load()) return true;
    return g_silent_checks.load() >= 2;
#else
    if (!g_init_completed) return true;
    return g_silent_checks >= 2;
#endif
}

// =============================================================
// [Part 1] 탐지 로직 - 분산 검증 적용
// =============================================================

bool check_zygisk_mounts() {
    mark_silent_check();
    FILE *fp = fopen("/proc/self/mountinfo", "r");
    if (!fp) {
        return false;
    }

    char line[1024];
    bool found_overlay = false;
    bool found_tmpfs = false;
    int magisk_mount_count = 0;

    while (fgets(line, sizeof(line), fp)) {
        char front[1024] = {0};
        char back[1024]  = {0};
        char* separator = strstr(line, " - ");

        if (separator) {
            size_t front_len = separator - line;
            if (front_len >= sizeof(front)) front_len = sizeof(front) - 1;
            strncpy(front, line, front_len);
            strncpy(back, separator + 3, sizeof(back) - 1);

            // Magisk/Zygisk 키워드 카운트
            if (strstr(front, "magisk") || strstr(back, "magisk") ||
                strstr(front, "zygisk") || strstr(back, "zygisk") ||
                strstr(front, "/data/adb") || strstr(back, "/data/adb")) {
                magisk_mount_count++;
            }

            // 안전한 문자열 비교
            if (strstr(back, "overlay")) {  // ← strstr로 변경
                found_overlay = true;
            }
            if (strstr(back, "tmpfs")) {  // ← strstr로 변경
                found_tmpfs = true;
            }
        }
    }
    fclose(fp);

    if (magisk_mount_count >= 5 && found_overlay && found_tmpfs) {
        LOGE(">> [탐지] Zygisk Confirmed (Complex Pattern: Mounts=%d + Overlay + Tmpfs)", magisk_mount_count);
        // [수정] 즉시 종료를 위해 점수 폭파
        silent_threat_increment();
        silent_threat_increment();
        silent_threat_increment();
        check_and_enforce();
        return true;
    }

    check_and_enforce();
    return false;
}

bool check_magisk_props() {
    const char* sus_props[] = {
            "ro.boot.verifiedbootstate",  // Magisk가 수정
            "ro.boot.flash.locked",
            "ro.boot.veritymode",
            "ro.debuggable"
    };

    for (const char* prop : sus_props) {
        char value[PROP_VALUE_MAX] = {0};
        __system_property_get(prop, value);

        // 예: ro.debuggable = 1 (의심)
        if (strcmp(prop, "ro.debuggable") == 0 && strcmp(value, "1") == 0) {
            LOGE(">> [탐지] Suspicious prop: %s=%s", prop, value);
            silent_threat_increment();
            return true;
        }
    }

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
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            // [방법 3 추가] Magisk 경로
            "/data/adb/magisk", "/data/adb/modules", "/data/adb/magisk.db",
            "/sbin/.magisk", "/dev/.magisk"
    };
    for (const char* path : rootPaths) {
        if (access(path, F_OK) == 0) {
            LOGE(">> [탐지] Rooting File Found: %s", path);
            // [수정] 치명적인 위협 발견! 점수를 즉시 임계값(3) 이상으로 만듦
            silent_threat_increment(); // 1점
            silent_threat_increment(); // 2점
            silent_threat_increment(); // 3점 -> 여기서 check_and_enforce 호출 시 종료됨
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

bool check_selinux_permissive() {
    FILE *fp = fopen("/sys/fs/selinux/enforce", "r");
    if (!fp) return false;

    char enforce[2] = {0};
    fgets(enforce, sizeof(enforce), fp);
    fclose(fp);

    // 0 = Permissive (루팅 의심)
    // 1 = Enforcing (정상)
    if (enforce[0] == '0') {
        LOGE(">> [탐지] SELinux is Permissive (Rooted)");
        silent_threat_increment();
        silent_threat_increment();
        silent_threat_increment();
        check_and_enforce();
        return true;
    }

    return false;
}

bool check_libc_inline_hook() {
    mark_silent_check();

    // ---------------------------------------------------------
    // [검사 1] 메모리 권한 검사 (RWX 탐지)
    // 공격자가 코드를 수정하기 위해 '쓰기' 권한을 부여했는지 확인
    // ---------------------------------------------------------
    FILE *fp = fopen("/proc/self/maps", "r");
    if (fp) {
        char line[1024];
        while (fgets(line, sizeof(line), fp)) {
            // libc.so가 포함된 라인만 찾음
            if (!strstr(line, "libc.so")) continue;

            // 포맷 파싱 (주소 범위, 권한 등)
            // 예: 7c000000-7c100000 r-xp ...
            char perms[5] = {0};
            // 맵 라인에서 두 번째 항목(권한)을 가져옴
            if (sscanf(line, "%*x-%*x %4s", perms) == 1) {
                // 'w' (쓰기)와 'x' (실행) 권한이 동시에 있으면 100% 의심
                if (strchr(perms, 'w') && strchr(perms, 'x')) {
                    LOGE(">> [탐지] libc.so has suspicious permissions (RWX): %s", perms);
                    silent_threat_increment();
                    fclose(fp);
                    check_and_enforce();
                    return true;
                }
            }
        }
        fclose(fp);
    }

    // ---------------------------------------------------------
    // [검사 2] 함수 주소 무결성 검사 (GOT/PLT 후킹 탐지)
    // ---------------------------------------------------------
    void* handle = dlopen("libc.so", RTLD_NOW);
    if (!handle) {
        // libc를 못 여는 것도 매우 수상함
        silent_threat_increment();
        return false;
    }

    const char* targets[] = { "open", "read", "write", "close" };
    for (const char* func_name : targets) {
        void* func_ptr = dlsym(handle, func_name);
        if (!func_ptr) continue;

        Dl_info info;
        if (dladdr(func_ptr, &info)) {
            // 해당 주소가 속한 라이브러리 이름(dli_fname) 가져오기
            if (info.dli_fname) {
                // 함수 포인터가 가리키는 곳이 libc.so가 아니라면 후킹된 것임!
                if (!strstr(info.dli_fname, "libc.so")) {
                    LOGE(">> [탐지] Hooked Function: %s points to %s", func_name, info.dli_fname);
                    silent_threat_increment();
                    dlclose(handle);
                    check_and_enforce();
                    return true;
                }
            }
        }
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

// perform_full_check 수정
inline bool perform_full_check() {
    // 1. [필수] 시퀀스 검증 (맨 처음 유지)
#ifdef __cplusplus
    if (g_init_completed.load() && !verify_check_sequence()) {
        LOGE(">> [경고] 검증 시퀀스 누락");
        silent_threat_increment();
    }
    g_silent_checks.store(0);
#else
    if (g_init_completed && !verify_check_sequence()) {
        LOGE(">> [경고] 검증 시퀀스 누락");
        silent_threat_increment();
    }
    g_silent_checks = 0;
#endif

    check_and_enforce();
    bool suspicious = false;

    // 2. [가중치 설정] 환경적 요인 (점수 +1)
    if (check_magisk_props()) {
        silent_threat_increment();
        // 즉시 종료 안 함
    }

    // 3. [Fast Fail] 매우 빠르고 치명적인 검사들 (즉시 종료 유도)
    // 파일 존재 여부나 포트 확인은 CPU를 거의 안 쓰고 즉시 결과를 냄

    if (check_root_files_native()) suspicious = true; // 파일 체크 (매우 빠름)
    check_and_enforce();

    if (check_frida_files()) suspicious = true; // 파일 체크 (매우 빠름)
    check_and_enforce();

    if (check_selinux_permissive()) suspicious = true; // 설정 체크 (빠름)
    check_and_enforce();

    if (check_tracer_pid()) suspicious = true; // Status 읽기 (빠름)
    check_and_enforce();

    if (check_frida_ports()) suspicious = true; // 소켓 연결 (빠름)
    check_and_enforce();

    // 4. [Deep Scan] 무겁고 정밀한 검사들 (여기까지 살아남았다면 실행)
    // 문자열 파싱이나 디렉토리 순회 등 비용이 드는 작업

    if (check_frida_threads()) suspicious = true;
    check_and_enforce();

    if (check_libc_inline_hook()) suspicious = true;
    check_and_enforce();

    if (check_zygisk_mounts()) suspicious = true; // MountInfo 파싱
    check_and_enforce();

    if (check_rwx_memory()) suspicious = true; // Maps 파싱 (가장 무거움)
    check_and_enforce();

    if (check_frida_artifacts()) suspicious = true; // Maps 파싱 (가장 무거움)
    check_and_enforce();

    // 5. [Final] 최종 확인
    if (suspicious) {
        if (check_anti_debug_fork()) {
            return true;
        }
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
    LOGE(">> [Native] Library Loaded");

    // 1. 초기화 시작 (플래그 False 설정)
    // 이 상태에서는 verify_check_sequence()가 무조건 True를 반환합니다.
#ifdef __cplusplus
    g_init_completed.store(false);
#else
    g_init_completed = false;
#endif

    srand(time(NULL));

    // 2. XOR 난독화 키 초기화 (기존 로직 유지)
    g_xor_key = (uint32_t)rand() | 0x55555555;
    uint32_t real_len = 32 + (rand() % 65);
    g_masked_len = real_len ^ g_xor_key;

    // 위협 스코어 인코딩 키 초기화
#ifdef __cplusplus
    g_threat_encode_key = (uint32_t)rand() | 0xABCDABCD;
    g_encoded_threat.store(0 ^ g_threat_encode_key);
#endif

    // 무결성 검증용 해시 계산
    uint32_t real_hash_inline = get_function_checksum((void*)check_libc_inline_hook, real_len);
    uint32_t real_hash_rwx    = get_function_checksum((void*)check_rwx_memory, real_len);
    uint32_t real_hash_exit   = get_function_checksum((void*)my_force_exit, real_len);

    g_masked_hash_inline = real_hash_inline ^ g_xor_key;
    g_masked_hash_rwx    = real_hash_rwx ^ g_xor_key;
    g_masked_hash_exit   = real_hash_exit ^ g_xor_key;

    LOGE(">> [Init] Multi-layer Protection Armed");

    // 3. 초기 보안 정책 집행
    // g_init_completed가 false이므로 "검증 시퀀스 누락" 로그가 뜨지 않고, 점수도 오르지 않습니다.
    // 하지만 Rooting이나 Frida 탐지 로직은 정상 작동합니다.
    enforce_policy(true);

    // 4. 초기화 완료 (플래그 True 설정)
    // 이후부터는 verify_check_sequence()가 정상적으로 검증을 수행합니다.
#ifdef __cplusplus
    g_init_completed.store(true);
#else
    g_init_completed = true;
#endif

    // 5. 모니터링 스레드 시작
    start_monitor_thread(2, 100);
}

// =============================================================
// [Part 4] JNI 인터페이스
// =============================================================

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityBridge_getSecureApiKey(JNIEnv* env, jobject) {
    check_and_enforce();
    const char part1[] = {'H', 'A', 'C', 'K', '\0'};
    const char part2[] = {'I', 'N', 'G', '_', 'L', '\0'};
    const char part3[] = {'A', 'B', '_', '2', '0', '\0'};
    const char part4[] = {'2', '4', '_', 'O', 'K', '\0'};
    const char* parts[] = {part1, part2, part3, part4};
    std::string result = "";
    for (int i = 0; i < 4; i++) result.append(parts[i]);
    check_and_enforce();
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jint JNICALL
Java_com_mobility_hack_security_SecurityBridge_detectRooting(JNIEnv* env, jobject thiz, jobject context) {
    check_and_enforce();

    if (g_root_detected.load()) return ERR_CODE_ROOTED;
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
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        } else if (pkgInfo != NULL) {
            LOGE(">> [탐지] Rooting App Found via PackageName: %s", pkg);
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
    check_and_enforce();
    return;
}

JNIEXPORT jstring JNICALL
Java_com_mobility_hack_util_Keys_getApiKey(JNIEnv *env, jobject thiz) {
    check_and_enforce();
    return env->NewStringUTF("DUMMY_KEY");
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_startFridaMonitoring(JNIEnv *env, jobject thiz) {
    check_and_enforce();
    start_monitor_thread(0, 3000);
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_checkFridaOnce(JNIEnv *env, jobject thiz) {
    check_and_enforce();
    enforce_policy(true);
}

JNIEXPORT jboolean JNICALL
Java_com_mobility_hack_security_SecurityEngine_wasFridaDetectedEarly(JNIEnv *env, jobject thiz) {
    check_and_enforce();
    return (jboolean)g_frida_detected.load();
}

} // extern "C"