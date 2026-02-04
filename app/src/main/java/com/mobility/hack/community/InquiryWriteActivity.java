package com.mobility.hack.community;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
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
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.network.InquiryWriteRequest;
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

    // [다중 미리보기] 컨테이너 및 캐시
    private LinearLayout previewContainer;
    private Map<String, LinkPreviewResponse> previewCache = new HashMap<>();

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
        previewContainer = findViewById(R.id.preview_container);

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

        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 1. 파란색 링크 처리
                highlightLinks(s);

                // 2. "줄바꿈(Enter)"이 발생했을 때만 미리보기 로딩!
                if (s.length() > 0 && s.charAt(s.length() - 1) == '\n') {
                    List<String> urls = extractAllUrls(s.toString());
                    updatePreviews(urls);
                }
            }
        });
    } // <--- onCreate가 여기서 닫혀야 합니다!

    // ==========================================================
    // 여기서부터는 onCreate 밖입니다.
    // ==========================================================

    private void highlightLinks(Editable s) {
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
        Matcher matcher = Patterns.WEB_URL.matcher(input);
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }

    private void updatePreviews(List<String> urls) {
        previewContainer.removeAllViews();

        for (String url : urls) {
            String fullUrl = url.startsWith("http") ? url : "https://" + url;

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
                    if (etContent.getText().toString().contains(url.replace("https://", "").replace("http://", ""))) {
                        addPreviewCardToLayout(url, response.body());
                    }
                }
            }

            @Override
            public void onFailure(Call<LinkPreviewResponse> call, Throwable t) {
                Log.e("SSRF", "Fail: " + t.getMessage());
            }
        });
    }

    private void addPreviewCardToLayout(String url, LinkPreviewResponse data) {
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
                Toast.makeText(InquiryWriteActivity.this, "링크 오류", Toast.LENGTH_SHORT).show();
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

    private void uploadFileThenSubmit(String title, String content) {
        MultipartBody.Part filePart = prepareFilePart("file", selectedFileUri);

        if (filePart == null) {
            handleFailure("파일 준비 실패");
            return;
        }

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

    private void finalWriteInquiry(String title, String content, Long fileId) {
        Long user_Id = tokenManager.fetchUserId();
        InquiryWriteRequest request = new InquiryWriteRequest(user_Id, title, content, fileId);

        apiService.writeInquiry(request).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(Call<InquiryResponse> call, Response<InquiryResponse> response) {
                resetSubmitUI();

                if (response.isSuccessful()) {
                    Toast.makeText(InquiryWriteActivity.this, "등록 성공!", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        Toast.makeText(InquiryWriteActivity.this, "실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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