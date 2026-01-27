package com.mobility.hack.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.R;

public class InquiryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry);

        // 상단 뒤로가기 버튼
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // '대여 및 반납' 클릭 시 공지사항(Path Traversal 실습) 화면으로 이동
        TextView tvRentReturn = findViewById(R.id.menu_rent_return);
        tvRentReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InquiryActivity.this, NoticeActivity.class);
                startActivity(intent);
            }
        });
        
        // 다른 항목들도 필요에 따라 추가적인 클릭 리스너를 달 수 있습니다.
    }
}