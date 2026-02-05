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
import com.mobility.hack.security.TokenManager;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PurchaseTicketActivity extends AppCompatActivity {

    public static final String EXTRA_BIKE_ID = "BIKE_ID"; // "bikeId" -> "BIKE_ID"로 수정

    private MaterialButtonToggleGroup timeSelectionGroup;
    private TextView totalAmountText;
    private TextView planDetailsText;
    private int selectedAmount = 1000;
    private int selectedHours = 1;
    private String bikeIdString; // QR에서 받아온 바이크 ID (문자열)
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_ticket);

        // 인텐트에서 바이크 ID 가져오기
        bikeIdString = getIntent().getStringExtra(EXTRA_BIKE_ID);
        if (bikeIdString == null || bikeIdString.isEmpty()) {
            Toast.makeText(this, "유효하지 않은 자전거 번호입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 뷰 초기화
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        timeSelectionGroup = findViewById(R.id.time_selection_group);
        totalAmountText = findViewById(R.id.total_amount_text);
        planDetailsText = findViewById(R.id.plan_details_text);
        MaterialButton nextButton = findViewById(R.id.next_button);

        // MainApplication에서 ApiService 및 TokenManager 인스턴스 가져오기
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // 툴바 뒤로가기 설정
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

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

        // '다음' 버튼 리스너 (포인트 차감 요청)
        nextButton.setOnClickListener(v -> {
            int bikeId;
            try {
                // "SN-2026-001"과 같은 형식에서 숫자만 추출
                bikeId = Integer.parseInt(bikeIdString.replaceAll("[^0-9]", ""));
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
                        // 성공 처리
                        int remainingBalance = pointResponse.getCurrentPoint();

                        // 1. SharedPreferences에서 기존 대여 정보 가져오기
                        SharedPreferences prefs = getSharedPreferences("RentalPrefs", MODE_PRIVATE);
                        long existingDuration = prefs.getLong("rental_duration", 0); // 기존에 저장된 총 대여 시간
                        long rentalStartTime = prefs.getLong("rental_start_time", 0); // 기존 대여 시작 시간

                        // 2. 새로 추가할 대여 시간 계산
                        long additionalDurationInMillis = (long) selectedHours * 60 * 60 * 1000;

                        // 3. 최종 대여 정보 결정
                        long newTotalDuration = existingDuration + additionalDurationInMillis; // 기존 시간에 새 시간 더하기
                        if (rentalStartTime == 0) { // 만약 첫 대여라면 (기존 시작 시간이 없다면), 시작 시간을 지금으로 기록
                            rentalStartTime = System.currentTimeMillis();
                        }

                        // 4. 새로운 총 대여 시간과 시작 시간을 SharedPreferences에 저장
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong("rental_duration", newTotalDuration);
                        editor.putLong("rental_start_time", rentalStartTime);
                        editor.apply();

                        Log.d("PurchaseTicketActivity", "시간 연장/구매 완료 - 새로운 총 시간(ms): " + newTotalDuration);

                        Toast.makeText(PurchaseTicketActivity.this, "포인트가 성공적으로 사용되었습니다.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(PurchaseTicketActivity.this, PurchaseSuccessActivity.class);
                        intent.putExtra(PurchaseSuccessActivity.EXTRA_REMAINING_BALANCE, remainingBalance);
                        startActivity(intent);
                        finish();
                    } else {
                        // 서버 에러 처리
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
                    // 네트워크 통신 실패 처리
                    Log.e("PurchaseTicketActivity", "API call failed", t);
                    Toast.makeText(PurchaseTicketActivity.this, "서버와 통신에 실패했습니다. 네트워크 연결을 확인해주세요.", Toast.LENGTH_LONG).show();
                }
            });
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
