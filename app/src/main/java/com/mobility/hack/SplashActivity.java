package com.mobility.hack;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.LoginResponse;
import com.mobility.hack.network.RefreshRequest;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.ride.MainActivity;
import com.mobility.hack.security.SecurityEngine; // [중요] 통합된 보안 엔진 임포트
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private TokenManager tokenManager;
    private ApiService apiService;

    // [변경] JNI 로드는 이제 SecurityEngine 내부에서 처리하므로 여기서 뺐습니다.
    // public native void startSystemCheck(); -> 이것도 SecurityEngine으로 이동했습니다.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tokenManager = new TokenManager(this);
        apiService = RetrofitClient.getClient(tokenManager).create(ApiService.class);

        // =============================================================
        // [보안] 3-in-1 통합 보안 엔진 (SecurityEngine) 사용
        // =============================================================
        SecurityEngine securityEngine = new SecurityEngine();

        // 1. 안티 디버깅 초기화 (선택 사항)
        securityEngine.initAntiDebug();

        // 2. 통합 시스템 검사 시작 (제어권 역전)
        // 'this'를 넘겨주는 이유는, C++이 검사 후 이 액티비티의 함수(onNetworkError 등)를
        // 다시 호출(Callback)해줘야 하기 때문입니다.
        securityEngine.startSystemCheck(this);
    }

    // =========================================================================
    // [C++ 호출용 콜백 함수들]
    // C++(native-lib)이 startSystemCheck(this)로 받은 객체(이 액티비티) 안에서
    // 아래 함수들을 찾아 실행합니다. (리플렉션)
    // =========================================================================

    /**
     * [기만 전술] 루팅이 감지되면 C++이 이 함수를 호출합니다.
     * 이름은 '네트워크 에러'처럼 위장되어 있습니다.
     */
    public void onNetworkError(int errorCode) {
        // UI 변경은 메인 스레드에서
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("System Maintenance")
                    .setMessage("현재 서버 긴급 점검 중입니다.\n서비스 이용에 불편을 드려 죄송합니다.\n(Error Code: " + errorCode + ")") // 0x47 -> 71
                    .setCancelable(false)
                    .setPositiveButton("확인", (dialog, which) -> {
                        // 확인 누르면 앱 종료
                        finishAffinity();
                        System.exit(0);
                    })
                    .show();
        });
    }

    /**
     * [정상] 시스템이 안전하면 C++이 이 함수를 호출합니다.
     * 기존의 로그인/메인 이동 로직이 여기서 실행됩니다.
     */
    public void onSystemStable() {
        runOnUiThread(() -> {
            // 스플래시 화면 2초 유지 후 로그인 로직 진행
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (tokenManager.isAutoLoginEnabled() && tokenManager.fetchRefreshToken() != null) {
                    refreshAccessToken();
                } else {
                    goToLoginActivity();
                }
            }, 2000);
        });
    }

    // =========================================================================
    // [기존 로직] 로그인 관련 헬퍼 함수들 (그대로 유지)
    // =========================================================================

    private void refreshAccessToken() {
        String refreshToken = tokenManager.fetchRefreshToken();
        apiService.refresh(new RefreshRequest(refreshToken)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NotNull Call<LoginResponse> call, @NotNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    tokenManager.saveAuthToken(loginResponse.getAccessToken());

                    if (loginResponse.getRefreshToken() != null && !loginResponse.getRefreshToken().isEmpty()) {
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
                Toast.makeText(SplashActivity.this, "네트워크 오류로 자동 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                goToLoginActivity();
            }
        });
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