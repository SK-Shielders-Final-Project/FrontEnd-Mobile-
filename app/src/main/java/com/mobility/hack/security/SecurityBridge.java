package com.mobility.hack.security;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;

public class SecurityBridge {
    static {
        // CMakeLists.txt에서 정의한 라이브러리 이름 "mobile"을 로드합니다.
        System.loadLibrary("mobile");
    }

    // 네이티브 함수 선언 (native-lib.cpp와 연결)
    public native int detectRooting(Context context);
    public native String getSecureApiKey();
    public static native void setSslPinningEnabled(boolean enabled);
    public static native boolean isSslPinningEnabled();

    /**
     * [app047 보안 기믹 재현]
     * 루팅이 탐지되면 즉시 종료하지 않고, '서버 점검 중'이라는 가짜 팝업을 띄웁니다.
     * 해커가 루팅 탐지 로직 때문이 아니라 단순 네트워크 문제로 오해하게 만드는 기만 전술입니다.
     */
    public void checkSecurity(final Context context) {
        // 네이티브에서 반환한 결과값 확인
        int resultCode = detectRooting(context);
        
        // 0x47은 app047 문제에서 사용된 탐지 코드 (가짜 에러 코드)
        if (resultCode == 0x47) { 
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(context)
                        .setTitle("System Maintenance")
                        .setMessage("현재 서버 점검 중입니다. 서비스 이용에 불편을 드려 죄송합니다.\n(Error Code: 0x47)")
                        .setCancelable(false)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 확인 클릭 시 앱 종료
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        }).show();
                }
            });
        }
    }
}