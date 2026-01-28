package com.example.mobilityhack; // 패키지 이름 변경

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.mobility.hack.auth.CheckPasswordActivity;
import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.security.SecurityEngine;

import com.google.android.material.appbar.MaterialToolbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.example.mobilityhack.auth.MyInfoActivity; // import 경로 변경
import com.example.mobilityhack.network.ApiService; // import 경로 변경
import com.example.mobilityhack.network.RetrofitClient; // import 경로 변경
import com.example.mobilityhack.network.dto.RentalRequest;
import com.example.mobilityhack.network.dto.RentalResponse;
import com.example.mobilityhack.ride.PurchaseTicketActivity; // import 경로 변경
import com.example.mobilityhack.ride.QrScanActivity; // import 경로 변경
import com.example.mobilityhack.util.Constants; // import 경로 변경

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- [보안 강화] 토큰 인증 확인 로직 추가 ---
        // MainApplication.getTokenManager()가 초기화되었는지, 그리고 저장된 토큰이 있는지 확인
        if (MainApplication.getTokenManager() == null || MainApplication.getTokenManager().fetchAuthToken() == null) {
            Toast.makeText(this, "로그인이 필요합니다. 로그인 페이지로 이동합니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            // 이전 액티비티 스택을 모두 지우고, 새로운 태스크를 시작합니다.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // 현재 MainActivity를 완전히 종료
            return;   // 아래의 UI 생성 코드를 실행하지 않음
        }
        // --- 로직 끝 ---

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SecurityEngine engine = new SecurityEngine();
        engine.initAntiDebug();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_my_info) {
            Intent intent = new Intent(this, CheckPasswordActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            MainApplication.getTokenManager().clearData();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
        setContentView(R.layout.activity_main);

        apiService = RetrofitClient.getApiService(this);

        MaterialToolbar toolbar = findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);

        Button buyTicketButton = findViewById(R.id.buy_ticket_button);
        Button scanQrButton = findViewById(R.id.scan_qr_button);

        buyTicketButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PurchaseTicketActivity.class);
            startActivity(intent);
        });

        scanQrButton.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setOrientationLocked(false);
            options.setBeepEnabled(false);
            options.setCaptureActivity(QrScanActivity.class);
            qrScannerLauncher.launch(options);
        });

        autoTestLogin();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_my_info) {
            Intent intent = new Intent(this, MyInfoActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_menu) {
            Toast.makeText(this, "메뉴 기능은 아직 준비 중입니다.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(this, "QR 코드 스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    String scannedId = result.getContents();
                    requestRentToServer(scannedId);
                }
            });

    private void requestRentToServer(String bikeId) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        int userId = prefs.getInt(Constants.KEY_USER_ID, -1);

        if (userId == -1) {
            Toast.makeText(this, "대여를 위해 먼저 로그인을 해주세요.", Toast.LENGTH_LONG).show();
            return;
        }

        RentalRequest request = new RentalRequest(bikeId, userId);
        apiService.rentBike(request).enqueue(new Callback<RentalResponse>() {
            @Override
            public void onResponse(Call<RentalResponse> call, Response<RentalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MainActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "대여 요청 실패: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RentalResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "대여 요청 중 네트워크 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Network Failure: ", t);
            }
        });
    }

    private void autoTestLogin() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        if(prefs.getInt(Constants.KEY_USER_ID, -1) == -1) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(Constants.KEY_USER_ID, 1);
            editor.apply();
            Toast.makeText(this, "(테스트) 자동 로그인 되었습니다. (ID: 1)", Toast.LENGTH_SHORT).show();
        }
    }
}
