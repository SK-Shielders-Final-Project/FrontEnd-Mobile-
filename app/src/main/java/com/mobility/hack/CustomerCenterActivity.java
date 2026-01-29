package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CustomerCenterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_center);

        TextView menuInquiry = findViewById(R.id.menu_inquiry);
        TextView menuInquiryList = findViewById(R.id.menu_inquiry_list);

        menuInquiry.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerCenterActivity.this, InquiryWriteActivity.class);
            startActivity(intent);
        });

        menuInquiryList.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerCenterActivity.this, InquiryListActivity.class);
            startActivity(intent);
        });
    }
}
