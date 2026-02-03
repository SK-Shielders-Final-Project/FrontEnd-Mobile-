package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // 로그용 추가
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog; // 다이얼로그용 추가
import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.LoginRequest;
import com.mobility.hack.network.LoginResponse;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.SecurityEngine;
import com.mobility.hack.ride.MainActivity;
import com.mobility.hack.security.TokenManager;
// [추가] 무결성 검증을 위한 클래스 임포트 (패키지명 본인 프로젝트에 맞게 수정 필요)
import com.mobility.hack.network.IntegrityRequest;
import com.mobility.hack.network.IntegrityResponse;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {
    private ApiService apiService;
    private TokenManager tokenManager;
    private CheckBox autoLoginCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // MainApplication에서 ApiService 및 TokenManager 인스턴스 가져오기
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // ---------------------------------------------------------
        // [보안 로직 추가] 화면 진입 즉시 무결성 검사 실행
        // ---------------------------------------------------------
        performIntegrityCheck();

        EditText usernameEditText = findViewById(R.id.editTextId);
        EditText passwordEditText = findViewById(R.id.editTextPassword);
        Button loginButton = findViewById(R.id.buttonLogin);
        TextView registerTextView = findViewById(R.id.textViewRegister);
        TextView findPasswordTextView = findViewById(R.id.textViewFindPassword);
        autoLoginCheckBox = findViewById(R.id.checkbox_auto_login);

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (!username.isEmpty() && !password.isEmpty()) {
                login(new LoginRequest(username, password));
            } else {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });

        findPasswordTextView.setOnClickListener(v -> {
            Intent intent = new Intent(this, FindPasswordActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 앱 무결성 검사 메서드
     * - 성공 시: 아무 동작 안 함 (로그인 화면 유지)
     * - 실패 시: 경고창 띄우고 앱 강제 종료
     */
    /**
     * 앱 무결성 검사 메서드 (안전 버전)
     */
    private void performIntegrityCheck() {
        String sig = "";
        String bin = "";

        // 1. JNI 호출 시도 (여기서 튕기는 것을 방지)
        try {
            sig = SecurityEngine.getNativeSignature(this);
            bin = SecurityEngine.getNativeBinaryHash(this);

            Log.d("SECURITY", "Signature: " + sig);
            Log.d("SECURITY", "Binary: " + bin);

        } catch (UnsatisfiedLinkError e) {
            // C++ 함수 이름이 패키지명과 다르거나, 라이브러리 로드 실패 시 발생
            Log.e("SECURITY", "CRITICAL ERROR: JNI 연결 실패! C++ 함수 이름을 확인하세요.", e);
            Toast.makeText(this, "보안 모듈 로드 실패 (로그 확인 필요)", Toast.LENGTH_LONG).show();
            return; // 더 이상 진행하지 않음 (로그인 유지 or 종료 정책 결정 필요)
        } catch (Exception e) {
            Log.e("SECURITY", "알 수 없는 오류 발생", e);
            return;
        }

        // 2. 요청 객체 생성
        IntegrityRequest req = new IntegrityRequest(sig, bin);

        // 3. 서버로 전송
        apiService.checkIntegrity(req).enqueue(new Callback<IntegrityResponse>() {
            @Override
            public void onResponse(@NotNull Call<IntegrityResponse> call, @NotNull Response<IntegrityResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isValid()) {
                        Log.d("SECURITY", "무결성 검증 통과");
                    } else {
                        // 검증 실패 시에만 다이얼로그 호출
                        showKillAppDialog();
                    }
                } else {
                    Log.e("SECURITY", "서버 응답 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(@NotNull Call<IntegrityResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Log.e("SECURITY", "네트워크 오류", t);
                // 네트워크 오류는 일단 통과시킴 (로그만 남김)
            }
        });
    }

    /**
     * 앱 강제 종료 다이얼로그
     */
    private void showKillAppDialog() {
        if (isFinishing()) return;

        new AlertDialog.Builder(LoginActivity.this)
                .setTitle("⛔ 보안 경고")
                .setMessage("변조된 앱이 감지되었습니다.\n안전을 위해 앱을 종료합니다.")
                .setCancelable(false) // 뒤로가기 금지
                .setPositiveButton("종료", (dialog, which) -> {
                    finishAffinity(); // 액티비티 스택 제거
                    System.exit(0);   // 프로세스 종료
                })
                .show();
    }

    private void login(LoginRequest loginRequest) {
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NotNull Call<LoginResponse> call, @NotNull Response<LoginResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                LoginResponse loginResponse = response.body();

                if (response.isSuccessful() && loginResponse != null && loginResponse.getAccessToken() != null && !loginResponse.getAccessToken().isEmpty()) {
                    tokenManager.saveAuthToken(loginResponse.getAccessToken());
                    tokenManager.saveUserId(loginResponse.getUserId());

                    if (autoLoginCheckBox.isChecked()) {
                        tokenManager.saveRefreshToken(loginResponse.getRefreshToken());
                        tokenManager.saveAutoLogin(true);
                    } else {
                        tokenManager.saveRefreshToken(null);
                        tokenManager.saveAutoLogin(false);
                    }

                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                } else {
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다. 아이디 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<LoginResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(LoginActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        // 기존의 모든 액티비티를 스택에서 제거하고, 새로운 태스크를 시작합니다.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}