package com.mobility.hack.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.ResetPasswordRequest;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ResetPasswordActivity extends AppCompatActivity {
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        tokenManager = new TokenManager(getApplicationContext());
        Retrofit retrofit = RetrofitClient.getClient(tokenManager);
        apiService = retrofit.create(ApiService.class);
        TextInputLayout newPasswordLayout = findViewById(R.id.textInputLayoutNewPassword);
        TextInputLayout newPasswordConfirmLayout = findViewById(R.id.textInputLayoutNewPasswordConfirm);
        TextInputEditText newPasswordEditText = findViewById(R.id.editTextNewPassword);
        TextInputEditText newPasswordConfirmEditText = findViewById(R.id.editTextNewPasswordConfirm);
        Button resetButton = findViewById(R.id.buttonResetPassword);
        addTextWatcher(newPasswordEditText, newPasswordLayout);
        addTextWatcher(newPasswordConfirmEditText, newPasswordConfirmLayout);
        resetButton.setOnClickListener(v -> {
            String newPassword = newPasswordEditText.getText().toString();
            String newPasswordConfirm = newPasswordConfirmEditText.getText().toString();
            String token = getIntent().getData().getQueryParameter("token");
            boolean hasError = false;
            if (newPassword.isEmpty()) {
                newPasswordLayout.setError("새로운 비밀번호를 입력해주세요.");
                hasError = true;
            }
            if (newPasswordConfirm.isEmpty()) {
                newPasswordConfirmLayout.setError("새로운 비밀번호 확인을 입력해주세요.");
                hasError = true;
            }
            if (hasError) return;
            if (!newPassword.equals(newPasswordConfirm)) {
                newPasswordLayout.setError("비밀번호가 일치하지 않습니다.");
                newPasswordConfirmLayout.setError("비밀번호가 일치하지 않습니다.");
                return;
            }
            ResetPasswordRequest request = new ResetPasswordRequest(token, newPassword);
            resetPassword(request);
        });
    }

    private void resetPassword(ResetPasswordRequest request) {
        apiService.resetPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NotNull Call<Void> call, @NotNull Response<Void> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, "비밀번호가 성공적으로 재설정되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ResetPasswordActivity.this, "비밀번호 재설정에 실패했습니다. 링크가 만료되었거나 잘못된 요청입니다.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<Void> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ResetPasswordActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTextWatcher(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}