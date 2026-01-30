package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.UserInfoResponse;
import com.mobility.hack.security.SecurityBridge;
import com.mobility.hack.security.SecurityEngine;
import com.mobility.hack.security.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

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
            TokenManager tokenManager = new TokenManager(this);
            String token = tokenManager.fetchAuthToken();

            if (token != null && !token.isEmpty()) {
                ApiService apiService = ((MainApplication) getApplication()).getApiService();
                long userId = tokenManager.fetchUserId();
                apiService.getUserInfo(userId).enqueue(new Callback<UserInfoResponse>() {
                    @Override
                    public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                        if (response.isSuccessful()) {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        } else {
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        }
                        finish();
                    }

                    @Override
                    public void onFailure(Call<UserInfoResponse> call, Throwable t) {
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
