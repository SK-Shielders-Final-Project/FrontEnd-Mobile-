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
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvContent, tvFilename;
    private LinearLayout layoutAttachment;
    private Button btnDownload;
    private InquiryResponse inquiry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_detail);

        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        tvFilename = findViewById(R.id.tv_filename);
        layoutAttachment = findViewById(R.id.layout_attachment);
        btnDownload = findViewById(R.id.btn_download);

        inquiry = (InquiryResponse) getIntent().getSerializableExtra("inquiry");

        if (inquiry != null) {
            tvTitle.setText(inquiry.getTitle());
            tvContent.setText(inquiry.getContent());

            if (inquiry.getStoredName() != null && !inquiry.getStoredName().isEmpty()) {
                layoutAttachment.setVisibility(View.VISIBLE);
                tvFilename.setText("첨부파일: " + inquiry.getStoredName());

                btnDownload.setOnClickListener(v -> {
                    // 핵심 취약점: stored_name을 검증 없이 그대로 파라미터로 사용
                    String vulnerableFilename = inquiry.getStoredName();
                    performVulnerableDownload(vulnerableFilename);
                });
            }
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void performVulnerableDownload(String filename) {
        String baseUrl = "http://your-server-address/api/"; // 실제 환경에 맞게 조정 필요
        String downloadUrl = baseUrl + "download.php?filename=" + filename;

        // 로그 및 시연용 출력
        Log.d("SecurityExploit", "[SecurityExploit] Path Traversal 시도: " + downloadUrl);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.downloadFile(filename).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InquiryDetailActivity.this, "파일 다운로드 요청 성공: " + filename, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(InquiryDetailActivity.this, "파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(InquiryDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}