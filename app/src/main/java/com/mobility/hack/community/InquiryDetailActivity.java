package com.mobility.hack.community;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.network.RetrofitClient;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryDetailActivity extends AppCompatActivity {
    private static final String TAG = "InquiryDetail";
    
    private TextView tvTitle, tvContent, tvDate, tvFilename;
    private LinearLayout layoutAttachment;
    private Button btnDownload;
    private Long inquiryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_detail);

        // [에러 해결] XML ID와 매칭 확인 (activity_inquiry_detail.xml)
        tvTitle = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);
        tvDate = findViewById(R.id.tv_detail_date);
        tvFilename = findViewById(R.id.tv_filename);
        layoutAttachment = findViewById(R.id.layout_attachment);
        btnDownload = findViewById(R.id.btn_download);

        // Intent로부터 ID 전달받음
        inquiryId = getIntent().getLongExtra("inquiry_id", -1L);

        if (inquiryId != -1L) {
            fetchInquiryDetail(inquiryId);
        } else {
            Toast.makeText(this, "유효하지 않은 요청입니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void fetchInquiryDetail(Long id) {
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        // ApiService에 정의된 getInquiryDetail 호출
        apiService.getInquiryDetail(id).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(@NotNull Call<InquiryResponse> call, @NotNull Response<InquiryResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    displayData(response.body());
                } else {
                    Log.e(TAG, "상세 조회 실패: " + response.code());
                    Toast.makeText(InquiryDetailActivity.this, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<InquiryResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "서버 연결 오류: " + t.getMessage());
                Toast.makeText(InquiryDetailActivity.this, "서버 연결 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayData(InquiryResponse data) {
        // [에러 해결] InquiryResponse에 정의된 Getter 사용
        tvTitle.setText(data.getTitle());
        tvContent.setText(data.getContent());
        tvDate.setText(data.getCreatedAt());

        if (data.getFileId() != null) {
            layoutAttachment.setVisibility(View.VISIBLE);
            tvFilename.setText("첨부파일: ID_" + data.getFileId());
            btnDownload.setOnClickListener(v -> {
                Toast.makeText(this, "보안 실습용 파일 다운로드 시도 중...", Toast.LENGTH_SHORT).show();
            });
        } else {
            layoutAttachment.setVisibility(View.GONE);
        }
    }
}
