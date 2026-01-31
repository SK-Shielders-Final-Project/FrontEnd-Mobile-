package com.mobility.hack.community;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
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
import com.mobility.hack.network.InquiryModifyRequest;
import com.mobility.hack.network.InquiryModifyResponse;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.security.TokenManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryEditActivity extends AppCompatActivity {
    private static final String TAG = "InquiryEdit";
    private ApiService apiService;
    private TokenManager tokenManager;
    private EditText etTitle, etContent;
    private ImageView ivAddImage;
    private TextView btnSubmit;
    private InquiryResponse inquiry;
    private Uri selectedFileUri;
    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_write);

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // UI 텍스트 변경
        TextView tvHeaderTitle = findViewById(R.id.tv_header_title);
        if (tvHeaderTitle != null) tvHeaderTitle.setText("문의 수정");

        btnSubmit = findViewById(R.id.btn_submit);
        if (btnSubmit != null) btnSubmit.setText("수정하기");

        View layoutAgree = findViewById(R.id.layout_agree);
        if (layoutAgree != null) layoutAgree.setVisibility(View.GONE);

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        ivAddImage = findViewById(R.id.iv_add_image);

        // 이전 상세 페이지에서 데이터 받기
        inquiry = (InquiryResponse) getIntent().getSerializableExtra("inquiry_data");
        if (inquiry != null) {
            if (etTitle != null) etTitle.setText(inquiry.getTitle());
            if (etContent != null) etContent.setText(inquiry.getContent());
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // [핵심] 갤러리 연동: 이미지 클릭 시 사진 선택
        if (ivAddImage != null) {
            ivAddImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                galleryLauncher.launch(intent);
            });
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (!isSubmitting) startUpdateProcess();
            });
        }
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    ivAddImage.setImageURI(selectedFileUri); // 미리보기 적용
                    Toast.makeText(this, "사진이 선택되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void startUpdateProcess() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        isSubmitting = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setAlpha(0.5f);

        // 사진을 새로 선택했다면 업로드 먼저 수행
        if (selectedFileUri != null) {
            uploadFileThenModify(title, content);
        } else {
            // 사진이 없으면 기존 file_id 유지
            performFinalModify(title, content, inquiry != null ? inquiry.getFileId() : null);
        }
    }

    private void uploadFileThenModify(String title, String content) {
        MultipartBody.Part filePart = prepareFilePart("file", selectedFileUri);
        if (filePart == null) {
            handleFailure("파일 준비 실패");
            return;
        }

        apiService.uploadFile(filePart).enqueue(new Callback<FileUploadResponse>() {
            @Override
            public void onResponse(@NotNull Call<FileUploadResponse> call, @NotNull Response<FileUploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 1단계 성공: 새로운 file_id로 2단계 수정 진행
                    performFinalModify(title, content, response.body().getFile_id());
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

    private void performFinalModify(String title, String content, Long fileId) {
        long userId = tokenManager.fetchUserId();
        InquiryModifyRequest request = new InquiryModifyRequest(
                userId,
                inquiry.getInquiryId(),
                title,
                content,
                fileId
        );

        apiService.modifyInquiry(request).enqueue(new Callback<InquiryModifyResponse>() {
            @Override
            public void onResponse(@NotNull Call<InquiryModifyResponse> call, @NotNull Response<InquiryModifyResponse> response) {
                isSubmitting = false;
                btnSubmit.setEnabled(true);
                btnSubmit.setAlpha(1.0f);

                if (response.isSuccessful() && response.body() != null && "Y".equals(response.body().getResult())) {
                    Toast.makeText(InquiryEditActivity.this, "문의사항이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // 상세 페이지 새로고침 트리거
                    finish();
                } else {
                    Toast.makeText(InquiryEditActivity.this, "수정 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<InquiryModifyResponse> call, @NotNull Throwable t) {
                handleFailure("통신 오류: " + t.getMessage());
            }
        });
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        try {
            String mimeType = getContentResolver().getType(fileUri);
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            String fileName = "edit_" + System.currentTimeMillis() + "." + (extension != null ? extension : "jpg");
            
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
            return null;
        }
    }

    private void handleFailure(String message) {
        isSubmitting = false;
        btnSubmit.setEnabled(true);
        btnSubmit.setAlpha(1.0f);
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
