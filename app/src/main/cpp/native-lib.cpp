#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityEngine_getApiKey(
        JNIEnv* env,
        jobject /* this */) {
    // [4] API Key 은닉 실습용
    std::string apiKey = "HACKING_LAB_SECRET_KEY_12345";
    return env->NewStringUTF(apiKey.c_str());
}