package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.mobility.hack.MainActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.LoginRequest;
import com.mobility.hack.network.LoginResponse;
import com.mobility.hack.security.TokenManager;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 자동 로그인 체크
        if (MainApplication.getTokenManager() != null && MainApplication.getTokenManager().fetchAuthToken() != null) {
            goToMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        EditText usernameEditText = findViewById(R.id.editTextId);
        EditText passwordEditText = findViewById(R.id.editTextPassword);
        Button loginButton = findViewById(R.id.buttonLogin);
        TextView registerTextView = findViewById(R.id.textViewRegister);
        TextView findPasswordTextView = findViewById(R.id.textViewFindPassword);

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            login(new LoginRequest(username, password));
        });

        registerTextView.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        findPasswordTextView.setOnClickListener(v -> startActivity(new Intent(this, FindPasswordActivity.class)));
    }

    private void login(LoginRequest loginRequest) {
        if (apiService == null) return;

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NotNull Call<LoginResponse> call, @NotNull Response<LoginResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    String accessToken = loginResponse.getAccessToken();
                    String refreshToken = loginResponse.getRefreshToken();
                    Long userId = loginResponse.getUserId();

                    // [2] [보안] 데이터 저장 전 방어 코드
                    TokenManager tm = MainApplication.getTokenManager();
                    if (tm != null && !accessToken.isEmpty()) {
                        tm.saveTokens(accessToken, refreshToken);
                        if (userId != 0L) tm.saveUserId(userId);
                        
                        // [2] 저장 완료 로그 기록
                        Log.i(TAG, "토큰 저장 완료: " + accessToken);
                        
                        Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                        
                        // [3] 데이터 저장을 위해 0.5초 지연 후 이동
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            goToMainActivity();
                        }, 500);
                    }
                } else {
                    Log.e(TAG, "Login failed: " + response.code());
                    Toast.makeText(LoginActivity.this, "아이디 또는 비밀번호 확인", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<LoginResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        // [3] 이전 스택을 완전히 비우는 플래그 적용
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
