package com.mobility.hack.ride;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.R;

import java.text.NumberFormat;
import java.util.Locale;

public class PaymentSuccessActivity extends AppCompatActivity {

    public static final String EXTRA_CHARGED_AMOUNT = "com.mobility.hack.ride.EXTRA_CHARGED_AMOUNT";
    public static final String EXTRA_TOTAL_BALANCE = "com.mobility.hack.ride.EXTRA_TOTAL_BALANCE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        TextView chargedAmountText = findViewById(R.id.charged_amount_text);
        TextView totalBalanceText = findViewById(R.id.total_balance_text);
        Button okButton = findViewById(R.id.ok_button);

        long chargedAmount = getIntent().getLongExtra(EXTRA_CHARGED_AMOUNT, 0);
        long totalBalance = getIntent().getLongExtra(EXTRA_TOTAL_BALANCE, 0);

        String formattedChargedAmount = NumberFormat.getCurrencyInstance(Locale.KOREA).format(chargedAmount);
        String formattedTotalBalance = NumberFormat.getCurrencyInstance(Locale.KOREA).format(totalBalance);

        chargedAmountText.setText("충전된 금액: " + formattedChargedAmount);
        totalBalanceText.setText("현재 잔액: " + formattedTotalBalance);

        okButton.setOnClickListener(v -> {
            Intent mainIntent = new Intent(PaymentSuccessActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        });
    }
}
