package com.example.mobilityhack.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilityhack.R;
import com.example.mobilityhack.network.ApiService;
import com.example.mobilityhack.network.RetrofitClient;
import com.example.mobilityhack.network.dto.VoucherRequest;
import com.example.mobilityhack.network.dto.VoucherResponse;
import com.example.mobilityhack.ride.UserHistoryActivity; // ride 패키지의 UserHistoryActivity를 import
import com.example.mobilityhack.util.Constants;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyInfoActivity extends AppCompatActivity {

    private static final String TAG = "MyInfoActivity";
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        apiService = RetrofitClient.getApiService(this);

        Button eventsButton = findViewById(R.id.events_button);
        Button registerCouponButton = findViewById(R.id.register_coupon_button);
        Button paymentHistoryButton = findViewById(R.id.payment_history_button);

        eventsButton.setOnClickListener(v -> {
            Toast.makeText(this, "이벤트 기능은 아직 준비 중입니다.", Toast.LENGTH_SHORT).show();
        });

        registerCouponButton.setOnClickListener(v -> {
            showVoucherInputDialog();
        });

        paymentHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void showVoucherInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("이용권 코드 입력");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("등록", (dialog, which) -> {
            String voucherCode = input.getText().toString().trim();
            if (voucherCode.isEmpty()) {
                Toast.makeText(this, "코드를 입력해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                redeemVoucherCode(voucherCode);
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void redeemVoucherCode(String voucherCode) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        int userId = prefs.getInt(Constants.KEY_USER_ID, -1);

        if (userId == -1) {
            Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_LONG).show();
            return;
        }

        VoucherRequest request = new VoucherRequest(voucherCode, userId);
        apiService.redeemVoucher(request).enqueue(new Callback<VoucherResponse>() {
            @Override
            public void onResponse(Call<VoucherResponse> call, Response<VoucherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    VoucherResponse result = response.body();
                    if (result.isSuccess()) {
                        VoucherResponse.SuccessData data = result.getData();
                        String successMessage = String.format(
                            "%s\n충전된 금액: %d\n현재 포인트: %d",
                            data.getMessage(), data.getRechargedAmount(), data.getTotalPoint()
                        );
                        Toast.makeText(MyInfoActivity.this, successMessage, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MyInfoActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = "이용권 등록 실패. 서버 응답 코드: " + response.code();
                    Toast.makeText(MyInfoActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<VoucherResponse> call, Throwable t) {
                String errorMsg = "이용권 등록 중 네트워크 오류가 발생했습니다.";
                Toast.makeText(MyInfoActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Network Failure: ", t);
            }
        });
    }
}
