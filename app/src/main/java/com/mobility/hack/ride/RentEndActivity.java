package com.mobility.hack.ride;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.R;

public class RentEndActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rent_end);

        Button requestPaymentButton = findViewById(R.id.request_payment_button);
        requestPaymentButton.setOnClickListener(v -> {
            Intent intent = new Intent(RentEndActivity.this, PaymentActivity.class);
            // 실제 결제 금액과 주문명은 이 곳에서 설정해야 합니다.
            intent.putExtra(PaymentActivity.EXTRA_AMOUNT, 1000); // 예: 1000원
            intent.putExtra(PaymentActivity.EXTRA_ORDER_NAME, "포인트 충전");
            startActivity(intent);
        });
    }
}
