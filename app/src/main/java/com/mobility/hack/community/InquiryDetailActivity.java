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
import com.mobility.hack.network.CommonResultResponse;
import com.mobility.hack.network.InquiryDeleteRequest;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.security.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryDetailActivity extends AppCompatActivity {
    private static final String TAG = "InquiryDetail";
    // 스웨거에 명시된 백엔드 서버 주소
    private static final String BASE_URL = "http://43.203.51.77:8080";

    private TextView tvTitle, tvContent, tvDate, tvFileName;
    private LinearLayout layoutAttachment;
    private InquiryResponse inquiry;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_detail);

        // 서비스 및 매니저 초기화
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // 뷰 연결
        tvTitle = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);
        tvDate = findViewById(R.id.tv_detail_date);
        tvFileName = findViewById(R.id.tv_file_name);
        layoutAttachment = findViewById(R.id.layout_attachment);

        // 목록에서 넘어온 초기 데이터 받기
        inquiry = (InquiryResponse) getIntent().getSerializableExtra("inquiry_data");

        if (inquiry != null) {
            // 1. 우선 전달받은 데이터로 화면을 그립니다.
            displayInquiryData();
            // 2. 서버에서 최신 상세 데이터를 다시 불러옵니다 (컴파일 에러 해결 지점).
            loadLatestDetail();
        }

        // 뒤로가기
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 수정 버튼 (PUT 방식 수정을 위해 데이터 전달)
        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Intent intent = new Intent(this, InquiryEditActivity.class);
            intent.putExtra("inquiry_data", inquiry);
            startActivity(intent);
        });

        // 삭제 버튼
        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteDialog());
    }

    /**
     * [컴파일 에러 해결] 서버에서 상세 내역을 다시 가져오는 로직
     */
    private void loadLatestDetail() {
        String token = "Bearer " + tokenManager.fetchAuthToken();
        long inquiryId = inquiry.getInquiryId();

        // ApiService의 getInquiryDetail(String, long) 규격에 맞게 호출
        apiService.getInquiryDetail(token, inquiryId).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(Call<InquiryResponse> call, Response<InquiryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inquiry = response.body();
                    displayInquiryData(); // 서버에서 받아온 최신 데이터(첨부파일 포함)로 갱신
                } else {
                    Log.e(TAG, "상세 로드 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<InquiryResponse> call, Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
            }
        });
    }

    private void displayInquiryData() {
        tvTitle.setText(inquiry.getTitle());
        tvContent.setText(inquiry.getContent());
        tvDate.setText(inquiry.getCreatedAt());

        // 첨부파일 처리 (스웨거 명세 기준)
        if (inquiry.getAttachment() != null && inquiry.getAttachment().getFileViewUri() != null) {
            layoutAttachment.setVisibility(View.VISIBLE);

            String fileName = inquiry.getAttachment().getOriginalFilename();
            tvFileName.setText(fileName != null ? fileName : "첨부파일 보기");

            // 클릭 시 브라우저를 통해 파일 다운로드/보기 실행
            layoutAttachment.setOnClickListener(v -> {
                String fileUrl = BASE_URL + inquiry.getAttachment().getFileViewUri();
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

    private void performDelete() {
        String token = "Bearer " + tokenManager.fetchAuthToken();
        long userId = tokenManager.fetchUserId();

        // 스웨거 명세: POST /api/user/inquiry/delete
        InquiryDeleteRequest request = new InquiryDeleteRequest(userId, inquiry.getInquiryId());
        apiService.deleteInquiry(token, request).enqueue(new Callback<CommonResultResponse>() {
            @Override
            public void onResponse(Call<CommonResultResponse> call, Response<CommonResultResponse> response) {
                if (response.isSuccessful() && response.body() != null && "Y".equals(response.body().getResult())) {
                    Toast.makeText(InquiryDetailActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<CommonResultResponse> call, Throwable t) {
                Toast.makeText(InquiryDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}