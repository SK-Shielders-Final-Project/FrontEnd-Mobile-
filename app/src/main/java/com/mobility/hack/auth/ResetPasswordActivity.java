package com.mobility.hack.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private ApiService apiService;
    private String resetToken; // 딥링크로 받은 토큰을 저장할 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        apiService = MainApplication.getRetrofit().create(ApiService.class);

        // --- 딥링크에서 토큰 파싱 ---
        Uri data = getIntent().getData();
        if (data != null) {
            resetToken = data.getQueryParameter("token");
            if (resetToken == null || resetToken.isEmpty()) {
                handleInvalidAccess();
                return;
            }
        } else {
            handleInvalidAccess();
            return;
        }

        TextInputLayout newPasswordLayout = findViewById(R.id.textInputLayoutNewPassword);
        TextInputLayout newPasswordConfirmLayout = findViewById(R.id.textInputLayoutNewPasswordConfirm);
        TextInputEditText newPasswordEditText = findViewById(R.id.editTextNewPassword);
        TextInputEditText newPasswordConfirmEditText = findViewById(R.id.editTextNewPasswordConfirm);
        Button resetPasswordButton = findViewById(R.id.buttonResetPassword);

        addTextWatcher(newPasswordEditText, newPasswordLayout);
        addTextWatcher(newPasswordConfirmEditText, newPasswordConfirmLayout);

        resetPasswordButton.setOnClickListener(v -> {
            String newPassword = newPasswordEditText.getText().toString();
            String newPasswordConfirm = newPasswordConfirmEditText.getText().toString();

            if (newPassword.isEmpty() || newPasswordConfirm.isEmpty()) {
                newPasswordLayout.setError("비밀번호를 입력해주세요.");
                return;
            }

            if (!newPassword.equals(newPasswordConfirm)) {
                newPasswordConfirmLayout.setError("비밀번호가 일치하지 않습니다.");
                return;
            }

            Map<String, String> resetPayload = new HashMap<>();
            resetPayload.put("token", resetToken);
            resetPayload.put("newPassword", newPassword);

            resetPassword(resetPayload);
        });
    }

    private void resetPassword(Map<String, String> payload) {
        apiService.resetPassword(payload).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NotNull Call<Void> call, @NotNull Response<Void> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, "비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();
                    // [수정] 성공 시, 로그인 화면으로 이동
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ResetPasswordActivity.this, "비밀번호 변경에 실패했습니다. 유효하지 않은 토큰이거나 다시 시도해주세요.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<Void> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ResetPasswordActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleInvalidAccess() {
        Toast.makeText(this, "유효하지 않은 접근입니다.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void addTextWatcher(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
