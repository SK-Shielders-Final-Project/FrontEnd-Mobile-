#include <jni.h>
#include <sys/ptrace.h>
#include <unistd.h>

extern "C" JNIEXPORT void JNICALL
Java_com_mobility_hack_security_SecurityEngine_initAntiDebug(
        JNIEnv* env,
        jobject /* this */) {
    // [신규] ptrace를 이용한 디버깅 방지 기초
    // ptrace(PTRACE_TRACEME, 0, 0, 0);
}