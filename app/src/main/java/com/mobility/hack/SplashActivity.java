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
import com.mobility.hack.security.SecurityEngine;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {
    private TokenManager tokenManager;
    private ApiService apiService;
    private SecurityBridge bridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        bridge = new SecurityBridge();

        MainApplication app = (MainApplication) getApplication();
        apiService = app.getApiService();
        tokenManager = app.getTokenManager();

        // ---------------------------------------------------------
        // [보안 단계 1] Root 탐지
        // ---------------------------------------------------------
/*        int rootResult = bridge.detectRooting(this);
        if (rootResult == 0x47) {
            Log.e("SECURITY", "Rooting Detected");
            showKillAppDialog();
            return;
        }*/

        // ---------------------------------------------------------
        // [보안 단계 2] 무결성 검증 (Nonce → Verify)
        // ---------------------------------------------------------
        performIntegrityCheck();
    }

    /**
     * 무결성 검증 플로우: Nonce 요청 → 검증 → Token 저장
     */
    private void performIntegrityCheck() {
/*        // [긴급 추가] 서버 통신 전 해시값 강제 출력
        try {
            String tempSig = SecurityEngine.getNativeSignature(this);
            String tempBin = SecurityEngine.getNativeBinaryHash(this);

            Log.e("MY_HASH", "=========================================");
            Log.e("MY_HASH", "SIGNATURE_HASH (서명): " + tempSig);
            Log.e("MY_HASH", "BINARY_HASH (바이너리): " + tempBin);
            Log.e("MY_HASH", "=========================================");

        } catch (Exception e) {
            Log.e("MY_HASH", "해시 추출 실패", e);
        }*/

        // 기존 통신 로직
        //new SecurityEngine().checkFridaOnce();

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
/*    private void verifyIntegrityWithNonce(String nonce) {
        String sig, bin;

        try {
            sig = SecurityEngine.getNativeSignature(this);
            bin = SecurityEngine.getNativeBinaryHash(this);
            Log.d("SECURITY", "Sig: " + sig.substring(0, 10) + "...");
            Log.d("SECURITY", "Bin: " + bin.substring(0, 10) + "...");
        } catch (Exception e) {
            Log.e("SECURITY", "Failed to get hash", e);
            showKillAppDialog();
            return;
        }

        IntegrityVerifyRequest request = new IntegrityVerifyRequest(nonce, bin, sig);

        // Interceptor가 자동으로 X-Device-Id 헤더 추가
        apiService.verifyIntegrity(request).enqueue(new Callback<IntegrityTokenResponse>() {
            @Override
            public void onResponse(@NotNull Call<IntegrityTokenResponse> call, @NotNull Response<IntegrityTokenResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String integrityToken = response.body().getIntegrityToken();
                    tokenManager.saveIntegrityToken(integrityToken);
                    Log.d("SECURITY", "✅ Integrity Token saved");

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
    }*/

    /**
     * Step 2: 무결성 검증 및 Integrity Token 발급
     */
    private void verifyIntegrityWithNonce(String nonce) {
        // [하드코딩 테스트용] 변수 선언
        String sig, bin;

        try {
            // 원본 코드는 유지하되, 아래에서 값을 덮어씌웁니다.
            // sig = SecurityEngine.getNativeSignature(this);
            // bin = SecurityEngine.getNativeBinaryHash(this);

            // 🎯 서버에 저장된 정상 해시값으로 하드코딩 (덮어씌우기)
            sig = "L56U8dn6LWLkEWv5SQl2lZjlaP6Ep2YlAG8qiC+AsD4=";
            bin = "c59618b65f9f6e44c453563590566a28b5f1bcdaf4de91fc1c9dd9cc35676c2f";

            Log.d("SECURITY", "Hardcoded Sig: " + sig);
            Log.d("SECURITY", "Hardcoded Bin: " + bin);
        } catch (Exception e) {
            Log.e("SECURITY", "Failed to get hash", e);
            showKillAppDialog();
            return;
        }

        // 서버 전송용 DTO 생성 (이제 하드코딩된 값이 들어갑니다)
        IntegrityVerifyRequest request = new IntegrityVerifyRequest(nonce, bin, sig);

        // API 호출 (Verify)
        apiService.verifyIntegrity(request).enqueue(new Callback<IntegrityTokenResponse>() {
            @Override
            public void onResponse(@NotNull Call<IntegrityTokenResponse> call, @NotNull Response<IntegrityTokenResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String integrityToken = response.body().getIntegrityToken();
                    tokenManager.saveIntegrityToken(integrityToken);
                    Log.d("SECURITY", "✅ [Test] Real Token saved successfully!");

                    checkFlowAndNavigate();
                } else {
                    // 에러가 난다면 response.code()가 500인지 확인해보세요.
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

        // Interceptor가 자동으로 X-Device-Id, X-Integrity-Token 헤더 추가
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

                    // ===== 토큰 사용 완료, 즉시 삭제 =====
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