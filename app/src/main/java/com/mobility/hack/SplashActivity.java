package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.security.TokenManager;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "CRASH_CHECK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "SplashActivity: App Started"); // [3] 로그 추적

        try {
            setContentView(R.layout.activity_splash);
            Log.e(TAG, "SplashActivity: Layout Set"); // [3] 로그 추적

            // [2] 보안 엔진 임시 비활성화 (권한/런타임 종료 방지)
            /*
            SecurityEngine engine = new SecurityEngine();
            SecurityBridge bridge = new SecurityBridge();
            engine.initAntiDebug();
            if (bridge.detectRooting(this) == 0x47) {
                bridge.checkSecurity(this);
                return;
            }
            */

            // [1] 초기화 예외 처리 및 화면 전환
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    TokenManager tokenManager = new TokenManager(this);
                    String token = tokenManager.fetchAuthToken();
                    
                    if (token != null) {
                        Log.e(TAG, "SplashActivity: Token Found -> MainActivity");
                        startActivity(new Intent(this, MainActivity.class));
                    } else {
                        Log.e(TAG, "SplashActivity: No Token -> LoginActivity");
                        startActivity(new Intent(this, LoginActivity.class));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "TokenManager Error: " + e.getMessage());
                    // 에러가 나더라도 무조건 로그인 화면으로 보냄
                    startActivity(new Intent(this, LoginActivity.class));
                }
                finish();
            }, 2000);

        } catch (Exception e) {
            Log.e(TAG, "SplashActivity Critical Error: " + e.getMessage());
            // 치명적 에러 시 즉시 로그인으로 탈출 시도
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
