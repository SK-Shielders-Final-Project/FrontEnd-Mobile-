package com.mobility.hack.util;

public class Keys {
    static {
        // CMakeLists.txt에서 설정한 라이브러리 이름인 "mobile"을 로드합니다.
        System.loadLibrary("mobile");
    }

    /**
     * NDK(C++)로부터 은닉된 API Key를 가져옵니다.
     */
    public native String getApiKey();
}