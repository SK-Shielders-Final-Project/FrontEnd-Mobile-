package com.mobility.hack.ride;

import androidx.appcompat.app.AppCompatActivity;

public class RentEndActivity extends AppCompatActivity {
    // [임시 조치] 빌드 오류로 인해 결제 관련 코드를 모두 주석 처리합니다.5
    /*
    public static final String EXTRA_AMOUNT = "com.mobility.hack.ride.AMOUNT";
    private static final String TAG = "RentEndActivity";

    private com.tosspayments.paymentsdk.view.TossPaymentView paymentView;
    private int paymentAmount;
    private com.mobility.hack.network.ApiService apiService;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.mobility.hack.R.layout.activity_rent_end);

        apiService = com.mobility.hack.network.RetrofitClient.getApiService();

        if (!getIntent().hasExtra(EXTRA_AMOUNT)) {
            android.widget.Toast.makeText(this, "결제 금액 정보가 없어 결제를 진행할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        paymentAmount = getIntent().getIntExtra(EXTRA_AMOUNT, 0);
        if (paymentAmount <= 0) {
            android.widget.Toast.makeText(this, "결제 금액은 0보다 커야 합니다.", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        paymentView = findViewById(com.mobility.hack.R.id.payment_view);

        findViewById(com.mobility.hack.R.id.request_payment_button).setOnClickListener(v -> {
            String orderId = java.util.UUID.randomUUID().toString();
            paymentView.requestPayment(
                    "카드",
                    orderId,
                    "테스트 결제",
                    paymentAmount,
                    com.mobility.hack.BuildConfig.TOSS_CLIENT_KEY
            );
        });

        paymentView.setCallback(new com.tosspayments.paymentsdk.view.TossPaymentView.TossPaymentCallback() {
            @Override
            public void onPaymentSuccess(com.tosspayments.paymentsdk.model.TossPaymentResult.Success success) {
                String paymentKey = success.getPaymentKey();
                String orderId = success.getOrderId();
                long amount = success.getAmount();

                if (paymentAmount != amount) {
                    android.widget.Toast.makeText(RentEndActivity.this, "결제 금액이 일치하지 않아 오류가 발생했습니다.", android.widget.Toast.LENGTH_LONG).show();
                    return;
                }

                confirmPaymentToServer(paymentKey, orderId, (int) amount);
            }

            @Override
            public void onPaymentFailed(com.tosspayments.paymentsdk.model.TossPaymentResult.Fail fail) {
                android.widget.Toast.makeText(RentEndActivity.this, "결제 실패: " + fail.getErrorMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmPaymentToServer(String paymentKey, String orderId, int amount) {
        // ... (내부 구현 생략) ...
    }
    */
}
