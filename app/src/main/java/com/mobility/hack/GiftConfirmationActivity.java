package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.ride.MainActivity;

import java.text.DecimalFormat;

public class GiftConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gift_confirmation);

        TextView confirmationMessage = findViewById(R.id.tvConfirmationMessage);
        Button confirmButton = findViewById(R.id.btnConfirm);

        Intent intent = getIntent();
        String receiverName = intent.getStringExtra("RECEIVER_NAME");
        int amount = intent.getIntExtra("GIFT_AMOUNT", 0);

        DecimalFormat formatter = new DecimalFormat("###,###");
        String formattedAmount = formatter.format(amount);

        String message = String.format("%s님에게 %sP를 선물했습니다.", receiverName, formattedAmount);
        confirmationMessage.setText(message);

        confirmButton.setOnClickListener(v -> {
            Intent mainIntent = new Intent(GiftConfirmationActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        });
    }
}
