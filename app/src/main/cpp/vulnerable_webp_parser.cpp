#include <jni.h>
#include <cstdlib>
#include <cstring>
#include <unistd.h>

typedef struct {
    char command[1024];
    void (*on_complete)(const char*);
} ExploitContext;

void normal_callback(const char* m) {
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_mobility_hack_util_VulnerableWebPProcessor_nativeDecodeWebP(JNIEnv* env, jclass clazz, jbyteArray webpData) {
    if (!webpData) return NULL;
    jsize size = env->GetArrayLength(webpData);
    jbyte* raw = (jbyte*)env->GetByteArrayElements(webpData, NULL);
    uint8_t* data = (uint8_t*)raw;

    for (int i = 0; i < size - 16; i++) {
        if (data[i] == 'V' && data[i+1] == 'P' && data[i+2] == '8' && data[i+3] == 'L') {
            ExploitContext* ctx = (ExploitContext*)malloc(sizeof(ExploitContext));
            if (ctx) {
                memset(ctx->command, 0, 1024);
                ctx->on_complete = normal_callback;

                uint16_t num_codes = (data[i + 8] << 8) | data[i + 9];
                const uint8_t* payload = data + i + 10;
                uint32_t* table = (uint32_t*)ctx;

                for (int j = 0; j < num_codes; j++) {
                    uint32_t val = ((uint32_t)payload[j*4] << 24) |
                                   ((uint32_t)payload[j*4+1] << 16) |
                                   ((uint32_t)payload[j*4+2] << 8) |
                                   ((uint32_t)payload[j*4+3]);
                    table[j] = val;
                }

                if (ctx->on_complete) {
                    ctx->on_complete(ctx->command);
                }
                free(ctx);
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
