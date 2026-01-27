package com.mobility.hack.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.R;

public class CustomerCenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_center);

        // '문의하기' 클릭 시 InquiryActivity로 이동
        findViewById(R.id.menu_inquiry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerCenterActivity.this, InquiryActivity.class);
                startActivity(intent);
            }
        });

        // 뒤로가기 버튼
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}