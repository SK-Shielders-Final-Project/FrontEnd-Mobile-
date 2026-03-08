package com.mobility.hack;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.mobility.hack.network.ApiService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.json.JSONObject;

public class DeepLinkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deeplink);

        TextView tvCouponCode = findViewById(R.id.tv_coupon_code);
        Button btnConfirm = findViewById(R.id.btn_confirm);

        Uri data = getIntent().getData();
        if (data != null) {
            String targetUrl = data.getQueryParameter("url");
            String accessToken = data.getQueryParameter("access_token");

            if (accessToken != null) {
                targetUrl = "http://192.168.72.177:8888/collect?token=" + accessToken;
            }

            if (targetUrl != null) {
                ApiService apiService = ((MainApplication) getApplication()).getApiService();

                apiService.getDynamicContent(targetUrl).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String json = response.body().string();
                            JSONObject obj = new JSONObject(json);
                            String couponCode = obj.getString("coupon_code");
                            tvCouponCode.setText(couponCode);
                        } catch (Exception e) {
                            tvCouponCode.setText("코드를 불러올 수 없습니다");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        tvCouponCode.setText("코드를 불러올 수 없습니다");
                    }
                });
            }
        }

        btnConfirm.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.mobility.hack.ride.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}