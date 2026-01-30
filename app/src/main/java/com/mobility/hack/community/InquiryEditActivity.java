package com.mobility.hack.community;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.CommonResultResponse;
import com.mobility.hack.network.InquiryModifyRequest;
import com.mobility.hack.network.InquiryResponse;
import com.mobility.hack.security.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryEditActivity extends AppCompatActivity {
    private static final String TAG = "InquiryEdit";
    private EditText etTitle, etContent;
    private InquiryResponse inquiry;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_write);

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // [1] 레이아웃 텍스트 변경
        TextView tvHeaderTitle = findViewById(R.id.tv_header_title);
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("문의 수정");
        }

        TextView btnSubmit = findViewById(R.id.btn_submit);
        if (btnSubmit != null) {
            btnSubmit.setText("수정하기");
        }

        // [4] 체크박스 숨김 처리 (수정 시에는 불필요)
        View layoutAgree = findViewById(R.id.layout_agree);
        if (layoutAgree != null) {
            layoutAgree.setVisibility(View.GONE);
        }

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);

        inquiry = (InquiryResponse) getIntent().getSerializableExtra("inquiry_data");

        if (inquiry != null) {
            if (etTitle != null) etTitle.setText(inquiry.getTitle());
            if (etContent != null) etContent.setText(inquiry.getContent());
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> performUpdate());
        }
    }

    private void performUpdate() {
        if (inquiry == null) return;

        String token = "Bearer " + tokenManager.fetchAuthToken();
        long userId = tokenManager.fetchUserId();

        InquiryModifyRequest request = new InquiryModifyRequest(
                userId,
                inquiry.getInquiryId(),
                etTitle.getText().toString(),
                etContent.getText().toString(),
                0L
        );

        apiService.modifyInquiry(token, request).enqueue(new Callback<CommonResultResponse>() {
            @Override
            public void onResponse(Call<CommonResultResponse> call, Response<CommonResultResponse> response) {
                if (response.isSuccessful() && response.body() != null && "Y".equals(response.body().getResult())) {
                    Toast.makeText(InquiryEditActivity.this, "문의사항이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    // [2] 성공 결과 전달 후 화면 닫기
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(InquiryEditActivity.this, "수정에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CommonResultResponse> call, Throwable t) {
                Log.e(TAG, "Update failed: " + t.getMessage());
                Toast.makeText(InquiryEditActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
