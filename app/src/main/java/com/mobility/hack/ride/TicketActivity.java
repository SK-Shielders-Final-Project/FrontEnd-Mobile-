package com.example.mobilityhack.ride; // 패키지 이름 변경

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilityhack.R;
import com.example.mobilityhack.network.ApiService;
import com.example.mobilityhack.network.RetrofitClient;
import com.example.mobilityhack.network.dto.VoucherRequest;
import com.example.mobilityhack.network.dto.VoucherResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketActivity extends AppCompatActivity {

    private static final String TAG = "TicketActivity";
    private ApiService apiService;

    // SharedPreferences의 이름과 사용자 ID 키를 정의합니다.
    public static final String PREFS_NAME = "user_prefs";
    public static final String KEY_USER_ID = "user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        apiService = RetrofitClient.getApiService(this);

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
        // SharedPreferences에서 현재 로그인한 사용자 ID를 가져옵니다.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int userId = prefs.getInt(KEY_USER_ID, -1); // 로그인하지 않은 경우 기본값 -1

        if (userId == -1) {
            Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_LONG).show();
            // 여기서 로그인 화면으로 이동시키는 로직을 추가할 수 있습니다.
            return;
        }

        VoucherRequest request = new VoucherRequest(voucherCode, userId);

        apiService.redeemVoucher(request).enqueue(new Callback<VoucherResponse>() {
            @Override
            public void onResponse(Call<VoucherResponse> call, Response<VoucherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    VoucherResponse result = response.body();
                    if (result.isSuccess()) {
                        // 성공 케이스
                        VoucherResponse.SuccessData data = result.getData();
                        String successMessage = String.format(
                            "%s\n충전된 금액: %d\n현재 포인트: %d",
                            data.getMessage(),
                            data.getRechargedAmount(),
                            data.getTotalPoint()
                        );
                        Toast.makeText(TicketActivity.this, successMessage, Toast.LENGTH_LONG).show();
                    } else {
                        // 서버가 정의한 실패 케이스
                        Toast.makeText(TicketActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    // HTTP 에러
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
