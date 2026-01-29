package com.mobility.hack.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.network.UpdateUserRequest;
import com.mobility.hack.network.UserInfoResponse;
import com.mobility.hack.security.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditMyInfoActivity extends AppCompatActivity {

    private EditText etId, etName, etEmail, etPhone, etPassword;
    private Button btnSave;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_my_info);

        tokenManager = new TokenManager(getApplicationContext());
        apiService = RetrofitClient.getClient(tokenManager).create(ApiService.class);

        etId = findViewById(R.id.et_id);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        btnSave = findViewById(R.id.btn_save);

        loadUserInfo();

        btnSave.setOnClickListener(v -> saveUserInfo());
    }

    private void loadUserInfo() {
        long userId = tokenManager.fetchUserId();
        apiService.getUserInfo(userId).enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserInfoResponse userInfo = response.body();
                    etId.setText(userInfo.getUsername()); // 아이디 필드에 username 설정
                    etName.setText(userInfo.getUsername());
                    etEmail.setText(userInfo.getEmail());
                    etPhone.setText(userInfo.getPhone());
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                Toast.makeText(EditMyInfoActivity.this, "사용자 정보 로딩 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInfo() {
        String username = etId.getText().toString();
        String name = etName.getText().toString();
        String email = etEmail.getText().toString();
        String phone = etPhone.getText().toString();
        String password = etPassword.getText().toString();

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        UpdateUserRequest request = new UpdateUserRequest(username, name, password, email, phone, 0);

        apiService.updateUserInfo(request).enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditMyInfoActivity.this, "정보가 성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditMyInfoActivity.this, "정보 수정에 실패했습니다. 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                Toast.makeText(EditMyInfoActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
