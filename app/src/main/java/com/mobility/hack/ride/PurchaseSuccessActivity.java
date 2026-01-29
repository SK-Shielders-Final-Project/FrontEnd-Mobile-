package com.mobility.hack.ride;

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
            finish();
        });
    }
}
