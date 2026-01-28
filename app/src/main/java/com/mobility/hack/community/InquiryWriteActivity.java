package com.mobility.hack.community;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.network.dto.InquiryResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryWriteActivity extends AppCompatActivity {
    private static final String TAG = "InquiryWriteActivity";
    private Uri selectedFileUri;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_write);

        apiService = RetrofitClient.getApiService(((MainApplication) getApplication()).getTokenManager());

        findViewById(R.id.iv_add_image).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            galleryLauncher.launch(intent);
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            uploadInquiry();
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

    private void uploadInquiry() {
        String token = ((MainApplication) getApplication()).getTokenManager().fetchAuthToken();
        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, RequestBody> partMap = new HashMap<>();
        partMap.put("title", RequestBody.create(MediaType.parse("text/plain"), "테스트 제목"));
        partMap.put("content", RequestBody.create(MediaType.parse("text/plain"), "테스트 내용"));

        MultipartBody.Part filePart = null;
        if (selectedFileUri != null) {
            try {
                File file = new File(getCacheDir(), "upload_file");
                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                FileOutputStream outputStream = new FileOutputStream(file);
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                inputStream.close();
                RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedFileUri)), file);
                filePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            } catch (Exception e) {
                Log.e(TAG, "File processing error: ", e);
                Toast.makeText(this, "파일 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        apiService.uploadInquiry("Bearer " + token, partMap, filePart).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(Call<InquiryResponse> call, Response<InquiryResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InquiryWriteActivity.this, "문의가 성공적으로 전송되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(InquiryWriteActivity.this, "전송 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<InquiryResponse> call, Throwable t) {
                Log.e(TAG, "Upload error: ", t);
                Toast.makeText(InquiryWriteActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
