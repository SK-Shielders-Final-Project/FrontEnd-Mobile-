package com.mobility.hack.ride;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.mobility.hack.R;

public class PointChargeActivity extends AppCompatActivity {

    private int selectedAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_charge);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        MaterialButtonToggleGroup amountSelectionGroup = findViewById(R.id.amount_selection_group);
        TextInputEditText customAmountInput = findViewById(R.id.custom_amount_input);
        MaterialButton chargeButton = findViewById(R.id.charge_button);

        toolbar.setNavigationOnClickListener(v -> finish());

        amountSelectionGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                customAmountInput.setText("");
                if (checkedId == R.id.amount_1000_button) {
                    selectedAmount = 1000;
                } else if (checkedId == R.id.amount_5000_button) {
                    selectedAmount = 5000;
                } else if (checkedId == R.id.amount_10000_button) {
                    selectedAmount = 10000;
                }
            } else {
                selectedAmount = 0;
            }
        });

        customAmountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    amountSelectionGroup.clearChecked();
                    try {
                        selectedAmount = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) {
                        selectedAmount = 0;
                    }
                } else {
                    selectedAmount = 0;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chargeButton.setOnClickListener(v -> {
            if (selectedAmount > 0) {
                Toast.makeText(this, selectedAmount + "원 충전을 요청합니다.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, PaymentActivity.class);
                intent.putExtra(PaymentActivity.EXTRA_AMOUNT, selectedAmount);
                intent.putExtra(PaymentActivity.EXTRA_ORDER_NAME, "포인트 충전");
                startActivity(intent);
            } else {
                Toast.makeText(this, "충전할 금액을 선택하거나 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
