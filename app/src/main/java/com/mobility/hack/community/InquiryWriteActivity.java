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

        // 사진이 있으면 업로드 먼저, 없으면 바로 등록
        if (selectedFileUri != null) {
            uploadFileThenSubmit(title, content);
        } else {
            finalWriteInquiry(title, content, null);
        }
    }

    private void uploadFileThenSubmit(String title, String content) {
        String token = "Bearer " + tokenManager.fetchAuthToken();

        // [M8] 웹팀 명세 준수: 파트 이름 "file"
        MultipartBody.Part filePart = prepareFilePart("file", selectedFileUri);

        if (filePart == null) {
            handleFailure("파일 준비 실패");
            return;
        }

        apiService.uploadFile(token, filePart).enqueue(new Callback<FileUploadResponse>() {
            @Override
            public void onResponse(@NotNull Call<FileUploadResponse> call, @NotNull Response<FileUploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // [M8] 웹팀 응답 규격 준수: file_id 추출
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

    private void finalWriteInquiry(String title, String content, Long fileId) {
        String token = "Bearer " + tokenManager.fetchAuthToken();
        long userId = tokenManager.fetchUserId();

        // [M8] 웹팀 요청 규격 준수: user_id, file_id 매핑
        InquiryWriteRequest request = new InquiryWriteRequest(userId, title, content, fileId);

        apiService.writeInquiry(token, String.valueOf(userId), request).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(@NotNull Call<InquiryResponse> call, @NotNull Response<InquiryResponse> response) {
                resetSubmitUI();
                if (response.isSuccessful()) {
                    Toast.makeText(InquiryWriteActivity.this, "문의가 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    // [M8] 500 에러 발생 시 로그 확인 필요
                    handleFailure("글 등록 실패 (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NotNull Call<InquiryResponse> call, @NotNull Throwable t) {
                handleFailure("등록 네트워크 오류: " + t.getMessage());
            }
        });
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        try {
            // [1] 실제 파일의 MimeType 추출
            String mimeType = getContentResolver().getType(fileUri);
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

            // [2] 백엔드는 소문자 확장자를 비교하므로 소문자로 통일
            if (extension == null) extension = "jpg";
            else extension = extension.toLowerCase(Locale.ROOT);

            // [3] 핵심: 서버의 extractExt() 로직을 위해 마침표(.)가 포함된 파일명 생성
            // 이 이름이 서버의 originalName으로 전달됩니다.
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

            // [4] 명세서 규격에 따라 파트 이름을 "file"로 설정
            return MultipartBody.Part.createFormData(partName, fileName, requestFile);
        } catch (Exception e) {
            Log.e("InquiryWrite", "파일 준비 실패: " + e.getMessage());
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