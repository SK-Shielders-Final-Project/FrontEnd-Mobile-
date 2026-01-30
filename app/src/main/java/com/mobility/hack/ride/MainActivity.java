package com.mobility.hack.ride;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.mobility.hack.auth.MyInfoActivity;
import com.mobility.hack.chatbot.ChatActivity;
import com.mobility.hack.network.BikeResponse; // [수정] BikeResponse 임포트 추가

import java.util.List;
import java.util.Locale; // [추가]
import java.util.concurrent.TimeUnit; // [추가]--

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ConstraintLayout rentalStatusLayout;
    private TextView tvRentalTimeValue;
    private Button btnExtendRental;
    private Button btnEndRental;

    private CountDownTimer rentalTimer;

    private GoogleMap mMap;
    private FileCacher fileCacher;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "QR 코드 스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    String scannedId = result.getContents();
                    Toast.makeText(this, "스캔된 ID: " + scannedId, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, PurchaseTicketActivity.class);
                    startActivity(intent);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fileCacher = new FileCacher(this);

        ImageButton btnChatBot = findViewById(R.id.btnChatBot);
        btnChatBot.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        LinearLayout btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MenuActivity.class);
            startActivity(intent);
        });

        LinearLayout btnMyInfo = findViewById(R.id.btnMyInfo);
        btnMyInfo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MyInfoActivity.class);
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
            Intent intent = new Intent(MainActivity.this, PointChargeActivity.class);
            startActivity(intent);
        });

        // ▼▼▼ [수정 1] 빠져있던 UI 요소들 초기화 코드 추가 ▼▼▼
        rentalStatusLayout = findViewById(R.id.rentalStatusLayout);
        tvRentalTimeValue = findViewById(R.id.tvRentalTimeValue);
        btnExtendRental = findViewById(R.id.btnExtendRental);
        btnEndRental = findViewById(R.id.btnEndRental);

        // '시간 연장' 버튼 클릭 시
        btnExtendRental.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PurchaseTicketActivity.class);
            startActivity(intent);
        });

        // '사용 종료' 버튼 클릭 시
        btnEndRental.setOnClickListener(v -> {
            Toast.makeText(this, "사용을 종료합니다.", Toast.LENGTH_SHORT).show();

            // 1. 실행 중인 타이머를 즉시 중지합니다.
            stopRentalTimer();

            // 2. 화면의 남은 시간 텍스트를 "00:00"으로 즉시 변경합니다.
            if (tvRentalTimeValue != null) {
                tvRentalTimeValue.setText("00:00");
            }

            // 3. SharedPreferences에서 모든 대여 관련 정보를 삭제하여 완전히 초기화합니다.
            SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("isRenting");
            editor.remove("rental_duration");
            editor.remove("rental_start_time");
            editor.apply(); // 변경사항 저장

            Log.d("MainActivity", "모든 대여 정보가 초기화되었습니다.");
            // 4. UI를 대여 전 상태(QR 버튼이 보이는 상태)로 되돌립니다.
            updateUiBasedOnRentalState();
        });
    }

    // ▼▼▼ [수정 2] 빠져있던 UI 상태 변경 로직 전체 추가 ▼▼▼

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 대여 상태를 확인하고 UI를 업데이트
        updateUiBasedOnRentalState();
    }

    private void updateUiBasedOnRentalState() {
        boolean isCurrentlyRenting = checkRentalStatus();
        ImageButton btnQrScan = findViewById(R.id.btnQrScan);

        if (isCurrentlyRenting) {
            // 대여 중: QR 버튼 숨기고, 대여 정보창 보이기
            btnQrScan.setVisibility(View.GONE);
            rentalStatusLayout.setVisibility(View.VISIBLE);

            startRentalTimer();

        } else {
            // 대여 중이 아닐 경우
            btnQrScan.setVisibility(View.VISIBLE);
            rentalStatusLayout.setVisibility(View.GONE);


            // [수정] 실행 중인 타이머가 있다면 중지
            stopRentalTimer();
        }
    }

    // SharedPreferences를 사용해 현재 대여 상태를 확인하는 함수
    private boolean checkRentalStatus() {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isRenting", false);
    }

    // SharedPreferences에 현재 대여 상태를 저장하는 함수
    private void setRentalStatus(boolean isRenting) {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isRenting", isRenting);
        editor.apply();
    }

    private void startRentalTimer() {
        // 1. 저장된 대여 정보 가져오기
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        long rentalDuration = prefs.getLong("rental_duration", 0);
        long rentalStartTime = prefs.getLong("rental_start_time", 0);

        if (rentalDuration <= 0 || rentalStartTime <= 0) {
            // 저장된 정보가 없으면 타이머를 시작하지 않음
            return;
        }

        // 2. 현재 시간을 기준으로 남은 시간 계산
        long elapsedTime = System.currentTimeMillis() - rentalStartTime; // 경과 시간
        long remainingTime = rentalDuration - elapsedTime; // 순수하게 남은 시간

        if (remainingTime <= 0) {
            // 앱이 꺼져있는 동안 대여 시간이 만료된 경우
            tvRentalTimeValue.setText("00:00");
            Toast.makeText(this, "대여 시간이 만료되었습니다.", Toast.LENGTH_LONG).show();
            setRentalStatus(false); // 대여 상태 해제
            updateUiBasedOnRentalState(); // UI 갱신
            return;
        }

        // 3. 기존 타이머가 있다면 취소하고 새로 시작 (onResume이 여러 번 불릴 경우를 대비)
        stopRentalTimer();

        // 4. CountDownTimer 생성 및 시작
        rentalTimer = new CountDownTimer(remainingTime, 1000) { // 1000ms = 1초 간격

            // 1초마다 호출되는 메소드
            @Override
            public void onTick(long millisUntilFinished) {
                // 남은 시간을 시:분:초 또는 분:초 형태로 변환
                long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(hours);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished));

                String timeFormatted;
                if (hours > 0) {
                    // 1시간 이상 남았을 때 (예: 01:29:59)
                    timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                } else {
                    // 1시간 미만 남았을 때 (예: 29:59)
                    timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                }
                // 변환된 시간을 TextView에 업데이트
                tvRentalTimeValue.setText(timeFormatted);
            }

            // 타이머가 완전히 종료되었을 때 호출되는 메소드
            @Override
            public void onFinish() {
                tvRentalTimeValue.setText("00:00");
                Toast.makeText(MainActivity.this, "대여 시간이 만료되었습니다.", Toast.LENGTH_LONG).show();
                setRentalStatus(false); // 대여 상태 해제
                updateUiBasedOnRentalState(); // UI를 원래대로 되돌림
            }
        }.start(); // 타이머 시작
    }

    private void stopRentalTimer() {
        if (rentalTimer != null) {
            rentalTimer.cancel();
            rentalTimer = null;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRentalTimer();
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

    /**
     * [수정] BikeDTO 대신 BikeResponse를 사용하여 지도에 마커를 표시
     */
    private void loadBikeData() {
        Toast.makeText(this, "주변 자전거를 찾습니다...", Toast.LENGTH_SHORT).show();

        fileCacher.getBikeList(new FileCacher.BikeCallback() {
            @Override
            public void onSuccess(List<BikeResponse> bikeList) {
                mMap.clear();

                for (BikeResponse bike : bikeList) {
                    LatLng position = new LatLng(
                            bike.getLatitude(),
                            bike.getLongitude()
                    );

                    // status_code 기준: 1 = 이용 가능, 0 = 사용 중
                    boolean isAvailable = bike.getStatusCode() == 1;

                    float markerColor = isAvailable
                            ? BitmapDescriptorFactory.HUE_GREEN
                            : BitmapDescriptorFactory.HUE_RED;

                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(position)
                            .title(bike.getSerialNumber())        // serial_number 표시
                            .snippet("상태: " + bike.getStatus()) // 상태
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor));

                    mMap.addMarker(markerOptions);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(
                        MainActivity.this,
                        "데이터 로드 실패: " + errorMessage,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

}