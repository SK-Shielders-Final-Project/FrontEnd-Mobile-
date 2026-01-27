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

        // '내가 문의한 내역' 클릭 시 신고내역보기(InquiryListActivity) 화면으로 이동
        findViewById(R.id.menu_my_inquiries).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InquiryActivity.this, InquiryListActivity.class);
                startActivity(intent);
            }
        });
    }
}