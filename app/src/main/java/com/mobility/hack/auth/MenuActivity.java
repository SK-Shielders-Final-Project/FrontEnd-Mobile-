package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.CustomerCenterActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.PaymentHistoryActivity;
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
            Intent intent = new Intent(MenuActivity.this, CustomerCenterActivity.class);
            startActivity(intent);
        });

        btnPaymentHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PaymentHistoryActivity.class);
            startActivity(intent);
        });

        fetchUserInfo();
    }

    private void fetchUserInfo() {
        if (tokenManager.fetchAuthToken() == null) {
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getUserInfo().enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserInfoResponse userInfo = response.body();
                    updateUserInterface(userInfo.getUsername(), userInfo.getTotalPoint());
                } else {
                    Toast.makeText(MenuActivity.this, "사용자 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
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
        finish();
    }
}
