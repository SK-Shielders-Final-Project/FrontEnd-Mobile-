package com.mobility.hack.ride;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.FileUploadResponse;
import com.mobility.hack.network.ReturnRequest;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReturnBikeActivity extends AppCompatActivity {

    private static final String TAG = "ReturnBikeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ImageView ivReturnBikeImage;
    private Button btnOpenCamera;
    private Button btnOpenGallery;
    private Button btnReturnBike;

    private ApiService apiService;
    private FusedLocationProviderClient fusedLocationClient;
    private String bikeId;
    private Uri selectedImageUri;
    private boolean isReturning = false;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivReturnBikeImage.setImageURI(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_bike);

        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
        bikeId = prefs.getString("BIKE_ID", null);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ivReturnBikeImage = findViewById(R.id.ivReturnBikeImage);
        btnOpenCamera = findViewById(R.id.btnOpenCamera);
        btnOpenGallery = findViewById(R.id.btnOpenGallery);
        btnReturnBike = findViewById(R.id.btnReturnBike);

        apiService = ((MainApplication) getApplication()).getApiService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        toolbar.setNavigationOnClickListener(v -> finish());

        btnOpenGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        btnOpenCamera.setOnClickListener(v -> {
            Toast.makeText(this, "사진 촬영 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show();
        });

        btnReturnBike.setOnClickListener(v -> {
            if (isReturning) return; // 중복 반납 방지

            if (selectedImageUri == null) {
                Toast.makeText(this, "사진을 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (bikeId == null || bikeId.isEmpty()) {
                Toast.makeText(this, "자전거 정보가 없습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                return;
            }
            handleReturn();
        });
    }

    private void handleReturn() {
        isReturning = true;
        btnReturnBike.setEnabled(false);
        btnReturnBike.setAlpha(0.5f);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            uploadFileThenReturnBike();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                uploadFileThenReturnBike();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                resetReturnUI();
            }
        }
    }

    private void uploadFileThenReturnBike() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
             resetReturnUI();
             return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                MultipartBody.Part filePart = prepareFilePart("file", selectedImageUri);
                if (filePart == null) {
                    handleFailure("파일 준비에 실패했습니다.");
                    return;
                }

                apiService.uploadFile(filePart).enqueue(new Callback<FileUploadResponse>() {
                    @Override
                    public void onResponse(@NotNull Call<FileUploadResponse> call, @NotNull Response<FileUploadResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getFile_id() != null) {
                            finalReturnBike(location.getLatitude(), location.getLongitude(), response.body().getFile_id());
                        } else {
                            handleFailure("파일 업로드 실패 (Code: " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<FileUploadResponse> call, @NotNull Throwable t) {
                        handleFailure("파일 업로드 중 오류가 발생했습니다: " + t.getMessage());
                    }
                });

            } else {
                handleFailure("위치 정보를 가져오는 데 실패했습니다.");
            }
        }).addOnFailureListener(e -> handleFailure("위치 정보 요청 실패: " + e.getMessage()));
    }

    private void finalReturnBike(double latitude, double longitude, Long fileId) {
        ReturnRequest returnRequest = new ReturnRequest(bikeId, latitude, longitude, fileId);
        Log.d(TAG, "Return Request Data: " + returnRequest.toString()); // 로그 추가

        apiService.returnBike(returnRequest).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NotNull Call<Void> call, @NotNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ReturnBikeActivity.this, "자전거가 성공적으로 반납되었습니다.", Toast.LENGTH_SHORT).show();

                    // 대여 정보 삭제
                    SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();

                    finish();
                } else {
                    handleFailure("자전거 반납에 실패했습니다. (Code: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NotNull Call<Void> call, @NotNull Throwable t) {
                handleFailure("반납 요청 중 오류가 발생했습니다: " + t.getMessage());
            }
        });
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        try {
            String mimeType = getContentResolver().getType(fileUri);
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

            if (extension == null) extension = "jpg";
            else extension = extension.toLowerCase(Locale.ROOT);

            String fileName = "upload_" + System.currentTimeMillis() + "." + extension;
            File file = new File(getCacheDir(), fileName);

            try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                 OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) outputStream.write(buf, 0, len);
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
            return MultipartBody.Part.createFormData(partName, fileName, requestFile);
        } catch (Exception e) {
            Log.e(TAG, "파일 준비 실패: " + e.getMessage());
            return null;
        }
    }

    private void handleFailure(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        resetReturnUI();
    }

    private void resetReturnUI() {
        isReturning = false;
        btnReturnBike.setEnabled(true);
        btnReturnBike.setAlpha(1.0f);
    }
}
