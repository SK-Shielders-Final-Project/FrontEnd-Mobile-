package com.mobility.hack.community;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.FileUploadResponse;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.network.InquiryWriteRequest;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryWriteActivity extends AppCompatActivity {
    private static final String TAG = "InquiryWrite";
    private ApiService apiService;
    private TokenManager tokenManager;
    private EditText etTitle, etContent;
    private CheckBox cbAgree;
    private TextView btnSubmit;
    private ImageView ivAddImage;
    private Uri selectedFileUri;
    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_write);

        MainApplication app = (MainApplication) getApplication();
        apiService = app.getApiService();
        tokenManager = app.getTokenManager();

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        cbAgree = findViewById(R.id.cb_agree);
        ivAddImage = findViewById(R.id.iv_add_image);
        btnSubmit = findViewById(R.id.btn_submit);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ivAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (!isSubmitting) startSubmitProcess();
            });
        }
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    ivAddImage.setImageURI(selectedFileUri);
                }
            }
    );

    private void startSubmitProcess() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cbAgree != null && !cbAgree.isChecked()) {
            Toast.makeText(this, "약관에 동의해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        isSubmitting = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setAlpha(0.5f);

        if (selectedFileUri != null) {
            uploadFileThenSubmit(title, content);
        } else {
            finalWriteInquiry(title, content, null);
        }
    }

    // [수정] 토큰 인자 제거
    private void uploadFileThenSubmit(String title, String content) {
        MultipartBody.Part filePart = prepareFilePart("file", selectedFileUri);

        if (filePart == null) {
            handleFailure("파일 준비 실패");
            return;
        }

        // [핵심] 토큰 없이 파일 파트만 전달 (Interceptor가 자동 처리)
        apiService.uploadFile(filePart).enqueue(new Callback<FileUploadResponse>() {
            @Override
            public void onResponse(@NotNull Call<FileUploadResponse> call, @NotNull Response<FileUploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    finalWriteInquiry(title, content, response.body().getFile_id());
                } else {
                    handleFailure("업로드 실패 (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NotNull Call<FileUploadResponse> call, @NotNull Throwable t) {
                handleFailure("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // [수정] 토큰 생성/로깅/전달 코드 모두 삭제
    private void finalWriteInquiry(String title, String content, Long fileId) {
        // 변수명을 요청하신 대로 user_Id로 변경했습니다.
        Long user_id = tokenManager.fetchUserId();

        // 생성자 파라미터에 변경된 user_Id를 전달합니다.
        InquiryWriteRequest request = new InquiryWriteRequest(user_id, title, content, fileId);

        // 인터셉터가 토큰을 자동으로 넣어주므로 request 객체만 보냅니다.
        apiService.writeInquiry(request).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(Call<InquiryResponse> call, Response<InquiryResponse> response) {
                resetSubmitUI();
                if (response.isSuccessful()) {
                    Toast.makeText(InquiryWriteActivity.this, "문의가 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    // 404가 계속 뜬다면 RetrofitClient의 BASE_URL 끝에 /가 있는지,
                    // ApiService의 경로 앞에 /가 중복되지 않았는지 확인해야 합니다.
                    Log.e(TAG, "등록 실패 코드: " + response.code());
                    Toast.makeText(InquiryWriteActivity.this, "등록 실패 (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<InquiryResponse> call, Throwable t) {
                handleFailure("네트워크 오류: " + t.getMessage());
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

            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) outputStream.write(buf, 0, len);
            outputStream.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
            return MultipartBody.Part.createFormData(partName, fileName, requestFile);
        } catch (Exception e) {
            Log.e(TAG, "파일 준비 실패: " + e.getMessage());
            return null;
        }
    }

    private void handleFailure(String message) {
        resetSubmitUI();
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void resetSubmitUI() {
        isSubmitting = false;
        btnSubmit.setEnabled(true);
        btnSubmit.setAlpha(1.0f);
    }
}