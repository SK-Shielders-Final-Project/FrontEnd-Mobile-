#include <jni.h>
#include <cstdlib>
#include <cstring>
#include <android/log.h>

#define TAG "WebPImageLoader"

// 메타데이터 처리를 위한 콜백 (정상적인 앱 구조)
typedef void (*PropertyHandler)(const char*);

void onPropertyExtracted(const char* data) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Original Handler Called: Metadata contents safely processed.");
}

struct ImageMetadata {
    char profile_data[256];
    PropertyHandler handler;
};

extern "C" JNIEXPORT jobject JNICALL
Java_com_mobility_hack_util_VulnerableWebPProcessor_nativeDecodeWebP(JNIEnv* env, jclass clazz, jbyteArray webpData) {
    __android_log_print(ANDROID_LOG_INFO, TAG, "=== Current system() addr: %p ===", (void*)&system);

    if (!webpData) return NULL;
    jsize length = env->GetArrayLength(webpData);
    jbyte* buffer = env->GetByteArrayElements(webpData, NULL);

    if (length > 12 && memcmp(buffer, "RIFF", 4) == 0) {
        size_t offset = 12;
        while (offset + 8 <= (size_t)length) {
            uint32_t chunkSize = *(uint32_t*)(buffer + offset + 4);

            if (memcmp(buffer + offset, "EXIF", 4) == 0) {
                struct ImageMetadata* meta = (struct ImageMetadata*)malloc(sizeof(struct ImageMetadata));
                if (meta) {
                    meta->handler = onPropertyExtracted;
                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Before copy: handler resides at %p", (void*)meta->handler);

                    if (offset + 8 + chunkSize <= (size_t)length) {
                        memcpy(meta->profile_data, buffer + offset + 8, chunkSize);
                    }

                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "After copy: handler resides at %p", (void*)meta->handler);

                    if (meta->handler) {
                        meta->handler(meta->profile_data);
                    }
                    free(meta);
                }
                break;
            }

            offset += 8 + chunkSize + (chunkSize & 1); // WebP Padding rule
            if (offset >= (size_t)length) break;
        }
    }

    env->ReleaseByteArrayElements(webpData, buffer, JNI_ABORT);

    jclass factory = env->FindClass("android/graphics/BitmapFactory");
    if (factory) {
        jmethodID decode = env->GetStaticMethodID(factory, "decodeByteArray", "([BII)Landroid/graphics/Bitmap;");
        if (decode) {
            return env->CallStaticObjectMethod(factory, decode, webpData, 0, length);
        }
    }
    return NULL;
}
