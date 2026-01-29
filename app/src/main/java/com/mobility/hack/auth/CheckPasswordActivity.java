package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.CheckPasswordRequest;
import com.mobility.hack.network.CheckPasswordResponse;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CheckPasswordActivity extends AppCompatActivity {
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_password);
        tokenManager = new TokenManager(getApplicationContext());
        // 수정된 접근 방식
        Retrofit retrofit = RetrofitClient.getClient(tokenManager);
        apiService = retrofit.create(ApiService.class);
        TextInputLayout passwordInputLayout = findViewById(R.id.textInputLayoutPassword);
        TextInputEditText passwordEditText = findViewById(R.id.editTextPassword);
        Button confirmButton = findViewById(R.id.buttonConfirm);
        // 입력 시 에러 메시지 제거
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        confirmButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString();
            if (!password.isEmpty()) {
                checkPassword(new CheckPasswordRequest(password), passwordInputLayout);
            } else {
                passwordInputLayout.setError("비밀번호를 입력해주세요.");
            }
        });
    }

    private void checkPassword(CheckPasswordRequest request, TextInputLayout textInputLayout) {
        apiService.checkPassword(request).enqueue(new Callback<CheckPasswordResponse>() {
            @Override
            public void onResponse(@NotNull Call<CheckPasswordResponse> call, @NotNull Response<CheckPasswordResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    textInputLayout.setError(null);
                    Intent intent = new Intent(CheckPasswordActivity.this, MyInfoActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    textInputLayout.setError("비밀번호가 일치하지 않습니다.");
                }
            }

            @Override
            public void onFailure(@NotNull Call<CheckPasswordResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                textInputLayout.setError("네트워크 오류: 연결을 확인해주세요.");
            }
        });
    }
}