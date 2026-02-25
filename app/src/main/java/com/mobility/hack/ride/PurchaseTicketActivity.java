package com.mobility.hack.ride;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.PointRequest;
import com.mobility.hack.network.PointResponse;
import com.mobility.hack.network.RentalRequest; // 추가
import com.mobility.hack.security.TokenManager;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PurchaseTicketActivity extends AppCompatActivity {

    public static final String EXTRA_BIKE_ID = "BIKE_ID";

    private MaterialButtonToggleGroup timeSelectionGroup;
    private TextView totalAmountText;
    private TextView planDetailsText;
    private int selectedAmount = 1000;
    private int selectedHours = 1;
    private String bikeIdString;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_ticket);

        bikeIdString = getIntent().getStringExtra(EXTRA_BIKE_ID);
        if (bikeIdString == null || bikeIdString.isEmpty()) {
            Toast.makeText(this, "유효하지 않은 자전거 번호입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        timeSelectionGroup = findViewById(R.id.time_selection_group);
        totalAmountText = findViewById(R.id.total_amount_text);
        planDetailsText = findViewById(R.id.plan_details_text);
        MaterialButton nextButton = findViewById(R.id.next_button);
        TextView dailyPassTab = findViewById(R.id.daily_pass_tab);

        dailyPassTab.setText("선택된 기기: " + bikeIdString);

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        timeSelectionGroup.check(R.id.hour_1_button);
        updatePaymentSummary(1);

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

        nextButton.setOnClickListener(v -> startRentalProcess()); // 변경
    }

    private void startRentalProcess() {
        RentalRequest rentalRequest = new RentalRequest(bikeIdString);
        apiService.startBikeRental(rentalRequest).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    proceedWithPointUsage();
                } else {
                    String errorMessage = "대여 시작에 실패했습니다.";
                    if (response.errorBody() != null) {
                        try {
                            String errorBodyString = response.errorBody().string();
                            Log.e("PurchaseTicketActivity", "Rental API Error: " + errorBodyString);
                            errorMessage += " (서버 오류: " + response.code() + ")";
                        } catch (IOException e) {
                            Log.e("PurchaseTicketActivity", "Error parsing error body", e);
                        }
                    }
                    Toast.makeText(PurchaseTicketActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("PurchaseTicketActivity", "Rental API call failed", t);
                Toast.makeText(PurchaseTicketActivity.this, "서버와 통신에 실패했습니다.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void proceedWithPointUsage() {
        int bikeId;
        try {
            String[] parts = bikeIdString.split("-");
            if (parts.length == 3) {
                bikeId = Integer.parseInt(parts[2]);
            } else {
                Toast.makeText(this, "자전거 번호 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "자전거 번호 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        PointRequest request = new PointRequest(selectedHours, bikeId);

        apiService.usePoint(request).enqueue(new Callback<PointResponse>() {
            @Override
            public void onResponse(Call<PointResponse> call, Response<PointResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PointResponse pointResponse = response.body();
                    int remainingBalance = pointResponse.getCurrentPoint();

                    SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
                    long existingDuration = prefs.getLong("rental_duration", 0);
                    long rentalStartTime = prefs.getLong("rental_start_time", 0);

                    long additionalDurationInMillis = (long) selectedHours * 60 * 60 * 1000;

                    long newTotalDuration = existingDuration + additionalDurationInMillis;
                    if (rentalStartTime == 0) {
                        rentalStartTime = System.currentTimeMillis();
                    }

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong("rental_duration", newTotalDuration);
                    editor.putLong("rental_start_time", rentalStartTime);
                    editor.putBoolean("isRenting", true);
                    editor.putString("BIKE_ID", bikeIdString); // bikeId 저장
                    editor.apply();

                    Log.d("PurchaseTicketActivity", "시간 연장/구매 완료 - 새로운 총 시간(ms): " + newTotalDuration);

                    Toast.makeText(PurchaseTicketActivity.this, "포인트가 성공적으로 사용되었습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PurchaseTicketActivity.this, PurchaseSuccessActivity.class);
                    intent.putExtra(PurchaseSuccessActivity.EXTRA_REMAINING_BALANCE, remainingBalance);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "포인트 사용에 실패했습니다.";
                    if (response.errorBody() != null) {
                        try {
                            String errorBodyString = response.errorBody().string();
                            Log.e("PurchaseTicketActivity", "API Error: " + errorBodyString);
                            errorMessage += " (서버 오류: " + response.code() + ")";
                        } catch (IOException e) {
                            Log.e("PurchaseTicketActivity", "Error parsing error body", e);
                        }
                    }
                    Toast.makeText(PurchaseTicketActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PointResponse> call, Throwable t) {
                Log.e("PurchaseTicketActivity", "API call failed", t);
                Toast.makeText(PurchaseTicketActivity.this, "서버와 통신에 실패했습니다. 네트워크 연결을 확인해주세요.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updatePaymentSummary(int hours) {
        selectedHours = hours;
        switch (hours) {
            case 2:
                selectedAmount = 2000;
                break;
            case 3:
                selectedAmount = 3000;
                break;
            default:
                selectedAmount = 1000;
                break;
        }

        String formattedAmount = NumberFormat.getCurrencyInstance(Locale.KOREA).format(selectedAmount);

        totalAmountText.setText(formattedAmount);
        planDetailsText.setText(String.format(Locale.getDefault(), "일일권(%d시간)", selectedHours));
    }
}