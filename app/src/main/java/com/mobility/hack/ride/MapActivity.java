// 삭제
/*
package com.mobility.hack.ride;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
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
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mobility.hack.R;
import com.mobility.hack.auth.MenuActivity;
import com.mobility.hack.chatbot.ChatActivity;

import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FileCacher fileCacher;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(this, "QR 코드 스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    String scannedId = result.getContents();
                    // TODO: 서버로 대여 요청 보내는 로직 추가
                    Toast.makeText(this, "스캔된 ID: " + scannedId, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fileCacher = new FileCacher(this);

        ImageButton btnChatBot = findViewById(R.id.btnChatBot);
        btnChatBot.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        LinearLayout btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, MenuActivity.class);
            startActivity(intent);
        });

        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> loadBikeData());

        ImageButton btnQrScan = findViewById(R.id.btnQrScan);
        btnQrScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setOrientationLocked(false);
            options.setBeepEnabled(false);
            qrScannerLauncher.launch(options);
        });

        LinearLayout btnPurchaseTicket = findViewById(R.id.btnPurchaseTicket);
        btnPurchaseTicket.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, PurchaseTicketActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        checkLocationPermissionAndMoveCamera();

        loadBikeData();
    }

    private void checkLocationPermissionAndMoveCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                } else {
                    LatLng defaultLocation = new LatLng(37.5665, 126.9780);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14));
                }
            }
        });
    }

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
*/
