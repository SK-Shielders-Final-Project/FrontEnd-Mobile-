package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.PasswordRequest;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.network.UserInfoResponse;
import com.mobility.hack.ride.UserHistoryActivity;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MyInfoActivity extends AppCompatActivity {
    private ApiService apiService;
    private TokenManager tokenManager;
    private TextView usernameTextView;
    private TextView emailTextView;
    private TextView rideCountTextView;
    private Button changePasswordButton;
    private Button userHistoryButton;
    private EditText passwordEditText;
    private Button verifyPasswordButton;
    private LinearLayout verifiedContentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);
        tokenManager = new TokenManager(getApplicationContext());
        Retrofit retrofit = RetrofitClient.getClient(tokenManager);
        apiService = retrofit.create(ApiService.class);

        usernameTextView = findViewById(R.id.textViewUsername);
        emailTextView = findViewById(R.id.textViewEmail);
        rideCountTextView = findViewById(R.id.textViewRideCount);
        changePasswordButton = findViewById(R.id.buttonChangePassword);
        userHistoryButton = findViewById(R.id.buttonUserHistory);
        passwordEditText = findViewById(R.id.editTextPassword);
        verifyPasswordButton = findViewById(R.id.buttonVerifyPassword);
        verifiedContentLayout = findViewById(R.id.layoutVerifiedContent);

        fetchUserInfo();

        verifyPasswordButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(MyInfoActivity.this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyPassword(new PasswordRequest(password));
        });

        changePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        userHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void fetchUserInfo() {
        long userId = tokenManager.fetchUserId();
        if (userId == 0) {
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getUserInfo(userId).enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(@NotNull Call<UserInfoResponse> call, @NotNull Response<UserInfoResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserInfoResponse userInfo = response.body();
                    usernameTextView.setText(userInfo.getUsername());
                    emailTextView.setText(userInfo.getEmail());
                    rideCountTextView.setText(String.valueOf(userInfo.getRideCount()));
                } else {
                    Toast.makeText(MyInfoActivity.this, "사용자 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<UserInfoResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(MyInfoActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyPassword(PasswordRequest passwordRequest) {
        apiService.verifyPassword(passwordRequest).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    verifiedContentLayout.setVisibility(View.VISIBLE);
                    verifyPasswordButton.setVisibility(View.GONE);
                    passwordEditText.setVisibility(View.GONE);
                    Toast.makeText(MyInfoActivity.this, "비밀번호 확인 성공", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MyInfoActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MyInfoActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
