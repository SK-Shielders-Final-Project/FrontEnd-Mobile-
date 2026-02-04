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


    // =========================================================
    // 2. [구 SecurityEngine] 보안 로직 및 탐지 (Instance 유지)
    // =========================================================
    /**
     * 네이티브에 숨겨진 API Key 가져오기
     */
    public native String getApiKey();

    /**
     * 종합 무결성 검사 (루팅 + 서명 등)
     */
    public native boolean checkIntegrity();

    /**
     * KernelSU 등 최신 루팅 툴 탐지
     */
    public native boolean isKernelSuDetected();

    /**
     * 안티 디버깅 초기화 (Frida, GDB 탐지)
     */
    public native void initAntiDebug();

    /**
     * [Root Check] 시스템 무결성 검사를 시작합니다.
     * @param activity : 검사 결과에 따라 콜백 함수(onNetworkError 등)를 호출할 대상 액티비티
     */
    public native void startSystemCheck(Object activity);

    // ▼ Frida 탐지 모니터링 시작 메서드
    public native void startFridaMonitoring();

    // 즉시 검사 함수 선언
    public native void checkFridaOnce();

    public native boolean wasFridaDetectedEarly();
}