#include <jni.h>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_mobility_hack_security_SecurityEngine_isKernelSuDetected(
        JNIEnv* env,
        jobject /* this */) {
    // [5] KernelSU 및 변조 탐지 로직
    return JNI_FALSE;
}