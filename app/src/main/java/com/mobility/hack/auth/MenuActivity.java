package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.annotations.SerializedName;
import com.mobility.hack.community.CustomerCenterActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.PaymentHistoryActivity;
import com.mobility.hack.PointGiftActivity;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.UserInfoResponse;
import com.mobility.hack.ride.TicketActivity;
import com.mobility.hack.security.TokenManager;

import java.text.DecimalFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuActivity extends AppCompatActivity {

    private ApiService apiService;
    private TokenManager tokenManager;

    private TextView textUserPoint;
    private TextView textUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        textUserPoint = findViewById(R.id.textUserPoint);
        textUsername = findViewById(R.id.textUsername);

        ImageButton btnClose = findViewById(R.id.btnClose);
        TextView btnLogout = findViewById(R.id.btnLogout);
        TextView btnCoupon = findViewById(R.id.btnCoupon);
        TextView btnCustomerCenter = findViewById(R.id.btnCustomerCenter);
        TextView btnPaymentHistory = findViewById(R.id.btnPaymentHistory);
        TextView btnPointGift = findViewById(R.id.btnPointGift);

        btnClose.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> performLogout());

        textUsername.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MyInfoActivity.class);
            startActivity(intent);
        });

        btnCoupon.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, TicketActivity.class);
            startActivity(intent);
        });

        btnCustomerCenter.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, com.mobility.hack.community.CustomerCenterActivity.class);
            startActivity(intent);
        });

        btnPaymentHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PaymentHistoryActivity.class);
            startActivity(intent);
        });

        btnPointGift.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PointGiftActivity.class);
            startActivity(intent);
        });

        fetchUserInfo();
    }

    private void fetchUserInfo() {
        long userId = tokenManager.fetchUserId();
        if (userId == 0L) { // Assuming 0 is not a valid user ID
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getUserInfo().enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserInfoResponse userInfo = response.body();
                    updateUserInterface(userInfo.getName(), userInfo.getTotalPoint());
                } else {
                    // 서버가 보낸 응용답 코드와 메시지를 로그로 자세히 출력
                    String errorBody = "내용 없음";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                    // Logcat에서 "MenuActivity"로 검색하여 이 로그를 확인하세요.
                    android.util.Log.e("MenuActivity", "서버 응답 실패: " + response.code() + ", 메시지: " + response.message() + ", 에러 내용: " + errorBody);

                    // 사용자에게 보여줄 토스트 메시지
                    Toast.makeText(MenuActivity.this, "정보 로딩 실패 (코드: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                Toast.makeText(MenuActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserInterface(String name, int point) {
        if (name != null) {
            textUsername.setText(name + " >");
        }
        DecimalFormat formatter = new DecimalFormat("###,###");
        String formattedPoint = formatter.format(point);
        textUserPoint.setText("현재 이용권 잔액: " + formattedPoint + "원");
    }

    private void performLogout() {
        tokenManager.clearData();
        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // 현재 액티비티를 종료하여 뒤로가기 시 다시 돌아오지 않도록 함
    }
}
