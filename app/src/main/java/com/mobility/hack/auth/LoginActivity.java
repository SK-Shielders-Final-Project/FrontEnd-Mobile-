package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.ride.MainActivity;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.LoginRequest;
import com.mobility.hack.network.LoginResponse;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;

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

        tokenManager = new TokenManager(getApplicationContext());
        Retrofit retrofit = RetrofitClient.getClient(tokenManager);
        apiService = retrofit.create(ApiService.class);

        EditText usernameEditText = findViewById(R.id.editTextId);
        EditText passwordEditText = findViewById(R.id.editTextPassword);
        Button loginButton = findViewById(R.id.buttonLogin);
        TextView registerTextView = findViewById(R.id.textViewRegister);
        TextView findPasswordTextView = findViewById(R.id.textViewFindPassword);
        autoLoginCheckBox = findViewById(R.id.checkbox_auto_login);

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            if (username.contains("' OR '1'='1'")) {
                Toast.makeText(this, "인증 우회 성공 (SQL Injection)", Toast.LENGTH_SHORT).show();
                goToMainActivity();
                return;
            }
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
            // TODO: 비밀번호 찾기 기능 구현 필요
        });
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
