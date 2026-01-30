package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.ride.TicketActivity;
import com.mobility.hack.security.TokenManager;
import java.text.DecimalFormat;

public class MenuActivity extends AppCompatActivity {
    private static final String TAG = "MenuActivity";
    private TokenManager tokenManager;
    private TextView textUserPoint, textUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        tokenManager = new TokenManager(this);

        textUserPoint = findViewById(R.id.textUserPoint);
        textUsername = findViewById(R.id.textUsername);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        
        // [2] 명시적 패키지 경로를 사용하여 ActivityNotFoundException 해결
        findViewById(R.id.btnCustomerCenter).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.mobility.hack.community.CustomerCenterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnCoupon).setOnClickListener(v -> {
            startActivity(new Intent(this, TicketActivity.class));
        });

        textUsername.setOnClickListener(v -> {
            startActivity(new Intent(this, MyInfoActivity.class));
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());
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
