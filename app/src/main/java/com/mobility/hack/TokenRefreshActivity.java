package com.mobility.hack;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.network.ApiClient;
import com.mobility.hack.network.AuthApiService;
import com.mobility.hack.network.request.RefreshTokenRequest;
import com.mobility.hack.network.response.AccessTokenResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TokenRefreshActivity extends AppCompatActivity {

    private static final String TAG = "TokenRefreshActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 

        // activity_main.xml에 실제로 존재하는 ID인 R.id.btnRefresh로 수정했습니다.
        View refreshTokenButton = findViewById(R.id.btnRefresh);
        refreshTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentRefreshToken = "dasfddsfgtfdssfd";
                refreshAccessToken(currentRefreshToken);
            }
        });
    }

    private void refreshAccessToken(String refreshToken) {
        AuthApiService apiService = ApiClient.getClient().create(AuthApiService.class);
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        apiService.refreshAccessToken(request).enqueue(new Callback<AccessTokenResponse>() {
            @Override
            public void onResponse(Call<AccessTokenResponse> call, Response<AccessTokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String newAccessToken = response.body().getAccessToken();
                    Log.d(TAG, "✅ 새 액세스 토큰 발급 성공: " + newAccessToken);
                    Toast.makeText(getApplicationContext(), "새 토큰: " + newAccessToken, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "❌ 토큰 재발급 실패: " + response.code());
                    Toast.makeText(getApplicationContext(), "토큰 재발급에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AccessTokenResponse> call, Throwable t) {
                Log.e(TAG, "❌ 통신 실패: " + t.getMessage());
                Toast.makeText(getApplicationContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
