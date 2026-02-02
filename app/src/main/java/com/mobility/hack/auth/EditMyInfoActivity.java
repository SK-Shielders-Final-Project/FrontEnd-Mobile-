package com.mobility.hack.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.UpdateUserRequest;
import com.mobility.hack.network.UserInfoResponse;
import com.mobility.hack.security.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditMyInfoActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword;
    private Button btnSave;
    private ApiService apiService;
    private TokenManager tokenManager;
    private UserInfoResponse currentUserInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_my_info);

        // MainApplication에서 ApiService 및 TokenManager 인스턴스 가져오기
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        btnSave = findViewById(R.id.btn_save);

        // 아이디 필드는 수정 불가능하도록 비활성화
        etName.setEnabled(false);

        loadUserInfo();

        btnSave.setOnClickListener(v -> saveUserInfo());
    }

    private void loadUserInfo() {
        long userId = tokenManager.fetchUserId();
        apiService.getUserInfo(userId).enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUserInfo = response.body();
                    etName.setText(currentUserInfo.getUsername());
                    etEmail.setText(currentUserInfo.getEmail());
                    etPhone.setText(currentUserInfo.getPhone());
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                Toast.makeText(EditMyInfoActivity.this, "사용자 정보 로딩 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInfo() {
        if (currentUserInfo == null) {
            Toast.makeText(this, "기존 사용자 정보를 불러오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 아이디는 수정하지 않으므로 기존 정보를 그대로 사용
        String username = currentUserInfo.getUsername();
        String email = etEmail.getText().toString();
        String phone = etPhone.getText().toString();
        String password = etPassword.getText().toString();

        if (password.isEmpty()) {
            Toast.makeText(this, "현재 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // username과 name에는 기존 아이디(수정 불가)를, 나머지 필드에는 새로 입력된 값을 전달
        UpdateUserRequest request = new UpdateUserRequest(username, username, password, email, phone, currentUserInfo.getAdminLev());

        apiService.updateUserInfo(request).enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditMyInfoActivity.this, "정보가 성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMessage = "정보 수정에 실패했습니다.";
                    if (response.code() == 401) {
                        errorMessage = "비밀번호가 일치하지 않습니다.";
                    }
                    Toast.makeText(EditMyInfoActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                Toast.makeText(EditMyInfoActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
