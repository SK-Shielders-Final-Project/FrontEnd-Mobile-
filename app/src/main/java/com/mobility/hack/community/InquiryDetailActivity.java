package com.mobility.hack.community;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private static final String BASE_URL = "http://43.203.51.77:8080";

    private TextView tvTitle, tvContent, tvDate, tvFileName;
    private LinearLayout layoutAttachment;
    private InquiryResponse inquiry;
    private ApiService apiService;
    private TokenManager tokenManager;

    // [3] 수정 결과를 받기 위한 런처 등록
    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // 수정 성공 후 돌아오면 상세 데이터 다시 로드 (또는 인텐트에서 새 데이터 파싱)
                    refreshInquiryDetail();
                }
            }
    );

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

        inquiry = (InquiryResponse) getIntent().getSerializableExtra("inquiry_data");

        if (inquiry != null) {
            displayInquiryData();
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // [3] 수정 버튼 클릭 시 런처 실행
        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            if (inquiry != null) {
                Intent intent = new Intent(this, InquiryEditActivity.class);
                intent.putExtra("inquiry_data", inquiry);
                editLauncher.launch(intent);
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(v -> showDeleteDialog());
    }

    private void displayInquiryData() {
        tvTitle.setText(inquiry.getTitle());
        tvContent.setText(inquiry.getContent());
        tvDate.setText(inquiry.getCreatedAt());

        if (inquiry.getAttachment() != null && inquiry.getAttachment().getFileViewUri() != null) {
            layoutAttachment.setVisibility(View.VISIBLE);
            tvFileName.setText(inquiry.getAttachment().getOriginalFilename());
            layoutAttachment.setOnClickListener(v -> {
                String fileUrl = BASE_URL + inquiry.getAttachment().getFileViewUri();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
                startActivity(intent);
            });
        } else {
            layoutAttachment.setVisibility(View.GONE);
        }
    }

    /**
     * [3] 수정 완료 후 서버에서 최신 데이터 다시 가져오기
     */
    private void refreshInquiryDetail() {
        String token = "Bearer " + tokenManager.fetchAuthToken();
        apiService.getInquiryDetail(inquiry.getInquiryId()).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(Call<InquiryResponse> call, Response<InquiryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inquiry = response.body();
                    displayInquiryData(); // 화면 갱신
                    Toast.makeText(InquiryDetailActivity.this, "정보가 갱신되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<InquiryResponse> call, Throwable t) {
                Log.e(TAG, "Refresh failed: " + t.getMessage());
            }
        });
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
        if (inquiry == null) return;
        InquiryDeleteRequest request = new InquiryDeleteRequest(userId, inquiry.getInquiryId());
        apiService.deleteInquiry(token, request).enqueue(new Callback<CommonResultResponse>() {
            @Override public void onResponse(Call<CommonResultResponse> call, Response<CommonResultResponse> response) {
                if (response.isSuccessful() && response.body() != null && "Y".equals(response.body().getResult())) {
                    Toast.makeText(InquiryDetailActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override public void onFailure(Call<CommonResultResponse> call, Throwable t) {
                Toast.makeText(InquiryDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
