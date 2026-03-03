// SplashActivity.java
package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.IntegrityVerifyRequest;
import com.mobility.hack.network.IntegrityTokenResponse;
import com.mobility.hack.network.LoginResponse;
import com.mobility.hack.network.NonceResponse;
import com.mobility.hack.network.RefreshRequest;
import com.mobility.hack.ride.MainActivity;
import com.mobility.hack.security.SecurityBridge;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {
    
    @Inject
    TokenManager tokenManager;
    
    @Inject
    ApiService apiService;
    
    private SecurityBridge bridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        bridge = new SecurityBridge();

        // Hilt @Inject를 사용하므로 수동 할당 코드 제거

        // ---------------------------------------------------------
        // [보안 단계 2] 무결성 검증 (Nonce → Verify)
        // ---------------------------------------------------------
        performIntegrityCheck();
    }

    /**
     * 무결성 검증 플로우: Nonce 요청 → 검증 → Token 저장
     */
    private void performIntegrityCheck() {
        // Step 1: Nonce 요청
        apiService.getNonce().enqueue(new Callback<NonceResponse>() {
            @Override
            public void onResponse(@NotNull Call<NonceResponse> call, @NotNull Response<NonceResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String nonce = response.body().getNonce();
                    Log.d("SECURITY", "Nonce received: " + nonce);
                    verifyIntegrityWithNonce(nonce);
                } else {
                    handleNetworkErrorAndExit("Nonce 발급 실패 (Code: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NotNull Call<NonceResponse> call, @NotNull Throwable t) {
                Log.e("SECURITY", "Nonce request failed", t);
                handleNetworkErrorAndExit("보안 검증을 위해 네트워크 연결이 필요합니다.");
            }
        });
    }

    /**
     * Step 2: 무결성 검증 및 Integrity Token 발급
     */
    private void verifyIntegrityWithNonce(String nonce) {
        String sig, bin;

        try {
            // 🎯 서버에 저장된 정상 해시값으로 하드코딩 (시연용 유지)
            sig = "L56U8dn6LWLkEWv5SQl2lZjlaP6Ep2YlAG8qiC+AsD4=";
            bin = "c59618b65f9f6e44c453563590566a28b5f1bcdaf4de91fc1c9dd9cc35676c2f";

            Log.d("SECURITY", "Hardcoded Sig: " + sig);
            Log.d("SECURITY", "Hardcoded Bin: " + bin);
        } catch (Exception e) {
            Log.e("SECURITY", "Failed to get hash", e);
            showKillAppDialog();
            return;
        }

        IntegrityVerifyRequest request = new IntegrityVerifyRequest(nonce, bin, sig);

        apiService.verifyIntegrity(request).enqueue(new Callback<IntegrityTokenResponse>() {
            @Override
            public void onResponse(@NotNull Call<IntegrityTokenResponse> call, @NotNull Response<IntegrityTokenResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String integrityToken = response.body().getIntegrityToken();
                    tokenManager.saveIntegrityToken(integrityToken);
                    Log.d("SECURITY", "✅ Integrity Token saved successfully!");

                    checkFlowAndNavigate();
                } else {
                    Log.e("SECURITY", "Integrity verification failed: " + response.code());
                    showKillAppDialog();
                }
            }

            @Override
            public void onFailure(@NotNull Call<IntegrityTokenResponse> call, @NotNull Throwable t) {
                Log.e("SECURITY", "Integrity verification error", t);
                handleNetworkErrorAndExit("무결성 검증 실패");
            }
        });
    }

    private void handleNetworkErrorAndExit(String message) {
        Toast.makeText(SplashActivity.this, message, Toast.LENGTH_LONG).show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finishAffinity();
            System.exit(0);
        }, 1500);
    }

    private void checkFlowAndNavigate() {
        if (tokenManager.isAutoLoginEnabled() && tokenManager.fetchRefreshToken() != null) {
            refreshAccessToken();
        } else {
            goToLoginActivity();
        }
    }

    private void refreshAccessToken() {
        String refreshToken = tokenManager.fetchRefreshToken();

        apiService.refresh(new RefreshRequest(refreshToken)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NotNull Call<LoginResponse> call, @NotNull Response<LoginResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                LoginResponse loginResponse = response.body();
                if (response.isSuccessful() && loginResponse != null && loginResponse.getAccessToken() != null) {
                    tokenManager.saveAuthToken(loginResponse.getAccessToken());
                    if (loginResponse.getRefreshToken() != null) {
                        tokenManager.saveRefreshToken(loginResponse.getRefreshToken());
                    }
                    tokenManager.clearIntegrityToken();
                    goToMainActivity();
                } else {
                    tokenManager.clearData();
                    goToLoginActivity();
                }
            }

            @Override
            public void onFailure(@NotNull Call<LoginResponse> call, @NotNull Throwable t) {
                Toast.makeText(SplashActivity.this, "자동 로그인 실패", Toast.LENGTH_SHORT).show();
                goToLoginActivity();
            }
        });
    }

    private void showKillAppDialog() {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setTitle("⛔ 보안 경고")
                .setMessage("변조된 앱이 감지되었습니다.\n안전을 위해 앱을 종료합니다.")
                .setCancelable(false)
                .setPositiveButton("종료", (dialog, which) -> {
                    finishAffinity();
                    System.exit(0);
                })
                .show();
    }

    private void goToMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
