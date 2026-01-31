package com.mobility.hack.ride;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.R;

import java.text.NumberFormat;
import java.util.Locale;

public class PurchaseSuccessActivity extends AppCompatActivity {

    public static final String EXTRA_REMAINING_BALANCE = "com.mobility.hack.ride.EXTRA_REMAINING_BALANCE";
    private static final String TAG = "PurchaseSuccessActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: starting");
        setContentView(R.layout.activity_purchase_success);
        Log.d(TAG, "onCreate: setContentView finished");

        TextView remainingBalanceText = findViewById(R.id.remaining_balance_text);
        Button okButton = findViewById(R.id.ok_button);

        int remainingBalance = getIntent().getIntExtra(EXTRA_REMAINING_BALANCE, 0);
        String formattedBalance = NumberFormat.getCurrencyInstance(Locale.KOREA).format(remainingBalance);
        remainingBalanceText.setText("남은 잔액: " + formattedBalance);
        Log.d(TAG, "onCreate: UI updated with balance: " + formattedBalance);

        okButton.setOnClickListener(v -> {
            Log.d(TAG, "OK button clicked. Saving rental status and returning to MainActivity.");

            // 1. "대여 중" 상태를 SharedPreferences에 저장합니다.
            // "RentalPrefs"라는 이름의 저장소를 열고, "isRenting" 키에 true 값을 저장합니다.
            android.content.SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isRenting", true);
            editor.apply(); // 비동기적으로 저장

            // 2. MainActivity로 돌아가는 '목적지 티켓'(Intent)을 생성합니다.
            Intent mainIntent = new Intent(PurchaseSuccessActivity.this, MainActivity.class);

            // 3. 중간에 거쳐온 모든 다른 화면들을 메모리에서 지우고 MainActivity를 맨 위로 가져옵니다.
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            // 4. MainActivity를 시작합니다.
            startActivity(mainIntent);

            // 5. 현재 화면(PurchaseSuccessActivity)을 종료합니다.
            finish();
        });
    }
}
