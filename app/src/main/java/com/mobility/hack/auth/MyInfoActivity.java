package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.network.UserInfoResponse;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MyInfoActivity extends AppCompatActivity {
    private ApiService apiService;
    private TokenManager tokenManager;

    private TextView tvId, tvName, tvEmail, tvPhone, tvPoints, tvJoinDate, tvUpdateDate;
    private Button btnEditInfo, btnChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);
        tokenManager = new TokenManager(getApplicationContext());
        Retrofit retrofit = RetrofitClient.getClient(tokenManager);
        apiService = retrofit.create(ApiService.class);

        tvId = findViewById(R.id.tv_id);
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvPoints = findViewById(R.id.tv_points);
        tvJoinDate = findViewById(R.id.tv_join_date);
        tvUpdateDate = findViewById(R.id.tv_update_date);
        btnEditInfo = findViewById(R.id.btn_edit_info);
        btnChangePassword = findViewById(R.id.btn_change_password);

        fetchUserInfo();

        btnEditInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditMyInfoActivity.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // EditMyInfoActivity에서 정보가 수정되었을 수 있으므로, 화면에 다시 보여질 때 사용자 정보를 새로고침합니다.
        fetchUserInfo();
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
                    tvId.setText(String.valueOf(userInfo.getUserId()));
                    tvName.setText(userInfo.getUsername());
                    tvEmail.setText(userInfo.getEmail());
                    tvPhone.setText(userInfo.getPhone());
                    tvPoints.setText(String.valueOf(userInfo.getTotalPoint()) + " P");
                    tvJoinDate.setText(formatDate(userInfo.getCreatedAt()));
                    tvUpdateDate.setText(formatDate(userInfo.getUpdatedAt()));
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

    private String formatDate(String dateString) {
        if (dateString == null) {
            return "";
        }
        try {
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy. MM. dd.", Locale.getDefault());
            Date date = originalFormat.parse(dateString);
            return date != null ? targetFormat.format(date) : "";
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString; // 파싱 실패 시 원본 문자열 반환
        }
    }
}
