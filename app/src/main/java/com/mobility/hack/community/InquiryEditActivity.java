package com.mobility.hack.community;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.FileUploadResponse;
import com.mobility.hack.network.InquiryModifyRequest;
import com.mobility.hack.network.InquiryModifyResponse;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.network.LinkPreviewResponse;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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

    // [미리보기] 컨테이너 및 캐시
    private LinearLayout previewContainer;
    private Map<String, LinkPreviewResponse> previewCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // [중요] 반드시 activity_inquiry_write를 써야 preview_container가 있습니다.
        setContentView(R.layout.activity_inquiry_write);

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // UI 설정
        TextView tvHeaderTitle = findViewById(R.id.tv_header_title);
        if (tvHeaderTitle != null) tvHeaderTitle.setText("문의 수정");

        btnSubmit = findViewById(R.id.btn_submit);
        if (btnSubmit != null) btnSubmit.setText("수정하기");

        View layoutAgree = findViewById(R.id.layout_agree);
        if (layoutAgree != null) layoutAgree.setVisibility(View.GONE);

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        ivAddImage = findViewById(R.id.iv_add_image);

        // [핵심] 미리보기 컨테이너 연결
        previewContainer = findViewById(R.id.preview_container);
        if (previewContainer == null) {
            Log.e("SSRF_TEST", "오류: preview_container를 찾을 수 없습니다! XML을 확인하세요.");
        }

        // 데이터 복원
        inquiry = (InquiryResponse) getIntent().getSerializableExtra("inquiry_data");
        if (inquiry != null) {
            if (etTitle != null) etTitle.setText(inquiry.getTitle());
            if (etContent != null) {
                String content = inquiry.getContent();
                etContent.setText(content);

                // [강화된 로직] 화면이 다 그려진 후(0.1초 뒤) 미리보기 실행
                // (XML 인플레이션 타이밍 문제 방지)
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d("SSRF_TEST", "초기 로딩 시작: " + content);
                    List<String> urls = extractAllUrls(content);
                    Log.d("SSRF_TEST", "발견된 URL 개수: " + urls.size());
                    updatePreviews(urls);
                    highlightLinks(etContent.getText());
                }, 100);
            }
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

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

        // 텍스트 감지 (엔터 키 입력 시 미리보기)
        if (etContent != null) {
            etContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    highlightLinks(s);
                    // 엔터(줄바꿈) 입력 시 동작
                    if (s.length() > 0 && s.charAt(s.length() - 1) == '\n') {
                        List<String> urls = extractAllUrls(s.toString());
                        updatePreviews(urls);
                    }
                }
            });
        }
    }

    // ==========================================================
    // [SSRF 로직]
    // ==========================================================

    private void highlightLinks(Editable s) {
        if (s == null) return;
        ForegroundColorSpan[] spans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            s.removeSpan(span);
        }
        Matcher matcher = Patterns.WEB_URL.matcher(s);
        while (matcher.find()) {
            s.setSpan(new ForegroundColorSpan(Color.BLUE), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private List<String> extractAllUrls(String input) {
        List<String> urls = new ArrayList<>();
        if (input == null) return urls;

        Matcher matcher = Patterns.WEB_URL.matcher(input);
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }

    private void updatePreviews(List<String> urls) {
        if (previewContainer == null) return;

        // 기존 뷰 제거 (중복 방지)
        previewContainer.removeAllViews();

        for (String url : urls) {
            String fullUrl = url.startsWith("http") ? url : "https://" + url;

            Log.d("SSRF_TEST", "미리보기 처리 중: " + fullUrl);

            if (previewCache.containsKey(fullUrl)) {
                addPreviewCardToLayout(fullUrl, previewCache.get(fullUrl));
            } else {
                requestServerSidePreview(fullUrl);
            }
        }
    }

    private void requestServerSidePreview(String url) {
        apiService.getLinkPreview(url).enqueue(new Callback<LinkPreviewResponse>() {
            @Override
            public void onResponse(Call<LinkPreviewResponse> call, Response<LinkPreviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    previewCache.put(url, response.body());
                    Log.d("SSRF_TEST", "서버 응답 성공: " + url);

                    // [수정] 텍스트 비교 로직 완화 (일단 무조건 추가 시도)
                    addPreviewCardToLayout(url, response.body());
                } else {
                    Log.e("SSRF_TEST", "서버 응답 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LinkPreviewResponse> call, Throwable t) {
                Log.e("SSRF_TEST", "통신 실패: " + t.getMessage());
            }
        });
    }

    private void addPreviewCardToLayout(String url, LinkPreviewResponse data) {
        if (previewContainer == null) return;

        // 중복 체크
        for(int i=0; i<previewContainer.getChildCount(); i++){
            if(url.equals(previewContainer.getChildAt(i).getTag())) return;
        }

        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setRadius(16f);
        card.setCardElevation(8f);
        card.setCardBackgroundColor(Color.parseColor("#F8F9FA"));
        card.setTag(url);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        card.addView(innerLayout);

        ImageView image = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 400);
        image.setLayoutParams(imgParams);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (data.getImage() != null && !data.getImage().isEmpty()) {
            Glide.with(this).load(data.getImage()).into(image);
        } else {
            image.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        innerLayout.addView(image);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(30, 30, 30, 30);
        innerLayout.addView(textLayout);

        TextView title = new TextView(this);
        title.setText(data.getTitle());
        title.setTextSize(16);
        title.setTextColor(Color.BLACK);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textLayout.addView(title);

        TextView desc = new TextView(this);
        desc.setText(data.getDescription());
        desc.setTextSize(14);
        desc.setTextColor(Color.GRAY);
        desc.setMaxLines(2);
        desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
        desc.setPadding(0, 8, 0, 0);
        textLayout.addView(desc);

        card.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(InquiryEditActivity.this, "링크 오류", Toast.LENGTH_SHORT).show();
            }
        });

        previewContainer.addView(card);
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    ivAddImage.setImageURI(selectedFileUri);
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

        if (selectedFileUri != null) {
            uploadFileThenModify(title, content);
        } else {
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
                    setResult(RESULT_OK);
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