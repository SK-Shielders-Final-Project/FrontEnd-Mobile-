package com.mobility.hack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.PointGiftRequest;
import com.mobility.hack.network.PointGiftResponse;
import com.mobility.hack.network.UserInfoResponse;
import com.mobility.hack.security.CryptoManager;
import com.mobility.hack.security.TokenManager;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PointGiftActivity extends AppCompatActivity {

    private static final String TAG = "PointGiftActivity";

    private EditText receiverNameEditText;
    private EditText amountEditText;
    private Button giftButton;
    private TextView currentPointsTextView;

    private ApiService apiService;
    private TokenManager tokenManager;
    private CryptoManager cryptoManager;
    private int currentUserPoint = 0;
    private String currentUserName = ""; // 사용자 이름을 저장할 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_gift);

        // MainApplication에서 인스턴스 가져오기
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();
        cryptoManager = new CryptoManager(tokenManager);

        receiverNameEditText = findViewById(R.id.etReceiver);
        amountEditText = findViewById(R.id.etPoint);
        giftButton = findViewById(R.id.btnSend);
        currentPointsTextView = findViewById(R.id.tv_current_points);

        // 초기에 버튼을 비활성화
        giftButton.setEnabled(false);

        fetchUserInfo();

        giftButton.setOnClickListener(v -> {
            String receiverName = receiverNameEditText.getText().toString();
            if (receiverName.isEmpty()) {
                Toast.makeText(this, "받는 사람을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amountEditText.getText().toString().isEmpty()) {
                Toast.makeText(this, "포인트를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            int amount = Integer.parseInt(amountEditText.getText().toString());

            if (amount > currentUserPoint) {
                Toast.makeText(this, "보유 포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    JSONObject giftPayload = new JSONObject();
                    giftPayload.put("receiverName", receiverName);
                    giftPayload.put("amount", amount);
                    giftPayload.put("senderName", currentUserName);

                    String encryptedPayload = cryptoManager.encrypt(giftPayload.toString());
                    PointGiftRequest request = new PointGiftRequest(encryptedPayload);
                    giftPoint(request, receiverName, amount);

                } catch (Exception e) {
                    Log.e(TAG, "Encryption failed", e);
                    Toast.makeText(this, "암호화 실패", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchUserInfo() {
        long userId = tokenManager.fetchUserId();
        if (userId == 0L) {
            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다. 다시 로그인 해주세요.", Toast.LENGTH_SHORT).show();
            currentPointsTextView.setText("보유 포인트: 로딩 실패");
            return;
        }

        apiService.getUserInfo().enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserInfoResponse userInfo = response.body();
                    currentUserPoint = userInfo.getTotalPoint();
                    currentUserName = userInfo.getName(); // 사용자 이름 저장

                    DecimalFormat formatter = new DecimalFormat("###,###");
                    String formattedPoint = formatter.format(currentUserPoint);
                    currentPointsTextView.setText("보유 포인트: " + formattedPoint + "P");
                    giftButton.setEnabled(true);
                } else {
                    currentPointsTextView.setText("보유 포인트: 로딩 실패");
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                currentPointsTextView.setText("보유 포인트: 네트워크 오류");
                Toast.makeText(PointGiftActivity.this, "사용자 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void giftPoint(PointGiftRequest request, final String receiverName, final int amount) {
        giftButton.setEnabled(false); // 중복 클릭 방지
        apiService.giftPoint(request).enqueue(new Callback<PointGiftResponse>() {
            @Override
            public void onResponse(Call<PointGiftResponse> call, Response<PointGiftResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String encryptedResult = response.body().getEncryptedResult();
                        String decryptedResult = cryptoManager.decrypt(encryptedResult);
                        JSONObject resultJson = new JSONObject(decryptedResult);

                        // 성공 시, GiftConfirmationActivity로 이동
                        Intent intent = new Intent(PointGiftActivity.this, GiftConfirmationActivity.class);
                        intent.putExtra("RECEIVER_NAME", receiverName);
                        intent.putExtra("GIFT_AMOUNT", amount);
                        startActivity(intent);
                        finish();

                    } catch (Exception e) {
                        Log.e(TAG, "Decryption or JSON parsing failed", e);
                        Toast.makeText(PointGiftActivity.this, "응답 처리 실패", Toast.LENGTH_SHORT).show();
                        giftButton.setEnabled(true);
                    }

                } else {
                    String errorBody = "내용 없음";
                    if (response.errorBody() != null) {
                        try {
                            errorBody = response.errorBody().string();
                        } catch (IOException e) {
                            Log.e(TAG, "errorBody 읽기 실패", e);
                        }
                    }
                    Log.e(TAG, "포인트 선물 실패: 코드=" + response.code() + ", 메시지=" + response.message() + ", 에러=" + errorBody);
                    Toast.makeText(PointGiftActivity.this, "포인트 선물에 실패했습니다. (로그 확인)", Toast.LENGTH_SHORT).show();
                    giftButton.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<PointGiftResponse> call, Throwable t) {
                Log.e(TAG, "포인트 선물 네트워크 오류", t);
                Toast.makeText(PointGiftActivity.this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                giftButton.setEnabled(true);
            }
        });
    }
}