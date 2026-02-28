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

import com.bumptech.glide.Glide;
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
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.auth.MenuActivity;
import com.mobility.hack.auth.MyInfoActivity;
import com.mobility.hack.chatbot.ChatActivity;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.BikeResponse;
import com.mobility.hack.util.NativeImageProcessor;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private ImageView ivBikeImage;
    private ImageButton btnCloseBikeInfo;
    private String currentBikeId;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "QR 코드 스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    String qrContent = result.getContents();
                    String finalBikeId = parseBikeNumberFromUrl(qrContent);
                    if (finalBikeId != null && !finalBikeId.isEmpty()) {
                        Intent intent = new Intent(MainActivity.this, PurchaseTicketActivity.class);
                        intent.putExtra("BIKE_ID", finalBikeId);
                        startActivity(intent);
                    }
                }
            });

    private String parseBikeNumberFromUrl(String url) {
        if (url == null || !url.startsWith("https://zdme.kro.kr")) return null;
        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            return uri.getQueryParameter("b");
        } catch (Exception e) { return null; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bikeInfoLayout = findViewById(R.id.bikeInfoLayout);
        tvBikeIdInfo = findViewById(R.id.tv_bike_id_info);
        ivBikeImage = findViewById(R.id.iv_bike_image);
        btnCloseBikeInfo = findViewById(R.id.btnCloseBikeInfo);
        rentalStatusLayout = findViewById(R.id.rentalStatusLayout);
        tvRentalTimeValue = findViewById(R.id.tvRentalTimeValue);
        btnExtendRental = findViewById(R.id.btnExtendRental);
        btnEndRental = findViewById(R.id.btnEndRental);

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        ApiService apiService = ((MainApplication) getApplication()).getApiService();
        fileCacher = new FileCacher(apiService);

        findViewById(R.id.btnChatBot).setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.btnMenu).setOnClickListener(v -> startActivity(new Intent(this, MenuActivity.class)));
        findViewById(R.id.btnMyInfo).setOnClickListener(v -> startActivity(new Intent(this, MyInfoActivity.class)));
        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadBikeData());
        findViewById(R.id.btnQrScan).setOnClickListener(v -> qrScannerLauncher.launch(new ScanOptions()));
        findViewById(R.id.btnPurchaseTicket).setOnClickListener(v -> startActivity(new Intent(this, PointChargeActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUiBasedOnRentalState();
    }

    private void updateUiBasedOnRentalState() {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        boolean isRenting = prefs.getBoolean("isRenting", false);
        findViewById(R.id.btnQrScan).setVisibility(isRenting ? View.GONE : View.VISIBLE);
        rentalStatusLayout.setVisibility(isRenting ? View.VISIBLE : View.GONE);
        if (isRenting) startRentalTimer(); else stopRentalTimer();
    }

    private void startRentalTimer() {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        long duration = prefs.getLong("rental_duration", 0);
        long startTime = prefs.getLong("rental_start_time", 0);
        long remaining = duration - (System.currentTimeMillis() - startTime);

        if (remaining <= 0) {
            tvRentalTimeValue.setText("00:00");
            return;
        }

        stopRentalTimer();
        rentalTimer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long millis) {
                long m = TimeUnit.MILLISECONDS.toMinutes(millis);
                long s = (TimeUnit.MILLISECONDS.toSeconds(millis)) % 60;
                tvRentalTimeValue.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
            }
            @Override
            public void onFinish() { updateUiBasedOnRentalState(); }
        }.start();
    }

    private void stopRentalTimer() {
        if (rentalTimer != null) { rentalTimer.cancel(); rentalTimer = null; }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(marker -> {
            BikeResponse bike = (BikeResponse) marker.getTag();
            if (bike != null) {
                currentBikeId = bike.getSerialNumber();
                tvBikeIdInfo.setText("자전거 번호: " + currentBikeId);
                bikeInfoLayout.setVisibility(View.VISIBLE);
                
                ApiService apiService = ((MainApplication) getApplication()).getApiService();
                apiService.getBikeImage(currentBikeId).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                byte[] imageBytes = response.body().bytes();
                                
                                // UI 스레드에서 먼저 이미지를 화면에 렌더링합니다.
                                runOnUiThread(() -> {
                                    Glide.with(MainActivity.this).load(imageBytes).into(ivBikeImage);
                                    
                                    // 이미지가 보인 직후(0.5초 뒤)에 네이티브 취약점을 터뜨립니다.
                                    ivBikeImage.postDelayed(() -> {
                                        NativeImageProcessor.processImage(imageBytes);
                                    }, 500);
                                });

                            } catch (IOException e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
            }
            return true;
        });
        checkLocationPermissionAndMoveCamera();
        loadBikeData();
    }

    private void loadBikeData() {
        fileCacher.getBikeList(new FileCacher.BikeCallback() {
            @Override
            public void onSuccess(List<BikeResponse> bikeList) {
                mMap.clear();
                for (BikeResponse bike : bikeList) {
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(bike.getLatitude(), bike.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(bike.getStatusCode() == 1 ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED)));
                    marker.setTag(bike);
                }
            }
            @Override public void onFailure(String errorMessage) {}
        });
    }

    private void checkLocationPermissionAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onDestroy() { super.onDestroy(); stopRentalTimer(); }
}
