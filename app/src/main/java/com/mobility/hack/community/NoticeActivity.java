package com.mobility.hack.community;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.R;
import java.io.File;

public class NoticeActivity extends AppCompatActivity {
    private static final String TAG = "SecurityExploit";
    private static final String BASE_PATH = "/data/user/0/com.mobility.hack/files/notices/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        // 뒤로가기 버튼
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 우측 상단 작성 버튼 클릭 시 InquiryWriteActivity로 이동
        View btnWrite = findViewById(R.id.btn_write);
        if (btnWrite != null) {
            btnWrite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(NoticeActivity.this, InquiryWriteActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    /**
     * 리스트 항목 클릭 시 호출 (XML에서 android:onClick="onNoticeClick" 설정)
     */
    public void onNoticeClick(View view) {
        // 실습을 위해 기본 파일명을 전달합니다. 해커는 이 파라미터를 가로채 조작합니다.
        downloadNotice("notice_v1.pdf");
    }

    private void downloadNotice(String fileName) {
        File file = new File(BASE_PATH + fileName);
        Log.d(TAG, "[Download] Requested: " + fileName);
        Log.d(TAG, "[Download] Path: " + file.getAbsolutePath());
        
        if (file.exists()) {
            Toast.makeText(this, "파일 다운로드 중...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "파일을 찾을 수 없습니다. (Path Traversal 실습 포인트)", Toast.LENGTH_SHORT).show();
        }
    }
}