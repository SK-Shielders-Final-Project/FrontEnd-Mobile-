package com.mobility.hack.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.R;

public class CustomerCenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_center);

        // 상단 뒤로가기 버튼
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // [1] '문의하기' 클릭 시 InquiryActivity로 이동
        TextView btnGoInquiry = findViewById(R.id.menu_inquiry);
        if (btnGoInquiry != null) {
            btnGoInquiry.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerCenterActivity.this, InquiryActivity.class);
                startActivity(intent);
            });
        }

        // 신고내역보기 클릭 시 InquiryListActivity로 이동
        TextView btnGoInquiryList = findViewById(R.id.menu_inquiry_list);
        if (btnGoInquiryList != null) {
            btnGoInquiryList.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerCenterActivity.this, InquiryListActivity.class);
                startActivity(intent);
            });
        }
    }
}
