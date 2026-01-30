package com.mobility.hack.ride;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.mobility.hack.R;

import java.text.NumberFormat;
import java.util.Locale;

public class PurchaseTicketActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup timeSelectionGroup;
    private TextView totalAmountText;
    private TextView planDetailsText;
    private int selectedAmount = 1000;
    private int selectedHours = 1;

    public static final String EXTRA_AMOUNT = "com.mobility.hack.ride.EXTRA_AMOUNT";
    public static final String EXTRA_ORDER_NAME = "com.mobility.hack.ride.EXTRA_ORDER_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_ticket);

        // 뷰 초기화
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        timeSelectionGroup = findViewById(R.id.time_selection_group);
        totalAmountText = findViewById(R.id.total_amount_text);
        planDetailsText = findViewById(R.id.plan_details_text);
        MaterialButton nextButton = findViewById(R.id.next_button);

        // 툴바 뒤로가기 설정
        toolbar.setNavigationOnClickListener(v -> finish());

        // 기본 선택 (1시간)
        timeSelectionGroup.check(R.id.hour_1_button);
        updatePaymentSummary(1);

        // 시간 선택 그룹 리스너
        timeSelectionGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.hour_1_button) {
                    updatePaymentSummary(1);
                } else if (checkedId == R.id.hour_2_button) {
                    updatePaymentSummary(2);
                } else if (checkedId == R.id.hour_3_button) {
                    updatePaymentSummary(3);
                }
            }
        });

        // '다음' 버튼 리스너 (결제 화면으로 이동)
        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra(EXTRA_AMOUNT, selectedAmount);
            intent.putExtra(EXTRA_ORDER_NAME, String.format(Locale.getDefault(), "일일권(%d시간)", selectedHours));
            startActivity(intent);
        });
    }

    // 결제 요약 정보 업데이트
    private void updatePaymentSummary(int hours) {
        selectedHours = hours;
        switch (hours) {
            case 2:
                selectedAmount = 2000; 
                break;
            case 3:
                selectedAmount = 3000;
                break;
            default: // 1시간
                selectedAmount = 1000;
                break;
        }

        String formattedAmount = NumberFormat.getCurrencyInstance(Locale.KOREA).format(selectedAmount);

        totalAmountText.setText(formattedAmount);
        planDetailsText.setText(String.format(Locale.getDefault(), "일일권(%d시간)", selectedHours));
    }
}
