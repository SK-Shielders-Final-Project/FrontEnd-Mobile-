package com.mobility.hack.network;

import com.mobility.hack.security.AuthInterceptor;
import com.mobility.hack.security.TokenManager;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // [명세 반영] 백엔드 서버 IP 주소 고정
    private static final String BASE_URL = "http://43.203.51.77:8080/";

    public static Retrofit getClient(TokenManager tokenManager) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenManager))
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static ApiService getApiService(TokenManager tokenManager) {
        return getClient(tokenManager).create(ApiService.class);
    }
}
