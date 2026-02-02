package com.mobility.hack.security;

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
        System.loadLibrary("native-lib");
    }

    // ========================================================================
    // [Native Functions] C++(native-lib.cpp)과 연결되는 함수들
    // ========================================================================


    public native String getApiKey(); // 추가
    public native boolean checkIntegrity(); //  추가 예정
    public native boolean isKernelSuDetected(); // 추가 예정
    public native void initAntiDebug();

    /**
     * [Root Check] 시스템 무결성 검사를 시작합니다.
     * @param activity : 검사 결과에 따라 콜백 함수(onNetworkError 등)를 호출할 대상 액티비티
     */
    public native void startSystemCheck(Object activity);
}