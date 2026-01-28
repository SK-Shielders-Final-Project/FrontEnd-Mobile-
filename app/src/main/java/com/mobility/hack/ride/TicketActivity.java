package com.mobility.hack.ride;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.network.dto.VoucherRequest;
import com.mobility.hack.network.dto.VoucherResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketActivity extends AppCompatActivity {

    private static final String TAG = "TicketActivity";
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        apiService = RetrofitClient.getApiService(((MainApplication) getApplication()).getTokenManager());

        findViewById(R.id.show_coupon_dialog_button).setOnClickListener(v -> {
            showVoucherInputDialog();
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
        VoucherRequest request = new VoucherRequest(voucherCode);
        Log.d(TAG, "redeemVoucherCode: " + voucherCode);
        apiService.redeemVoucher(request).enqueue(new Callback<VoucherResponse>() {
            @Override
            public void onResponse(Call<VoucherResponse> call, Response<VoucherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    VoucherResponse data = response.body();
                    Log.d(TAG, "onResponse: success, recharged: " + data.getRechargedPoint() + ", total: " + data.getTotalPoint());
                    String successMessage = String.format(
                        "쿠폰이 성공적으로 사용되었습니다.\n충전된 금액: %d\n현재 포인트: %d",
                        data.getRechargedPoint(),
                        data.getTotalPoint()
                    );
                    Toast.makeText(TicketActivity.this, successMessage, Toast.LENGTH_LONG).show();
                } else {
                    String errorBody = "";
                    if(response.errorBody() != null) {
                        try {
                            errorBody = response.errorBody().string();
                        } catch (java.io.IOException e) {
                            Log.e(TAG, "Error reading error body: " + e.getMessage());
                        }
                    }
                    Log.e(TAG, "onResponse: failed, code: " + response.code() + " body: " + errorBody);
                    String errorMsg = "이용권 등록 실패. 서버 응답 코드: " + response.code();
                    Toast.makeText(TicketActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<VoucherResponse> call, Throwable t) {
                // 네트워크 요청 실패
                String errorMsg = "이용권 등록 중 네트워크 오류가 발생했습니다.";
                Toast.makeText(TicketActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Network Failure: ", t);
            }
        });
    }
}
