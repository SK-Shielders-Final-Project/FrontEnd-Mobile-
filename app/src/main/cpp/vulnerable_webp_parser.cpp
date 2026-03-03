#include <jni.h>
#include <android/log.h>
#include <cstdlib>
#include <cstring>

#define LOG_TAG "VulnerableLibWebP_1.3.1"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef struct {
    uint8_t bits;
    uint16_t value;
} HuffmanCode;

typedef struct {
    HuffmanCode huffman_table[256]; // 256 * 4 bytes (approx) = 1024 bytes
    void (*process_callback)(const char*);
} VP8LDecoder;

void internal_log(const char* msg) {
    LOGD("[ENGINE] %s", msg);
}

static void BuildVulnerableHuffmanTable(VP8LDecoder* decoder, int num_codes, const uint8_t* data) {
    LOGD("Building Huffman table with %d entries (Max capacity: 256)", num_codes);
    for (int i = 0; i < num_codes; i++) {
        // 💣 256을 넘어가면 decoder->process_callback을 덮어씀
        decoder->huffman_table[i].bits = data[i];
        decoder->huffman_table[i].value = (uint16_t)i;
    }
}

extern "C" {
JNIEXPORT jobject JNICALL
Java_com_mobility_hack_util_VulnerableWebPProcessor_nativeDecodeWebP(JNIEnv* env, jclass clazz, jbyteArray webpData) {
    if (!webpData) return NULL;
    jsize size = env->GetArrayLength(webpData);
    jbyte* raw = (jbyte*)env->GetByteArrayElements(webpData, NULL);
    uint8_t* data = (uint8_t*)raw;

    for (int i = 0; i < size - 12; i++) {
        if (memcmp(data + i, "VP8L", 4) == 0) {
            LOGD("Detected VP8L chunk. Simulating CVE-2023-4863...");
            VP8LDecoder* decoder = (VP8LDecoder*)malloc(sizeof(VP8LDecoder));
            if (decoder) {
                decoder->process_callback = internal_log;

                // 💣 수정: 2바이트를 읽어 256 이상의 num_codes 허용
                uint16_t num_codes = (data[i + 8] << 8) | data[i + 9];
                const uint8_t* code_lengths = data + i + 10;

                BuildVulnerableHuffmanTable(decoder, num_codes, code_lengths);

                if (decoder->process_callback) {
                    decoder->process_callback("Processing complete.");
                }
                free(decoder);
            }
            break;
        }
    }

    jobject resultBitmap = NULL;
    jclass bfClass = env->FindClass("android/graphics/BitmapFactory");
    if (bfClass) {
        jmethodID decodeMethod = env->GetStaticMethodID(bfClass, "decodeByteArray", "([BII)Landroid/graphics/Bitmap;");
        if (decodeMethod) {
            resultBitmap = env->CallStaticObjectMethod(bfClass, decodeMethod, webpData, 0, size);
        }
    }
    env->ReleaseByteArrayElements(webpData, raw, JNI_ABORT);
    return resultBitmap;
}
}
