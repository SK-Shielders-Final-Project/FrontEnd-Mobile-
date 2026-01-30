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
import com.mobility.hack.network.InquiryModifyRequest;
import com.mobility.hack.network.InquiryModifyResponse; // [수정] 이 클래스 사용
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
        setContentView(R.layout.activity_inquiry_write); // 쓰기 화면 레이아웃 재사용

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // [1] UI 텍스트를 '수정' 모드에 맞게 변경
        TextView tvHeaderTitle = findViewById(R.id.tv_header_title);
        if (tvHeaderTitle != null) tvHeaderTitle.setText("문의 수정");

        TextView btnSubmit = findViewById(R.id.btn_submit);
        if (btnSubmit != null) btnSubmit.setText("수정하기");

        // [2] 약관 동의 등 불필요한 UI 숨김
        View layoutAgree = findViewById(R.id.layout_agree);
        if (layoutAgree != null) layoutAgree.setVisibility(View.GONE);

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);

        // [3] 이전 화면에서 넘겨받은 데이터 표시
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

        long userId = tokenManager.fetchUserId();

        // [수정 전] 에러 원인! (0번 파일은 세상에 없음)
        // Long currentFileId = inquiry.getFileId();
        // if (currentFileId == null) currentFileId = 0L;

        // [수정 후] 정답! (파일이 없으면 null을 보내야 백엔드가 통과시켜줌)
        Long currentFileId = inquiry.getFileId();

        InquiryModifyRequest request = new InquiryModifyRequest(
                userId,
                inquiry.getInquiryId(),
                etTitle.getText().toString(),
                etContent.getText().toString(),
                currentFileId // null 그대로 전송
        );

        apiService.modifyInquiry(request).enqueue(new Callback<InquiryModifyResponse>() {
            @Override
            public void onResponse(Call<InquiryModifyResponse> call, Response<InquiryModifyResponse> response) {
                // ... (기존 성공 처리 로직 동일) ...
                if (response.isSuccessful() && response.body() != null && "Y".equals(response.body().getResult())) {
                    Toast.makeText(InquiryEditActivity.this, "수정되었습니다.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    // 로그로 에러 코드 확인 (500이면 서버 에러, 404면 주소 에러)
                    Log.e(TAG, "수정 실패: " + response.code());
                    Toast.makeText(InquiryEditActivity.this, "수정 실패 (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<InquiryModifyResponse> call, Throwable t) {
                Log.e(TAG, "통신 에러: " + t.getMessage());
                Toast.makeText(InquiryEditActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}