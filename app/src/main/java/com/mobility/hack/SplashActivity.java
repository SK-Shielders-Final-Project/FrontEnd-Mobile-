package com.mobility.hack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.SecurityBridge;
import com.mobility.hack.security.SecurityEngine;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1. 보안 엔진 초기화 및 체크 (유지)
        SecurityEngine engine = new SecurityEngine();
        SecurityBridge bridge = new SecurityBridge();

        // 안티 디버깅 활성화
        engine.initAntiDebug();

        // NDK 기반 루팅 탐지 수행 (0x47 체크)
        int rootStatus = bridge.detectRooting(this);
        if (rootStatus == 0x47) {
            Toast.makeText(this, "보안 위협이 탐지되었습니다. (Rooted)", Toast.LENGTH_LONG).show();
            bridge.checkSecurity(this);
            return;
        }

        // 2. 화면 전환 로직 (2초 대기)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("MobilityToken", MODE_PRIVATE);
            String token = prefs.getString("access_token", null);

            if (token != null && !token.isEmpty()) {
                // 프로젝트 구조에 맞는 API 호출 방식 적용
                ApiService apiService = RetrofitClient.getInstance().getApiService();
                apiService.validateToken("Bearer " + token).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        } else {
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        }
                        finish();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, 2000);
    }
}