package com.mobility.hack.community;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.R;

public class InquiryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // [중요] Rebuild Project 후 이 코드는 com.mobility.hack.community 패키지의 
        // layout.activity_inquiry를 정확히 참조하게 됩니다.
        setContentView(R.layout.activity_inquiry);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // '대여 및 반납' 클릭 시 NoticeActivity로 이동
        TextView btnRentalReturn = findViewById(R.id.menu_rent_return);
        if (btnRentalReturn != null) {
            btnRentalReturn.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.mobility.hack.community.NoticeActivity.class);
                startActivity(intent);
            });
        }

        // '내가 문의한 내역' 클릭 시 InquiryListActivity로 이동
        TextView btnMyInquiries = findViewById(R.id.menu_my_inquiries);
        if (btnMyInquiries != null) {
            btnMyInquiries.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.mobility.hack.community.InquiryListActivity.class);
                startActivity(intent);
            });
        }
    }
}
