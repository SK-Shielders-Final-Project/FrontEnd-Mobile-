package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.security.SecurityBridge;
import com.mobility.hack.security.SecurityEngine;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1. 보안 엔진 초기화 및 체크
        SecurityEngine engine = new SecurityEngine();
        SecurityBridge bridge = new SecurityBridge();

        // 안티 디버깅 활성화
        engine.initAntiDebug();

        // 루팅 탐지 수행
        int rootStatus = bridge.detectRooting(this);
        if (rootStatus == 0x47) {
            Toast.makeText(this, "보안 위협이 탐지되었습니다. (Rooted)", Toast.LENGTH_LONG).show();
            // bridge.checkSecurity(this)를 호출하여 기만 팝업을 띄울 수도 있습니다.
            bridge.checkSecurity(this);
            return; // 루팅 탐지 시 더 이상 진행하지 않음
        } else {
            Toast.makeText(this, "보안 엔진 가동 중... 안전함", Toast.LENGTH_SHORT).show();
        }

        // 2. 화면 전환 로직 (2초 대기)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // 가짜 로그인 상태 체크
                boolean isLoggedIn = true;

                Intent intent;
                if (isLoggedIn) {
                    // 로그인 성공 시 메인 화면으로 이동
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // 로그인 실패 시 로그인 화면으로 이동
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }

                startActivity(intent);
                finish(); // 스플래시 화면 종료
            }
        }, 2000);
    }
}