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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.auth.MenuActivity;
import com.mobility.hack.auth.MyInfoActivity;
import com.mobility.hack.chatbot.ChatActivity;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.BikeResponse;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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

    private LinearLayout bikeInfoLayout;
    private TextView tvBikeIdInfo;
    private ImageButton btnCloseBikeInfo;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "QR 코드 스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    String qrContent = result.getContents();
                    Log.d("MainActivity", "Scanned QR Content: " + qrContent);

                    String bikeNumber = parseBikeNumberFromUrl(qrContent);

                    if (bikeNumber != null && !bikeNumber.isEmpty()) {
                        String finalBikeId = formatBikeId(bikeNumber);
                        Toast.makeText(this, "인식된 자전거: " + finalBikeId, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MainActivity.this, PurchaseTicketActivity.class);
                        intent.putExtra("BIKE_ID", finalBikeId);
                        startActivity(intent);

                    } else {
                        Toast.makeText(this, "유효하지 않은 GO-EQST QR 코드입니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private String parseBikeNumberFromUrl(String url) {
        if (url == null || !url.startsWith("https://zdme.kro.kr")) {
            return null;
        }
        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            return uri.getQueryParameter("b");
        } catch (Exception e) {
            Log.e("MainActivity", "QR 코드 URL 파싱 오류", e);
            return null;
        }
    }

    private String formatBikeId(String bikeNumber) {
        if (bikeNumber == null || bikeNumber.length() != 7) {
            return "UNKNOWN";
        }
        return "SN-" + bikeNumber.substring(0, 4) + "-" + bikeNumber.substring(4);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bikeInfoLayout = findViewById(R.id.bikeInfoLayout);
        tvBikeIdInfo = findViewById(R.id.tv_bike_id_info);
        btnCloseBikeInfo = findViewById(R.id.btnCloseBikeInfo);

        // 닫기 버튼 클릭 시, 패딩도 리셋
        btnCloseBikeInfo.setOnClickListener(v -> {
            bikeInfoLayout.setVisibility(View.GONE);
            if (mMap != null) {
                mMap.setPadding(0, 0, 0, 0);
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        ApiService apiService = ((MainApplication) getApplication()).getApiService();
        fileCacher = new FileCacher(apiService);

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
            options.setOrientationLocked(true);
            options.setBeepEnabled(false);
            qrScannerLauncher.launch(options);
        });

        LinearLayout btnPurchaseTicket = findViewById(R.id.btnPurchaseTicket);
        btnPurchaseTicket.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PointChargeActivity.class);
            startActivity(intent);
        });

        rentalStatusLayout = findViewById(R.id.rentalStatusLayout);
        tvRentalTimeValue = findViewById(R.id.tvRentalTimeValue);
        btnExtendRental = findViewById(R.id.btnExtendRental);
        btnEndRental = findViewById(R.id.btnEndRental);

        btnExtendRental.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PurchaseTicketActivity.class);
            startActivity(intent);
        });

        btnEndRental.setOnClickListener(v -> {
            Toast.makeText(this, "사용을 종료합니다.", Toast.LENGTH_SHORT).show();

            stopRentalTimer();

            if (tvRentalTimeValue != null) {
                tvRentalTimeValue.setText("00:00");
            }

            SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("isRenting");
            editor.remove("rental_duration");
            editor.remove("rental_start_time");
            editor.apply();

            Log.d("MainActivity", "모든 대여 정보가 초기화되었습니다.");
            updateUiBasedOnRentalState();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUiBasedOnRentalState();
    }

    private void updateUiBasedOnRentalState() {
        boolean isCurrentlyRenting = checkRentalStatus();
        ImageButton btnQrScan = findViewById(R.id.btnQrScan);

        if (isCurrentlyRenting) {
            btnQrScan.setVisibility(View.GONE);
            rentalStatusLayout.setVisibility(View.VISIBLE);
            startRentalTimer();
        } else {
            btnQrScan.setVisibility(View.VISIBLE);
            rentalStatusLayout.setVisibility(View.GONE);
            stopRentalTimer();
        }
    }

    private boolean checkRentalStatus() {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isRenting", false);
    }

    private void setRentalStatus(boolean isRenting) {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isRenting", isRenting);
        editor.apply();
    }

    private void startRentalTimer() {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        long rentalDuration = prefs.getLong("rental_duration", 0);
        long rentalStartTime = prefs.getLong("rental_start_time", 0);

        if (rentalDuration <= 0 || rentalStartTime <= 0) {
            return;
        }

        long elapsedTime = System.currentTimeMillis() - rentalStartTime;
        long remainingTime = rentalDuration - elapsedTime;

        if (remainingTime <= 0) {
            tvRentalTimeValue.setText("00:00");
            Toast.makeText(this, "대여 시간이 만료되었습니다.", Toast.LENGTH_LONG).show();
            setRentalStatus(false);
            updateUiBasedOnRentalState();
            return;
        }

        stopRentalTimer();

        rentalTimer = new CountDownTimer(remainingTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(hours);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished));

                String timeFormatted;
                if (hours > 0) {
                    timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                } else {
                    timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                }
                tvRentalTimeValue.setText(timeFormatted);
            }

            @Override
            public void onFinish() {
                tvRentalTimeValue.setText("00:00");
                Toast.makeText(MainActivity.this, "대여 시간이 만료되었습니다.", Toast.LENGTH_LONG).show();
                setRentalStatus(false);
                updateUiBasedOnRentalState();
            }
        }.start();
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

        mMap.setOnMarkerClickListener(marker -> {
            BikeResponse bike = (BikeResponse) marker.getTag();
            if (bike != null) {
                tvBikeIdInfo.setText("자전거 번호: " + bike.getSerialNumber());
                bikeInfoLayout.setVisibility(View.VISIBLE);

                // --- [수정] 패딩 값을 더 크게 주어 확실히 위로 이동 ---
                bikeInfoLayout.post(() -> {
                    int padding = (int) (getResources().getDisplayMetrics().heightPixels * 0.4);
                    mMap.setPadding(0, 0, 0, padding);
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 500, null);
                });
                // --- [끝] ---
            }
            return true;
        });

        mMap.setOnMapClickListener(latLng -> {
            bikeInfoLayout.setVisibility(View.GONE);
            // [수정] 지도 클릭 시, 패딩 리셋
            if (mMap != null) {
                mMap.setPadding(0, 0, 0, 0);
            }
        });

        checkLocationPermissionAndMoveCamera();
        loadBikeData();
    }

    private void checkLocationPermissionAndMoveCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
            } else {
                LatLng defaultLocation = new LatLng(37.5665, 126.9780);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
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
            public void onSuccess(List<BikeResponse> bikeList) {
                mMap.clear();

                for (BikeResponse bike : bikeList) {
                    LatLng position = new LatLng(bike.getLatitude(), bike.getLongitude());
                    boolean isAvailable = bike.getStatusCode() == 1;
                    float markerColor = isAvailable ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED;

                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(position)
                            .title(bike.getSerialNumber())
                            .snippet("상태: " + bike.getStatus())
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor));

                    Marker marker = mMap.addMarker(markerOptions);
                    marker.setTag(bike);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, "데이터 로드 실패: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
