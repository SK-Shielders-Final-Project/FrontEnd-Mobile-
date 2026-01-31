package com.mobility.hack.ride;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.network.VoucherRequest;
import com.mobility.hack.network.VoucherResponse;
import com.mobility.hack.security.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketActivity extends AppCompatActivity {

    private static final String TAG = "TicketActivity";
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TokenManager tokenManager = new TokenManager(this);
        apiService = RetrofitClient.getApiService(tokenManager);

        showVoucherInputDialog();
    }

    private void showVoucherInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("이용권 코드 입력");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // Add a container with padding
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("등록", (dialog, which) -> {
            String voucherCode = input.getText().toString().trim();
            if (voucherCode.isEmpty()) {
                Toast.makeText(getApplicationContext(), "코드를 입력해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                redeemVoucherCode(voucherCode);
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> finish());
        dialog.show();
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
                    Toast.makeText(getApplicationContext(), successMessage, Toast.LENGTH_LONG).show();
                } else {
                    String errorMsg = "이용권 등록에 실패했습니다."; // 기본 오류 메시지
                    if (response.errorBody() != null) {
                        try {
                            String errorBodyString = response.errorBody().string();
                            Log.e(TAG, "onResponse: failed, code: " + response.code() + " body: " + errorBodyString);
                            // 오류 응답 본문을 VoucherResponse 형식으로 파싱하여 서버 메시지 추출
                            VoucherResponse errorResponse = new Gson().fromJson(errorBodyString, VoucherResponse.class);
                            if (errorResponse != null && errorResponse.getMessage() != null && !errorResponse.getMessage().isEmpty()) {
                                errorMsg = errorResponse.getMessage(); // 서버가 보낸 오류 메시지 사용 (예: "중복된 코드입니다")
                            }
                        } catch (java.io.IOException | JsonSyntaxException e) {
                            Log.e(TAG, "Error parsing error body: " + e.getMessage());
                            errorMsg = "이용권 등록 실패 (응답 코드: " + response.code() + ")";
                        }
                    }
                    Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<VoucherResponse> call, Throwable t) {
                // 네트워크 요청 실패
                String errorMsg = "이용권 등록 중 네트워크 오류가 발생했습니다.";
                Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Network Failure: ", t);
            }
        });
    }
}
