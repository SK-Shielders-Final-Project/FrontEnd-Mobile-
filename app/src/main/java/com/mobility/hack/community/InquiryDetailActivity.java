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
import com.mobility.hack.network.CommonResultResponse;
import com.mobility.hack.network.InquiryResponse; // 통일된 경로
import com.mobility.hack.security.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryDetailActivity extends AppCompatActivity {
    private static final String TAG = "InquiryDetail";
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

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        tvTitle = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);
        tvDate = findViewById(R.id.tv_detail_date);
        tvFileName = findViewById(R.id.tv_file_name);
        layoutAttachment = findViewById(R.id.layout_attachment);

        // 데이터 수신
        inquiry = (InquiryResponse) getIntent().getSerializableExtra("inquiry_data");

        if (inquiry != null) {
            displayInquiryData();
            loadLatestDetail();
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Intent intent = new Intent(this, InquiryEditActivity.class);
            intent.putExtra("inquiry_data", inquiry);
            startActivity(intent);
        });

        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteDialog());
    }

    private void loadLatestDetail() {
        String token = "Bearer " + tokenManager.fetchAuthToken();

        // 만약 InquiryResponse에 getInquiryId()가 없다면 getId()로 수정하세요.
        long inquiryId = inquiry.getInquiryId();

        apiService.getInquiryDetail(token, inquiryId).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(Call<InquiryResponse> call, Response<InquiryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inquiry = response.body();
                    displayInquiryData();
                }
            }

            @Override
            public void onFailure(Call<InquiryResponse> call, Throwable t) {
                Log.e(TAG, "상세 데이터 갱신 실패: " + t.getMessage());
            }
        });
    }

    private void displayInquiryData() {
        tvTitle.setText(inquiry.getTitle());
        tvContent.setText(inquiry.getContent());
        tvDate.setText(inquiry.getCreatedAt());

        if (inquiry.getFileId() != null) {
            layoutAttachment.setVisibility(View.VISIBLE);
            tvFileName.setText("첨부파일 다운로드");
            layoutAttachment.setOnClickListener(v -> {
                String fileUrl = BASE_URL + "/api/file/download/" + inquiry.getFileId();
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
        InquiryDeleteRequest request = new InquiryDeleteRequest(userId, inquiry.getInquiryId());

        apiService.deleteInquiry( request).enqueue(new Callback<CommonResultResponse>() {
            @Override
            public void onResponse(Call<CommonResultResponse> call, Response<CommonResultResponse> response) {
                if (response.isSuccessful() && response.body() != null && "Y".equals(response.body().getResult())) {
                    Toast.makeText(InquiryDetailActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(InquiryDetailActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CommonResultResponse> call, Throwable t) {
                Toast.makeText(InquiryDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}