package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.RegisterResponse;
import com.mobility.hack.network.UpdateInfoRequest;
import com.mobility.hack.network.UpdateInfoResponse;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * BaseActivity를 상속받아 컴파일 에러 해결 및 코드 간소화
 */
public class MyInfoActivity extends BaseActivity {

    private TextInputLayout passwordInputLayout;
    private TextInputEditText usernameEditText, nameEditText, emailEditText, phoneEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        // [에러 해결] BaseActivity에서 상속받은 apiService 사용 (별도 초기화 불필요)

        passwordInputLayout = findViewById(R.id.textInputLayoutPassword);
        usernameEditText = findViewById(R.id.editTextUsername);
        nameEditText = findViewById(R.id.editTextName);
        emailEditText = findViewById(R.id.editTextEmail);
        phoneEditText = findViewById(R.id.editTextPhone);
        passwordEditText = findViewById(R.id.editTextPassword);

        Button updateButton = findViewById(R.id.buttonUpdate);
        Button goToChangePasswordButton = findViewById(R.id.buttonGoToChangePassword);

        fetchUserInfo();

        updateButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String name = nameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String phone = phoneEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (username.isEmpty() || name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            UpdateInfoRequest request = new UpdateInfoRequest(username, name, password, email, phone);
            updateUserInfo(request);
        });

        goToChangePasswordButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        });
    }

    private void fetchUserInfo() {
        long userId = MainApplication.getTokenManager().fetchUserId();
        if (userId == 0) {
            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish(); 
            return;
        }

        if (apiService == null) return;
        apiService.getUserInfo(userId).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NotNull Call<RegisterResponse> call, @NotNull Response<RegisterResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse userData = response.body();
                    usernameEditText.setText(userData.getUsername());
                    nameEditText.setText(userData.getName());
                    emailEditText.setText(userData.getEmail());
                    phoneEditText.setText(userData.getPhone());
                }
            }
            @Override
            public void onFailure(@NotNull Call<RegisterResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(MyInfoActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserInfo(UpdateInfoRequest request) {
        if (apiService == null) return;
        apiService.updateUserInfo(request).enqueue(new Callback<UpdateInfoResponse>() {
            @Override
            public void onResponse(@NotNull Call<UpdateInfoResponse> call, @NotNull Response<UpdateInfoResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(MyInfoActivity.this, "정보가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    passwordEditText.setText("");
                } else {
                    passwordInputLayout.setError("비밀번호가 일치하지 않습니다.");
                }
            }
            @Override
            public void onFailure(@NotNull Call<UpdateInfoResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(MyInfoActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
