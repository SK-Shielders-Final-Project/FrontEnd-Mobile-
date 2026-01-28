package com.mobility.hack.community;

import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.auth.BaseActivity;
import com.mobility.hack.network.InquiryRequest;
import com.mobility.hack.network.InquiryResponse;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InquiryWriteActivity extends BaseActivity {
    private static final String TAG = "InquiryWriteActivity";
    private EditText etTitle, etContent;
    private CheckBox cbAgree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry_write);

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        cbAgree = findViewById(R.id.cb_agree);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // [문의하기] 버튼 클릭 리스너
        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            // 1. 약관 동의 체크 여부 확인
            if (cbAgree != null && !cbAgree.isChecked()) {
                Toast.makeText(this, "동의가 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. 서버로 전송
            sendInquiryToServer(title, content);
        });
    }

    /**
     * 명세서(POST /api/user/inquiry/write) 연동 로직
     */
    private void sendInquiryToServer(String title, String content) {
        // [보안] 세션에서 현재 사용자 ID 가져오기
        long userId = 0L;
        if (MainApplication.getTokenManager() != null) {
            userId = MainApplication.getTokenManager().fetchUserId();
        }

        if (userId == 0L) {
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Request 객체 생성 (file_id는 실습용 기본값 1 전달)
        InquiryRequest request = new InquiryRequest(userId, title, content, 1);

        if (apiService == null) return;

        apiService.writeInquiry(request).enqueue(new Callback<InquiryResponse>() {
            @Override
            public void onResponse(@NotNull Call<InquiryResponse> call, @NotNull Response<InquiryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "문의 등록 성공! ID: " + response.body().getInquiryId());
                    Toast.makeText(InquiryWriteActivity.this, "문의가 접수되었습니다.", Toast.LENGTH_SHORT).show();
                    finish(); // 등록 후 화면 종료
                } else {
                    Log.e(TAG, "문의 등록 실패: " + response.code());
                    Toast.makeText(InquiryWriteActivity.this, "등록에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<InquiryResponse> call, @NotNull Throwable t) {
                Log.e(TAG, "네트워크 오류: " + t.getMessage());
                Toast.makeText(InquiryWriteActivity.this, "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
