package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // Delay를 위해 추가
import android.os.Looper;  // Main Thread 처리를 위해 추가
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.IntegrityRequest;
import com.mobility.hack.network.IntegrityResponse;
import com.mobility.hack.network.LoginResponse;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        MainApplication app = (MainApplication) getApplication();
        apiService = app.getApiService();
        tokenManager = app.getTokenManager();

        // ---------------------------------------------------------
        // [보안 단계 1] C++ 레벨 탐지 시작
        // ---------------------------------------------------------
        SecurityEngine engine = new SecurityEngine();
        SecurityBridge bridge = new SecurityBridge();

        //performIntegrityCheck();

        // [수정] 보안 검사 없이 1.5초 뒤 강제 이동 (테스트용)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkFlowAndNavigate();
        }, 1500);
    }

    /**
     * [보안 단계 3] 서버 연동 무결성 검사
     */
    private void performIntegrityCheck() {

        String sig = "";
        String bin = "";

        try {
            sig = SecurityEngine.getNativeSignature(this);
            bin = SecurityEngine.getNativeBinaryHash(this);
            Log.d("SECURITY", "Splash Check - Sig: " + sig + ", Bin: " + bin);
        } catch (UnsatisfiedLinkError e) {
            Log.e("SECURITY", "JNI Linking Error", e);
            showKillAppDialog();
            return;
        } catch (Exception e) {
            Log.e("SECURITY", "Unknown Error in JNI", e);
            showKillAppDialog();
            return;
        }

        apiService.checkIntegrity(new IntegrityRequest(sig, bin)).enqueue(new Callback<IntegrityResponse>() {
            @Override
            public void onResponse(@NotNull Call<IntegrityResponse> call, @NotNull Response<IntegrityResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isValid()) {
                        Log.d("SECURITY", "무결성 검증 통과 -> 앱 진입 시도");
                        checkFlowAndNavigate();
                    } else {
                        // 검증 실패 (변조됨)
                        showKillAppDialog();
                    }
                } else {
                    // 서버 오류(500 등) 발생 시에도 보안을 위해 종료 처리
                    Log.e("SECURITY", "Integrity Server Error: " + response.code());
                    handleNetworkErrorAndExit("서버 통신 오류가 발생했습니다. (Code: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NotNull Call<IntegrityResponse> call, @NotNull Throwable t) {
                Log.e("SECURITY", "Integrity Network Error", t);

                // [수정됨] Fail-Closed 정책 적용
                // 네트워크 오류 발생 시 검증 불가로 판단하고 앱 종료
                handleNetworkErrorAndExit("보안 검증을 위해 네트워크 연결이 필요합니다.");
            }
        });
    }

    /**
     * [보안/UX] 네트워크 오류 시 토스트 출력 후 앱 종료
     * 토스트가 뜰 시간을 확보하기 위해 1.5초 딜레이를 줌
     */
    private void handleNetworkErrorAndExit(String message) {
        Toast.makeText(SplashActivity.this, message, Toast.LENGTH_LONG).show();

        // 즉시 종료하면 토스트가 보이지 않을 수 있으므로 Handler 사용
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finishAffinity(); // 액티비티 스택 비우기
            System.exit(0);   // 프로세스 강제 종료
        }, 1500);
    }

    // ... (나머지 checkFlowAndNavigate, refreshAccessToken, showKillAppDialog 등 기존 코드 유지) ...

    private void checkFlowAndNavigate() {
        if (tokenManager != null && tokenManager.isAutoLoginEnabled() && tokenManager.fetchRefreshToken() != null) {
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
                LoginResponse loginResponse = response.body();
                if (response.isSuccessful() && loginResponse != null && loginResponse.getAccessToken() != null) {
                    tokenManager.saveAuthToken(loginResponse.getAccessToken());
                    if (loginResponse.getRefreshToken() != null) {
                        tokenManager.saveRefreshToken(loginResponse.getRefreshToken());
                    }
                    goToMainActivity();
                } else {
                    tokenManager.clearData();
                    goToLoginActivity();
                }
            }

            @Override
            public void onFailure(@NotNull Call<LoginResponse> call, @NotNull Throwable t) {
                Toast.makeText(SplashActivity.this, "네트워크 오류로 자동 로그인 실패", Toast.LENGTH_SHORT).show();
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