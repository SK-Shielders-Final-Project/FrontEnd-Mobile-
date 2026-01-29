package com.mobility.hack.ride;

import android.content.Context;

import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.BikeResponse;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileCacher {

    private ApiService apiService;

    public FileCacher(Context context) {
        TokenManager tokenManager =
                new TokenManager(context.getApplicationContext());

        this.apiService = RetrofitClient
                .getClient(tokenManager)
                .create(ApiService.class);
    }

    public interface BikeCallback {
        void onSuccess(List<BikeResponse> bikeList);
        void onFailure(String errorMessage);
    }

    public void getBikeList(BikeCallback callback) {

        apiService.getBikes().enqueue(new Callback<List<BikeResponse>>() {

            @Override
            public void onResponse(Call<List<BikeResponse>> call,
                                   Response<List<BikeResponse>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else if (response.code() == 401) {
                    // 토큰 만료 / 인증 실패는 여기서 처리
                    callback.onFailure("인증이 만료되었습니다. 다시 로그인해주세요.");
                } else {
                    callback.onFailure("서버 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<BikeResponse>> call, Throwable t) {
                callback.onFailure("네트워크 오류: " + t.getMessage());
            }
        });
    }
}
