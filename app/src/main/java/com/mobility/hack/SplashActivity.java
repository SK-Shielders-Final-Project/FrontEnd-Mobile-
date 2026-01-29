package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.LoginResponse;
import com.mobility.hack.network.RefreshRequest;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.ride.MainActivity;
import com.mobility.hack.security.SecurityBridge;
import com.mobility.hack.security.SecurityEngine;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private TokenManager tokenManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tokenManager = new TokenManager(this);
        apiService = RetrofitClient.getClient(tokenManager).create(ApiService.class);

        SecurityEngine engine = new SecurityEngine();
        SecurityBridge bridge = new SecurityBridge();
        engine.initAntiDebug();
        int rootStatus = bridge.detectRooting(this);
        if (rootStatus == 0x47) {
            Toast.makeText(this, "보안 위협이 탐지되었습니다. (Rooted)", Toast.LENGTH_LONG).show();
            bridge.checkSecurity(this);
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (tokenManager.isAutoLoginEnabled() && tokenManager.fetchRefreshToken() != null) {
                refreshAccessToken();
            } else {
                goToLoginActivity();
            }
        }, 2000); // Splash 2초 표시
    }

    private void refreshAccessToken() {
        String refreshToken = tokenManager.fetchRefreshToken();
        apiService.refresh(new RefreshRequest(refreshToken)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NotNull Call<LoginResponse> call, @NotNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 새 액세스 토큰 저장
                    tokenManager.saveAuthToken(response.body().getAccessToken());
                    // 메인 화면으로 이동
                    goToMainActivity();
                } else {
                    // 리프레시 토큰이 만료되었거나 유효하지 않음
                    tokenManager.clearData(); // 모든 토큰 정보 삭제
                    goToLoginActivity();
                }
            }

            @Override
            public void onFailure(@NotNull Call<LoginResponse> call, @NotNull Throwable t) {
                // 네트워크 오류 등
                Toast.makeText(SplashActivity.this, "네트워크 오류로 자동 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                goToLoginActivity();
            }
        });
    }

    private void goToMainActivity() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    private void goToLoginActivity() {
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        finish();
    }
}
