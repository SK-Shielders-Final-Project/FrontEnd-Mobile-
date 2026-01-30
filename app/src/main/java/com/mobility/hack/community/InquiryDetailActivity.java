package com.mobility.hack.community;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.InquiryDeleteRequest;
import com.mobility.hack.network.InquiryDeleteResponse; // 이 클래스 없으면 만드세요 (result 필드 1개)
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.network.InquiryDetailResponseDto; // 상세 조회용 DTO
import com.mobility.hack.security.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryDetailActivity extends AppCompatActivity {
    private static final String TAG = "InquiryDetail";
    // [중요] 포트 8081 확인
    private static final String BASE_URL = "http://43.203.51.77:8081";

    private TextView tvTitle, tvContent, tvDate, tvFileName;
    private LinearLayout layoutAttachment;
    private InquiryResponse inquiry; // 목록에서 넘어온 데이터
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
            displayInquiryData(inquiry);
            loadLatestDetail(); // 서버에서 최신 내용 다시 불러오기
        } else {
            Toast.makeText(this, "데이터 오류", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Intent intent = new Intent(this, InquiryEditActivity.class);
            intent.putExtra("inquiry_data", inquiry);
            startActivity(intent);
        });

        // 2. 삭제 버튼 연결
        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteDialog());
    }

    // [수정] 토큰 인자 제거 (Interceptor 사용)
    private void loadLatestDetail() {
        if (inquiry == null) return;

        // InquiryResponse에 getInquiryId()가 있어야 합니다. (없으면 getId()로 수정)
        long inquiryId = inquiry.getInquiryId();

        // [핵심] 토큰 없이 ID만 보냄
        apiService.getInquiryDetails(inquiryId).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(Call<InquiryResponse> call, Response<InquiryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 화면 갱신 (DTO 변환 필요할 수 있음)
                    InquiryResponse detail = response.body();
                    tvTitle.setText(detail.getTitle());
                    tvContent.setText(detail.getContent());
                    // ... 필요한 필드 갱신
                }
            }
            @Override
            public void onFailure(Call<InquiryResponse> call, Throwable t) {
                Log.e(TAG, "상세 조회 실패: " + t.getMessage());
            }
        });
    }

    private void displayInquiryData(InquiryResponse data) {
        tvTitle.setText(data.getTitle());
        tvContent.setText(data.getContent());
        tvDate.setText(data.getCreatedAt());

        if (data.getFileId() != null && data.getFileId() > 0) {
            layoutAttachment.setVisibility(View.VISIBLE);
            tvFileName.setText("첨부파일 다운로드");
            layoutAttachment.setOnClickListener(v -> {
                // 다운로드 URL도 포트 확인 필요
                String fileUrl = BASE_URL + "/api/files/download/" + data.getFileId();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
                startActivity(intent);
            });
        } else {
            layoutAttachment.setVisibility(View.GONE);
        }
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("문의 삭제")
                .setMessage("정말로 이 문의글을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> performDelete())
                .setNegativeButton("취소", null)
                .show();
    }

    // [수정] 토큰 제거, InquiryDeleteRequest 사용
    private void performDelete() {
        Long userId = tokenManager.fetchUserId();
        Long inquiryId = inquiry.getInquiryId();

        InquiryDeleteRequest request = new InquiryDeleteRequest(userId, inquiryId);

        // [핵심] request만 보냄 (Interceptor가 토큰 주입)
        apiService.deleteInquiry(request).enqueue(new Callback<InquiryDeleteResponse>() {
            @Override
            public void onResponse(Call<InquiryDeleteResponse> call, Response<InquiryDeleteResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InquiryDetailActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish(); // 목록으로 돌아가기
                } else {
                    // 404면 주소 문제, 403이면 본인 글 아님
                    Log.e(TAG, "삭제 실패: " + response.code());
                    Toast.makeText(InquiryDetailActivity.this, "삭제 실패 (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<InquiryDeleteResponse> call, Throwable t) {
                Toast.makeText(InquiryDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}