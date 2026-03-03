#include <jni.h>
#include <string>

// 보안 로직을 모두 제거하고, JNI 함수들은 비워두거나 기본값을 반환하도록 수정합니다.

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityBridge_getSecureApiKey(JNIEnv* env, jobject) {
    // 비워둠
    return env->NewStringUTF("");
}

JNIEXPORT jint JNICALL
Java_com_mobility_hack_security_SecurityBridge_detectRooting(JNIEnv* env, jobject, jobject) {
    // 비워둠
    return 0;
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_initAntiDebug(JNIEnv* env, jobject) {
    // 비워둠
}

JNIEXPORT jstring JNICALL
Java_com_mobility_hack_util_Keys_getApiKey(JNIEnv *env, jobject) {
    // 비워둠
    return env->NewStringUTF("");
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_startFridaMonitoring(JNIEnv *env, jobject) {
    // 비워둠
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_checkFridaOnce(JNIEnv *env, jobject) {
    // 비워둠
}

JNIEXPORT jboolean JNICALL
Java_com_mobility_hack_security_SecurityEngine_wasFridaDetectedEarly(JNIEnv *env, jobject) {
    // 비워둠
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SslGuard_setSslPinningEnabled(JNIEnv* env, jclass, jboolean) {
    // 비워둠
}

JNIEXPORT jboolean JNICALL
Java_com_mobility_hack_security_SslGuard_verifyCert(JNIEnv *env, jobject, jbyteArray, jboolean) {
    // 항상 유효하다고 가정
    return JNI_TRUE;
}

} // extern "C"
