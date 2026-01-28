package com.mobility.hack.community;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.dto.InquiryResponse;
import com.mobility.hack.util.TokenManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvContent, tvFilename;
    private LinearLayout layoutAttachment;
    private Button btnDownload;
    private InquiryResponse inquiry;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_detail);

        // MainApplication에서 관리되는 ApiService와 TokenManager 인스턴스를 가져옵니다.
        MainApplication application = (MainApplication) getApplication();
        apiService = application.getApiService();
        tokenManager = application.getTokenManager();

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
                    String filename = inquiry.getStoredName();
                    downloadFile(filename);
                });
            }
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void downloadFile(String filename) {
        String token = tokenManager.fetchAuthToken();
        if (token == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.downloadFile("Bearer " + token, filename).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InquiryDetailActivity.this, "파일 다운로드 요청 성공: " + filename, Toast.LENGTH_SHORT).show();
                    // TODO: 파일 저장 로직 구현
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
