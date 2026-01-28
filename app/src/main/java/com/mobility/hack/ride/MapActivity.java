package com.mobility.hack.ride;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.mobility.hack.R;
import com.mobility.hack.auth.MenuActivity;
import com.mobility.hack.chatbot.ChatActivity;

import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FileCacher fileCacher;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // [추가] 위치 서비스 클라이언트 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fileCacher = new FileCacher(this);

        // 챗봇 버튼 초기화 및 클릭 이벤트
        ImageButton btnChatBot = findViewById(R.id.btnChatBot);
        btnChatBot.setOnClickListener(v -> {
            // ChatActivity로 이동하는 의도(Intent) 생성
            Intent intent = new Intent(MapActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        // 메뉴 버튼 초기화 및 클릭 이벤트
        LinearLayout btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            // MenuActivity로 이동하는 Intent 생성
            Intent intent = new Intent(MapActivity.this, MenuActivity.class);
            startActivity(intent);
        });

        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> loadBikeData());
    }

    // QR 스캔 버튼 초기화 및 클릭 이벤트
/*    ImageButton btnQrScan = findViewById(R.id.btnQrScan);
        btnQrScan.setOnClickListener(v -> {
        // QrScanActivity로 이동하는 Intent 생성
        Intent intent = new Intent(MapActivity.this, QrScanActivity.class);
        startActivity(intent);
    });

    // 이용권 구매 버튼 클릭 이벤트
    LinearLayout btnPurchaseTicket = findViewById(R.id.btnPurchaseTicket);
    btnPurchaseTicket.setOnClickListener(v -> {
        // RentEndActivity로 이동하는 Intent 생성
        Intent intent = new Intent(MapActivity.this, RentEndActivity.class);
        startActivity(intent);
    });
    */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // [수정] 권한 확인 후 현재 위치로 이동
        checkLocationPermissionAndMoveCamera();

        loadBikeData();
    }

    private void checkLocationPermissionAndMoveCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 요청
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // 권한이 있을 경우: 지도에 내 위치 파란 점 표시 및 현재 위치 가져오기
        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                } else {
                    // 위치를 못 잡을 경우 기본값 (서울시청)
                    LatLng defaultLocation = new LatLng(37.5665, 126.9780);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14));
                }
            }
        });
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissionAndMoveCamera();
            } else {
                Toast.makeText(this, "위치 권한이 거부되었습니다. 기본 위치로 표시합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadBikeData() {
        Toast.makeText(this, "주변 자전거를 찾습니다...", Toast.LENGTH_SHORT).show();
        fileCacher.getBikeList(new FileCacher.BikeCallback() {
            @Override
            public void onSuccess(List<BikeDTO> bikeList) {
                mMap.clear();
                for (BikeDTO bike : bikeList) {
                    LatLng position = new LatLng(bike.getLatitude(), bike.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(position)
                            .title(bike.getModelName())
                            .snippet("상태: " + (bike.isAvailable() ? "이용가능" : "사용중"))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    mMap.addMarker(markerOptions);
                }
            }
            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MapActivity.this, "데이터 로드 실패: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}