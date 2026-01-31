package com.mobility.hack.community;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.InquiryDeleteRequest;
import com.mobility.hack.network.InquiryDeleteResponse;
import com.mobility.hack.network.InquiryDetailResponseDto;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.security.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryDetailActivity extends AppCompatActivity {
    private static final String TAG = "InquiryDetail";
    private static final int REQUEST_CODE_EDIT = 100;

    private TextView tvTitle, tvContent, tvDate, tvFileName;
    private LinearLayout layoutAttachment;
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

        // 1. 목록 화면에서 넘겨준 데이터 받기
        inquiry = (InquiryResponse) getIntent().getSerializableExtra("inquiry_data");

        if (inquiry != null) {
            tvTitle.setText(inquiry.getTitle());
            tvContent.setText(inquiry.getContent());
            tvDate.setText(inquiry.getCreatedAt());

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

            // [수정] TokenManager에 있는 정확한 메서드명(fetchAuthToken) 사용
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