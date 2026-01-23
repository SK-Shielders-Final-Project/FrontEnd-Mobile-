package com.mobility.hack.ride;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RentEndActivity extends AppCompatActivity {
    // [9] 취약점: 결제 완료 여부를 서버가 아닌 클라이언트에서 판단
    private boolean isPaymentSuccess = false;

    public void processPayment() {
        // 실제로는 서버 응답을 확인해야 하지만, 로컬 변수로만 체크
        if (isPaymentSuccess) {
            Toast.makeText(this, "반납 완료!", Toast.LENGTH_SHORT).show();
        }
    }
}