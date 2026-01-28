package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.mobility.hack.auth.LoginActivity;
import com.mobility.hack.community.CustomerCenterActivity;
import com.mobility.hack.ride.QrScanActivity;
import com.mobility.hack.security.TokenManager;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MainActivity";
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 세션 체크 (보안 강화)
        TokenManager tokenManager = new TokenManager(this);
        if (tokenManager.fetchAuthToken() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // 2. 구글 지도 초기화
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 3. UI 컴포넌트 연결 및 리스너 등록
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // 메뉴 버튼 -> 고객센터 이동
        ImageButton btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, CustomerCenterActivity.class));
            });
        }

        // QR 스캔 버튼 -> QrScanActivity 이동
        ImageButton btnQrScan = findViewById(R.id.btnQrScan);
        if (btnQrScan != null) {
            btnQrScan.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, QrScanActivity.class));
            });
        }
    }

    /**
     * 구글 지도가 준비되면 호출되는 콜백
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // [에러 해결] 지도가 로드되면 서울 시청 중심으로 카메라 이동
        LatLng seoul = new LatLng(37.5665, 126.9780);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 15f));
        
        Log.d(TAG, "Map is ready. Centered at Seoul.");
    }
}
