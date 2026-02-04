package com.mobility.hack.community;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.InquiryDeleteRequest;
import com.mobility.hack.network.InquiryDeleteResponse;
import com.mobility.hack.network.InquiryDetailResponseDto;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.network.LinkPreviewResponse;
import com.mobility.hack.security.TokenManager;

import java.util.regex.Matcher;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryDetailActivity extends AppCompatActivity {
    private static final String TAG = "InquiryDetail";
    private static final int REQUEST_CODE_EDIT = 100;

    private TextView tvTitle, tvContent, tvDate, tvFileName;
    private LinearLayout layoutAttachment;
    // [추가] 미리보기 카드를 담을 컨테이너
    private LinearLayout detailPreviewContainer;

    private InquiryResponse inquiry;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_detail);

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        tvTitle = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);
        tvDate = findViewById(R.id.tv_detail_date);
        tvFileName = findViewById(R.id.tv_file_name);
        layoutAttachment = findViewById(R.id.layout_attachment);

        // [추가] XML에 추가한 미리보기 컨테이너 연결
        detailPreviewContainer = findViewById(R.id.detail_preview_container);

        // 1. 목록 화면에서 넘겨준 데이터 받기
        inquiry = (InquiryResponse) getIntent().getSerializableExtra("inquiry_data");

        if (inquiry != null) {
            tvTitle.setText(inquiry.getTitle());
            tvContent.setText(inquiry.getContent());
            tvDate.setText(inquiry.getCreatedAt());

            // 초기 데이터에도 URL이 있을 수 있으므로 미리보기 로딩 시도
            loadUrlPreviews(inquiry.getContent());

            // 최신 데이터 갱신
            loadLatestDetail();
        } else {
            Toast.makeText(this, "데이터 오류", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 수정 버튼
        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Intent intent = new Intent(this, InquiryEditActivity.class);
            intent.putExtra("inquiry_data", inquiry);
            startActivityForResult(intent, REQUEST_CODE_EDIT);
        });

        // 삭제 버튼
        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteDialog());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT && resultCode == RESULT_OK) {
            loadLatestDetail();
            Toast.makeText(this, "내용이 갱신되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLatestDetail() {
        if (inquiry == null) return;

        apiService.getInquiryDetails(inquiry.getInquiryId()).enqueue(new Callback<InquiryDetailResponseDto>() {
            @Override
            public void onResponse(Call<InquiryDetailResponseDto> call, Response<InquiryDetailResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayDetailData(response.body());
                } else {
                    Log.e(TAG, "상세 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<InquiryDetailResponseDto> call, Throwable t) {
                Log.e(TAG, "통신 오류", t);
            }
        });
    }

    private void displayDetailData(InquiryDetailResponseDto data) {
        tvTitle.setText(data.getTitle());
        tvContent.setText(data.getContent());
        tvDate.setText(data.getCreatedAt());

        // [추가] 갱신된 내용에서 URL 파싱 및 미리보기 생성 (SSRF 트리거)
        loadUrlPreviews(data.getContent());

        if (data.getAttachment() != null) {
            layoutAttachment.setVisibility(View.VISIBLE);

            String fileName = data.getAttachment().getOriginalName();
            tvFileName.setText(fileName != null ? fileName : "첨부파일 다운로드");

            layoutAttachment.setOnClickListener(v -> {
                String downloadUrl = data.getDownloadUrl();
                if (downloadUrl != null) {
                    downloadFileInternally(downloadUrl, fileName);
                } else {
                    Toast.makeText(this, "다운로드 경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            layoutAttachment.setVisibility(View.GONE);
        }
    }

    // =========================================================
    // [SSRF 시연용] URL 미리보기 기능 추가
    // =========================================================

    // 내용에서 모든 URL 추출 및 미리보기 요청
    private void loadUrlPreviews(String content) {
        if (content == null || detailPreviewContainer == null) return;

        // 컨테이너 초기화
        detailPreviewContainer.removeAllViews();

        // URL 정규식으로 추출
        Matcher matcher = Patterns.WEB_URL.matcher(content);
        while (matcher.find()) {
            String url = matcher.group();
            // SSRF 취약점 트리거 (서버 API 호출)
            requestServerSidePreview(url);
        }
    }

    // 서버에 미리보기 요청 (SSRF 발생 지점)
    private void requestServerSidePreview(String url) {
        String fullUrl = url.startsWith("http") ? url : "https://" + url;

        apiService.getLinkPreview(fullUrl).enqueue(new Callback<LinkPreviewResponse>() {
            @Override
            public void onResponse(Call<LinkPreviewResponse> call, Response<LinkPreviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // UI 스레드에서 카드 추가
                    addPreviewCard(fullUrl, response.body());
                }
            }

            @Override
            public void onFailure(Call<LinkPreviewResponse> call, Throwable t) {
                // 실패 시 조용히 넘어감 (로그만 남김)
                Log.e("DetailSSRF", "Fail: " + t.getMessage());
            }
        });
    }

    // 미리보기 카드 UI 생성
    private void addPreviewCard(String url, LinkPreviewResponse data) {
        // 중복 방지
        for(int i=0; i<detailPreviewContainer.getChildCount(); i++){
            if(url.equals(detailPreviewContainer.getChildAt(i).getTag())) return;
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

        // 이미지
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

        // 텍스트 영역
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(30, 30, 30, 30);
        innerLayout.addView(textLayout);

        // 제목
        TextView title = new TextView(this);
        title.setText(data.getTitle());
        title.setTextSize(16);
        title.setTextColor(Color.BLACK);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textLayout.addView(title);

        // 설명
        TextView desc = new TextView(this);
        desc.setText(data.getDescription());
        desc.setTextSize(14);
        desc.setTextColor(Color.GRAY);
        desc.setMaxLines(2);
        desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
        desc.setPadding(0, 8, 0, 0);
        textLayout.addView(desc);

        // 클릭 시 이동
        card.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(InquiryDetailActivity.this, "링크 오류", Toast.LENGTH_SHORT).show();
            }
        });

        detailPreviewContainer.addView(card);
    }

    // =========================================================

    private void downloadFileInternally(String fileUrl, String fileName) {
        try {
            Toast.makeText(this, "다운로드를 시작합니다...", Toast.LENGTH_SHORT).show();

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle(fileName);
            request.setDescription("파일을 다운로드 중입니다.");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            String accessToken = tokenManager.fetchAuthToken();
            if (accessToken != null) {
                request.addRequestHeader("Authorization", "Bearer " + accessToken);
            }

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
            }
        } catch (Exception e) {
            Log.e(TAG, "다운로드 시작 실패", e);
            Toast.makeText(this, "다운로드 관리자 실행 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("문의 삭제")
                .setMessage("정말로 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> performDelete())
                .setNegativeButton("취소", null)
                .show();
    }

    private void performDelete() {
        Long userId = tokenManager.fetchUserId();
        InquiryDeleteRequest request = new InquiryDeleteRequest(userId, inquiry.getInquiryId());

        apiService.deleteInquiry(request).enqueue(new Callback<InquiryDeleteResponse>() {
            @Override
            public void onResponse(Call<InquiryDeleteResponse> call, Response<InquiryDeleteResponse> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(InquiryDetailActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(InquiryDetailActivity.this, "삭제 실패 (" + r.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<InquiryDeleteResponse> call, Throwable t) {
                Toast.makeText(InquiryDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}