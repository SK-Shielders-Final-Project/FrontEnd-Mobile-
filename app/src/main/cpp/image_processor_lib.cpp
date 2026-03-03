#include <jni.h>
#include <vector>

// 악성 코드를 제거하고, 원본 이미지 데이터를 그대로 반환합니다.
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_mobility_hack_util_NativeImageProcessor_processImage(JNIEnv *env, jclass, jbyteArray image_data) {
    return image_data;
}
