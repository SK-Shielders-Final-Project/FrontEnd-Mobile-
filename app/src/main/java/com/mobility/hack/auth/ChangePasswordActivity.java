package com.mobility.hack.auth;

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
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.network.dto.ChangePasswordRequest;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        apiService = RetrofitClient.getApiService(((MainApplication) getApplication()).getTokenManager());

        TextInputLayout currentPasswordLayout = findViewById(R.id.textInputLayoutCurrentPassword);
        TextInputLayout newPasswordLayout = findViewById(R.id.textInputLayoutNewPassword);
        TextInputLayout newPasswordConfirmLayout = findViewById(R.id.textInputLayoutNewPasswordConfirm);

        TextInputEditText currentPasswordEditText = findViewById(R.id.editTextCurrentPassword);
        TextInputEditText newPasswordEditText = findViewById(R.id.editTextNewPassword);
        TextInputEditText newPasswordConfirmEditText = findViewById(R.id.editTextNewPasswordConfirm);

        Button changePasswordButton = findViewById(R.id.buttonChangePassword);

        addTextWatcher(currentPasswordEditText, currentPasswordLayout);
        addTextWatcher(newPasswordEditText, newPasswordLayout);
        addTextWatcher(newPasswordConfirmEditText, newPasswordConfirmLayout);

        changePasswordButton.setOnClickListener(v -> {
            String currentPassword = currentPasswordEditText.getText().toString();
            String newPassword = newPasswordEditText.getText().toString();
            String newPasswordConfirm = newPasswordConfirmEditText.getText().toString();

            boolean hasError = false;
            if (currentPassword.isEmpty()) {
                currentPasswordLayout.setError("현재 비밀번호를 입력해주세요.");
                hasError = true;
            }
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
                newPasswordLayout.setError("새로운 비밀번호가 일치하지 않습니다.");
                newPasswordConfirmLayout.setError("새로운 비밀번호가 일치하지 않습니다.");
                return;
            }

            ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);
            changePassword(request, currentPasswordLayout);
        });
    }

    private void changePassword(ChangePasswordRequest request, TextInputLayout currentPasswordLayout) {
        String token = ((MainApplication) getApplication()).getTokenManager().fetchAuthToken();
        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.changePassword("Bearer " + token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NotNull Call<Void> call, @NotNull Response<Void> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    currentPasswordLayout.setError("현재 비밀번호가 일치하지 않습니다.");
                }
            }

            @Override
            public void onFailure(@NotNull Call<Void> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ChangePasswordActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
