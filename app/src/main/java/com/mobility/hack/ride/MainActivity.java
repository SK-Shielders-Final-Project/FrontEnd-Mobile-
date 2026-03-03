package com.mobility.hack.ride;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mobility.hack.R;
import com.mobility.hack.auth.MenuActivity;
import com.mobility.hack.auth.MyInfoActivity;
import com.mobility.hack.chatbot.ChatActivity;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.BikeResponse;
import com.mobility.hack.util.VulnerableWebPProcessor;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    @Inject
    ApiService apiService;

    private ConstraintLayout rentalStatusLayout;
    private TextView tvRentalTimeValue;
    private Button btnExtendRental;
    private Button btnEndRental;
    private CountDownTimer rentalTimer;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private LinearLayout bikeInfoLayout;
    private TextView tvBikeIdInfo;
    private ImageView ivBikeImage;
    private ImageButton btnCloseBikeInfo;
    private String currentBikeId;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "QR 코드 스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    String qrContent = result.getContents();
                    Log.d("MainActivity", "Scanned QR Content: " + qrContent);

                    String finalBikeId = parseBikeNumberFromUrl(qrContent);

                    if (finalBikeId != null && !finalBikeId.isEmpty()) {
                        Toast.makeText(this, "인식된 자전거: " + finalBikeId, Toast.LENGTH_SHORT).show();

                        // [복구] 대여 화면으로 이동
                        Intent intent = new Intent(MainActivity.this, PurchaseTicketActivity.class);
                        intent.putExtra("BIKE_ID", finalBikeId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "유효하지 않은 QR 코드입니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private String parseBikeNumberFromUrl(String url) {
        if (url == null || !url.startsWith("https://zdme.kro.kr")) return null;
        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            return uri.getQueryParameter("b");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 요소 초기화
        bikeInfoLayout = findViewById(R.id.bikeInfoLayout);
        tvBikeIdInfo = findViewById(R.id.tv_bike_id_info);
        ivBikeImage = findViewById(R.id.iv_bike_image);
        btnCloseBikeInfo = findViewById(R.id.btnCloseBikeInfo);
        rentalStatusLayout = findViewById(R.id.rentalStatusLayout);
        tvRentalTimeValue = findViewById(R.id.tvRentalTimeValue);
        btnExtendRental = findViewById(R.id.btnExtendRental);
        btnEndRental = findViewById(R.id.btnEndRental);

        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        currentBikeId = prefs.getString("BIKE_ID", null);

        // 버튼 리스너 복구
        btnCloseBikeInfo.setOnClickListener(v -> {
            bikeInfoLayout.setVisibility(View.GONE);
            if (mMap != null) mMap.setPadding(0, 0, 0, 0);
        });

        btnExtendRental.setOnClickListener(v -> {
            Intent intent = new Intent(this, PurchaseTicketActivity.class);
            if (currentBikeId != null) intent.putExtra("BIKE_ID", currentBikeId);
            startActivity(intent);
        });

        btnEndRental.setOnClickListener(v -> startActivity(new Intent(this, ReturnBikeActivity.class)));

        findViewById(R.id.btnChatBot).setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.btnMenu).setOnClickListener(v -> startActivity(new Intent(this, MenuActivity.class)));
        findViewById(R.id.btnMyInfo).setOnClickListener(v -> startActivity(new Intent(this, MyInfoActivity.class)));
        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadBikeData());
        
        findViewById(R.id.btnQrScan).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setOrientationLocked(true);
            options.setBeepEnabled(false);
            qrScannerLauncher.launch(options);
        });

        findViewById(R.id.btnPurchaseTicket).setOnClickListener(v -> startActivity(new Intent(this, PointChargeActivity.class)));

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(marker -> {
            BikeResponse bike = (BikeResponse) marker.getTag();
            if (bike != null) {
                currentBikeId = bike.getSerialNumber();
                tvBikeIdInfo.setText("자전거 번호: " + currentBikeId);
                ivBikeImage.setImageResource(R.drawable.bg_edittext_rounded);
                bikeInfoLayout.setVisibility(View.VISIBLE);

                apiService.getBikeImage(currentBikeId).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                byte[] imageBytes = response.body().bytes();
                                // 취약한 디코더 호출 (트리거)
                                final Bitmap bitmap = VulnerableWebPProcessor.nativeDecodeWebP(imageBytes);

                                runOnUiThread(() -> {
                                    if (bitmap != null) {
                                        ivBikeImage.setImageBitmap(bitmap);
                                    } else {
                                        ivBikeImage.setImageResource(R.drawable.bg_edittext_rounded);
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });

                bikeInfoLayout.post(() -> {
                    int padding = (int) (getResources().getDisplayMetrics().heightPixels * 0.4);
                    mMap.setPadding(0, 0, 0, padding);
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 500, null);
                });
            }
            return true;
        });

        // [복구] 지도 빈 공간 클릭 시 정보 창 닫기
        mMap.setOnMapClickListener(latLng -> {
            bikeInfoLayout.setVisibility(View.GONE);
            if (mMap != null) mMap.setPadding(0, 0, 0, 0);
        });

        checkLocationPermissionAndMoveCamera();
        loadBikeData();
    }

    private void checkLocationPermissionAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationPermissionAndMoveCamera();
        }
    }

    private void loadBikeData() {
        apiService.getBikes().enqueue(new Callback<List<BikeResponse>>() {
            @Override
            public void onResponse(Call<List<BikeResponse>> call, Response<List<BikeResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mMap.clear();
                    for (BikeResponse bike : response.body()) {
                        LatLng pos = new LatLng(bike.getLatitude(), bike.getLongitude());
                        float color = (bike.getStatusCode() == 1) ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED;
                        Marker marker = mMap.addMarker(new MarkerOptions().position(pos).title(bike.getSerialNumber()).icon(BitmapDescriptorFactory.defaultMarker(color)));
                        marker.setTag(bike);
                    }
                }
            }
            @Override
            public void onFailure(Call<List<BikeResponse>> call, Throwable t) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUiBasedOnRentalState();
    }

    private void updateUiBasedOnRentalState() {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        boolean isRenting = prefs.getBoolean("isRenting", false);
        View btnQrScan = findViewById(R.id.btnQrScan);
        if (btnQrScan != null) btnQrScan.setVisibility(isRenting ? View.GONE : View.VISIBLE);
        rentalStatusLayout.setVisibility(isRenting ? View.VISIBLE : View.GONE);
        if (isRenting) startRentalTimer();
    }

    private void startRentalTimer() {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        long remaining = prefs.getLong("rental_duration", 0) - (System.currentTimeMillis() - prefs.getLong("rental_start_time", 0));
        if (remaining <= 0) return;
        if (rentalTimer != null) rentalTimer.cancel();
        rentalTimer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long m) {
                tvRentalTimeValue.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(m), TimeUnit.MILLISECONDS.toMinutes(m)%60, TimeUnit.MILLISECONDS.toSeconds(m)%60));
            }
            @Override
            public void onFinish() { tvRentalTimeValue.setText("00:00"); }
        }.start();
    }
}
