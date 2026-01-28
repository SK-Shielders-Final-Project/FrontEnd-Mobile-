package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.network.dto.RegisterResponse;
import com.mobility.hack.network.dto.UpdateInfoRequest;
import com.mobility.hack.network.dto.UpdateInfoResponse;
import com.mobility.hack.util.TokenManager;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyInfoActivity extends AppCompatActivity {

    private ApiService apiService;

    private TextInputLayout passwordInputLayout;
    private TextInputEditText usernameEditText, nameEditText, emailEditText, phoneEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        apiService = RetrofitClient.getApiService(((MainApplication) getApplication()).getTokenManager());

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
                Toast.makeText(this, "현재 비밀번호를 포함한 모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            UpdateInfoRequest request = new UpdateInfoRequest(username, name, password, email, phone);
            updateUserInfo(request);
        });

        goToChangePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });
    }

    private void fetchUserInfo() {
        String token = ((MainApplication) getApplication()).getTokenManager().fetchAuthToken();
        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService.getUserInfo("Bearer " + token).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NotNull Call<RegisterResponse> call, @NotNull Response<RegisterResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse userData = response.body();
                    usernameEditText.setText(userData.getUsername());
                    nameEditText.setText(userData.getName());
                    emailEditText.setText(userData.getEmail());
                    phoneEditText.setText(userData.getPhone());
                } else {
                    Toast.makeText(MyInfoActivity.this, "정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<RegisterResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(MyInfoActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserInfo(UpdateInfoRequest request) {
        String token = ((MainApplication) getApplication()).getTokenManager().fetchAuthToken();
        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        apiService.updateUserInfo("Bearer " + token, request).enqueue(new Callback<UpdateInfoResponse>() {
            @Override
            public void onResponse(@NotNull Call<UpdateInfoResponse> call, @NotNull Response<UpdateInfoResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MyInfoActivity.this, "정보가 성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    UpdateInfoResponse updatedData = response.body();
                    nameEditText.setText(updatedData.getName());
                    usernameEditText.setText(updatedData.getUsername());
                    passwordEditText.setText("");
                } else {
                    passwordInputLayout.setError("정보 수정에 실패했습니다. 비밀번호를 확인해주세요.");
                }
            }

            @Override
            public void onFailure(@NotNull Call<UpdateInfoResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(MyInfoActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
