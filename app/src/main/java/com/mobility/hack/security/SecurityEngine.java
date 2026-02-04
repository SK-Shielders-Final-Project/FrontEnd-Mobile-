package com.mobility.hack.security;

import android.content.Context;
import android.app.Activity;

/**
 * [통합 보안 모듈]
 * 기존의 RootDetector, SecurityBridge, SecurityEngine을 하나로 합친 클래스입니다.
 * - API 키 관리
 * - 안티 디버깅
 * - 강력한 C++ 기반 루팅 탐지 및 기만 전술 실행
 */
public class SecurityEngine {

    // JNI 라이브러리 로드
    static {
        System.loadLibrary("mobile");
    }

    // =========================================================
    // 1. [구 SignatureUtils] 무결성 검증 유틸 (Static 유지)
    // =========================================================
    /**
     * 앱의 서명(Signature)을 해시값으로 추출
     */
    public static native String getNativeSignature(Context context);

    /**
     * APK 바이너리 자체의 해시값을 추출 (위변조 확인)
     */
    public static native String getNativeBinaryHash(Context context);


    // 즉시 검사 함수 선언
    public native void checkFridaOnce();

}