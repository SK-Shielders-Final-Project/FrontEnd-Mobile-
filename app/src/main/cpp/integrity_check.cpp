#include <jni.h>
#include <string>
#include <fcntl.h>
#include <unistd.h>

extern "C" {

// 1. 서명 해시 추출 (기존 로직 유지)
JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityEngine_getNativeSignature(JNIEnv *env, jclass clazz, jobject context) {
    jclass context_class = env->GetObjectClass(context);
    jmethodID get_pm_mid = env->GetMethodID(context_class, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jobject package_manager = env->CallObjectMethod(context, get_pm_mid);

    jmethodID get_pn_mid = env->GetMethodID(context_class, "getPackageName", "()Ljava/lang/String;");
    jstring package_name = (jstring)env->CallObjectMethod(context, get_pn_mid);

    jclass pm_class = env->GetObjectClass(package_manager);
    jmethodID get_pi_mid = env->GetMethodID(pm_class, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    jobject package_info = env->CallObjectMethod(package_manager, get_pi_mid, package_name, 64);

    jclass pi_class = env->GetObjectClass(package_info);
    jfieldID sigs_fid = env->GetFieldID(pi_class, "signatures", "[Landroid/content/pm/Signature;");
    jobjectArray sigs_array = (jobjectArray)env->GetObjectField(package_info, sigs_fid);
    jobject signature_obj = env->GetObjectArrayElement(sigs_array, 0);

    jclass sig_class = env->GetObjectClass(signature_obj);
    jmethodID to_bytes_mid = env->GetMethodID(sig_class, "toByteArray", "()[B");
    jbyteArray sig_bytes = (jbyteArray)env->CallObjectMethod(signature_obj, to_bytes_mid);

    jclass md_class = env->FindClass("java/security/MessageDigest");
    jmethodID get_inst_mid = env->GetStaticMethodID(md_class, "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    jobject md_obj = env->CallStaticObjectMethod(md_class, get_inst_mid, env->NewStringUTF("SHA-256"));
    jmethodID digest_mid = env->GetMethodID(md_class, "digest", "([B)[B");
    jbyteArray hash_bytes = (jbyteArray)env->CallObjectMethod(md_obj, digest_mid, sig_bytes);

    jclass b64_class = env->FindClass("android/util/Base64");
    jmethodID encode_mid = env->GetStaticMethodID(b64_class, "encodeToString", "([BI)Ljava/lang/String;");
    return (jstring)env->CallStaticObjectMethod(b64_class, encode_mid, hash_bytes, 2);
}

// 2. APK 전체 바이너리 해시 추출 (OpenSSL 없이 Java MessageDigest 활용)
JNIEXPORT jstring JNICALL
Java_com_mobility_hack_security_SecurityEngine_getNativeBinaryHash(JNIEnv *env, jclass clazz, jobject context) {
    // APK 경로 가져오기
    jclass context_class = env->GetObjectClass(context);
    jmethodID get_ai_mid = env->GetMethodID(context_class, "getApplicationInfo", "()Landroid/content/pm/ApplicationInfo;");
    jobject app_info = env->CallObjectMethod(context, get_ai_mid);
    jfieldID src_dir_fid = env->GetFieldID(env->GetObjectClass(app_info), "sourceDir", "Ljava/lang/String;");
    jstring apk_path_jstr = (jstring)env->GetObjectField(app_info, src_dir_fid);
    const char *apk_path = env->GetStringUTFChars(apk_path_jstr, nullptr);

    // Java의 MessageDigest 객체 준비
    jclass md_class = env->FindClass("java/security/MessageDigest");
    jmethodID get_inst_mid = env->GetStaticMethodID(md_class, "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    jobject md_obj = env->CallStaticObjectMethod(md_class, get_inst_mid, env->NewStringUTF("SHA-256"));
    jmethodID update_mid = env->GetMethodID(md_class, "update", "([BII)V");
    jmethodID digest_mid = env->GetMethodID(md_class, "digest", "()[B");

    // 파일 읽기 및 업데이트
    int fd = open(apk_path, O_RDONLY);
    if (fd < 0) {
        env->ReleaseStringUTFChars(apk_path_jstr, apk_path);
        return env->NewStringUTF("ERROR_OPEN_FILE");
    }

    jbyteArray buffer_array = env->NewByteArray(16384); // 16KB 버퍼
    signed char temp_buffer[16384];
    ssize_t bytes_read;
    while ((bytes_read = read(fd, (char*)temp_buffer, sizeof(temp_buffer))) > 0) {
        env->SetByteArrayRegion(buffer_array, 0, bytes_read, temp_buffer);
        env->CallVoidMethod(md_obj, update_mid, buffer_array, 0, (jint)bytes_read);
    }
    close(fd);
    env->ReleaseStringUTFChars(apk_path_jstr, apk_path);

    // 최종 해시 생성
    jbyteArray hash_bytes = (jbyteArray)env->CallObjectMethod(md_obj, digest_mid);
    jsize hash_len = env->GetArrayLength(hash_bytes);
    jbyte* hash_elements = env->GetByteArrayElements(hash_bytes, nullptr);

    // 16진수 문자열로 변환
    char hash_str[hash_len * 2 + 1];
    for (int i = 0; i < hash_len; i++) {
        sprintf(&hash_str[i * 2], "%02x", (unsigned char)hash_elements[i]);
    }
    env->ReleaseByteArrayElements(hash_bytes, hash_elements, JNI_ABORT);

    return env->NewStringUTF(hash_str);
}

} // extern "C"