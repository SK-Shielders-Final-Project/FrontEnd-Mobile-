package com.mobility.hack.util;

import android.graphics.Bitmap;
import android.util.Log;

public class VulnerableWebPProcessor {
    private static final String TAG = "VulnerableWebPProc";

    static {
        try {
            System.loadLibrary("mobile");
            Log.i(TAG, "Native library loaded.");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library.", e);
        }
    }

    /**
     * 이미지 바이트 배열을 취약한 네이티브 파서로 전달하는 메소드.
     * @param webpData 공격용 페이로드가 포함된 바이트 배열
     * @return 렌더링된 비트맵 (오버플로우 성공 시 리턴되지 않음)
     */
    public static native Bitmap nativeDecodeWebP(byte[] webpData);
}
